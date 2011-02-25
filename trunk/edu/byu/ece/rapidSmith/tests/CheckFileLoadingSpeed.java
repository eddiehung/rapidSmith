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

import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

public class CheckFileLoadingSpeed {

	
	public static void main(String[] args) {
		if(args.length > 2 || args.length == 0){
			MessageGenerator.briefMessageAndExit(
				"USAGE: [-c | --compressed] <serializedFileName>");
		}
		if(args[0].contains("-c")){
			long start = System.currentTimeMillis();
			Object o = FileTools.loadFromCompressedFile(args[1]);
			long stop = System.currentTimeMillis();
			System.out.println("Loaded compressed file " + args[1] + " in " +
					(stop-start) + " ms (" + o.getClass().getCanonicalName() + ")");
		}
		else{
			long start = System.currentTimeMillis();
			Object o = FileTools.loadFromFile(args[0]);
			long stop = System.currentTimeMillis();
			System.out.println("Loaded file " + args[0] + " in " +
					(stop-start) + " ms (" + o.getClass().getCanonicalName() + ")");
		}
	}
}
