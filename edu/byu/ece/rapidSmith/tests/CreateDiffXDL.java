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

public class CreateDiffXDL {
	public static void main(String[] args){
		if(args.length != 2){
			MessageGenerator.briefMessageAndExit("USAGE: <firstXDLDesign.xdl> <secondXDLDesign.xdl>");
		}
		Design d1 = new Design(args[0]);
		Design d2 = new Design(args[1]);
		d1.saveComparableXDLFile(args[0].replace(".xdl", "_diff.xdl"));
		d2.saveComparableXDLFile(args[1].replace(".xdl", "_diff.xdl"));
	}
}
