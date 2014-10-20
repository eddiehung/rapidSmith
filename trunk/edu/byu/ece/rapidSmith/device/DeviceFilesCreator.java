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

import edu.byu.ece.rapidSmith.device.helper.Connection;
import edu.byu.ece.rapidSmith.device.helper.HashPool;
import edu.byu.ece.rapidSmith.device.helper.WireHashMap;
import edu.byu.ece.rapidSmith.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
		
		ArrayList<String> partNames = new ArrayList<>();
		switch(familyType){
			case KINTEX7:
				partNames.add("xc7k160tfbg676");
				partNames.add("xc7k355tffg901");
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
				partNames.add("xc7vx330tffg1157");
				partNames.add("xc7vx415tffg1157");
				partNames.add("xc7vx485tffg1157");
				partNames.add("xc7vh580thcg1155");
				partNames.add("xc7vh870thcg1931");
				break;
			case VIRTEXE:
				partNames.add("xcv2600efg1156");
				break;
			case ZYNQ:
				partNames.add("xc7z010clg400");
				partNames.add("xc7z020clg400");
				partNames.add("xc7z030fbg676");
				partNames.add("xc7z045fbg676");
				partNames.add("xc7z100ffg1156");
				break;
			case ARTIX7:
				partNames.add("xc7a100tcsg324");
				partNames.add("xc7a200tfbg484");
				break;
			default:
				MessageGenerator.briefErrorAndExit("Sorry, the device family "+ familyType +
				" is currently not supported.");
		}

		
		// Create XDLRC files for the wireEnumerator
		ArrayList<String> fileNames = new ArrayList<>();
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
		HashMap<String,Integer> tileMap = new HashMap<>();
		String xdlrcFileName = partName + "_brief.xdlrc";
		if(!RunXilinxTools.generateBriefXDLRCFile(partName, xdlrcFileName)){
			return null;
		}
		try{
			BufferedReader br = new BufferedReader(new FileReader(xdlrcFileName));
			String line;
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

			// reload the device
			Device dev = FileTools.loadDevice(partName);

			// Remove backwards edges
			addMissingWireConnections(dev, we);
			removeBackwardsEdgesFromDevice(dev, we);

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
	 */
	public static void removeBackwardsEdgesFromDevice(Device dev, WireEnumerator we){
		HashPool<WireHashMap> tileWiresPool = new HashPool<>();

		// Traverse the entire device and find which wires to remove first
		for(Tile tile : dev.getTileMap().values()) {
			if (tile.getWireHashMap() == null)
				continue;

			// create a safe wire map to modify
			WireHashMap wireHashMap = new WireHashMap(tile.getWireHashMap());

			// Create a set of wires that can be driven by other wires within the tile
			// We need this to do a fast look up later on
			Set<Integer> sourceWires = getSourceWiresOfTile(we, tile);

			// Identify any wire connections that are not a "source" wire to "sink" wire
			// connection.
			Set<Integer> wires = new HashSet<>(tile.getWires());
			List<Connection> wiresToBeRemoved = new ArrayList<>();
			for (Integer wire : wires) {
				for (WireConnection wc : tile.getWireConnections(wire)) {
					// never remove PIPs.  We only are searching for different names
					// of the same wire.  A PIP connect unique wires.
					if (wc.isPIP())
						continue;
					if (!sourceWires.contains(wire) ||
							!wireIsSink(we, wc.getTile(tile), wc.getWire())) {
						wiresToBeRemoved.add(new Connection(wire, wc));
					}
				}
			}

			// Remove the edges by creating a new WireConnection arrays sans the
			// connection of interest.
			for (Connection c : wiresToBeRemoved) {
				WireConnection[] currentWires = wireHashMap.get(c.getWire());
				currentWires = removeWire(currentWires, c.getDestinationWire());
				wireHashMap.put(c.getWire(), currentWires);
			}
			// Update the tile with the new wire map.  Search for possible reuse.
			tile.setWireHashMap(tileWiresPool.add(wireHashMap));
		}
	}

	private static Set<Integer> getSourceWiresOfTile(WireEnumerator we, Tile tile) {
		Set<Integer> sourceWires = new HashSet<>();
		for(Integer wire : tile.getWires()){
			if (we.getWireType(wire) == WireType.SITE_SOURCE)
				sourceWires.add(wire);
			for(WireConnection wc : tile.getWireConnections(wire)){
				if(wc.isPIP()){
					sourceWires.add(wc.getWire());
				}
			}
		}
		return sourceWires;
	}

	private static void addMissingWireConnections(Device dev, WireEnumerator we) {
		HashPool<WireHashMap> tileWiresPool = new HashPool<>();

		for (Tile tile : dev.getTileMap().values()) {
			if (tile.getWireHashMap() == null)
				continue;

			// Create a safe copy to playaround with without messing up any other tiles wires
			WireHashMap whm = new WireHashMap(tile.getWireHashMap());

			// Traverse all non-PIP wire connections starting at this source wire.  If any
			// such wire connections lead to a sink wire that is not already a connection of
			// the source wire, mark it to be added as a connection
			for (Integer wire : whm.keySet()) {
				Set<WireConnection> wcToAdd = new HashSet<>();
				Set<WireConnection> checkedConnections = new HashSet<>();
				Queue<WireConnection> connectionsToFollow = new LinkedList<>();

				// Add the wire to prevent building a connection back to itself
				checkedConnections.add(new WireConnection(wire, 0, 0, false));
				for (WireConnection wc : whm.get(wire)) {
					if (!wc.isPIP()) {
						checkedConnections.add(wc);
						connectionsToFollow.add(wc);
					}
				}

				while (!connectionsToFollow.isEmpty()) {
					WireConnection midwc = connectionsToFollow.remove();
					Tile midTile = midwc.getTile(tile);
					Integer midWire = midwc.getWire();

					// Dead end checks
					if (midTile.getWireHashMap() == null || midTile.getWireConnections(midWire) == null)
						continue;

					for (WireConnection sinkwc : midTile.getWireConnections(midWire)) {
						if (sinkwc.isPIP()) continue;

						Integer sinkWire = sinkwc.getWire();
						Tile sinkTile = sinkwc.getTile(midTile);
						int colOffset = midwc.getColumnOffset() + sinkwc.getColumnOffset();
						int rowOffset = midwc.getRowOffset() + sinkwc.getRowOffset();

						// This represents the wire connection from the original source to the sink wire
						WireConnection source2sink = new WireConnection(sinkWire, rowOffset, colOffset, false);
						boolean wirePreviouslyChecked = !checkedConnections.add(source2sink);

						// Check if we've already processed this guy and process him if we haven't
						if (wirePreviouslyChecked)
							continue;
						connectionsToFollow.add(source2sink);

						// Only add the connection if the wire is a sink.  Other connections are
						// useless for wire traversing.
						if (wireIsSink(we, sinkTile, sinkWire))
							wcToAdd.add(source2sink);
					}
				}

				// If there are wires to add, add them here by creating a new WireConnection array
				// combining the old and new wires.
				if (!wcToAdd.isEmpty()) {
					List<WireConnection> newConnections = new ArrayList<>(Arrays.asList(whm.get(wire)));
					newConnections.addAll(wcToAdd);
					WireConnection[] arrView = newConnections.toArray(
							new WireConnection[newConnections.size()]);
					whm.put(wire, arrView);
				}
			}
		}
	}

	// A wire is a sink if it is a site source (really should check in the tile sinks but
	// the wire type check is easier and should be sufficient or the wire is the source of
	// a PIP.
	private static boolean wireIsSink(WireEnumerator we, Tile tile, Integer wire) {
		if (we.getWireType(wire) == WireType.SITE_SINK)
			return true;
		if (tile.getWireHashMap() == null || tile.getWireConnections(wire) == null)
			return false;
		for (WireConnection wc : tile.getWireConnections(wire)) {
			if (wc.isPIP())
				return true;
		}
		return false;
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
