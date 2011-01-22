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
package edu.byu.ece.rapidSmith.device;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.util.FileTools;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class is used to patch missing information in the XDLRC primitive pins,
 * specificially for RAMB types.  
 * @author Chris Lavin
 * Created on: Jun 9, 2010
 */
public class V5RAMBPatch{

	public HashMap<PrimitiveType,HashMap<String,Integer>> rambPinMappings;
	
	@SuppressWarnings("unchecked")
	public V5RAMBPatch(){
		if(new File(getV5RAMBPatchFileName()).exists()){
			rambPinMappings = (HashMap<PrimitiveType,HashMap<String,Integer>>) 
			FileTools.loadFromCompressedFile(getV5RAMBPatchFileName());
		}
		else{
			MessageGenerator.briefError("Warning: Missing Virtex 5 RAMB " +
				"patch file, some routing functionality will not work.");
		}
		
	}

	public Integer getExternalPin(Pin pin){
		return rambPinMappings.get(pin.getInstance().getType()).get(pin.getName());		
	}
	
	private void printDebug(WireEnumerator we){
		for(PrimitiveType type : rambPinMappings.keySet()){
			System.out.println(type);
			for(String internalPin :  rambPinMappings.get(type).keySet()){
				Integer externalPin = rambPinMappings.get(type).get(internalPin);
				System.out.println("internalPin="+ internalPin +" externalPin=" + externalPin);
				System.out.println("    " + internalPin + " : " + we.getWireName(externalPin));
			}
		}
	}
	
	
	public static String getV5RAMBPatchFileName(){
		return FileTools.getRapidSmithPath() + File.separator + "patches"+ File.separator + "virtex5" +
			File.separator + FileTools.v5RAMBPinMappingFileName;
	}
	
	public static void updateMappingsFile(String pathToFiles, WireEnumerator we){
		HashMap<PrimitiveType,HashMap<String,Integer>> map = new HashMap<PrimitiveType, HashMap<String,Integer>>();
		String[] fileNames = {	"FIFO36_72_EXP.txt",
								"FIFO36_EXP.txt",
								"RAMB18X2.txt",
								"RAMB18X2SDP.txt",
								"RAMB36SDP_EXP.txt",
								"RAMB36_EXP.txt",
								"RAMBFIFO18.txt",
								"RAMBFIFO18_36.txt"
								};
		
		if(!pathToFiles.endsWith(File.separator)){
			pathToFiles += File.separator;
		}
		
		for(String fileName : fileNames) {
			String primitiveName = fileName.substring(0, fileName.length()-4);
			PrimitiveType type = Utils.getInstance().createPrimitiveType(primitiveName);
			HashMap<String,Integer> primitivePinMap = new HashMap<String, Integer>();

			BufferedReader br;
			try{
				br = new BufferedReader(new FileReader(pathToFiles+fileName));
				String line = null;
				while((line = br.readLine()) != null) {
					String[] tokens = line.split(" ");
					String internalPinName = tokens[1];
					String externalPinName = tokens[3].substring(0, tokens[3].length()-1);
					primitivePinMap.put(internalPinName, we.getWireEnum(externalPinName));
				}
			}
			catch(FileNotFoundException e){
				MessageGenerator.briefError("ERROR: Could not find file: " + pathToFiles + fileName);
			}
			catch(IOException e){
				MessageGenerator.briefError("ERROR: Problem reading file: " + pathToFiles + fileName);
			}
			
			map.put(type,primitivePinMap);
		}

		FileTools.saveToCompressedFile(map, getV5RAMBPatchFileName());
	}
	
	public static void main(String[] args){
		if(args.length == 0){
			MessageGenerator.briefMessageAndExit("USAGE: <path to txt files containing V5 RAMB pin mappings>");
		}
		
		String s = args[0];
		
		WireEnumerator we = FileTools.loadWireEnumerator("xc5vlx20tff323");
		updateMappingsFile(s, we);
		we.writeDebugFile("we.txt");
		V5RAMBPatch v = new V5RAMBPatch();
		v.printDebug(we);
	}
}
