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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.DeviceFilesCreator;

/**
 * This class is used to generate the device files used in rapidSmith.  It generally only is
 * run once to parse XDLRC and generate the devices files for all devices in a particular 
 * architecture.  Multiple families can be specified at a time.
 * @author Chris Lavin
 * Created on: May 3, 2010
 */
public class GenerateDeviceLibraries {

	/**
	 * Generates all the unique Xilinx packaged named devices in the families specified by
	 * families
	 * @param families Names of architectures (virtex4, virtex5,...) for 
	 * which part names are generated.
	 * @return An ArrayList of Strings of all unique parts with unique packages of specified 
	 * Xilinx families.
	 */
	public static ArrayList<String> generatePartNames(String[] families){
		if(families == null){
			return null;
		}
		
		String line;
		String lastPartName =null;
		String[] tokens;
		Process p;
		BufferedReader input;
		ArrayList<String> partNames = new ArrayList<String>();
		try {
			for(int i=0; i < families.length; i++){
				p = Runtime.getRuntime().exec("partgen -arch " + families[i]);
				input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				while((line = input.readLine()) != null){
					tokens = line.split("\\s+");
					
					if(tokens.length > 0 && tokens[0].startsWith("xc")){
						lastPartName = tokens[0];
					}
					else{
						if(lastPartName != null){
							partNames.add(lastPartName+tokens[1]);
						}
					}
				}
				
				if(p.waitFor() != 0){
					System.out.println("Part name generation failed, are the Xilinx tools on your path?");
					return null;				
				}
			}
		} 
		catch (IOException e) {
			System.out.println("Part name generation failed");
			return null;
		}
		catch (InterruptedException e) {
			System.out.println("Part name generation failed");
			return null;
		}
		return partNames;
	}	
	
	public static void main(String[] args){
		if(args.length == 0){
			MessageGenerator.briefMessageAndExit("USAGE: <Xilinx Family Name(s) (ex: virtex4)>");
		}
				
		for(String familyNames : args){
			FileTools.makeDir(familyNames);
			ArrayList<String> partNames = generatePartNames(args);
			for(String name : partNames){
				System.out.println(name);
				String[] partName = {name};
				DeviceFilesCreator.main(partName);				
			}
		}
	}
}
