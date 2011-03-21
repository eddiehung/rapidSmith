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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.DeviceFilesCreator;

/**
 * This class will create the device and wire enumerator files 
 * necessary for RapidSmith to operate.   
 * @author Chris Lavin
 */
public class Installer{
	public static String nl = System.getProperty("line.separator");
	public static String disclaimer = 
		"This material is based upon work supported by the National" + nl + 
		"Science Foundation under Grant No. 0801876. Any opinions," + nl + 
		"findings, and conclusions or recommendations expressed in this" + nl + 
		"material are those of the author(s) and do not necessarily" + nl + 
		"reflect the views of the National Science Foundation.";
	
	/**
	 * Parses the text file passed in through main() containing the
	 * desired parts and or families to generate device files for. The file should have 
	 * each part/family on a separate line.
	 * @param fileName Name of the part name file.
	 * @return A list of all the part/family names in the file.
	 */
	public static String[] parsePartNameFile(String fileName){
		ArrayList<String> partAndFamilyNames = new ArrayList<String>();
		try{
			BufferedReader br = new BufferedReader(new FileReader(fileName));
			String line = null;
			int lineNumber = 0;
			while((line = br.readLine()) != null){
				lineNumber++;
				String partName = line.trim();
				partAndFamilyNames.add(partName);
			}
		}
		catch(FileNotFoundException e){
			MessageGenerator.briefErrorAndExit("Error, part name file not found: " + fileName);
		}
		catch(IOException e){
			MessageGenerator.briefErrorAndExit("Error while reading part name file: " + fileName);
		}
		
		// Convert to an array
		String[] names = new String[partAndFamilyNames.size()];
		names = partAndFamilyNames.toArray(names);
		return names;
	}
	
	public static void main(String[] args){
		MessageGenerator.printHeader("RapidSmith Release " + Device.rapidSmithVersion +" - Installer");
		String[] names = null;
		long timeStart = System.currentTimeMillis();
		if(args.length == 0){
			String nl = System.getProperty("line.separator");
			
			MessageGenerator.briefMessageAndExit("  USAGE: <Xilinx Family Name(s) | Part Name(s) " +
					"| parameterFileName.txt>" + nl +
					"    EXAMPLES:" + nl +
					"      \"virtex4 virtex5\"" + nl +
					"      \"virtex4 xc5vlx20tff323\"" + nl +
					"      \"listOfpartsAndFamiliesFile.txt\" (each parameter on a separate line)" + nl);
		}
		
		System.out.println("DISCLAIMER:");
		System.out.println(disclaimer + nl + nl);
		
		System.out.println("Have you read the above disclaimer and agree to the GPLv2 license" + nl +
				"agreement accompanying this software (/docs/gpl2.txt)");
		MessageGenerator.agreeToContinue();
		
		System.out.println("START: " + FileTools.getTimeString());
		// Check if user supplied file with parameters
		File tmp = new File(args[0]);
		if(tmp.exists() && tmp.isFile()){
			names = parsePartNameFile(args[0]);
		}
		else{
			names = args;
		}
		ArrayList<String> partNames = new ArrayList<String>();
		for(String name : names){
			name = name.toLowerCase();
			if(name.startsWith("x")){
				partNames.add(name);
			}
			else{
				name = name.toUpperCase();
				partNames.addAll(RunXilinxTools.getPartNames(name, false));
			}
			
			for(String partName : partNames){
				System.out.println("Creating/Verifying files for " + partName);
				DeviceFilesCreator.createPartFiles(partName);				
			}
		}
		System.out.println("END: " + FileTools.getTimeString());
		System.out.println("Time Elapsed: " + (System.currentTimeMillis() - timeStart)/60000.0 + " minutes");
		MessageGenerator.printHeader("Installer Completed Successfully!");
	}
}

