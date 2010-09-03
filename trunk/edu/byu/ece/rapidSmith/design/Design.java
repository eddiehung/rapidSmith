/*
 * Copyright (c) 2010 Brigham Young University
 * 
 * This file is part of the BYU RapidSmith Tools.
 * 
 * BYU RapidSmith Tools is free software: you may redistribute it 
 * and/or modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation, either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * BYU RapidSmith Tools is distributed in the hope that it will be 
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * A copy of the GNU General Public License is included with the BYU 
 * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also 
 * get a copy of the license at <http://www.gnu.org/licenses/>.
 * 
 */
package edu.byu.ece.rapidSmith.design;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import edu.byu.ece.rapidSmith.design.parser.DesignParser;
import edu.byu.ece.rapidSmith.design.parser.ParseException;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.PrimitiveType;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * The design class houses an entire XDL design or hard macro.  It keeps
 * track of all of its member instances, nets, modules and attributes
 * and can load/import save/export XDL files.  When an XDL design is loaded 
 * into this class it also populates the Device and WireEnumerator classes
 * that correspond to the part this design targets.
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class Design implements Serializable{
	
	private static final long serialVersionUID = 6586577338969915167L;
	
	/** Name of the design */
	private String name;
	/** All of the attributes in the design */
	private ArrayList<Attribute> attributes;
	/** This is the Xilinx part, package and speed grade that this design targets */
	private String partName;
	/** XDL is typically generated from NCD, this is the version of that NCD file (v3.2 is typical)*/
	private String NCDVersion; 
	/** This is the list of modules or macros in the design */
	private HashMap<String,Module> modules;
	/** Keeps track of module instances and groups them according to module instance name */
	private HashMap<String,ModuleInstance> moduleInstances;
	/** This is a list of all the instances of primitives and macros in the design */
	private HashMap<String,Instance> instances;
	/** A map used to keep track of all used primitive sites used by the design */
	private HashMap<PrimitiveSite,Instance> usedPrimitiveSites;
	/** This is a list of all the nets in the design */
	private HashMap<String,Net> nets;
	/** A flag designating if this is a design or hard macro */
	private boolean isHardMacro;
	
	/** This is the actual part database device for the design specified by partName */
	private transient Device dev;
	/** This is the accompanying wire enumeration class to convert wire integers to Strings and vice versa */
	private transient WireEnumerator we;

	/** This is the special design name used by Xilinx to denote an XDL design as a hard macro */
	public static final String hardMacroDesignName = "__XILINX_NMC_MACRO";
	/** Keeps track of all slice primitive types, initialized statically */
	public static HashSet<PrimitiveType> sliceTypes;
	/** Keeps track of all DSP48 primitive types, initialized statically  */
	public static HashSet<PrimitiveType> dspTypes;
	/** Keeps track of all BRAM primitive types, initialized statically  */
	public static HashSet<PrimitiveType> bramTypes;
	
	/**
	 * Constructor which initializes all member data structures. Sets partName to null.
	 * NCDVersion is set to null. isHardMacro is set to false.
	 */
	public Design(){
		name = null;
		partName = null;
		NCDVersion = "v3.2"; // By default, it is often this value
		attributes = new ArrayList<Attribute>();
		modules = new HashMap<String, Module>();
		instances = new HashMap<String, Instance>();
		usedPrimitiveSites = new HashMap<PrimitiveSite, Instance>();
		nets = new HashMap<String, Net>();
		isHardMacro = false;
		moduleInstances = new HashMap<String, ModuleInstance>();
	}

	/**
	 * Loads the corresponding Device and WireEnumerator based on partName.
	 */
	public void loadDeviceAndWireEnumerator(){
		dev = FileTools.loadDevice(partName);
		we = FileTools.loadWireEnumerator(partName);
	}
	
	/**
	 * Checks if the primitive site site is used in this design.
	 * @param site The site to check for.
	 * @return True if this design uses site, false otherwise.
	 */
	public boolean isPrimitiveSiteUsed(PrimitiveSite site){
		return usedPrimitiveSites.containsKey(site);
	}

	/**
	 * Marks a primitive site as used by a particular instance.
	 * @param site The site to be marked as used.
	 * @param inst The instance using the site.
	 */
	protected Instance setPrimitiveSiteUsed(PrimitiveSite site, Instance inst){
		return usedPrimitiveSites.put(site, inst);
	}
	
	protected Instance releasePrimitiveSite(PrimitiveSite site){
		return usedPrimitiveSites.remove(site);
	}
	
	/**
	 * Gets and returns the instance which resides at site.
	 * @param site The site of the desired instance.
	 * @return The instance at site, or null if the primitive site is unoccupied.
	 */
	public Instance getInstanceAtPrimitiveSite(PrimitiveSite site){
		return usedPrimitiveSites.get(site);
	}
	
	/**
	 * Hard macro instances do not have a unified container, so we use a HashMap.
	 * This function allows the separation of an instance based on its hard macro instance
	 * name.
	 * @param inst The instance to add to the hashMap
	 * @return The ModuleInstance that the instance was added to
	 */
	public ModuleInstance addInstanceToModuleInstances(Instance inst, String moduleInstanceName){
		String key = moduleInstanceName;// + inst.getModuleTemplate().name;
		ModuleInstance mi = moduleInstances.get(key);
		
		if(mi == null){
			mi = new ModuleInstance(moduleInstanceName, this);
			moduleInstances.put(key, mi);
			mi.setModule(inst.getModuleTemplate());
		}
		mi.addInstance(inst);
		inst.setModuleInstance(mi);
		return mi;
	}

	/**
	 * Gets and returns the moduleInstance called name.
	 * @param name The name of the moduleInstance to get.
	 * @return The moduleInstance name, or null if it does not exist.
	 */
	public ModuleInstance getModuleInstance(String name){
		return moduleInstances.get(name);
	}
	/**
	 * Creates, adds to design, and returns a new 
	 * ModuleInstance called name and based on module.
	 * The module is also added to the design if not already present.
	 * @param name The name of the new ModuleInstance created.
	 * @param module The Module that the new ModuleInstance instances.
	 * @return A new ModuleInstance
	 */
	public ModuleInstance createModuleInstance(String name, Module module){
		if(modules.get(module.getName()) == null)
			modules.put(module.getName(), module);
		
		ModuleInstance modInst = new ModuleInstance(name, this);
		moduleInstances.put(modInst.getName(), modInst);
		modInst.setModule(module);
		
		HashMap<String, Instance> instanceMap = new HashMap<String, Instance>();
		for(Instance templateInst : module.getInstances()){
			Instance inst = new Instance();
			inst.setName(modInst.getName()+"/"+templateInst.getName());
			inst.setModuleTemplate(module);
			inst.setModuleTemplateInstance(templateInst);
			inst.setAttributes(new ArrayList<Attribute>(templateInst.getAttributes()));
			inst.setBonded(templateInst.getBonded());
			inst.setType(templateInst.getType());
			
			this.addInstance(inst);
			inst.setModuleInstance(modInst);
			modInst.addInstance(inst);
			if(templateInst.equals(module.getAnchor())){
				modInst.setAnchor(inst);
			}
			instanceMap.put(inst.getName(), inst);
		}
		
		HashMap<Pin,Port> pinToPortMap = new HashMap<Pin,Port>();
		for(Port port : module.getPortList()){
			pinToPortMap.put(port.getPin(),port);
		}
		
		for(Net templateNet : module.getNets()){
			Net net = new Net(modInst.getName()+"/"+templateNet.getName(), templateNet.getType());
			
			HashSet<Instance> instanceList = new HashSet<Instance>();
			Port port = null;
			for(Pin templatePin : templateNet.getPins()){
				Port temp = pinToPortMap.get(templatePin);
				port = (temp != null)? temp : port;
				Instance inst = instanceMap.get(modInst.getName()+"/"+templatePin.getInstanceName());
				if(inst == null)
					System.out.println("DEBUG: could not find Instance "+modInst.getName()+"/"+templatePin.getInstanceName());
				instanceList.add(inst);
				Pin pin = new Pin(templatePin.isOutPin(), templatePin.getName(), inst);
				net.addPin(pin);
			}
			
			if(port == null){
				modInst.addNet(net);
				net.addAttribute("_MACRO", "", modInst.getName());
				net.setModuleInstance(modInst);
				net.setModuleTemplate(module);
				net.setModuleTemplateNet(templateNet);
			}else{
				net.setName(modInst.getName()+"/"+port.getName());
			}
			this.addNet(net);
			for(Instance inst : instanceList){
				inst.addToNetList(net);
			}
		}
		return modInst;
	}
	
	/**
	 * Gets and returns the current name of the design.
	 * @return The current name of the design.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Gets and returns the current attributes of the design.
	 * @return The current attributes of the design.
	 */
	public ArrayList<Attribute> getAttributes(){
		return attributes;
	}
	
	/**
	 * Adds the attribute with value to this design.
	 * @param physicalName Physical name of the attribute.
	 * @param value Value to set the new attribute to.
	 */
	public void addAttribute(String physicalName, String logicalName, String value){
		attributes.add(new Attribute(physicalName, logicalName, value));
	}
	
	/**
	 * Add the attribute to the design.
	 * @param attribute The attribute to add.
	 */
	public void addAttribute(Attribute attribute){
		attributes.add(attribute);
	}
	
	/**
	 * Checks if the design attribute has an attribute with a physical
	 * name called phyisicalName.  
	 * @param physicalName The physical name of the attribute to check for.
	 * @return True if the design contains an attribute with the 
	 * physical name physicalName, false otherwise.
	 */
	public boolean hasAttribute(String physicalName){
		for(Attribute attr : attributes){
			if(attr.getPhysicalName().equals(physicalName)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets the list of attributes for this design.
	 * @param attributes The new list of attributes to associate with this
	 * design.
	 */
	public void setAttributes(ArrayList<Attribute> attributes){
		this.attributes = attributes;
	}
	
	/**
	 * Gets and returns the HashMap of all of the module instance members separated by
	 * module instance name.
	 * @return The HashMap containing all current module instances.
	 */
	public HashMap<String,ModuleInstance> getModuleInstances(){
		return moduleInstances;
	}

	/**
	 * This will return the part name with speed grade of the part this design or 
	 * hard macro targets (ex: xc4vsx35ff668-10).
	 * @return The part name with package and speed grade information.
	 */
	public String getPartName(){
		return this.partName;
	}
	
	/**
	 * Gets the NCD version present in the XDL design.
	 * @return The NCD version string.
	 */
	public String getNCDVersion(){
		return this.NCDVersion;
	}
	
	/**
	 * Determines if this design is a hard macro.
	 * @return True if this design is a hard macro, false otherwise.
	 */
	public boolean isHardMacro(){
		return this.isHardMacro;
	}
	
	/**
	 * Adds a module to the design.
	 * @param module The module to add.
	 */
	public void addModule(Module module){
		modules.put(module.getName(),module);
	}
	
	/** 
	 * Adds an instance to the design. 
	 * @param inst This instance to add.
	 */
	public void addInstance(Instance inst){
		if(inst.isPlaced()){
			usedPrimitiveSites.put(inst.getPrimitiveSite(), inst);
		}
		inst.setDesign(this);
		instances.put(inst.getName(), inst);
	}
	
	/**
	 * Adds a net to the design.
	 * @param net The net to add.
	 */
	public void addNet(Net net){
		nets.put(net.getName(), net);
	}
	
	/**
	 * Removes a net from the design
	 * @param name The name of the net to remove.
	 */
	public void removeNet(String name){
		nets.remove(name);
	}
	
	/**
	 * Get a module by name.
	 * @param name The name of the module to get.
	 * @return The module called name, if it exists, null otherwise.
	 */
	public Module getModule(String name){
		return modules.get(name);
	}
	
	/**
	 * Gets the core module that corresponds to this class if it
	 * is a hard macro (a hard macro design will have only one module).
	 * @return The module of the hard macro, null if this is not a hard macro design.
	 */
	public Module getHardMacro(){
		if(isHardMacro){
			for(Module module : modules.values()){
				return module;
			}
		}
		return null;
	}
	
	/**
	 * Get an instance by name.
	 * @param name Name of the instance to get.
	 * @return The instance name, or null if it does not exist.
	 */
	public Instance getInstance(String name){
		return instances.get(name);
	}
	
	/**
	 * Get an net by name.
	 * @param name Name of the net to get.
	 * @return The net name, or null if it does not exist.
	 */
	public Net getNet(String name){
		return nets.get(name);
	}
	
	/**
	 * Gets and returns all of the instance in the design.
	 * @return All the instances of the design.
	 */
	public Collection<Instance> getInstances(){
		return instances.values();
	}
	
	/**
	 * Gets and returns the hash map of instances in the design.
	 * @return The hash map of instances.
	 */
	public HashMap<String,Instance> getInstanceMap(){
		return instances;
	}
	
	/**
	 * Gets and returns all of the modules of the design.
	 * @return All the modules of the design.
	 */
	public Collection<Module> getModules(){
		return modules.values();
	}
	
	/**
	 * Gets and returns all of the nets of the design.
	 * @return The hash map of nets.
	 */
	public Collection<Net> getNets(){
		return nets.values();
	}
	
	/**
	 * Gets and returns the hash map of nets in the design.
	 * @return All of the nets of the design.
	 */
	public HashMap<String,Net> getNetMap(){
		return nets;
	}
	
	/**
	 * Gets and returns the wire enumerator for this part.
	 * @return The wire enumerator for this part.
	 */
	public WireEnumerator getWireEnumerator(){
		return we;
	}

	/**
	 * Sets the WireEnumerator for this design.  
	 * @param we The WireEnumerator to set for this design.
	 */
	public void setWireEnumerator(WireEnumerator we){
		this.we = we;
	}

	/**
	 * Gets the device specific to this part and returns it. (This should 
	 * be the same device loaded with the XDL design file).
	 * @return The device specific to this part.
	 */
	public Device getDevice(){
		return dev;
	}

	/**
	 * Sets the name of the design 
	 * @param name New name for the design
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Sets the device specific to this part.  Generally only used by the parser
	 * when loading a design, but could be used to convert a design to a different
	 * part (among a host of other transformations).
	 * @param dev The device to set this design with.
	 */
	public void setDevice(Device dev){
		this.dev = dev;
	}

	/**
	 * Sets the Xilinx part name, it should include package and speed grade also.
	 * For example xc4vfx12ff668-10 is a valid part name.
	 * @param partName Name of the Xilinx FPGA part.
	 */
	public void setPartName(String partName){
		if(this.partName != null){
			MessageGenerator.briefErrorAndExit("Sorry, cannot change a Design part name" +
				"after one has already been set. Please create a new Design for that.");
		}
		this.partName = partName;
		loadDeviceAndWireEnumerator();
	}
	
	/**
	 * Sets the NCD version as shown in the XDL file.
	 * @param ver The NCD version.
	 */
	public void setNCDVersion(String ver){
		this.NCDVersion = ver;
	}
	
	/**
	 * Sets the design as a hard macro or not (a hard macro
	 * will have only one module as a member of the design).
	 * @param value true if it is a hard macro, false otherwise.
	 */
	public void setIsHardMacro(boolean value){
		this.isHardMacro = value;
	}
	
	/**
	 * Sets the nets of the design from the ArrayList netList.
	 * @param netList The new list of nets to replace the current
	 * nets of the design.
	 */
	public void setNets(ArrayList<Net> netList){
		nets.clear();
		for(Net net : netList) {
			addNet(net);
		}
	}

	/**
	 * Unroutes the current design by removing all PIPs.
	 */
	public void unrouteDesign(){
		// Just remove all the PIPs
		for(Net net : nets.values()){
			net.getPIPs().clear();
		}
	}
	
	public void flattenDesign(){
		if(isHardMacro){
			MessageGenerator.briefError("ERROR: Cannot flatten a hard macro design");
			return;
		}
		for(ModuleInstance mi : moduleInstances.values()){
			for(Instance instance : mi.getInstances()){
				instance.detachFromModule();
			}
			for(Net net : mi.getNets()){
				net.detachFromModule();
			}
		}
		modules.clear();
	}
	
	/**
	 * Load a standard XDL file and return the XDL_Design object.
	 * @param fileName The name of the xdl file to load.
	 */
	public void loadXDLFile(String fileName){
		DesignParser parser = null;
		try {
			parser = new DesignParser(new BufferedInputStream (new FileInputStream(fileName)));
			parser.setDesign(this);
			parser.XDL_File();
		} 
		catch (FileNotFoundException e){
			MessageGenerator.briefErrorAndExit("Could not find XDL file: " + fileName);
		}
		catch (ParseException e) {
			MessageGenerator.briefErrorAndExit("There was a problem parsing the XDL file: " +
				fileName + File.separator + e.getMessage());
		}
	}
	
	/**
	 * Saves the XDL design to a minimalist XDL file.  This is the same
	 * as saveXDLFile(fileName, false);
	 * @param fileName Name of the file to save the design to.
	 */
	public void saveXDLFile(String fileName){
		saveXDLFile(fileName, false);
	}
	
	/**
	 * Saves the XDL design and adds comments based on the parameter addComments.
	 * @param fileName Name of the file to save the design to. 
	 * @param addComments Adds the same comments found in XDL designs created by the 
	 * Xilinx xdl tool.
	 */
	public void saveXDLFile(String fileName, boolean addComments){
		String nl = System.getProperty("line.separator");
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));

			if(addComments){
				bw.write(nl+"# ======================================================="+nl);
				bw.write("# "+this.getClass().getCanonicalName()+" XDL Generation $Revision: 1.01$"+nl);
				bw.write("# time: "+ FileTools.getTimeString() +nl+nl);
				bw.write("# ======================================================="+nl+nl+nl);
				
				bw.write("# ======================================================="+nl);
				bw.write("# The syntax for the design statement is:                "+nl);
				bw.write("# design <design_name> <part> <ncd version>;             "+nl);
				bw.write("# or                                                     "+nl);
				bw.write("# design <design_name> <device> <package> <speed> <ncd_version>"+nl);
				bw.write("# ======================================================="+nl);
			}
				
			if(!isHardMacro){
				bw.write("design \"" + name + "\" " + partName + " " + NCDVersion + " ,"+nl);
				bw.write("  cfg \"");
				for(Attribute attr : attributes){
					bw.write(nl+"\t" + attr.toString());
				}
				bw.write("\";"+nl+nl+nl);
			}
			else{
				bw.write("design \"" + name + "\" " + partName + ";"+nl+nl);
			}
			
			
			if(modules.size() > 0){
				if(addComments){
					bw.write("# ======================================================="+nl);
					bw.write("# The syntax for modules is:"+nl);
					bw.write("#     module <name> <inst_name> ;"+nl);
					bw.write("#     port <name> <inst_name> <inst_pin> ;"+nl);
					bw.write("#     ."+nl);
					bw.write("#     ."+nl);
					bw.write("#     instance ... ;"+nl);
					bw.write("#     ."+nl);
					bw.write("#     ."+nl);
					bw.write("#     net ... ;"+nl);
					bw.write("#     ."+nl);
					bw.write("#     ."+nl);
					bw.write("#     endmodule <name> ;"+nl);
					bw.write("# ======================================================="+nl+nl);
				}

				for(String moduleName : modules.keySet()){
					Module module = modules.get(moduleName);
					if(addComments){
						bw.write("# ======================================================="+nl);
						bw.write("# MODULE of \""+moduleName+"\"" + nl);
						bw.write("# ======================================================="+nl);
					}
					bw.write("module "+"\""+moduleName+"\" "+"\""+module.getAnchor().getName()+"\" , cfg \"");
					
					for(Attribute attr : module.getAttributes()){
						bw.write(attr.toString()+" ");
					}
					bw.write("\";" + nl);
					for(Port port : module.getPortList()){
						bw.write("  port \"" + port.getName() +"\" \"" + port.getInstanceName() +"\" \"" + port.getPinName()+"\";" + nl);
					}
					for(Instance inst : module.getInstances()){
						String placed = inst.isPlaced() ? "placed " + inst.getTile() + " " + inst.getPrimitiveSiteName() : "unplaced";
						bw.write("  inst \"" + inst.getName() + "\" \"" + inst.getType() +"\"," + placed +"  ," +nl);
						bw.write("    cfg \"");
						for(Attribute attr : inst.getAttributes()){
							bw.write(" " + attr.toString());
						}
						bw.write(" \"" + nl + "    ;" + nl);
					}
					for(Net net : module.getNets()){
						bw.write("  net \"" + net.getName() + "\" ," );
						if(net.getAttributes() != null){
							bw.write(" cfg \"");
							for(Attribute attr : net.getAttributes()){
								bw.write(" " + attr.toString());
							}
							bw.write("\",");
						}
						bw.write(nl);
						for(Pin pin : net.getPins()){
							if(pin.isOutPin()){
								bw.write("    outpin \"" + pin.getInstanceName() + "\" " + pin.getName() +" ," + nl);
							}else {
								bw.write("    inpin \"" + pin.getInstanceName() + "\" " + pin.getName() +" ," + nl);
							}
						}
						for(PIP pip : net.getPIPs()){
							bw.write("    pip " + pip.getTile() +" "+ pip.getStartWireName(we) + " -> " + pip.getEndWireName(we) + " ," + nl);
						}
						bw.write("    ;" + nl);
					}
					bw.write("endmodule \""+moduleName+"\" ;" + nl + nl);
				}
			}
			if(!isHardMacro){
				if(addComments){
					if(moduleInstances.size() > 0){
						bw.write(nl);
						bw.write("#  ======================================================="+nl);
						bw.write("#  MODULE INSTANCES"+nl);
						bw.write("#  ======================================================="+nl);
						for(ModuleInstance mi : moduleInstances.values()){
							bw.write("# instance \""+mi.getName()+"\" \""+mi.getModule().getName()+"\" , ");
							if(mi.getAnchor() == null){
								System.out.println("Anchor is null");
							}
							if(mi.getAnchor().isPlaced()){
								bw.write("placed " + mi.getAnchor().getTile() + " " +
										mi.getAnchor().getPrimitiveSiteName() + " ;" + nl);
							}
							else{
								bw.write("unplaced  ;" + nl);
							}
						}
						bw.write(nl);
					}
					
					bw.write("#  ======================================================="+nl);
					bw.write("#  The syntax for instances is:"+nl);
					bw.write("#      instance <name> <sitedef>, placed <tile> <site>, cfg <string> ;"+nl);
					bw.write("#  or"+nl);
					bw.write("#      instance <name> <sitedef>, unplaced, cfg <string> ;"+nl);
					bw.write("# "+nl);
					bw.write("#  For typing convenience you can abbreviate instance to inst."+nl);
					bw.write("# "+nl);
					bw.write("#  For IOs there are two special keywords: bonded and unbonded"+nl);
					bw.write("#  that can be used to designate whether the PAD of an unplaced IO is"+nl);
					bw.write("#  bonded out. If neither keyword is specified, bonded is assumed."+nl);
					bw.write("# "+nl);
					bw.write("#  The bonding of placed IOs is determined by the site they are placed in."+nl);
					bw.write("# "+nl);
					bw.write("#  If you specify bonded or unbonded for an instance that is not an"+nl);
					bw.write("#  IOB it is ignored."+nl);
					bw.write("# "+nl);
					bw.write("#  Shown below are three examples for IOs. "+nl); 
					bw.write("#     instance IO1 IOB, unplaced ;          # This will be bonded"+nl);
					bw.write("#     instance IO1 IOB, unplaced bonded ;   # This will be bonded"+nl);
					bw.write("#     instance IO1 IOB, unplaced unbonded ; # This will be unbonded"+nl);
					bw.write("#  ======================================================="+nl);
				}
				for(Instance inst : getInstances()){
					String placed = inst.isPlaced() ? "placed " + inst.getTile() +
							" " + inst.getPrimitiveSiteName() : "unplaced";
					String module = inst.getModuleInstanceName()==null ? "" : "module \"" + 
							inst.getModuleInstanceName() +"\" \"" + inst.getModuleTemplate().getName() + "\" \"" +
							inst.getModuleTemplateInstance().getName() + "\" ,";
					bw.write("inst \"" + inst.getName() + "\" \"" + inst.getType() +"\"," + placed + "  ," + module + nl);
					bw.write("  cfg \"");
					for(Attribute attr : inst.getAttributes()){
						bw.write(" " + attr.toString());
					}
					bw.write(" \"" + nl + "  ;" + nl);
				}
				bw.write(nl);
				
				if(addComments){
					bw.write("#  ================================================"+nl);
					bw.write("#  The syntax for nets is:"+nl);
					bw.write("#     net <name> <type>,"+nl);
					bw.write("#       outpin <inst_name> <inst_pin>,"+nl);
					bw.write("#       ."+nl);
					bw.write("#       ."+nl);
					bw.write("#       inpin <inst_name> <inst_pin>,"+nl);
					bw.write("#       ."+nl);
					bw.write("#       ."+nl);
					bw.write("#       pip <tile> <wire0> <dir> <wire1> , # [<rt>]"+nl);
					bw.write("#       ."+nl);
					bw.write("#       ."+nl);
					bw.write("#       ;"+nl);
					bw.write("# "+nl);
					bw.write("#  There are three available wire types: wire, power and ground."+nl);
					bw.write("#  If no type is specified, wire is assumed."+nl);
					bw.write("# "+nl);
					bw.write("#  Wire indicates that this a normal wire."+nl);
					bw.write("#  Power indicates that this net is tied to a DC power source."+nl);
					bw.write("#  You can use \"power\", \"vcc\" or \"vdd\" to specify a power net."+nl);
					bw.write("# "+nl);
					bw.write("#  Ground indicates that this net is tied to ground."+nl);
					bw.write("#  You can use \"ground\", or \"gnd\" to specify a ground net."+nl);
					bw.write("# "+nl); 
					bw.write("#  The <dir> token will be one of the following:"+nl);
					bw.write("# "+nl);
					bw.write("#     Symbol Description"+nl);
					bw.write("#     ====== =========================================="+nl);
					bw.write("#       ==   Bidirectional, unbuffered."+nl);
					bw.write("#       =>   Bidirectional, buffered in one direction."+nl);
					bw.write("#       =-   Bidirectional, buffered in both directions."+nl);
					bw.write("#       ->   Directional, buffered."+nl);
					bw.write("# "+nl); 
					bw.write("#  No pips exist for unrouted nets."+nl);
					bw.write("#  ================================================"+nl);
				}				

				for(Net net : getNets()){
					String type = net.getType().equals(NetType.WIRE) ? "" : net.getType().toString().toLowerCase();
					bw.write("  net \"" + net.getName() + "\" "+type+"," );
					if(net.getAttributes() != null){
						bw.write(" cfg \"");
						for(Attribute attr : net.getAttributes()){
							bw.write(" " + attr.toString());
						}
						bw.write("\",");
					}
					bw.write(nl);
					for(Pin pin : net.getPins()){
						if(pin.isOutPin()){
							bw.write("    outpin \"" + pin.getInstanceName() + "\" " + pin.getName() +" ," + nl);
						}else {
							bw.write("    inpin \"" + pin.getInstanceName() + "\" " + pin.getName() +" ," + nl);
						}
					}
					for(PIP pip : net.getPIPs()){
						bw.write("    pip " + pip.getTile() +" "+ pip.getStartWireName(we) + " -> " + pip.getEndWireName(we) + " ," + nl);
					}
					bw.write("    ;" + nl);
				}
				
				bw.write(nl);
				
				if(addComments){
					int sliceCount = 0;
					int bramCount = 0;
					int dspCount = 0;
					for(Instance instance : instances.values()){
						PrimitiveType type = instance.getType();
						if(sliceTypes.contains(type)){
							sliceCount++;
						}
						else if(dspTypes.contains(type)){
							dspCount++;
						}
						else if(bramTypes.contains(type)){
							bramCount++;
						}
					}
					
					bw.write("# ======================================================="+nl);
					bw.write("# SUMMARY"+nl);
					bw.write("# Number of Module Defs: " + modules.size() + nl);
					bw.write("# Number of Module Insts: " + moduleInstances.size() + nl);
					bw.write("# Number of Primitive Insts: "+ instances.size() +nl);
					bw.write("#     Number of SLICES: "+ sliceCount +nl);
					bw.write("#     Number of DSP48s: "+ dspCount +nl);
					bw.write("#     Number of BRAMs: "+ bramCount +nl);
					bw.write("# Number of Nets: " + nets.size() + nl);
					bw.write("# ======================================================="+nl+nl+nl);
				}
			}
			else{
				if(addComments){
					Module mod = getHardMacro();
					bw.write("# ======================================================="+nl);
					bw.write("# MACRO SUMMARY"+nl);
					bw.write("# Number of Module Insts: " + mod.getInstances().size() + nl);
					HashMap<PrimitiveType,Integer> instTypeCount = new HashMap<PrimitiveType,Integer>();
					for(Instance inst : mod.getInstances()){
						Integer count = instTypeCount.get(inst.getType());
						if(count == null){
							instTypeCount.put(inst.getType(),1);
						}
						else{
							count++;
							instTypeCount.put(inst.getType(),count);
						}
					}
					for(PrimitiveType type : instTypeCount.keySet()){
						bw.write("#   Number of " + type.toString() + "s: " + instTypeCount.get(type) + nl);
					}
					bw.write("# Number of Module Ports: " + mod.getPortList().size() + nl);
					bw.write("# Number of Module Nets: "+ mod.getNets().size() +nl);
					bw.write("# ======================================================="+nl+nl+nl);
				}
			}
			bw.close();
		}
		catch(IOException e){
			MessageGenerator.briefErrorAndExit("Error writing XDL file: " +
				fileName + File.separator + e.getMessage());
		}
	}
	
	static {
		sliceTypes = new HashSet<PrimitiveType>();
		sliceTypes.add(PrimitiveType.SLICEL);
		sliceTypes.add(PrimitiveType.SLICEM);
		sliceTypes.add(PrimitiveType.SLICEX);
		
		dspTypes = new HashSet<PrimitiveType>();
		dspTypes.add(PrimitiveType.DSP48);
		dspTypes.add(PrimitiveType.DSP48A);
		dspTypes.add(PrimitiveType.DSP48A1);
		dspTypes.add(PrimitiveType.DSP48E);
		dspTypes.add(PrimitiveType.DSP48E1);
		
		bramTypes = new HashSet<PrimitiveType>();
		bramTypes.add(PrimitiveType.RAMB16);
		bramTypes.add(PrimitiveType.RAMB16BWE);
		bramTypes.add(PrimitiveType.RAMB16BWER);
		bramTypes.add(PrimitiveType.RAMB18E1);
		bramTypes.add(PrimitiveType.RAMB18X2);
		bramTypes.add(PrimitiveType.RAMB18X2SDP);
		bramTypes.add(PrimitiveType.RAMB36_EXP);
		bramTypes.add(PrimitiveType.RAMB36E1);
		bramTypes.add(PrimitiveType.RAMB36SDP_EXP);
		bramTypes.add(PrimitiveType.RAMB8BWER);
		bramTypes.add(PrimitiveType.RAMBFIFO18);
		bramTypes.add(PrimitiveType.RAMBFIFO18_36);
		bramTypes.add(PrimitiveType.RAMBFIFO36);
		bramTypes.add(PrimitiveType.RAMBFIFO36E1);
	}
}
