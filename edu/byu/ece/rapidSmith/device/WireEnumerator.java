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

import java.io.*;
import java.util.*;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

import edu.byu.ece.rapidSmith.device.helper.WireExpressions;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.PartNameTools;

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
	/** Set of all pip sink wires */
	private HashSet<String> pipSinks;
	/** Set of all pip sink wires */
	private HashSet<String> pipSources;
	/** Keeps track of unique copy in memory */
	private static WireEnumerator singleton = null;
	/** Xilinx FPGA family name (virtex4, virtex5, ...) */
	private FamilyType familyType = null;

	private List<String> tokens;
	
	public static final String wireEnumeratorVersion = "0.2";
	
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
		if(singleton == null){
			return singleton = new WireEnumerator();
		}
		
		FamilyType baseFamilyType = 
			PartNameTools.getBaseTypeFromFamilyType(singleton.familyType);
		familyType = PartNameTools.getBaseTypeFromFamilyType(familyType);
		if(!baseFamilyType.equals(familyType)){
			singleton = new WireEnumerator();
		}
		return singleton;
	}

	/**
	 * Reads a line and splits it into parts.
	 * @return The next line from the file, null if EOF.
	 */
	private String readLine(BufferedReader br){
		String line = null;
		try{
			line = br.readLine();
		}
		catch(IOException e){
			MessageGenerator.briefErrorAndExit("Error parsing XDLRC file.");
		}
		if(line != null){
			tokens = split(line);
		}
		return line;
	}

	private static List<String> split(String line) {
		List<String> parts = new ArrayList<>();
		int startIndex = 0, endIndex;

		if (line.length() > 0 && line.charAt(0) == '\t') {
			parts.add("");
			startIndex = 1;
			while (line.length() > startIndex && line.charAt(startIndex) == '\t')
				startIndex++;
		}

		while (line.length() > startIndex) {
			endIndex = line.indexOf(" ", startIndex);
			if (endIndex == -1) {
				parts.add(line.substring(startIndex));
				break;
			} else if (endIndex != startIndex) {
				parts.add(line.substring(startIndex, endIndex));
			}
			startIndex = endIndex + 1;
		}

		return parts;
	}

	/**
	 * Parses the XDLRC files for all the wire names in a device architecture.
	 * @param fileNames The name of the XDLRC file to parse
	 * @param outputFileName Name of the output wire enumerator file.
	 */
	public void parseXDLRCFiles(ArrayList<String> fileNames, String outputFileName){
		SortedSet<String> wireSet = new TreeSet<>();
		WireExpressions wireExp = new WireExpressions();
		wireMap = new HashMap<>();
		pipSinks = new HashSet<>();
		pipSources = new HashSet<>();
		BufferedReader in;
		String line;
		int lineCount;
		int tileCount = 0;
		int pipCount = 0;
		HashSet<String> externalInpin = new HashSet<>();
		HashSet<String> externalOutpin = new HashSet<>();
		
		for(String fileName : fileNames){
		
			System.out.println(" WireEnumerator Parsing " + fileName);
			lineCount = 0;
			try {
				in = new BufferedReader(new FileReader(fileName));
				while((line = readLine(in)) != null){
					if (tokens.size() > 0 && tokens.get(0).equals("(primitive_defs"))
						break;
					if(tokens.size() > 1){
						switch (tokens.get(1)) {
							case "(pip":
								wireSet.add(tokens.get(3));
								pipSources.add(tokens.get(3));
								pipCount++;

								if (tokens.get(5).endsWith(")")) {
									wireSet.add(tokens.get(5).substring(0, tokens.get(5).length() - 1));
									pipSinks.add(tokens.get(5).substring(0, tokens.get(5).length() - 1));
								} else {
									wireSet.add(tokens.get(5));
									pipSinks.add(tokens.get(5));
								}
								break;
							case "(wire":
								if (tokens.get(2).charAt(tokens.get(2).length() - 1) == ')') {
									wireSet.add(tokens.get(2).substring(0, tokens.get(2).length() - 1));
								} else {
									wireSet.add(tokens.get(2));
								}
								break;
							case "(conn":
								if (tokens.get(3).charAt(tokens.get(3).length() - 1) == ')') {
									wireSet.add(tokens.get(3).substring(0, tokens.get(3).length() - 1));
								} else {
									wireSet.add(tokens.get(3));
								}
								break;
							case "(pinwire":
								if (tokens.get(3).equals("input")) {
									externalInpin.add(tokens.get(4).substring(0, tokens.get(4).length() - 1));
								} else {
									externalOutpin.add(tokens.get(4).substring(0, tokens.get(4).length() - 1));
								}
								break;
						}
					}
					
					// Read next line
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
			
			if(externalInpin.contains(str) || externalOutpin.contains(str)){
				if(externalInpin.contains(str)){
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
			/* public static final String wireEnumeratorVersion;     */
			//=======================================================//
			hos.writeString(wireEnumeratorVersion);
			
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
			int[] pips = new int[pipSources.size()];
			int j=0;
			for(String s : pipSources){
				pips[j] = getWireEnum(s);
				j++;
			}
			if(!FileTools.writeIntArray(hos, pips)){
				System.out.println("Failed to write out pipWires.");
				return false;
			}

			pips = new int[pipSinks.size()];
			j=0;
			for(String s : pipSinks){
				pips[j] = getWireEnum(s);
				j++;
			}
			if(!FileTools.writeIntArray(hos, pips)){
				System.out.println("Failed to write out pipWires.");
				return false;
			}

			hos.close();
		} catch (IOException e){
			MessageGenerator.briefErrorAndExit("Error writing to file: " + fileName);
		}
		
		return true;
	}
	
	public boolean readCompactEnumFile(String fileName, FamilyType familyType){
		// Set the family name for this wire enumerator
		this.familyType = familyType;
		
		try {
			Hessian2Input his = FileTools.getInputStream(fileName);
 			
			//=======================================================//
			/* public static final String wireEnumeratorVersion;     */
			//=======================================================//
			String check = his.readString();
//			if(!check.equals(wireEnumeratorVersion)){
//				MessageGenerator.briefErrorAndExit("Error, the current version " +
//					"of RAPIDSMITH is not compatible with the wire enumerator " +
//					"file(s) present on this installation.  Delete the 'device' " +
//					"directory and run the Installer again to regenerate new wire" +
//					" enumerator files.\nCurrent RAPIDSMITH wire enumerator file " +
//					"version: " + wireEnumeratorVersion +", existing device file " +
//					"version: " + check + ".");
//			}
			
			//=======================================================//
			/* private String[] wireArray;                           */
			//=======================================================//
			wireArray = FileTools.readStringArray(his);

			//=======================================================//
			/* private HashMap<String,Integer> wireMap;              */
			//=======================================================//
			wireMap = new HashMap<>();
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
			/* private HashSet<String> pipSources;                 */
			//=======================================================//
			pipSources = new HashSet<>();
			for(int i : FileTools.readIntArray(his)){
				pipSources.add(wireArray[i]);
			}

			if (check.equals("0.2")) {
				//=======================================================//
			/* private HashSet<String> pipSinks;                 */
				//=======================================================//
				pipSinks = new HashSet<>();
				for (int i : FileTools.readIntArray(his)) {
					pipSinks.add(wireArray[i]);
				}
			}
			his.close();
		} catch (FileNotFoundException e){
			MessageGenerator.briefErrorAndExit("Error: could not find file: " + fileName);
		} catch (IOException e){
			MessageGenerator.briefErrorAndExit("Error reading in compactEnum file.");
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
	 * Checks if a particular wire is the source of a PIP
	 * @param wire The name of the wire to check if it is a PIP
	 * @return True if the wire is the source of a PIP, false otherwise.
	 */
	public boolean isPIPSourceWire(String wire){
		return pipSources.contains(wire);
	}
	public boolean isPIPSourceWire(int wire){
		return pipSources.contains(getWireName(wire));
	}

	/**
	 * Checks if a particular wire is the sink of a PIP
	 * @param wire The name of the wire to check if it is a PIP
	 * @return True if the wire is the sink of a PIP, false otherwise.
	 */
	public boolean isPIPSinkWire(String wire){
		return pipSinks.contains(wire);
	}
	public boolean isPIPSinkWire(int wire){
		return pipSinks.contains(getWireName(wire));
	}

	public boolean isPIPWire(String wire) {
		return isPIPSinkWire(wire) || isPIPSourceWire(wire);
	}

	public boolean isPIPWire(int wire) {
		return isPIPSinkWire(wire) || isPIPSourceWire(wire);
	}

	public String[] getWires() {
		return wireArray;
	}
	
	public static void main(String[] args){
		if(args.length != 1){
			MessageGenerator.briefMessageAndExit("USAGE: <partname>");
		}
		WireEnumerator we = FileTools.loadWireEnumerator(args[0]);
		for (int i = 0; i < we.wireArray.length; i++) {
			System.out.println(i + " " + we.wireArray[i]);
		}
	}
}
