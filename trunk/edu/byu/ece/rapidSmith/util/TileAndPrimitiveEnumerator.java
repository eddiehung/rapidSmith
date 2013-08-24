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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class generates java files for the device package for Xilinx FPGAs
 * to create enumerations for tile type names and primitive type names.  
 * Specifically it creates the files: PrimitiveType.java, TileType.java, Utils.java.
 * @author Chris Lavin
 */
public class TileAndPrimitiveEnumerator{

	/** Keeps all the unique tile names/types in a sorted set */
	private SortedSet<String> tileSet;
	/** Keeps all the unique instance names/types in a sorted set */
	private SortedSet<String> primitiveSet;
	/** Stores the names of the FPGA family names (Virtex4, Virtex5, ...)*/
	private static final FamilyType[] families = {FamilyType.ARTIX7, 
		FamilyType.KINTEX7, FamilyType.SPARTAN2, 
		FamilyType.SPARTAN2E, FamilyType.SPARTAN3, FamilyType.SPARTAN3A,
		FamilyType.SPARTAN3ADSP, FamilyType.SPARTAN3E, FamilyType.SPARTAN6,
		FamilyType.VIRTEX, FamilyType.VIRTEX2, FamilyType.VIRTEX2P, 
		FamilyType.VIRTEX4, FamilyType.VIRTEX5, FamilyType.VIRTEX6, 
		FamilyType.VIRTEX7, FamilyType.VIRTEXE, FamilyType.ZYNQ};
	/** All of the part names to check */ 
	public ArrayList<String> partNames;
	/** List of all xdlrc file names generated */
	public ArrayList<String> xdlrcFileNames;
	/** Stores the system's line terminator ("\n", "\r\n", "\r", etc)*/
	public String nl;
	/** Name of the PrimitiveType enum type to be created */
	public static final String primitiveTypeName = "PrimitiveType";
	/** Name of the TileType enum type to be created */
	public static final String tileTypeName = "TileType";
	/** Name of the Utils class to be created */
	public static final String utilsName = "Utils";
	
	/**
	 * Initializes the class to all new empty data structures
	 */
	public TileAndPrimitiveEnumerator(){
		tileSet = new TreeSet<String>();
		primitiveSet = new TreeSet<String>();
		partNames = new ArrayList<String>();
		xdlrcFileNames = new ArrayList<String>();
		nl = System.getProperty("line.separator");
	}
	
	/**
	 * This creates a list of parts that will give a universal list 
	 * of primitive types and tile types for all the families supported
	 * (virtex4, virtex5, virtex6, spartan3a, spartan3adsp, spartan3e, spartan6).
	 */
	private void generatePartNames(){
		for(FamilyType type : families){
			switch(type){
			case ARTIX7: 
				break;
			case KINTEX7: 
				partNames.add("xc7k30tfbg484");
				partNames.add("xc7k160tfbg676");
				break;
			case SPARTAN2:
				partNames.add("xc2s15cs144");
				break;
			case SPARTAN2E:
				partNames.add("xc2s50eft256");
				partNames.add("xc2s600efg456");
				break;
			case SPARTAN3:
				partNames.add("xc3s50pq208");
				partNames.add("xc3s400fg456");
				partNames.add("xc3s4000fg676");
				break;
			case SPARTAN3A:
				partNames.add("xc3s1400afg484");
				partNames.add("xc3s700anfgg484");
				partNames.add("xc3s200aft256");
				partNames.add("xc3s50atq144");
				break;
			case SPARTAN3ADSP:
				partNames.add("xc3sd1800acs484");
				break;
			case SPARTAN3E:
				partNames.add("xc3s100evq100");
				partNames.add("xc3s250evq100");
				partNames.add("xc3s1200eft256");
				break;
			case SPARTAN6:
				partNames.add("xc6slx45fgg484");
				partNames.add("xc6slx75csg484");
				partNames.add("xc6slx100tfgg484");
				partNames.add("xc6slx25csg324");
				partNames.add("xc6slx4tqg144");
				break;
			case VIRTEX:
				partNames.add("xcv400bg432");
				break;
			case VIRTEX2:
				partNames.add("xc2v500fg256");
				break;
			case VIRTEX2P:
				partNames.add("xc2vp2fg256");
				partNames.add("xc2vpx70ff1704");		
				break;
			case VIRTEX4:
				partNames.add("xc4vfx12ff668");
				partNames.add("xc4vfx100ff1517");
				break;
			case VIRTEX5: 
				partNames.add("xc5vlx20tff323");
				partNames.add("xc5vlx30ff324");
				partNames.add("xc5vtx150tff1156");
				partNames.add("xc5vfx70tff1136");
				break;
			case VIRTEX6:
				partNames.add("xc6vhx255tff1155");
				partNames.add("xc6vcx130tff484");
				break;
			case VIRTEX7: 
				partNames.add("xc7v450tffg1157"); 
				partNames.add("xc7vx485tffg1157");
				partNames.add("xc7v1500tfhg1157");
				break;
			case VIRTEXE:
				partNames.add("xcv50ecs144");
				partNames.add("xcv405ebg560");
				break;
			}
		}
	}
	
	/**
	 * Generates XDLRC (brief reports) for the partNames in this class.  Each
	 * XDLRC files is then parsed to get all tile type names as well as primitive
	 * type names.
	 * @return True if operation was successful, false otherwise.
	 */
	private boolean generateAndSearchXDLRC(){
		BufferedReader input;
		String[] tokens;
		String line;
		try {
			for(String part : partNames){
				if(!new File(part+".xdlrc").exists()){
					xdlrcFileNames.add(part+".xdlrc");
					if(!RunXilinxTools.generateBriefXDLRCFile(part,part+".xdlrc")){
						return false;
					}
				}

				input = new BufferedReader(new FileReader(part + ".xdlrc"));
				
				while((line = input.readLine()) != null){
					tokens = line.split("\\s+");
					
					if(tokens.length > 1 && tokens[1].equals("(tile")){
						this.tileSet.add(tokens[5]);
						//this.tileSet.add(this.removeCoordinates(tokens[4]));
					}
					else if(tokens.length > 1 && tokens[1].equals("(primitive_def")){
						this.primitiveSet.add(tokens[2]);
					}
				}
			}
		} 
		catch (IOException e) {
			System.out.println("XDLRC Generation failed");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Adds the license and comment header with package information to all files generated.
	 * @param bw The stream to write to.
	 * @throws IOException
	 */
	protected static void addHeaderToFile(BufferedWriter bw, @SuppressWarnings("rawtypes") Class c) throws IOException{
		String nl = System.getProperty("line.separator");
		bw.write("/*" + nl);
		bw.write(" * Copyright (c) 2010-2011 Brigham Young University" + nl);
		bw.write(" * " + nl);
		bw.write(" * This file is part of the BYU RapidSmith Tools." + nl);
		bw.write(" * " + nl);
		bw.write(" * BYU RapidSmith Tools is free software: you may redistribute it" + nl); 
		bw.write(" * and/or modify it under the terms of the GNU General Public License " + nl);
		bw.write(" * as published by the Free Software Foundation, either version 2 of " + nl);
		bw.write(" * the License, or (at your option) any later version." + nl);
		bw.write(" * "+ nl);
		bw.write(" * BYU RapidSmith Tools is distributed in the hope that it will be" + nl); 
		bw.write(" * useful, but WITHOUT ANY WARRANTY; without even the implied warranty" + nl);
		bw.write(" * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " + nl);
		bw.write(" * General Public License for more details." + nl);
		bw.write(" * " + nl);
		bw.write(" * A copy of the GNU General Public License is included with the BYU" + nl); 
		bw.write(" * RapidSmith Tools. It can be found at doc/gpl2.txt. You may also " + nl);
		bw.write(" * get a copy of the license at <http://www.gnu.org/licenses/>." + nl);
		bw.write(" * " + nl);
		bw.write(" */" + nl);
		bw.write("package edu.byu.ece.rapidSmith.device;");
		bw.write(nl + nl);
		bw.write("/*" + nl);
		bw.write(" * This file was generated by:" + nl);
		bw.write(" *   " + c.toString() + nl);
		bw.write(" * Generated on:" + nl);
		bw.write(" *   " + FileTools.getTimeString() + nl);
		bw.write(" * The following Xilinx families are supported:" + nl);
		bw.write(" *   ");
		for(FamilyType family : families){
			bw.write(family.toString().toLowerCase() + " ");
		}
		bw.write(nl);
		bw.write(" */" + nl);
		bw.write(nl);
	}
	
	/**
	 * Generates the java files PrimitiveType.java, TileType.java, Utils.java.
	 * It does this based on the families specified in families.  If the 
	 * variable families is null, it will not create the files.
	 * @return true if it was successful in creating the 3 java files, false otherwise.
	 */
	public boolean createFiles(){
		generatePartNames();

		if(!generateAndSearchXDLRC()){
			return false;
		}
		
		FileWriter output;
		BufferedWriter bw;
		Iterator<String> itr;
		String path = getPathToFiles();
		//=======================================================//
		/* Generate TileType.java for device package             */
		//=======================================================//
		try{
			output = new FileWriter(path + tileTypeName + ".java");
			bw = new BufferedWriter(output);	
			addHeaderToFile(bw, this.getClass());
			bw.write("/**" + nl);
			bw.write(" * This enum enumerates all of the Tile types of the following FPGA families: " + nl);
			bw.write(" *   ");
			for(FamilyType family : families){
				bw.write(family.toString().toLowerCase() + " ");
			}
			bw.write(nl);
			bw.write(" */" + nl);
			bw.write("public enum " + tileTypeName + "{"+nl);
			
			itr = this.tileSet.iterator();
			while(itr.hasNext()){
				bw.write("\t" + itr.next() + ","+nl);
			}
			bw.write("}"+ nl +nl);
			bw.close();
			output.close();
		}
		catch(IOException e){
			System.out.println("Problems writing class: " + tileTypeName);
			return false;
		}
		
		//=======================================================//
		/* Generate PrimitiveType.java for device package        */
		//=======================================================//
		try{
			output = new FileWriter(path + primitiveTypeName + ".java");
			bw = new BufferedWriter(output);	
			addHeaderToFile(bw, this.getClass());
			bw.write("/**" + nl);
			bw.write(" * This enum enumerates all of the Primitive types of the following FPGA families: " + nl);
			bw.write(" *   ");
			for(FamilyType family : families){
				bw.write(family.toString().toLowerCase() + " ");
			}
			bw.write(nl);
			bw.write(" */" + nl);
			bw.write("public enum " + primitiveTypeName + "{"+nl);
			
			String[] primitiveTypes = new String[primitiveSet.size()];
			primitiveTypes = primitiveSet.toArray(primitiveTypes);
			for(int i = 0; i < primitiveTypes.length; i++){
				if(i == primitiveTypes.length - 1){
					bw.write("\t" + primitiveTypes[i] + ";"+nl);
				}
				else{
					bw.write("\t" + primitiveTypes[i] + ","+nl);
				}
			}
			
			bw.write(nl);
			
			bw.write("}"+ nl +nl);
			bw.close();
			output.close();
		}
		catch(IOException e){
			System.out.println("Problems writing class: " + primitiveTypeName);
			return false;
		}

		//=======================================================//
		/* Generate Utils.java for device package                */
		//=======================================================//
		try{
			output = new FileWriter(path + utilsName + ".java");
			bw = new BufferedWriter(output);	
			addHeaderToFile(bw, this.getClass());
			bw.write("/**" + nl);
			bw.write(" * This class serves as a method to translate Strings to and from " + nl);
			bw.write(" * " + primitiveTypeName + "s and " + tileTypeName + "s. " + nl);
			bw.write(" */" + nl);
			bw.write("public class " + utilsName + "{" + nl + nl);
			bw.write("\tprivate static " + utilsName + " _singleton = null;" + nl +
						 "\tprivate static java.util.HashMap<String, " + primitiveTypeName + "> primitiveMap;" + nl +
						 "\tprivate static java.util.HashMap<String, " + tileTypeName + "> tileMap;" + nl + nl);
			
			bw.write("\tprivate " + utilsName + "(){" + nl +
						 "\t}" + nl);	
			bw.write("\t/**" + nl +
						 "\t * Get the singleton instance of the class (we don't need multiple instances" + nl +
						 "\t * of this class)" + nl +
						 "\t * @return The singleton instance of " + utilsName + nl +
						 "\t */" + nl + nl);
			bw.write("\tpublic static " + utilsName + " getInstance(){"+ nl +
						 "\t\tif(_singleton == null){" + nl +
						 "\t\t\t_singleton = new " + utilsName + "();" + nl + 
						 "\t\t}" + nl +
						 "\t\treturn _singleton;" + nl +
						 "\t}" + nl);
			
			bw.write("\tstatic{"+nl);
			bw.write("\t\tprimitiveMap = new java.util.HashMap<String, " + primitiveTypeName + ">("+
						      this.primitiveSet.size()+");"+ nl +
						 "\t\ttileMap = new java.util.HashMap<String, " + tileTypeName + ">("+this.tileSet.size()+");"+ nl +nl);
			
			itr = this.primitiveSet.iterator();
			while(itr.hasNext()){
				String curr = itr.next();
				bw.write("\t\tprimitiveMap.put(\"" + curr + "\", " + primitiveTypeName + "."+curr+");"+nl);
			}
			
			bw.write(nl);
			itr = this.tileSet.iterator();
			while(itr.hasNext()){
				String curr = itr.next();
				bw.write("\t\ttileMap.put(\"" + curr + "\", " + tileTypeName + "."+curr+");"+nl);
			}
			bw.write("\t}"+ nl +nl);
			
			bw.write("\t/**" + nl + 
				 "\t * Returns a " + primitiveTypeName + " enum based on the given string. If such" + nl +
				 "\t * an enum does not exist, it will return null." + nl + 
				 "\t * @param s The string to be converted to an enum type" + nl + 
				 "\t * @return The " + primitiveTypeName + " corresponding to the string s, null if none exists." + nl + 
				 "\t */" + nl + 
				 "\tpublic " + primitiveTypeName + " create" + primitiveTypeName + "(String s){" + nl + 
				 "\t\treturn " + utilsName + ".primitiveMap.get(s);" + nl + 
				 "\t}" + nl + nl +
				 "\t/**" + nl +
				 "\t * Returns a " + tileTypeName + " enum based on the given string s.  If such an enum" + nl + 
				 "\t * does not exist, it will return null" + nl +
				 "\t * @param s The string to be converted to an enum type"+ nl +
				 "\t * @return The " + tileTypeName + " corresponding to String s, null if none exists." + nl +
				 "\t */"+ nl +
				 "\tpublic " + tileTypeName + " create" + tileTypeName + "(String s){"+ nl +
				 "\t\treturn " + utilsName + ".tileMap.get(s);"+ nl +
				 "\t}"+ nl + nl +
				 "}"+nl);
			
			bw.close();
			output.close();
		}
		catch(IOException e){
			System.out.println("Problems writing class: " + utilsName);
			return false;
		}
		
		return true;
	}

	/**
	 * Gets and returns a list of Xilinx FPGA base families compatible with 
	 * this tool.
	 * @return A list of Xilinx FPGA base families compatible with this tool.
	 */
	public static FamilyType[] getFamilies(){
		return families;
	}
	
	protected static String getPathToFiles(){
		String slash = File.separator;
		String path = FileTools.getRapidSmithPath() + slash + 
			"edu" + slash +
			"byu" + slash +
			"ece" + slash +
			"rapidSmith" + slash +
			"device" + slash;

		return path; 
	}
	
	private static ArrayList<String> checkForExistingFiles(){
		ArrayList<String> files = new ArrayList<String>();
		String path = getPathToFiles();
		
		String name = path+primitiveTypeName+".java";
		if(new File(name).exists()){
			files.add(name);
		}
		
		name = path+tileTypeName+".java";
		if(new File(name).exists()){
			files.add(name);
		}
		
		name = path+utilsName+".java";
		if(new File(name).exists()){
			files.add(name);
		}
		return files;
	}
	
	/**
	 * Allows this class to be invoked from the command line.  
	 */
	public static void main(String args[]){
		// Create a new instance of this class
		TileAndPrimitiveEnumerator me = new TileAndPrimitiveEnumerator();
		
		MessageGenerator.printHeader(me.getClass().getCanonicalName());
		
		ArrayList<String> existingFiles = checkForExistingFiles();
		if(existingFiles.size() > 0){
			System.out.println("This will overwrite existing copies of the following java files:");
			for(String name : existingFiles){
				System.out.println("  " + name);
			}
			MessageGenerator.promptToContinue();
		}
		
		
		System.out.print("Enumerating Tiles and Primitives...");
		
		// Create the .java files for the Design package
		if(!me.createFiles()){
			System.out.println("Error generating .java files.");
		}
		
		System.out.println("DONE!");
		
		// Delete the XDLRC files when finished
		if(args.length > 0 && args[0].equals("-deleteXDLRC")){
			System.gc();
			for(String fileName : me.xdlrcFileNames){
				System.out.println("Removing: " + fileName);
				FileTools.deleteFile(fileName);
			}			
		}
	}
}
