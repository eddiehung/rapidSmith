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
package edu.byu.ece.rapidSmith.device.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.byu.ece.rapidSmith.device.WireConnection;

/**
 * DO NOT USE THIS CLASS!  This class was specially developed for the Device 
 * wire connections hash map.  It is specifically optimized for that purpose.
 * Created on: Mar 18, 2011
 */
public class WireHashMap{

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.85f;
    
    /**
     * The keys table. Length MUST Always be a power of two.
     */
    public transient int[] keys;
    
    /**
     * The corresponding values table.
     */
    public transient WireConnection[][] values;
    
    /**
     * The number of key-value mappings contained in this map.
     */
    transient int size;
    
    /**
     * The next size value at which to resize (capacity * load factor).
     * @serial
     */
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

	/**
	 * This map requires an initial capacity.  This map will not grow.
	 * @param capacity
	 */
    public WireHashMap(int capacity, float loadFactor){
        if (capacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               capacity);
        if (capacity > MAXIMUM_CAPACITY)
            capacity = MAXIMUM_CAPACITY;

        // Find a power of 2 >= initialCapacity
        int finalCapacity = 4;
        while (finalCapacity < capacity)
            finalCapacity <<= 1;
        
        this.loadFactor = loadFactor;
        threshold = (int)(finalCapacity * loadFactor);
        
        keys = new int[finalCapacity];
        values = new WireConnection[finalCapacity][];
        size = 0;
    }
    
    public WireHashMap(int capacity){
    	this(capacity, DEFAULT_LOAD_FACTOR);
    }
    
    public WireHashMap(){
    	this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Returns index for hash code h.
     */
    static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }


    public boolean isEmpty() {
        return size == 0;
    }
    
    
    public WireConnection[] get(int key){
        
        int i = key & (keys.length-1);//indexFor(key.intValue(), keys.length);
        while(values[i] != null && keys[i] != key){
        	i+=3;
        	if(i >= keys.length) i=i&3;
        }
        return values[i];
    } 

    public void put(int key, WireConnection[] value){
        int i = key & (keys.length-1);//indexFor(key.intValue(), keys.length);
        while(values[i] != null && keys[i] != key){
        	i+=3;
        	if(i >= keys.length) i=i&3;
        }
        if(keys[i] == 0) size++;
        keys[i] = key;
        values[i] = value;

        if(size > threshold){
        	grow();
        }
    }
    
    private void grow(){
    	int newCapacity = keys.length*2;
        threshold = (int)(newCapacity * loadFactor);
    	int[] oldKeys = keys;
    	WireConnection[][] oldValues = values;
        keys = new int[newCapacity];
        values = new WireConnection[newCapacity][];
        size = 0;
        for(int i=0; i < oldValues.length; i++){
			if(oldValues[i] != null){
				put(oldKeys[i], oldValues[i]);
			}
		}
    }
    
    public Set<Integer> keySet(){
    	HashSet<Integer> keySet = new HashSet<Integer>();
    	for(int i=0; i < keys.length; i++)
			if(values[i] != null) 
				keySet.add(keys[i]);
    	return keySet;
    }
    
    public ArrayList<WireConnection[]> values(){
    	ArrayList<WireConnection[]> valueList = new ArrayList<WireConnection[]>(size);
    	for (int i = 0; i < values.length; i++) {
			if(values[i] != null)
				valueList.add(values[i]);
		}
    	return valueList;
    }
}
