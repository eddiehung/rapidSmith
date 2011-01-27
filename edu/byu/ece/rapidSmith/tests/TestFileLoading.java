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
package edu.byu.ece.rapidSmith.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.primitiveDefs.PrimitiveDefList;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;
import edu.byu.ece.rapidSmith.util.PartNameTools;

/**
 * This class will load a device, wire enumerator or primitive def list,
 * measure its loading time and heap size and report it.  This is merely
 * for testing purposes.
 * @author Chris Lavin
 * Created on: Jan 22, 2011
 */
public class TestFileLoading{

	/**
	 * Create a simple bash script that will run this class on every device,
	 * wire enumerator and primitive def list installed in RapidSmith.
	 * @param fileName Name of the output bash script.
	 * @return True if operation was successful, false otherwise.
	 */
	protected static boolean createTestBashScript(String fileName){
		try{
			boolean verbose = true;
			HashSet<String> availableFamilies = new HashSet<String>();
			String nl = "\n";
			String cmd = "java -Xmx1200M " + TestFileLoading.class.getCanonicalName();
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			bw.write("#!/bin/bash" + nl);

			bw.write("echo \"Devices\"" + nl);
			for(String partName : FileTools.getAvailableParts()){
				availableFamilies.add(PartNameTools.getFamilyNameFromPart(partName));
				bw.write(cmd + " -d " + partName + (verbose ? " -verbose" : "") + nl);
				verbose = false;
			}
			
			bw.write("echo \"Wire Enumerators\"" + nl);
			String[] familyNames = new String[availableFamilies.size()];
			familyNames = availableFamilies.toArray(familyNames);
			
			Arrays.sort(familyNames);
			verbose = true;
			for(String familyName : familyNames){
				bw.write(cmd + " -w " + familyName + (verbose ? " -verbose" : "") + nl);
				verbose = false;
			}
		
			bw.write("echo \"Primitive Def Lists\"" + nl);
			verbose = true;
			for(String familyName : familyNames){
				bw.write(cmd + " -p " + familyName + (verbose ? " -verbose" : "") + nl);
				verbose = false;
			}
			
			bw.write(nl);
			bw.close();
			return true;
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}
	
	
	@SuppressWarnings("unused")
	public static void main(String[] args){
		//createTestBashScript("testRapidSmithFiles.sh");
		if(args.length < 2 && args.length > 3){
			MessageGenerator.briefMessageAndExit("USAGE: -<d|w|p (d:Device, w:WireEnumerator, p:PrimitiveDefs)> <partname|familyName> [-verbose]");	
		}
		FamilyType familyType = null;
		long initial_usage, total_usage;
		long start, stop;
		Device dev;
		WireEnumerator we;
		PrimitiveDefList list;
		
		if(args[0].equals("-w") || args[0].equals("-p")){
			familyType = FamilyType.valueOf(args[1].toUpperCase());
		}
		
		// Measure Initial Heap Size
		Runtime rt = Runtime.getRuntime();
		System.gc();
		initial_usage = rt.totalMemory() - rt.freeMemory();

		// Start Timer
		start = System.nanoTime();

		if(args[0].equals("-d")){
			dev = FileTools.loadDevice(args[1]);
		}
		else if(args[0].equals("-w")){
			we = FileTools.loadWireEnumerator(familyType);
		}
		else if(args[0].equals("-p")){
			list = FileTools.loadPrimitiveDefs(familyType);
		}
		
		// Stop Timer
		stop = System.nanoTime();
		
		// Measure Final Heap Size
		System.gc();
		total_usage = rt.totalMemory() - rt.freeMemory() - initial_usage;

		// Print out header if verbose switch is passed in
		if(args.length > 2 && args[2].equals("-verbose")){
			System.out.println("--------------------------------------------------------------------");	
			System.out.println("| Family    | Part Name       | File Size | Heap Usage | Load Time |");
			System.out.println("--------------------------------------------------------------------");
		}
		
		// Calculate file size
		long fileSize = 0;
		if(args[0].equals("-d")){
			fileSize = new File(FileTools.getDeviceFileName(args[1])).length();
		}
		else if(args[0].equals("-w")){
			fileSize = new File(FileTools.getWireEnumeratorFileName(familyType)).length();
		}
		else if(args[0].equals("-p")){
			fileSize = new File(FileTools.getPrimitiveDefsFileName(familyType)).length();
		}
		
		if(args[0].equals("-d")){
			System.out.printf(" %-12s %-16s   %6dKB       %4dMB     %6.3fs\n",
					familyType==null ? PartNameTools.getExactFamilyNameFromPart(args[1]) : familyType.toString().toUpperCase(),
					args[1], 
					fileSize/1024, 
					total_usage/(1024*1024),
					((stop-start)/1000000000.0));
			
		}
		else{
			System.out.printf(" %-12s %-16s   %6dKB       %4.1fMB     %6.3fs\n",
					familyType==null ? PartNameTools.getExactFamilyNameFromPart(args[1]) : familyType.toString().toUpperCase(),
					" ", 
					fileSize/1024, 
					total_usage/(1024.0*1024.0),
					((stop-start)/1000000000.0));
			
		}
	}
}
