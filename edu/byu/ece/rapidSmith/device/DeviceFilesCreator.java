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
package edu.byu.ece.rapidSmith.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import edu.byu.ece.rapidSmith.device.helper.Connection;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.PartNameTools;
import edu.byu.ece.rapidSmith.util.RunXilinxTools;
import edu.byu.ece.rapidSmith.util.FamilyType;

/**
 * This class has a number of static methods to create Device and WireEnumerator objects from
 * XDLRC files.  It also makes sure not to recreate XDLRC files if they already exist.
 * @author Chris Lavin
 * Created on: May 20, 2010
 */
public class DeviceFilesCreator{

	/**
	 * Makes sure an XDLRC file exists, will create if necessary.
	 * @param partName Name of the part to create the XDLRC file for.
	 * @return Name of the existing or newly created XDLRC file.
	 */
	public static String createXDLRC(String partName){
		FileTools.makeDirs(FileTools.getPartFolderPath(partName));
		String xdlrcFileName = FileTools.getPartFolderPath(partName) +
				partName + "_full.xdlrc";
		
		if(new File(xdlrcFileName).exists() && FileTools.getFileSize(xdlrcFileName) > 100000000){
			return xdlrcFileName;
		}
		if(!RunXilinxTools.generateFullXDLRCFile(partName, xdlrcFileName)){
			MessageGenerator.briefErrorAndExit("Exiting from XDLRC Generation failure.");
		}
		return xdlrcFileName;
	}
	
	/**
	 * Either loads or creates the WireEnumerator for the family  of partName.
	 * @param partName Name of the part to create WireEnumerator for.
	 * @return The loaded or newly created WireEnumerator
	 */
	public static WireEnumerator createWireEnumerator(String partName){
		FamilyType familyType = PartNameTools.getFamilyTypeFromPart(partName);
		String wireEnumeratorFileName = FileTools.getWireEnumeratorFileName(partName);
		if(new File(wireEnumeratorFileName).exists()){
			WireEnumerator we = new WireEnumerator();
			we.readCompactEnumFile(wireEnumeratorFileName, PartNameTools.getFamilyTypeFromPart(partName));
			return we;
		}
		
		ArrayList<String> partNames = new ArrayList<String>();
		switch(familyType){
			case KINTEX7:
				partNames.add("xc7k30tfbg484");
				partNames.add("xc7k160tfbg676");
				break;
			case SPARTAN2:
				partNames.add("xc2s100tq144");
				break;
			case SPARTAN2E:
				partNames.add("xc2s100eft256");
				partNames.add("xc2s600efg456");
				break;
			case SPARTAN3:
				partNames.add("xc3s1000fg320");
				break;
			case SPARTAN3A:
				partNames.add("xc3s50atq144");
				partNames.add("xc3s1400afg484");
				break;
			case SPARTAN3ADSP:
				partNames.add("xc3sd1800acs484");
				break;
			case SPARTAN3E:
				partNames.add("xc3s100evq100");
				partNames.add("xc3s250evq100");
				partNames.add("xc3s1200eft256");
				break;
			case SPARTAN6:
				partNames.add("xc6slx25tcsg324");
				partNames.add("xc6slx75tfgg676");
				break;
			case VIRTEX:
				partNames.add("xcv100bg256");
				partNames.add("xcv1000bg560");
				break;
			case VIRTEX2:
				partNames.add("xc2v1000bg575");
				break;
			case VIRTEX2P:
				partNames.add("xc2vp20fg676");
				partNames.add("xc2vpx20ff896");
				break;
			case VIRTEX4:
				partNames.add("xc4vfx12ff668");
				partNames.add("xc4vfx100ff1517");
				break;
			case VIRTEX5:
				partNames.add("xc5vlx20tff323");
				partNames.add("xc5vlx30ff324");
				partNames.add("xc5vfx30tff665");
				partNames.add("xc5vtx150tff1156");
				break;			
			case VIRTEX6:
				partNames.add("xc6vhx255tff1155");
				partNames.add("xc6vcx75tff484");			
				break;
			case VIRTEX7:
				partNames.add("xc7v450tffg1157");
				partNames.add("xc7vx485tffg1157");
				partNames.add("xc7v1500tfhg1157");
			case VIRTEXE:
				partNames.add("xcv2600efg1156");
				break;
			case ARTIX7:
			default:
				MessageGenerator.briefErrorAndExit("Sorry, the device family "+ familyType +
				" is currently not supported.");
		}

		
		// Create XDLRC files for the wireEnumerator
		ArrayList<String> fileNames = new ArrayList<String>();
		for(String name : partNames){
			fileNames.add(createXDLRC(name));
		}
		
		WireEnumerator we = new WireEnumerator();
		we.parseXDLRCFiles(fileNames,wireEnumeratorFileName);
		
		// Remove XDLRC files afterwards
		for(String fileName : fileNames){
			FileTools.deleteFile(fileName);
		}
		return we;
	}
	
	/**
	 * Creates a tileMap for XDLRC parsing and creating device.  This creates a brief
	 * version of the XDLRC to get tile names with coordinates.
	 * @param partName The name of the part to create the tileMap for.
	 * @return The created tile map
	 */
	public static HashMap<String,Integer> createDeviceTileMap(String partName){
		partName = PartNameTools.removeSpeedGrade(partName);
		HashMap<String,Integer> tileMap = new HashMap<String, Integer>();
		String xdlrcFileName = partName + "_brief.xdlrc";
		if(!RunXilinxTools.generateBriefXDLRCFile(partName, xdlrcFileName)){
			return null;
		}
		try{
			BufferedReader br = new BufferedReader(new FileReader(xdlrcFileName));
			String line = null;
			while((line = br.readLine()) != null) {
				String[] tokens = line.split("\\s+");
				if(tokens.length > 1 && tokens[1].equals("(tile")){
					int loc = Integer.parseInt(tokens[2]) << 16 | Integer.parseInt(tokens[3]);
					tileMap.put(tokens[4], loc);
				}
			}
			br.close();
		}
		catch(IOException e){
			MessageGenerator.briefErrorAndExit("Error Parsing XDLRC for TileMap Creation");
		}
		
		// Delete the brief xdlrcFile
		FileTools.deleteFile(xdlrcFileName);
		
		return tileMap;
	}
	
	/**
	 * Creates the device and primitive defs specified by partName. 
	 * @param partName Name of the part to create
	 * @param we Wire Enumerator corresponding to partName's family.
	 */
	public static void createDevice(String partName, WireEnumerator we){
		String deviceFileName = FileTools.getDeviceFileName(partName);
		String primitiveDefsFileName = FileTools.getPrimitiveDefsFileName(partName);
		boolean createPrimitiveDefs = !new File(primitiveDefsFileName).exists();
		if(new File(deviceFileName).exists() && FileTools.getFileSize(deviceFileName) > 1000){
			if(FileTools.getDeviceVersion(partName).equals(Device.deviceFileVersion)){
				return;
			}
			else{
				MessageGenerator.briefMessage("Warning: existing device file for " + partName +
					" is not compatible with current version of tools.  Overwritting the existing" +
					" file with new version.");
			}
		}
		
		// Create XDLRC File if it already hasn't been created
		String xdlrcFileName = createXDLRC(partName);
		try{
			// Initialize Parser
			XDLRCParser parser = new XDLRCParser();
			
			// Parse XDLRC File
			parser.parseXDLRC(xdlrcFileName, createPrimitiveDefs);
			
			// Write out primitiveDefs
			if(createPrimitiveDefs){
				FileTools.saveToCompressedFile(parser.getPrimitiveDefs(), primitiveDefsFileName);
			}
			
			// Write the Device to File
			parser.getDevice().writeDeviceToCompactFile(deviceFileName);
			
			// Remove backwards edges
			removeBackwardsEdgesFromDevice(partName);
			
			// Delete XDLRC file
			FileTools.deleteFile(xdlrcFileName);
			
		}
		catch(OutOfMemoryError e){
			System.out.println("The JVM ran out of memory, this parser needs a lot of memory.\n" +
					"Try -Xmx1600M as a parameter to java.");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Helper method to remove wires from an array and return a new array without the wire.
	 * @param currentArray The current array.
	 * @param toBeRemoved The wire to be removed.
	 * @return A new array without the wire toBeRemoved.
	 */
	private static WireConnection[] removeWire(WireConnection[] currentArray, WireConnection toBeRemoved){
		if(currentArray == null || currentArray.length == 0){
			return new WireConnection[0];
		}
		if(currentArray.length == 1 && currentArray[0].equals(toBeRemoved)){
			return new WireConnection[0];
		}
		int idx = -1;
		for(int i = 0; i < currentArray.length; i++){
			if(currentArray[i].equals(toBeRemoved)){
				idx = i;
				break;
			}
		}
		if(idx == -1){
			return currentArray;
		}
		
		WireConnection[] newArray = new WireConnection[currentArray.length-1];
		int i = 0;
		for(int j = 0; j < currentArray.length; j++){
			if(j != idx){
				newArray[i] = currentArray[j];
				i++;
			}
		}
		return newArray;
	}
	
	/**
	 * This method will read in a device files and remove backward edges from the wire
	 * connections in each tile.  This makes routing easier as it removes edges that don't
	 * exist in the FPGA.
	 * @param partName Name of the part to remove edges from.
	 */
	public static void removeBackwardsEdgesFromDevice(String partName){
		Device dev = FileTools.loadDevice(partName);
		WireEnumerator we = FileTools.loadWireEnumerator(partName);
		
		// Traverse the entire device and find which wires to remove first
		HashMap<Tile,ArrayList<Connection>> wiresToBeRemoved = new HashMap<Tile, ArrayList<Connection>>(); 
		for(Tile[] tileArray : dev.getTiles()){
			for(Tile t : tileArray){
				if(t.getWireHashMap() == null) continue;
				ArrayList<Connection> connectionsToRemove = new ArrayList<Connection>();
				// Create a set of wires that can be driven by other wires within the tile
				// We need this to do a fast look up later on
				HashSet<Integer> wiresSourcedByTileWires = new HashSet<Integer>();
				for(WireConnection[] wireArray : t.getWireHashMap().values()){
					for(WireConnection w : wireArray){
						if(w.getColumnOffset() == 0 && w.getRowOffset() == 0){
							wiresSourcedByTileWires.add(w.getWire());
						}
					}
				}
				
				for(Integer wire : t.getWires()){
					for(WireConnection w : t.getWireConnections(wire)){
						// Check if this wire has connections back to wire
						Tile wireTile = w.getTile(t);
						WireConnection[] wireConns = wireTile.getWireConnections(w.getWire());
						if(wireConns == null) continue;
						boolean backwardsConnection = false;
						
						for(WireConnection w2 : wireConns){
							Tile check = w2.getTile(wireTile);
							if(check.equals(t) && w2.getWire() == wire.intValue()){
								backwardsConnection = !wiresSourcedByTileWires.contains(wire); 
							}
						}

						// Long lines are the only bi-directional wires, keep those connections
						if(we.getWireType(w.getWire()).equals(WireType.LONG)){
							backwardsConnection = false;
						}

						if(backwardsConnection){
							connectionsToRemove.add(new Connection(wire, w));
						}
					}
				}
				wiresToBeRemoved.put(t, connectionsToRemove);
			}
		}
		
		// Remove all backward edges from device
		for(Tile t : wiresToBeRemoved.keySet()){
			for(Connection c : wiresToBeRemoved.get(t)){
				WireConnection[] currentWires = t.getWireHashMap().get(c.getWire());
				currentWires = removeWire(currentWires, c.getDestinationWire());
				t.getWireHashMap().put(c.getWire(), currentWires);
			}
		}
		
		// Add all wires to wirePool for file creation
		for(Tile[] tileArray : dev.tiles){
			for(Tile t : tileArray){
				if(t.getWireHashMap() == null) continue;
				for(WireConnection[] wires : t.getWireHashMap().values()){
					if(wires == null) continue;
					for(int i = 0; i < wires.length; i++){
						wires[i] = dev.wirePool.add(wires[i]);
					}
				}
			}
		}
		
		// Rebuild pools for file creation
		for(Tile[] tileArray : dev.tiles){
			for(Tile t : tileArray){
				dev.incrementalRemoveDuplicateTileResources(t, we);
			}
		}
		for(WireConnection w : dev.routeThroughMap.keySet()){
			PIPRouteThrough p = dev.routeThroughPool.add(dev.getRouteThrough(w));
			dev.routeThroughMap.put(w, p);
		}
		dev.createWireConnectionEnumeration();
		dev.removeDuplicatePrimitivePinMaps();
		dev.populateSinkPins(we);
		dev.removeDuplicateTileSinks(we);
		dev.debugPoolCounts();
		
		// Overwrite old file
		dev.writeDeviceToCompactFile(FileTools.getDeviceFileName(partName));
	}
	
	/**
	 * Main method for this class that ensures all part files are created for the 
	 * partName given.
	 * @param partName The name of the device to generate Device, PrimitiveDefs and Wire 
	 * Enumerator files for.
	 */
	public static void createPartFiles(String partName){
		// Create Wire Enumerator if it already hasn't been created
		WireEnumerator we = DeviceFilesCreator.createWireEnumerator(partName);
		
		// Create Device and PrimitiveDefs if they already haven't been created
		DeviceFilesCreator.createDevice(partName, we);		
	}
	
	/**
	 * Creates the appropriate primitive defs
	 * @param args The first argument should be the Xilinx part name with package and speed grade
	 */
	public static void main(String args[]){
		if(args.length != 1){
			MessageGenerator.briefMessageAndExit("USAGE: <Xilinx partname>");
		}
		
		createPartFiles(args[0]);
	}
}
