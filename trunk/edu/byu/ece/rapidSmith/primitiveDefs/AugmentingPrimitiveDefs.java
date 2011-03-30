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
package edu.byu.ece.rapidSmith.primitiveDefs;

import edu.byu.ece.rapidSmith.device.PrimitiveType;
import edu.byu.ece.rapidSmith.util.FamilyType;
import edu.byu.ece.rapidSmith.util.FileTools;

public class AugmentingPrimitiveDefs {
	
	private static PrimitiveType[] virtex56IOBTypes = {PrimitiveType.IOB, PrimitiveType.IOBM, PrimitiveType.IOBS};
	private static String[] virtex5IOBStandards = {
		"BLVDS_25", "DIFF_HSTL_I", "DIFF_HSTL_I_18", "DIFF_HSTL_I_DCI",        
		"DIFF_HSTL_I_DCI_18", "DIFF_HSTL_II", "DIFF_HSTL_II_18", 
		"DIFF_HSTL_II_DCI", "DIFF_HSTL_II_DCI_18", "DIFF_SSTL18_I", 
		"DIFF_SSTL18_I_DCI", "DIFF_SSTL18_II", "DIFF_SSTL18_II_DCI", 
		"DIFF_SSTL2_I", "DIFF_SSTL2_I_DCI", "DIFF_SSTL2_II", 
		"DIFF_SSTL2_II_DCI", "GTL", "GTL_DCI", "GTLP", "GTLP_DCI", 
		"HSLVDCI_15", "HSLVDCI_18", "HSLVDCI_25", "HSLVDCI_33", "HSTL_I", 
		"HSTL_I_12", "HSTL_I_18", "HSTL_I_DCI", "HSTL_I_DCI_18", "HSTL_II", 
		"HSTL_II_18", "HSTL_II_DCI", "HSTL_II_DCI_18", "HSTL_II_T_DCI", 
		"HSTL_II_T_DCI_18", "HSTL_III", "HSTL_III_18", "HSTL_III_DCI", 
		"HSTL_III_DCI_18", "HSTL_IV", "HSTL_IV_18", "HSTL_IV_DCI", 
		"HSTL_IV_DCI_18", "HT_25", "LVCMOS12", "LVCMOS15", "LVCMOS18", 
		"LVCMOS25", "LVCMOS33", "LVDCI_15", "LVDCI_18", "LVDCI_25", 
		"LVDCI_33", "LVDCI_DV2_15", "LVDCI_DV2_18", "LVDCI_DV2_25", 
		"LVDS_25", "LVDSEXT_25", "LVPECL_25", "LVTTL", "PCI33_3", "PCI66_3", 
		"PCIX", "RSDS_25", "SSTL18_I", "SSTL18_I_DCI", "SSTL18_II", 
		"SSTL18_II_DCI", "SSTL18_II_T_DCI", "SSTL2_I", "SSTL2_I_DCI", 
		"SSTL2_II", "SSTL2_II_DCI", "SSTL2_II_T_DCI"}; 
	private static String[] virtex6IOBStandards = {
		"BLVDS_25", "DIFF_HSTL_I", "DIFF_HSTL_I_18", "DIFF_HSTL_I_DCI", 
		"DIFF_HSTL_I_DCI_18", "DIFF_HSTL_II", "DIFF_HSTL_II_18", 
		"DIFF_HSTL_II_DCI", "DIFF_HSTL_II_DCI_18", "DIFF_HSTL_II_T_DCI_18", 
		"DIFF_SSTL15", "DIFF_SSTL15_DCI", "DIFF_SSTL15_T_DCI", 
		"DIFF_SSTL18_I", "DIFF_SSTL18_I_DCI", "DIFF_SSTL18_II", 
		"DIFF_SSTL18_II_DCI", "DIFF_SSTL18_II_T_DCI", "DIFF_SSTL2_I", 
		"DIFF_SSTL2_I_DCI", "DIFF_SSTL2_II", "DIFF_SSTL2_II_DCI", 
		"DIFF_SSTL2_II_T_DCI", "HSTL_I", "HSTL_I_12", "HSTL_I_18", 
		"HSTL_I_DCI", "HSTL_I_DCI_18", "HSTL_II", "HSTL_II_18", "HSTL_II_DCI",
		"HSTL_II_DCI_18", "HSTL_II_T_DCI", "HSTL_II_T_DCI_18", "HSTL_III", 
		"HSTL_III_18", "HSTL_III_DCI", "HSTL_III_DCI_18", "HT_25", "LVCMOS12", 
		"LVCMOS15", "LVCMOS18", "LVCMOS25", "LVDCI_15", "LVDCI_18", "LVDCI_25",
		"LVDCI_DV2_15", "LVDCI_DV2_18", "LVDCI_DV2_25", "LVDS_25", 
		"LVDSEXT_25", "LVPECL_25", "LVPECL_25", "RSDS_25", "SSTL15", 
		"SSTL15_DCI", "SSTL15_T_DCI", "SSTL18_I", "SSTL18_I_DCI", "SSTL18_II",
		"SSTL18_II_DCI", "SSTL18_II_T_DCI", "SSTL2_I", "SSTL2_I_DCI", 
		"SSTL2_II", "SSTL2_II_DCI", "SSTL2_II_T_DCI"};
	private static String[] driveOptions = {"2", "4", "6", "8", "12", "16", "24"};
	private static String[] slewOptions = {"SLOW", "FAST"};
	
	private static void addNewElement(PrimitiveDefList list, PrimitiveType type, String elementName, String[] cfgList){
		PrimitiveDef p = list.getPrimitiveDef(type);
		Element element = null;
		for(Element e : p.getElements()){
			if(e.getName().equals(elementName)){
				element = e;
			}
		}
		if(element == null){
			element = new Element();
			element.setName(elementName);
			p.addElement(element);
		}
		for(String option : cfgList){
			element.addCfgOption(option);
		}
	}
	
	
	public static void main(String[] args) {
		FamilyType familyType = FamilyType.VIRTEX5;
		PrimitiveDefList list = FileTools.loadPrimitiveDefs(familyType);
		
		switch(familyType){
			case VIRTEX5:
				// Add missing IOB information
				for(PrimitiveType type : virtex56IOBTypes){
					addNewElement(list, type, "ISTANDARD", virtex5IOBStandards);
					addNewElement(list, type, "OSTANDARD", virtex5IOBStandards);
					addNewElement(list, type, "DRIVE", driveOptions);
					addNewElement(list, type, "SLEW", slewOptions);
				}
				break;
			case VIRTEX6:
				// Add missing IOB information
				for(PrimitiveType type : virtex56IOBTypes){
					addNewElement(list, type, "ISTANDARD", virtex6IOBStandards);
					addNewElement(list, type, "OSTANDARD", virtex6IOBStandards);
					addNewElement(list, type, "DRIVE", driveOptions);
					addNewElement(list, type, "SLEW", slewOptions);
				}
				break;
		}
		
		
		for(Element e : list.getPrimitiveDef(PrimitiveType.IOBS).getElements()){
			System.out.print(e.getName() + " ");
			if(e.getCfgOptions() != null){
				for(String option : e.getCfgOptions()){
					System.out.print(option + " ");
				}				
			}
			System.out.println();
		}
		
		//System.out.println(list.toString());
		
	}
}
