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
package edu.byu.ece.rapidSmith.device.helper;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A helper class to help remove duplicate objects and reduce memory usage and file
 * size of the Device class. 
 * @author Chris Lavin
 */
public class TileSources implements Serializable{

	private static final long serialVersionUID = -139462627137160891L;
	/** Sources of the tile */
	public int[] sources;
	
	public TileSources(int[] sources){
		this.sources = sources;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hash = 0;
		
		if(sources == null){
			return hash;
		}
		else{
			Arrays.sort(sources);
			for(Integer i : sources){
				hash += i * 7;
			}
			return hash;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TileSources other = (TileSources) obj;
		if(other.sources == null && sources == null){
			return true;
		}
		if(other.sources == null || sources == null){
			return false;
		}
		Arrays.sort(other.sources);
		Arrays.sort(sources);
		for(int i=0; i< sources.length; i++){
			if(sources[i] != other.sources[i]){
				return false;
			}
		}
		return true;
	}
	
	
}
