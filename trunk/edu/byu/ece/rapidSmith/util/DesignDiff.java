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

import java.util.ArrayList;
import java.util.Arrays;

import edu.byu.ece.rapidSmith.design.Attribute;
import edu.byu.ece.rapidSmith.design.Design;

public class DesignDiff{

	private boolean identical = true;
	
	public void printDifference(String name, String design1, String design2) {
		System.out.println(name +" = ");
		System.out.println("    Design 1: <"+design1+">");
		System.out.println("    Design 2: <"+design2+">");
		identical = false;
	}
	
	public boolean compareAttributes(ArrayList<Attribute> a1, ArrayList<Attribute> a2, String verbose){
		if(a1 == null && a2 == null){
			return true;
		}
		else if(a1 == null){
			if(verbose != null){
				printDifference("Attributes in "+verbose+ " don't match", "null", "non-null");
			}
			return false;
		}
		else if(a2 == null) {
			if(verbose != null){
				printDifference("Attributes in "+verbose+ " don't match", "non-null", "null");
			}
		}
		else if(a1.size() == 0 && a2.size() == 0){
			return true;
		}
		else if(a1.size() != a2.size()){
			if(verbose != null) printDifference("Number of Attributes in " + verbose +
					"differ", Integer.toString(a1.size()), Integer.toString(a2.size()));
			return false; 
		}
		
		String[] a1Strings = new String[a1.size()];
		String[] a2Strings = new String[a2.size()];
		for(int i = 0; i < a1Strings.length; i++){
			a1Strings[i] = a1.get(i).toString();
			a2Strings[i] = a2.get(i).toString();
		}
		Arrays.sort(a1Strings);
		Arrays.sort(a2Strings);
		for(int i = 0; i < a1Strings.length; i++){
			if(!a1Strings[i].equals(a2Strings[i])){
				if(verbose != null) printDifference("Attributes in " + verbose + " differ", a1Strings[i], a2Strings[i]);
				return false;				
			}
		}
		return true;
	}
	
	
	/**
	 * This method will compare two designs and optionally print the differences found. Unless 
	 * the parameter verbose is specified, the method will return at the first sign of a difference.
	 * @param design1 First design to compare.
	 * @param design2 Second design to compare.
	 * @param verbose A flag which indicates to print all differences found between the two designs
	 * @return True if the designs are identical, false otherwise.
	 */
	public boolean compareDesigns(Design design1, Design design2, boolean verbose){
		// Compare Design elements
		if(!design1.getName().equals(design2.getName())){
			if(verbose)	printDifference("Design Name", design1.getName(), design2.getName());
			else return false;
		}
		if(!design1.getPartName().equals(design2.getPartName())){
			if(verbose)	printDifference("Part Name", design1.getPartName(), design2.getPartName());
			else return false;			
		}
		if(!design1.getNCDVersion().equals(design2.getNCDVersion())){
			if(verbose)	printDifference("NCD Version", design1.getNCDVersion(), design2.getNCDVersion());
			else return false;			
		}
		if(design1.isHardMacro() != design2.isHardMacro()){
			if(verbose)	printDifference("Is Hard Macro?", Boolean.toString(design1.isHardMacro()),
													   Boolean.toString(design2.isHardMacro()));
			else return false;			
		}
		
		// Compare Counts of Design Objects
		if(design1.getInstances().size() != design2.getInstances().size()){
			if(verbose) printDifference("Design Instance Count", Integer.toString(design1.getInstances().size()),
																  Integer.toString(design2.getInstances().size()));
			else return false;
		}
		if(design1.getNets().size() != design2.getNets().size()){
			if(verbose) printDifference("Design Net Count", Integer.toString(design1.getNets().size()),
																  Integer.toString(design2.getNets().size()));
			else return false;
		}
		if(design1.getModules().size() != design2.getModules().size()){
			if(verbose) printDifference("Design Module Count", Integer.toString(design1.getModules().size()),
																  Integer.toString(design2.getModules().size()));
			else return false;
		}
		if(design1.getModuleInstances().size() != design2.getModuleInstances().size()){
			if(verbose) printDifference("Design ModuleInstance Count", Integer.toString(design1.getModuleInstances().size()),
																	 Integer.toString(design2.getModuleInstances().size()));
			else return false;
		}
		
		// Compare Individual Elements in Design
		
		// Attributes
		if(!compareAttributes(design1.getAttributes(), design2.getAttributes(), "Design Attributes")){
			identical = false;
		}
		// TODO - Finish this later
		return identical;
	}
	
	public static void main(String[] args){
		if(args.length != 2){
			MessageGenerator.briefMessageAndExit("USAGE: <design1.xdl> <design2.xdl>");			
		}
		Design design1 = new Design();
		Design design2 = new Design();
		
		design1.loadXDLFile(args[0]);
		design2.loadXDLFile(args[1]);
		
		DesignDiff dd = new DesignDiff();
		dd.compareDesigns(design1, design2, true);
	}
}
