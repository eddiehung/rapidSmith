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

public class Element implements Serializable{

	private static final long serialVersionUID = -4173250951419860912L;

	private String name;
	private boolean bel;
	private ArrayList<PrimitiveDefPin> pins;
	private ArrayList<String> cfgOptions;
	private ArrayList<Connection> connections;
	
	public Element(){
		name = null;
		bel = false;
		pins = new ArrayList<PrimitiveDefPin>();
		cfgOptions = null;
		connections = new ArrayList<Connection>();
	}
	
	public void addPin(PrimitiveDefPin p){
		pins.add(p);
	}
	public void addConnection(Connection c){
		connections.add(c);
	}
	public void addCfgOption(String option){
		if(cfgOptions == null){
			cfgOptions = new ArrayList<String>();
		}
		cfgOptions.add(option);
	}
	
	
	// Getters and Setters
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isBel() {
		return bel;
	}
	public void setBel(boolean bel) {
		this.bel = bel;
	}
	public ArrayList<PrimitiveDefPin> getPins() {
		return pins;
	}
	public void setPins(ArrayList<PrimitiveDefPin> pins) {
		this.pins = pins;
	}
	public ArrayList<String> getCfgOptions() {
		return cfgOptions;
	}
	public void setCfgOptions(ArrayList<String> cfgOptions) {
		this.cfgOptions = cfgOptions;
	}
	public ArrayList<Connection> getConnections() {
		return connections;
	}
	public void setConnections(ArrayList<Connection> connections) {
		this.connections = connections;
	}
	
	@Override
	public String toString(){
		StringBuilder s = new StringBuilder();
		String nl = System.getProperty("line.separator");
		s.append("(element " + name +" "+ pins.size() +(bel ? " # BEL" : "")+nl);
		for(PrimitiveDefPin p : pins){
			s.append("\t\t\t"+p.toString() + nl);
		}
		if(cfgOptions != null){
			s.append("\t\t\t(cfg");
			for(String option : cfgOptions){
				s.append(" " + option);
			}
			s.append(")"+nl);
		}
		for(Connection c : connections){
			s.append("\t\t\t"+c.toString() + nl);
		}
		s.append("\t\t)");
		return s.toString();
	}
}
