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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class is a special data structure used for Xilinx FPGA devices to help reduce memory footprint
 * of objects.  It keeps exactly one copy of an object of type E and maintains a unique integer enumeration
 * of each object.  It depends on the type E's equals() and hashCode() function to determine uniqueness.
 * @author Chris Lavin
 * Created on: Apr 30, 2010
 * @param <E> The type of object to use.
 */
public class HashPool<E> extends HashMap<Integer,ArrayList<E>> {

	private static final long serialVersionUID = -7643508400771696765L;

	private ArrayList<E> enumerations;
	
	private HashMap<E,ArrayList<Integer>> enumerationMap;
	
	public HashPool(){
		super();
		enumerations = new ArrayList<E>();
		enumerationMap = new HashMap<E,ArrayList<Integer>>();
	}
	
	private void addToEnumerationMap(E obj, Integer enumeration){
		ArrayList<Integer> enumerationMatches = enumerationMap.get(obj);
		if(enumerationMatches == null){
			enumerationMatches = new ArrayList<Integer>();
			enumerationMatches.add(enumeration);
			enumerationMap.put(obj, enumerationMatches);
		}
		else{
			enumerationMatches.add(enumeration);
		}
	}
	
	/**
	 * Gets the Integer enumeration of the object based on the HashPool.
	 * @param obj The object to get an enumeration value for.
	 * @return The enumeration value of the object obj, or -1 if none exists.
	 */
	public Integer getEnumerationValue(E obj){
		ArrayList<Integer> enumerationMatches = enumerationMap.get(obj);
		if(enumerationMatches == null){
			System.out.println("Object does not have enumeration value: " + obj.toString() + " in class: " + this.getClass().getCanonicalName());
			throw new IllegalArgumentException();
			//return -1;
		}
		else{
			for(Integer i : enumerationMatches){
				if(enumerations.get(i).equals(obj)){
					return i;
				}
			}
		}
		System.out.println("Object does not have enumeration value: " + obj.toString() + " in class: " + this.getClass().getCanonicalName());
		throw new IllegalArgumentException();
		//return -1;
	}
	
	/**
	 * Adds the object to the pool if an identical copy doesn't already exist.
	 * @param obj The object to be added
	 * @return The unique object contained in the HashPool
	 */
	public E add(E obj){
		int hash = obj.hashCode();
		ArrayList<E> hashMatches = get(hash);
		if(hashMatches == null){
			hashMatches = new ArrayList<E>();
			hashMatches.add(obj);
			put(hash, hashMatches);
			addToEnumerationMap(obj,enumerations.size());
			enumerations.add(obj);
			return obj;
		}
		else{
			for(E e :hashMatches){
				if(e.equals(obj)){
					return e;
				}
			}
			hashMatches.add(obj);
			put(hash, hashMatches);
			addToEnumerationMap(obj,enumerations.size());
			enumerations.add(obj);
			return obj;
		}
	}
	
	/**
	 * Checks the HashPool if it contains an equal object to obj as defined by the equals() method.
	 * @param obj The object to check for.
	 * @return True if the HashPool contains the object, false otherwise.
	 */
	public boolean contains(E obj){
		ArrayList<E> hashMatches = get(obj.hashCode());
		if(hashMatches == null){
			return false;
		}
		for(E e :hashMatches){
			if(e.equals(obj)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the identical object in the HashPool that is equal by definition of the equals()
	 * method.  Returns null if no equivalent object exists in pool
	 * @param obj The object to find in the pool
	 * @return The object in the pool that is equal to obj, null otherwise.
	 */
	public E find(E obj){
		ArrayList<E> hashMatches = get(obj.hashCode());
		if(hashMatches == null){
			return null;
		}
		for(E e :hashMatches){
			if(e.equals(obj)){
				return e;
			}
		}
		return null;
	}

	/**
	 * @return the enumerations
	 */
	public ArrayList<E> getEnumerations() {
		return enumerations;
	}

	/**
	 * @param enumerations the enumerations to set
	 */
	public void setEnumerations(ArrayList<E> enumerations) {
		this.enumerations = enumerations;
	}

	/**
	 * @return the enumerationMap
	 */
	public HashMap<E, ArrayList<Integer>> getEnumerationMap() {
		return enumerationMap;
	}

	/**
	 * @param enumerationMap the enumerationMap to set
	 */
	public void setEnumerationMap(HashMap<E, ArrayList<Integer>> enumerationMap) {
		this.enumerationMap = enumerationMap;
	}
	
}
