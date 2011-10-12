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
package edu.byu.ece.rapidSmith.constraints;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This is an elementary UCF parser which will create an array of Constraint
 * objects from a given UCF file.
 * Created on: May 5, 2011
 */
public class UCFParser {
	/** Character input stream from the UCF file */
	private BufferedInputStream reader;
	/** Current line number */
	private int line;
	
	/**
	 * Helper method to create a constraint object and have the constraint
	 * parsed. 
	 * @param constraint The string containing the constraint.
	 * @return The newly created constraint object.
	 */
	public Constraint createConsraint(String constraint){
		Constraint c = new Constraint();
		if(!c.setConstraintString(constraint)){
			MessageGenerator.briefErrorAndExit("Error: Failed parsing constraint: <" + constraint + ">, on line: " + line);
		}
		return c;
	}
	
	/**
	 * This is the main method to parse a UCF file
	 * @param fileName Name of the UCF file to parse
	 * @return A list of Constraint objects representing the constraints 
	 */
	public ArrayList<Constraint> parseUCF(String fileName){
		ArrayList<Constraint> constraints = new ArrayList<Constraint>();
		line = 0;
		try {
			reader = new BufferedInputStream(new FileInputStream(fileName));
			
			int ch = -1;
			int idx = 0;
			char[] buffer = new char[8192];
			boolean lineStart = true;
			boolean inComment = false;
			while((ch = reader.read()) != -1){
				if(ch == '#'){
					inComment = true;
				}
				else if(ch == ';' && !inComment && idx > 0){
					String c = new String(buffer, 0, idx);
					if(!c.trim().equals("")){
						constraints.add(createConsraint(c));
					}
					idx = 0;
				}
				else if(!inComment && ch != '\r' && ch != '\n'){
					// Add to buffer
					buffer[idx++] = (char) ch;
				}
				
				if(ch == '\n'){
					lineStart = true;
					inComment = false;
					line++;
				}
				else{
					lineStart = false;					
				}

			}
		} 
		catch(FileNotFoundException e){
			e.printStackTrace();
		} 
		catch(IOException e){
			e.printStackTrace();
		}
			
		return constraints;
	}
	
	
	public static void main(String[] args) {
		UCFParser p = new UCFParser();
		ArrayList<Constraint> constraints = p.parseUCF(args[0]);
		
		for(Constraint c : constraints){
			System.out.println(c.toString());
		}
	}
}
