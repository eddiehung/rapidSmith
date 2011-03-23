/*
 * Copyright (c) 2010-2011 Brigham Young University
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

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

public class CheckDesignMemoryUsage {
	public static void main(String[] args) {
		if(args.length != 1){
			MessageGenerator.briefMessageAndExit("USAGE: <designFile.xdl>");
		}
		// Measure Initial Heap Size
		Runtime rt = Runtime.getRuntime();
		System.gc();
		long initial_usage = rt.totalMemory() - rt.freeMemory();

		// Start Timer
		long start = System.nanoTime();
		
		Design design = new Design(args[0]);
		
		// Stop Timer
		long stop = System.nanoTime();
		
		// Measure Final Heap Size
		System.gc();
		long total_usage = rt.totalMemory() - rt.freeMemory() - initial_usage;
		
		System.out.printf("Loaded %s design in %5.3f seconds using %d MBs of heap space.%s", 
			design.getPartName(), (stop-start)/1000000000.0, total_usage/(1024*1024), System.getProperty("line.separator"));
	}
}
