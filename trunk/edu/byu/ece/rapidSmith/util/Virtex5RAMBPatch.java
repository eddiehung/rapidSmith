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
package edu.byu.ece.rapidSmith.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.PrimitiveType;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefPin;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDef;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefList;
import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.Pin;


public class Virtex5RAMBPatch {

	public Device dev;
	public WireEnumerator we;
	public Design design;
	public HashSet<PrimitiveType> rambs;
	public HashSet<String> externalNames;
	
	public Virtex5RAMBPatch(){
		design = new Design();
		
		rambs = new HashSet<PrimitiveType>();
		rambs.add(PrimitiveType.RAMB18X2);
		rambs.add(PrimitiveType.RAMB18X2SDP);
		rambs.add(PrimitiveType.RAMB36SDP_EXP);
		rambs.add(PrimitiveType.RAMB36_EXP);
		rambs.add(PrimitiveType.RAMBFIFO18);
		rambs.add(PrimitiveType.RAMBFIFO18_36);
		rambs.add(PrimitiveType.FIFO36_72_EXP);
		rambs.add(PrimitiveType.FIFO36_EXP);
		externalNames = new HashSet<String>();
	}
	
	public HashSet<String> getMissing(PrimitiveType type){
		/*System.out.println("*** External Pin Names of Universal BRAM Primitive in Virtex 5 ***");
		Primitive p = dev.getPrimitive("RAMB36_X0Y11");
		for(String name : p.getPins().keySet()){
			System.out.println("  " + we.getWireName(p.getExternalPinName(name)) + " : " + name);
		}*/
		PrimitiveSite p = dev.getPrimitiveSite("RAMB36_X0Y11");
		HashSet<String> internalNames = new HashSet<String>();
		HashSet<String> missing = new HashSet<String>();
		internalNames.addAll(p.getPins().keySet());
		for(Integer name : p.getPins().values()){
			externalNames.add(we.getWireName(name));
		}
		
		PrimitiveDefList defs = FileTools.loadPrimitiveDefs(design.getPartName());
		PrimitiveDef def = defs.getPrimitiveDef(type);
		for(PrimitiveDefPin pin : def.getPins()){
			//if(!internalNames.contains(pin.getInternalName())){
				missing.add(pin.getInternalName());
			//}
		}
		return missing;
	}
	
	private void getMappingManually(ArrayList<Pin> pins, ArrayList<PIP> pips, HashMap<String,String> pinMappings, HashSet<String> missing){
		for(int i = 0; i < pins.size(); i++){
			System.out.println("PIN " + i + ": " + pins.get(i).getName());
		}
		for(int i = 0; i < pips.size(); i++){
			System.out.println("PIP " + i + ": " + pips.get(i).getEndWireName(we));
		}
		String line = "";
		while(!line.equalsIgnoreCase("d")){
			System.out.println("Enter Match Manually by Pin and then PIP number, type 'd' when finished:");
			System.out.print(">> ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try{
				line = br.readLine();
				if(!line.equalsIgnoreCase("d")){
					String[] tokens = line.split(" ");
					int pin = Integer.parseInt(tokens[0]);
					int pip = Integer.parseInt(tokens[1]);
					if(missing.contains(pins.get(pin).getName())) {
						pinMappings.put(pins.get(pin).getName(), pips.get(pip).getEndWireName(we));
					}
				}
			}
			catch(Exception e){
				System.out.println("Error");
			}
		}
	}
	
	public void analyzeDesign(){
		HashMap<String,String> inPinMappings = new HashMap<String, String>();
		HashMap<String,String> outPinMappings = new HashMap<String, String>();
		String nl = System.getProperty("line.separator");
		HashSet<String> missing = null;
		PrimitiveType type = null;
		for(Net net : design.getNets()){
			ArrayList<Pin> pinSinks = new ArrayList<Pin>();
			ArrayList<PIP> pipSinks = new ArrayList<PIP>();
			
			for(Pin pin : net.getPins()){
				if(pin.isOutPin()) continue;
				if(!rambs.contains(pin.getInstance().getType())){ 
					continue;
				}
				else{
					pinSinks.add(pin);
					if(missing == null) {
						missing = getMissing(pin.getInstance().getType());
						type = pin.getInstance().getType();
					}
				}
			}
			
			for(PIP pip : net.getPIPs()){
				// Tile t = dev.getTile(pip.getTile().toString());
				// Wire[] wires = t.getWires().get(we.getWireEnum(pip.getEndWireName(we)));
				// if(wires == null || wires.length == 0){
				if(externalNames.contains(pip.getEndWireName(we))){
					pipSinks.add(pip);
				}
			}
			
			
			if(pinSinks.size() == pipSinks.size()){
				if(pinSinks.size() == 1){
					if(missing.contains(pinSinks.get(0).getName())){
						inPinMappings.put(pinSinks.get(0).getName(), pipSinks.get(0).getEndWireName(we));
					}
				}
				else{
					int count = 0;
					for(PIP pip : pipSinks){
						for(Pin pin : pinSinks){
							if(pip.getEndWireName(we).contains(pin.getName())){
								if(missing.contains(pin.getName())){
									inPinMappings.put(pin.getName(), pip.getEndWireName(we));
								}
								pip.setTile(null);
								pin.setIsOutputPin(true);
								count++;
							}
						}
					}
					if(count < pipSinks.size()){
						if(pipSinks.size() - count == 1){
							for(PIP pip : pipSinks){
								if(!pip.getTile().equals(null)){
									for(Pin pin : pinSinks){
										if(!pin.isOutPin()){
											if(missing.contains(pin.getName())){
												inPinMappings.put(pin.getName(), pip.getEndWireName(we));
											}
											pip.setTile(null);
											pin.setIsOutputPin(true);
											count++;
										}
									}
								}
							}
						}
						else {
							/*
							System.out.println("Could not determine mappings: ");
							for(XDL_Pin pin : pinSinks){
								System.out.println("  PIN: " + pin.getPinName());	
							}
							for(XDL_PIP pip : pipSinks){
								System.out.println("  PIP: " + pip.getWire1());
							}*/
							getMappingManually(pinSinks, pipSinks, inPinMappings, missing);
							
						}
					}
				}
			}
			else{
				System.out.println("Mismatch of mapping sizes");
				for(Pin pin : pinSinks){
					System.out.println("  PIN: " + pin.getName());	
				}
				for(PIP pip : pipSinks){
					System.out.println("  PIP: " + pip.getEndWireName(we));
				}
			}
			
			// Get Outputs
			if(net.getSource() != null && rambs.contains(net.getSource().getInstance().getType())){
				for(PIP pip : net.getPIPs()){
					if(externalNames.contains(pip.getStartWireName(we))){
						outPinMappings.put(net.getSource().getName(), pip.getStartWireName(we));
					}
				}
			}
		}
		
		
		System.out.println("Primitive: " + type.toString());
		System.out.println("Total Pin Count: " + missing.size());
		System.out.println("Total Mapped Pin Count: " + (inPinMappings.size()+outPinMappings.size()));
		System.out.println("Mapped Input Pin Count: " + inPinMappings.size());
		System.out.println("Mapped Output Pin Count: " + outPinMappings.size());
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(type.toString() + ".txt"));
			for(String pin : inPinMappings.keySet()){
				String s = "\t\t\t(pinwire "+pin+" input "+inPinMappings.get(pin)+")" + nl;
				bw.write(s);
			}
			for(String pin : outPinMappings.keySet()){
				String s = "\t\t\t(pinwire "+pin+" output "+outPinMappings.get(pin)+")" + nl;
				bw.write(s);
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(type.toString() + "_missing.txt"));
			HashSet<String> found = new HashSet<String>();
			found.addAll(inPinMappings.keySet());
			found.addAll(outPinMappings.keySet());
			
			for(String s : missing){
				if(!found.contains(s)) {
					bw.write(s + nl);
				}
			}
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		if(args.length != 1){
			System.out.println("USAGE: <input.xdl>");
			System.exit(0);
		}
		
		Virtex5RAMBPatch patch = new Virtex5RAMBPatch();
		patch.design.loadXDLFile(args[0]);
		patch.dev = FileTools.loadDevice(patch.design.getPartName());
		patch.we = FileTools.loadWireEnumerator(patch.design.getPartName());
		
		patch.analyzeDesign();
	}
}
