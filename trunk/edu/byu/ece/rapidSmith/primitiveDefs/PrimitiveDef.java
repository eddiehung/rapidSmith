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
package edu.byu.ece.rapidSmith.primitiveDefs;

import java.io.Serializable;
import java.util.ArrayList;

import edu.byu.ece.rapidSmith.device.PrimitiveType;

public class PrimitiveDef implements Serializable{

	private static final long serialVersionUID = -7246158565182505932L;
	private PrimitiveType type;
	private ArrayList<PrimitiveDefPin> pins;
	private ArrayList<Element> elements;

	public PrimitiveDef(){
		setType(null);
		pins = new ArrayList<PrimitiveDefPin>();
		elements = new ArrayList<Element>();
	}
	
	public void addPin(PrimitiveDefPin p){
		pins.add(p);
	}
	public void addElement(Element e){
		elements.add(e);
	}
	
	// Setters and Getters
	public void setType(PrimitiveType type) {
		this.type = type;
	}
	public PrimitiveType getType() {
		return type;
	}
	public ArrayList<PrimitiveDefPin> getPins() {
		return pins;
	}
	public void setPins(ArrayList<PrimitiveDefPin> pins) {
		this.pins = pins;
	}
	public ArrayList<Element> getElements() {
		return elements;
	}
	public void setElements(ArrayList<Element> elements) {
		this.elements = elements;
	}
	
	@Override
	public String toString(){
		StringBuilder s = new StringBuilder();
		String nl = System.getProperty("line.separator");
		s.append("(primitive_def " + type.toString() +" "+ pins.size() + " " + elements.size() + nl);
		for(PrimitiveDefPin p : pins){
			s.append("\t\t"+p.toString()+nl);
		}
		for(Element e : elements){
			s.append("\t\t"+e.toString()+nl);
		}
		s.append("\t)");
		return s.toString();
	}
}
