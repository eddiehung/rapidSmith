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

public class PrimitiveDefPin implements Serializable {

	private static final long serialVersionUID = -5733624692261327342L;

	private String externalName;
	private String internalName;
	private boolean isOutput;
	
	public PrimitiveDefPin(){
		externalName = null;
		internalName = null;
		isOutput = false;
	}
	
	public String getExternalName() {
		return externalName;
	}
	public void setExternalName(String externalName) {
		this.externalName = externalName;
	}
	public String getInternalName() {
		return internalName;
	}
	public void setInternalName(String internalName) {
		this.internalName = internalName;
	}
	public boolean isOutput() {
		return isOutput;
	}
	public void setOutput(boolean isOutput) {
		this.isOutput = isOutput;
	}
	
	@Override
	public String toString(){
		if(externalName == null){
			return "(pin " + internalName + (isOutput ? " output)" : " input)");
		}
		return "(pin " + externalName + " " + internalName + (isOutput ? " output)" : " input)");
	}
}
