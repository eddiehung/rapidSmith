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
package edu.byu.ece.rapidSmith.router;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.Pin;


/**
 * This class is used by the StaticSourceHandler to sort pins based on
 * how they should be allocated for TIEOFFs.
 * @author Chris Lavin
 * Created on: Jul 13, 2010
 */
public class PinSorter{
	/** These are the pins that are the most needy of the TIEOFF source pins */
	LinkedList<StaticSink> highPriorityForTIEOFF;
	/** These are pins that could be source by the TIEOFF, but not as necessary */
	ArrayList<StaticSink> attemptTIEOFF;
	/** These are pins that will be sourced by a slice */
	ArrayList<StaticSink> useSLICE;
		
	/**
	 * Initializes data stuctures
	 */
	public PinSorter(){
		highPriorityForTIEOFF = new LinkedList<StaticSink>();
		attemptTIEOFF = new ArrayList<StaticSink>();
		useSLICE = new ArrayList<StaticSink>();
		
	}

	/**
	 * This methods sorts the pins as they are added.
	 * @param node The switch matrix sink node.
	 * @param pin The current sink pin being sorted.
	 * @param net The current net.
	 * @param needHard1 The set of wires that require a connection to HARD1 on the TIEOFF.
	 * @param needSLICE The set of wires that require a slice to supply the static source.
	 */
	public void addPin(Node node, Pin pin, Net net, HashSet<Integer> needHard1, HashSet<Integer> needSLICE){
		StaticSink ss = new StaticSink(node, pin, net);
		if(needHard1.contains(node.wire)) {
			highPriorityForTIEOFF.addFirst(ss);
		}
		else if(needSLICE.contains(node.wire)){
			useSLICE.add(ss);
		}
		else if(net.getType().equals(NetType.GND)){
			highPriorityForTIEOFF.addLast(ss);
		}
		else {
			attemptTIEOFF.add(ss);
		}
	}
	
	public void addPinToSliceList(Node node, Pin pin, Net net){
		StaticSink ss = new StaticSink(node, pin, net);
		useSLICE.add(ss);
	}
	
	/**
	 * This is just a small class to help the PinSorter class keep track of things.
	 * @author Chris Lavin
	 */
	public class StaticSink{

		public Node node;
		public Pin pin;
		public Net net;
		
		public StaticSink(Node node, Pin pin, Net net) {
			this.node = node;
			this.pin = pin;
			this.net = net;
		}
	}
}
