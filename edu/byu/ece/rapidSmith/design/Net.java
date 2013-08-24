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

import java.io.Serializable;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

/**
 * This class represents the nets in XDL.  It keeps track of the type, 
 * source and sinks as well as routing resources (PIPs).  It also has 
 * references to keep track of the module it may be a part of.
 * @author Chris Lavin
 * Created on: Jun 25, 2010
 */
public class Net implements Comparable<Net>, Serializable {

	private static final long serialVersionUID = 6252168375875946963L;

	/** Unique name of the net */
	private String name;
	/** Type of the net (VCC, GND, WIRE, ...) */
	private NetType type;
	/** Attributes of the net, often are not used */
	private ArrayList<Attribute> attributes;
	/** Source and sink pins of the net */
	private ArrayList<Pin> pins;
	/** Routing resources or Programmable-Interconnect-Points */ 
	private ArrayList<PIP> pips;
	/** The source pin for the net */
	private Pin source;
	/** The number of sinks this net contains */
	private int fanOut;
	/** The module instance this net is a member of */
	private ModuleInstance moduleInstance;
	/** The module template (or definition) this net is a member of */
	private Module moduleTemplate;
	/** The net in the module template corresponding to this net */
	private Net moduleTemplateNet;
	
	/**
	 * Default Constructor
	 */
	public Net(){
		this.name = null;
		this.type = NetType.WIRE;
		this.pins = new ArrayList<Pin>();
		this.pips = new ArrayList<PIP>();
		this.source = null;
		this.fanOut = 0;
		moduleInstance = null;
		moduleTemplate = null;
		moduleTemplateNet = null;
	}
	
	/**
	 * Initializing constructor
	 * @param name Name of the new net
	 * @param type Type of the new net
	 */
	public Net(String name, NetType type){
		this.name = name;
		this.type = type;
		this.pins = new ArrayList<Pin>();
		this.pips = new ArrayList<PIP>();
		this.source = null;
		this.fanOut = 0;
		moduleInstance = null;
		moduleTemplate = null;
		moduleTemplateNet = null;
	}
	
	/**
	 * Gets and return the current name of the net.
	 * @return The name of the net.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Gets and returns the type of the net.
	 * @return The type of the net.
	 */
	public NetType getType(){
		return type;
	}
	
	/**
	 * Gets the pins (source and sinks) of the net.
	 * @return The pins of the net.
	 */
	public ArrayList<Pin> getPins(){
		return pins;
	}
	
	/**
	 * Gets the PIPs (routing resources) used by the net.
	 * @return The PIPs used by the net.
	 */
	public ArrayList<PIP> getPIPs(){
		return pips;
	}
	
	/**
	 * Gets the fan-out (number of sinks) of the net.
	 * @return The fan-out of the net.
	 */
	public int getFanOut(){
		return fanOut;
	}
	
	/**
	 * Sets the list of attributes for the net.
	 * @param attributes The new list of attributes.
	 */
	public void setAttributes(ArrayList<Attribute> attributes){
		this.attributes = attributes;
	}

	/**
	 * Gets and returns the attributes for the net.
	 * @return The attributes for the net.
	 */
	public ArrayList<Attribute> getAttributes(){
		return attributes;
	}

	/**
	 * Checks if the net has any attributes.
	 * @return True if the net has one or more attributes, false otherwise. 
	 */
	public boolean hasAttributes(){
		return getAttributes() != null;
	}
	
	/**
	 * Checks if the net has any PIPs.
	 * @return True if the net contains 1 or more PIPs, false otherwise.
	 */
	public boolean hasPIPs(){
		return pips.size() > 0;
	}
	
	/**
	 * Sets the name of the net. User is responsible to make sure the 
	 * net name is unique to all other net names.
	 * @param name New name of the net.
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Sets the type of the net.
	 * @param type New type of the net.
	 */
	public void setType(NetType type){
		this.type = type;
	}
	
	/**
	 * Sets the pins (source and sinks) of the net.
	 * @param list The new pin list.
	 */
	public boolean setPins(ArrayList<Pin> list){
		Pin src = null;
		this.fanOut = 0;
		for(Pin p : list){
			if(p.isOutPin()){
				if(src != null){
					return false;
				}
				src = p;
			}
			else{
				this.fanOut++;
			}
			p.setNet(this);
			p.getInstance().addToNetList(this);
		}
		this.pins = list;
		this.source = src;
		return true;
	}
	
	/**
	 * Sets the PIPs of the net.
	 * @param list The new list of PIPs.
	 */
	public void setPIPs(ArrayList<PIP> list){
		this.pips = list;
	}

	/**
	 * Adds a new pin to the net.  Also checks if this is a new 
	 * sink and updates the fan-out accordingly.
	 * @param pin The new pin to add.
	 * @return True if the operation completed successfully, false otherwise.
	 */
	public boolean addPin(Pin pin){
		if(pin.isOutPin()){
			if(source != null){
				return false;
			}
			this.source = pin;
		}
		else{
			fanOut++;
		}
		pins.add(pin);
		pin.setNet(this);
		if(pips.size() > 0){
			// TODO - Possibly add a state to each net that determines if it is 
			// routed or not
			unroute();
		}
		return true;
	}
	/**
	 * Adds a list of pins to the net.
	 * @param pinsToAdd The list of new pins to add.
	 * @return True if the operation completed successfully, false otherwise.
	 */
	public boolean addPins(ArrayList<Pin> pinsToAdd) {
		boolean success = true;
		for(Pin pin : pinsToAdd){
			if(!this.addPin(pin))
				success = false;
		}
		return success;
	}
	
	/**
	 * Removes a pin from the list of pins in the net.
	 * Updates the fan-out and source accordingly.
	 * @param pin The pin to remove.
	 * @return True if the operation completed successfully, false otherwise.
	 */
	public boolean removePin(Pin pin){
		if(pin.isOutPin() && pin.equals(source)){
			this.source = null;
		}
		else{
			fanOut--;
		}
		pin.setNet(null);
		if(pips.size() > 0){
			// TODO - Be smarter about unrouting only the resources 
			// connected to the pin, such as unroute(Pin);
			unroute();
		}
		return pins.remove(pin);
	}
	
	/**
	 * Adds a PIP to the net.
	 * @param pip The PIP to add.
	 */
	public void addPIP(PIP pip){
		pips.add(pip);
	}
	
	/**
	 * Removes a PIP from the net.
	 * @param pip The PIP to remove.
	 * @return True if the operation completed successfully, false otherwise.
	 */
	public boolean removePIP(PIP pip){
		return pips.remove(pip);
	}
	
	/**
	 * Checks if the net is a static net (source is VCC/GND).
	 * @return True if net is source'd by VCC or GND.
	 */
	public boolean isStaticNet(){
		return (this.type.equals(NetType.VCC)) || (this.type.equals(NetType.GND));
	}

	/**
	 * Checks if a net is a clk net and should use the clock routing 
	 * resources.
	 * @return True if this net is a clock net, false otherwise.
	 */
	public boolean isClkNet(){
		// This is kind of difficult to quantify, but we'll use the following
		// checks to see if a net is a clk net:
		// Most of the sink pins have "CLK" in the internal pin name
		int count = 0;
		for(Pin p : pins){
			if(p.getName().contains("CLK")){
				count++;
			}
		}
		if(count >= (pins.size()/2)){
			return true;
		}
		return false;
	}
	
	/**
	 * Removes the source of the net.  This net is now
	 * without a source.
	 */
	public void removeSource(){
		this.source = null;
	}
	
	public void setSource(Pin source){
		this.source = source;
	}
	
	/**
	 * Replaces the current source with the new source and
	 * adds it to the pin list in the net.
	 * @param newSource The new source of the net.
	 */
	public boolean replaceSource(Pin newSource){
		if(!newSource.isOutPin()){
			return false;
		}
		if(this.source != null){
			removePin(this.source);
		}
		this.source = newSource;
		return this.pins.add(newSource);
	}

	/**
	 * Gets and returns the source of the net.
	 * @return The current source of the net, or null if it does not exist.
	 */
	public Pin getSource(){
		return source;
	}
	
	/**
	 * Gets and returns the tile where the source pin resides.
	 * @return The tile where the source pin resides, or null if there is 
	 * no source for this net. 
	 */
	public Tile getSourceTile(){
		return source != null ? source.getTile() : null;
	}
	
	/**
	 * This removes all PIPs from this net, causing it to be in an unrouted state.
	 */
	public void unroute(){
		this.pips.clear();
	}
	
	/**
	 * Gets and returns the total number of pins plus the total number 
	 * of PIPs.
	 * @return The sum of pins and PIPs in the net.
	 */
	public int getPinAndPIPCount(){
		return pins.size() + pips.size();
	}
	
	/**
	 * Adds an attribute to the net.
	 * @param physicalName The physical name portion of the attribute.
	 * @param logicalName The logical name portion of the attribute.
	 * @param value The value of the attribute
	 */
	public void addAttribute(String physicalName, String logicalName, String value){
		if(attributes == null){
			attributes = new ArrayList<Attribute>();
		}
		attributes.add(new Attribute(physicalName, logicalName, value));
	}
	
	/**
	 * Add the attribute to this net.
	 * @param attribute The attribute to add.
	 */
	public void addAttribute(Attribute attribute){
		if(attributes == null){
			attributes = new ArrayList<Attribute>();
		}
		attributes.add(attribute);
	}
	
	/**
	 * Gets the module template this net is a member of
	 * @return The module template this net is a member of
	 */
	public Module getModuleTemplate(){
		return moduleTemplate;
	}

	/**
	 * Sets the module class this net implements.
	 * @param module The module which this net implements.
	 */
	public void setModuleTemplate(Module module){
		this.moduleTemplate = module;
	}
	
	/**
	 * Sets the reference to the template net from a module template corresponding to this net.
	 * @param moduleTemplateNet The template net in the module to which this
	 * net corresponds.
	 */
	public void setModuleTemplateNet(Net moduleTemplateNet){
		this.moduleTemplateNet = moduleTemplateNet;
	}

	/**
	 * Gets and returns the net found in the module which this net implements.
	 * @return The net found in the module which this net implements.
	 */
	public Net getModuleTemplateNet(){
		return moduleTemplateNet;
	}
	
	/**
	 * Sets the module instance which this net belongs to.
	 * @param moduleInstance The nets new moduleInstance.
	 */
	public void setModuleInstance(ModuleInstance moduleInstance){
		this.moduleInstance = moduleInstance;
	}

	/**
	 * Gets and returns the nets current module instance it belongs to.
	 * @return The module instance of this net, or null if none exists.
	 */
	public ModuleInstance getModuleInstance(){
		return moduleInstance;
	}

	/**
	 * This method will detach and remove all reference of the net to a module 
	 * or module instance.
	 */
	public void detachFromModule(){
		this.moduleInstance = null;
		this.moduleTemplate = null;
		this.moduleTemplateNet = null;
		this.setAttributes(null);
	}
	
	/**
	 * Compares two nets based on fan-out.
	 */
	@Override
	public int compareTo(Net o){
		// Sort by greatest to least fan out
		return o.getFanOut() - this.getFanOut();

	}
	
	/**
	 * Creates an XDL string representation of the net.
	 * @param we The design WireEnumerator (for converting int wires to strings).
	 * @return The string XDL representation of this net.
	 */
	public String toString(WireEnumerator we) {
		String nl = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();
		sb.append("net \"");
		sb.append(name); 
		sb.append("\" ");
		if(type != NetType.WIRE) sb.append(type.toString().toLowerCase());
		sb.append(", ");
		if(hasAttributes()){
			sb.append("cfg \" ");
			for(Attribute a : this.attributes){
				sb.append(a.getPhysicalName());
				sb.append(":");
				sb.append(a.getLogicalName());
				sb.append(":");
				sb.append(a.getValue());
				sb.append(" ");
			}
		}
		sb.append(nl);
		for(Pin p : pins){
			sb.append("  "+p.getPinType().toString().toLowerCase()+" \"");
			sb.append(p.getInstanceName());
			sb.append("\" ");
			sb.append(p.getName());
			sb.append(", ");
			sb.append(nl);
		}
		for(PIP pip : pips){
			sb.append("  pip ");
			sb.append(pip.getTile());
			sb.append(" ");
			sb.append(pip.getStartWireName(we));
			sb.append(" -> ");
			sb.append(pip.getEndWireName(we));
			sb.append(" ,");
			sb.append(nl);
		}
		sb.append("  ;");
		sb.append(nl);
		return sb.toString();		
	}

	/**
	 * Creates a hashCode for the net based on its name.
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	/**
	 * Checks if two nets are equal by name.
	 */
	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Net other = (Net) obj;
		if(name == null){
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		return true;
	}
	
}
