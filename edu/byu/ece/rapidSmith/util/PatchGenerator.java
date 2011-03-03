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
package edu.byu.ece.rapidSmith.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.PrimitiveType;
import edu.byu.ece.rapidSmith.device.Utils;

/**
 * This class generates a patch java class used for mapping missing internal pin
 * names to external pin names for primitive sites missing the mappings from the XDLRC
 * report file.
 * Created on: Feb 24, 2011
 */
public class PatchGenerator {
	
	protected static String pinMappingPatchClassName = "PinMappingPatch";
	
	private static void checkDir(File dir){
		if(!dir.isDirectory()){
			MessageGenerator.briefErrorAndExit(dir.getAbsolutePath() +
				" is not a directory.");
		}		
	}
	
	public static void main(String[] args) {
		if(args.length != 1){
			MessageGenerator.briefMessageAndExit("USAGE: <directory to patch files>");
		}
		String path = TileAndPrimitiveEnumerator.getPathToFiles();
		String patchFileName = path + pinMappingPatchClassName + ".java";
		String nl = System.getProperty("line.separator");
		System.out.print("This will overwrite the patch file: " + nl +
				"  "+ patchFileName +"."+ nl +
				"Are you sure you want to continue ");
		MessageGenerator.agreeToContinue();
		String dir = args[0];
		File directory = new File(dir);
		checkDir(directory);
		BufferedWriter bw;
		try{
			bw = new BufferedWriter(new FileWriter(patchFileName)); 	
			TileAndPrimitiveEnumerator.addHeaderToFile(bw, PatchGenerator.class);
			
			bw.write(nl);
			bw.write("import java.util.HashMap;"+nl);
			bw.write(nl);
			bw.write("class "+ pinMappingPatchClassName +" {" + nl);
			bw.write("\tprivate static HashMap<PrimitiveType, HashMap<String, String>> patch;" + nl);
			bw.write(nl);
			
			bw.write("\tpublic static String getPinMapping(PrimitiveType type, String internalName){" + nl);
			bw.write("\t\treturn patch.get(type).get(internalName);" + nl);
			bw.write("\t}" + nl);

			bw.write(nl);
			bw.write("\tstatic {" + nl);
			bw.write("\t\tpatch = new HashMap<PrimitiveType, HashMap<String, String>>();" + nl);
			bw.write("\t\tHashMap<String, String> map = null;" + nl);
			for(String familyName : directory.list()){
				FamilyType familyType = PartNameTools.getFamilyTypeFromFamilyName(familyName);
				if(familyType == null) continue;
				File familyDir = new File(directory.getAbsoluteFile() + File.separator + familyName);
				checkDir(familyDir);
				for(String primitiveSiteName : familyDir.list()){
					File primitiveDir = new File(familyDir.getAbsoluteFile() + File.separator + primitiveSiteName);
					checkDir(primitiveDir);
					for(String primitiveName : primitiveDir.list()){
						String fileName = primitiveDir.getAbsoluteFile() + File.separator + primitiveName;
						PrimitiveType type = Utils.createPrimitiveType(primitiveName.replace(".txt", ""));
						bw.write("\t\tmap = new HashMap<String, String>();" + nl);
						bw.write("\t\tpatch.put(PrimitiveType."+type+", map);" + nl);
						ArrayList<String> lines = FileTools.getLinesFromTextFile(fileName);
						for(String line : lines){
							String[] tokens = line.split("[(\\s)]+");
							bw.write("\t\tmap.put(\""+tokens[2]+"\", \""+tokens[4]+"\");" + nl);
						}
					}
				}
			}
			bw.write("\t}" + nl);
			bw.write("}" + nl);
			bw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
			MessageGenerator.briefErrorAndExit("Error creating file " + patchFileName);
		}
		System.out.println(patchFileName + " generated successfully.");
	}
}
