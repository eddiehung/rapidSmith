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
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

import edu.byu.ece.rapidSmith.device.helper.WireExpressions;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.FileTools;

/**
 * This utility class creates and governs the translation of the wires enumeration value from its
 * actual name.  This enumeration is done so that we can reduce the overhead of manipulating,
 * comparing and storing strings.  Using integers is much more efficient.  This is a by-family enumeration
 * of Xilinx FPGAs.  Each Xilinx FPGA family will require its own enumerator class.
 * @author Chris Lavin
 */
public class WireEnumerator implements Serializable {

	private static final long serialVersionUID = 2388225105007691213L;

	/** A list of all wire names where the index value is the enumeration value */
	private String[] wireArray;
	/** An array used for conversion of wire name to wire enumeration */
	private HashMap<String,Integer> wireMap;
	/** An array used for conversion of wire enumeration to wire name */
	private WireType[] wireTypeArray;
	/** An array that returns the direction of a wire based on wire enumeration value */
	private WireDirection[] wireDirectionArray;
	/** Set of all pip wire names */
	private HashSet<String> pipWireNames;
	/** Keeps track of unique copy in memory */
	private static WireEnumerator singleton = null;
	/** Xilinx FPGA family name (virtex4, virtex5, ...) */
	private FamilyType familyType = null;
	
	/**
	 * Constructor, does not initialize anything
	 */
	public WireEnumerator(){
	}
	
	/**
	 * This method will either return the currently loaded wire enumerator
	 * if the wire enumerator exists in memory, or will return a new empty
	 * wire enumerator.
	 * @param familyType The base family type to be loaded.
	 * @return A new wire enumerator or the currently matching loaded wire enumerator.
	 */
	public static WireEnumerator getInstance(FamilyType familyType){
		if(singleton == null || !singleton.familyType.equals(familyType)){
			singleton = new WireEnumerator();
		}
		return singleton;
	}
	
	/**
	 * Parses the XDLRC files for all the wire names in a device architecture.
	 * @param fileNames The name of the XDLRC file to parse
	 * @param outputFileName Name of the output wire enumerator file.
	 */
	public void parseXDLRCFiles(ArrayList<String> fileNames, String outputFileName){
		SortedSet<String> wireSet = new TreeSet<String>();
		WireExpressions wireExp = new WireExpressions();
		wireMap = new HashMap<String,Integer>();
		pipWireNames = new HashSet<String>();
		BufferedReader in;
		String line;
		int lineCount = 0;
		int tileCount = 0;
		int pipCount = 0;
		HashSet<String> internalInpin = new HashSet<String>();
		HashSet<String> internalOutpin = new HashSet<String>();
		HashSet<String> externalInpin = new HashSet<String>();
		HashSet<String> externalOutpin = new HashSet<String>();
		
		for(String fileName : fileNames){
		
			System.out.println(" WireEnumerator Parsing " + fileName);
			lineCount = 0;
			try {
				in = new BufferedReader(new FileReader(fileName));
				line = in.readLine();
				String [] tokens;
				while(line != null){
					tokens = line.split("\\s+");
					if(tokens.length > 1){
						if(tokens[1].equals("(pip")){
							if(tokens[3].endsWith(")")){
								wireSet.add(tokens[3].substring(0, tokens[3].length()-1));
								pipWireNames.add(tokens[3].substring(0, tokens[3].length()-1));
								pipCount++;
							}
							else{
								wireSet.add(tokens[3]);
								pipWireNames.add(tokens[3]);
								pipCount++;
							}
							if(tokens[5].endsWith(")")){
								wireSet.add(tokens[5].substring(0, tokens[5].length()-1));
								pipWireNames.add(tokens[5].substring(0, tokens[5].length()-1));
							}
							else{
								wireSet.add(tokens[5]);
								pipWireNames.add(tokens[5]);
							}
						}
						else if(tokens[1].equals("(wire")){
							if(tokens[2].charAt(tokens[2].length()-1) == ')'){
								wireSet.add(tokens[2].substring(0, tokens[2].length()-1));
							}
							else{
								wireSet.add(tokens[2]);
							}					
						}
						else if(tokens[1].equals("(conn")){
							if(tokens[3].charAt(tokens[3].length()-1) == ')'){
								wireSet.add(tokens[3].substring(0, tokens[3].length()-1));
							}
							else{
								wireSet.add(tokens[3]);
							}					
						}
						else if(tokens[1].equals("(pinwire")){
							if(tokens[3].equals("input")){
								internalInpin.add(tokens[2]);
								externalInpin.add(tokens[4].substring(0, tokens[4].length()-1));
							}
							else{
								internalOutpin.add(tokens[2]);
								externalOutpin.add(tokens[4].substring(0, tokens[4].length()-1));
							}
						}
					}
					
					// Read next line
					line = in.readLine();
					lineCount++;
					if(lineCount % 10000000 == 0){
						System.out.println("  Processing line number " + lineCount + " of file " + fileName);
					}
				}
				in.close();
			} 
			catch (FileNotFoundException e) {
				System.out.println("Could not open file: " + fileName + " , does it exist?");
				System.exit(1);
			}
			catch (IOException e){
				System.out.println("Error reading file: " + fileName);
				System.exit(1);			
			}
		}
		System.out.println("  totalTiles = " + tileCount);
		System.out.println("  totalPIPs = " + pipCount);
		
		/*
		 * After the XDLRC file is parsed, all of the wire names are given an enumeration
		 * value in this method.  It then writes out the wire enumeration information to fileName.
		 */
		
		int i = 0;

		
		// These don't need to be in there
		wireSet.remove("0");
		wireSet.remove("1");
		
		// Create Arrays
		wireArray = new String[wireSet.size()];
		wireDirectionArray = new WireDirection[wireSet.size()];
		wireTypeArray = new WireType[wireSet.size()];
		
		for(String str : wireSet){
			wireMap.put(str, i);
			wireArray[i] = str;
			
			if(internalInpin.contains(str) || internalOutpin.contains(str) || externalInpin.contains(str) || externalOutpin.contains(str)){			
				if(internalInpin.contains(str)){
					wireTypeArray[i] = WireType.SITE_SINK;
					wireDirectionArray[i] = WireDirection.INTERNAL;
				}
				else if(internalOutpin.contains(str)){
					wireTypeArray[i] = WireType.SITE_SOURCE;
					wireDirectionArray[i] = WireDirection.INTERNAL;
				}
				else if(externalInpin.contains(str)){
					wireTypeArray[i] = WireType.SITE_SINK;
					wireDirectionArray[i] = WireDirection.EXTERNAL;
				}
				else if(externalOutpin.contains(str)) {
					wireTypeArray[i] = WireType.SITE_SOURCE;
					wireDirectionArray[i] = WireDirection.EXTERNAL;
				}
				else{
					System.out.println("This should never printout, WireEnumerator - parseXDLRC()");
				}
				
			}
			else {
				determineTypeAndDirection(str, i, wireExp);			
			}
			i++;
		}

		writeCompactEnumFile(outputFileName);
	}

	/**
	 * Gets and returns the Xilinx FPGA family name (all lower case) that 
	 * corresponds to this instance of the wire enumerator.
	 * @return The family name of this wire enumerator.
	 */
	public String getFamilyName(){
		if(familyType == null) return null;
		return familyType.toString().toLowerCase();
	}
	
	/**
	 * Gets and returns the family type enumeration that corresponds
	 * to this instance of the wire enumerator
	 * @return The family type of this wire enumerator.
	 */
	public FamilyType getFamilyType(){
		return familyType;
	}
	
	/**
	 * Given the wire name, it determines the WireDirection of the wire.
	 * @param wireName Name of the wire
	 * @param wire The enumeration value of the wire
	 */
	private void determineTypeAndDirection(String wireName, int wire, WireExpressions wireExp){
		// Determine Direction
		wireDirectionArray[wire] = wireExp.getWireDirection(wireName);
		
		// Determine Type
		wireTypeArray[wire] = wireExp.getWireType(wireName); 
	}
	
	/**
	 * Save class members to a file.  Used to save this class to a file.  Should be used with
	 * the corresponding method readCompactEnumFile().
	 * @param fileName Name of the file to be created.
	 * @return True if successful, false otherwise.
	 */
	public boolean writeCompactEnumFile(String fileName){
		try{
			Hessian2Output hos = FileTools.getOutputStream(fileName);
			
			//=======================================================//
			/* private HashMap<String,Integer> wireMap;              */
			//=======================================================//
		     	// We'll rebuild this from wireArray

			//=======================================================//
			/* private String[] wireArray;                           */
			//=======================================================//
			if(!FileTools.writeStringArray(hos, wireArray)){
				System.out.println("Failed to write out wireArray.");
				return false;
			}

			//=======================================================//
			/* private WireType[] wireTypeArray;                     */
			/* private WireDirection[] wireDirectionArray;           */
			//=======================================================//			
			int[] wireAttributes = new int[wireArray.length];
			for(int i=0; i < wireAttributes.length; i++){
				wireAttributes[i] = (getWireDirection(i).ordinal() << 16) + getWireType(i).ordinal();
			}
			if(!FileTools.writeIntArray(hos, wireAttributes)){
				System.out.println("Failed to write out wireAttributes.");
				return false;
			}
			
			//=======================================================//
			/* private HashSet<String> pipWireNames;                 */
			//=======================================================//
			int[] pips = new int[pipWireNames.size()];
			int j=0;
			for(String s : pipWireNames){
				pips[j] = getWireEnum(s);
				j++;
			}
			if(!FileTools.writeIntArray(hos, pips)){
				System.out.println("Failed to write out pipWires.");
				return false;
			}
			
			hos.close();
		} catch (IOException e){
			System.out.println("Error writing to file: " + fileName);
			System.exit(1);
		}
		
		return true;
	}
	
	public boolean readCompactEnumFile(String fileName, FamilyType familyType){
		// Set the family name for this wire enumerator
		this.familyType = familyType;
		
		try {
			Hessian2Input his = FileTools.getInputStream(fileName);
 			
			//=======================================================//
			/* private String[] wireArray;                           */
			//=======================================================//
			wireArray = FileTools.readStringArray(his);

			//=======================================================//
			/* private HashMap<String,Integer> wireMap;              */
			//=======================================================//
			wireMap = new HashMap<String,Integer>();
			for(int i=0; i < wireArray.length; i++){
				wireMap.put(wireArray[i], i);
			}
			
			//=======================================================//
			/* private WireType[] wireTypeArray;                     */
			/* private WireDirection[] wireDirectionArray;           */
			//=======================================================//			
			int[] wireAttributes = FileTools.readIntArray(his);
			wireDirectionArray = new WireDirection[wireAttributes.length];
			wireTypeArray = new WireType[wireAttributes.length];
			WireDirection[] directions = WireDirection.values();
			WireType[] types = WireType.values();
			for(int i=0; i < wireAttributes.length; i++){
				wireDirectionArray[i] =  directions[0x0000FFFF & (wireAttributes[i] >> 16)];
				wireTypeArray[i] = types[0x0000FFFF & wireAttributes[i]];
			}
			
			//=======================================================//
			/* private HashSet<String> pipWireNames;                 */
			//=======================================================//
			pipWireNames = new HashSet<String>();
			for(int i : FileTools.readIntArray(his)){
				pipWireNames.add(wireArray[i]);
			}		

			his.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error: could not find file: " + fileName);
			System.exit(1);
		} catch (IOException e) {
			System.out.println("Error reading in compactEnum file.");
			System.exit(1);
		}
		return true;
	}
		
	//========================================================================//
	// Lookup Methods
	//========================================================================//		
	/**
	 * Gets and returns the unique wire's integer enumeration based on name
	 * @param name Name of the wire to get enumeration for
	 * @return The enumeration value or -1 for a bad wire name.
	 */
	public int getWireEnum(String name){
		if(this.wireMap.containsKey(name)){
			return this.wireMap.get(name);			
		}
		return -1;
	}
	
	/**
	 * Gets a wire's name based on its enumeration value 
	 * @param wire The wire's enumeration of the wire name to be returned
	 * @return The string representation (XDL) of the wire name
	 */
	public String getWireName(int wire){
		return wireArray[wire];
	}
	
	/**
	 * Returns the WireType based on the wire's enumeration value. WireTypes are generally DOUBLE, HEX, PENT,...
	 * @param wire The wire to get the WireType for
	 * @return The WireType of the wire
	 */
	public WireType getWireType(int wire){
		return wireTypeArray[wire];
	}
	
	/**
	 * Returns the WireDirection based on the wire's enumeration value.
	 * @param wire The wire to get the WireDirection for
	 * @return The WireDirection of the wire
	 */
	public WireDirection getWireDirection(int wire){
		return wireDirectionArray[wire];
	}
	
	/**
	 * Checks if a particular wire is part of a PIP (both start or end wire)
	 * @param wire The name of the wire to check if it is a PIP
	 * @return True if the wire is part of a PIP, false otherwise.
	 */
	public boolean isPIPWire(String wire){
		return pipWireNames.contains(wire);
	}
	public boolean isPIPWire(int wire){
		return pipWireNames.contains(getWireName(wire));
	}
	
	public String[] getWires() {
		return wireArray;
	}
	//========================================================================//
	// Test Methods
	//========================================================================//
	public void writeDebugFile(String fileName){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			bw.write("=== Wire Enumerations ===\n");
			for(int i=0; i < wireArray.length; i++){
				bw.write(i + " " 
						+ wireArray[i] + " " 
						+ getWireEnum(wireArray[i]) + " " 
						+ getWireType(i) + " " 
						+ getWireDirection(i) + " " 
						+ isPIPWire(wireArray[i]) 
						+ "\n");
			}
			
			bw.write("=== Wire PIPs ===\n");
			String[] sortedPips = new String[pipWireNames.size()];
			sortedPips = pipWireNames.toArray(sortedPips);
			Arrays.sort(sortedPips);
			for(String s : sortedPips){
				bw.write(getWireEnum(s) + " " + s + " " +  getWireType(getWireEnum(s)) +
						" " + getWireDirection(getWireEnum(s)) + "\n");
			}
			
			bw.write("wires=" + wireArray.length + " pips=" + pipWireNames.size());
			bw.close();			
		} catch (IOException e) {
			System.out.println("Error: could not write out debug file: " + fileName);
			System.exit(1);
		}
	}
	
	@SuppressWarnings("unused")
	public void performanceTest(){
		int testSize = 10000000;
		Integer tmpWire;
		String tmpName;
		WireType tmpWireType;
		WireDirection tmpWireDirection;
		long start, stop;
		Random generator = new Random();
		int[] randomNumbers = new int[testSize];
		String[] randomWires = new String[testSize];
		for(int i : randomNumbers){
			i = generator.nextInt(wireArray.length);
		}
		for(String s : randomWires){
			s = getWireName(generator.nextInt(wireArray.length));
		}
		
		System.out.print("Testing getWireEnum(String name)...");
		start = System.nanoTime();
		for(String s : randomWires){
			tmpWire = this.getWireEnum(s);
		}
		stop = System.nanoTime();
		System.out.print("DONE!\n");
		System.out.println("  TOTAL:" + (stop-start) + "ns");
		System.out.println("  PER  :" + (((double)stop-(double)start)/(double)testSize) + "ns");

		System.out.print("Testing getWireName(Integer wire)...");
		start = System.nanoTime();
		for(Integer i: randomNumbers){
			tmpName = this.getWireName(i);
		}
		stop = System.nanoTime();
		System.out.print("DONE!\n");
		System.out.println("  TOTAL:" + (stop-start) + "ns");
		System.out.println("  PER  :" + (((double)stop-(double)start)/(double)testSize) + "ns");

		System.out.print("Testing getWireType(Integer wire)...");
		start = System.nanoTime();
		for(Integer i: randomNumbers){
			tmpWireType = this.getWireType(i);
		}
		stop = System.nanoTime();
		System.out.print("DONE!\n");
		System.out.println("  TOTAL:" + (stop-start) + "ns");
		System.out.println("  PER  :" + (((double)stop-(double)start)/(double)testSize) + "ns");

		System.out.print("Testing getWireDirection(Integer wire)...");
		start = System.nanoTime();
		for(Integer i: randomNumbers){
			tmpWireDirection = this.getWireDirection(i);
		}
		stop = System.nanoTime();
		System.out.print("DONE!\n");
		System.out.println("  TOTAL:" + (stop-start) + "ns");
		System.out.println("  PER  :" + (((double)stop-(double)start)/(double)testSize) + "ns");		
	}
}
