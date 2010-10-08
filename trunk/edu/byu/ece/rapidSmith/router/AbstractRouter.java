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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireEnumerator;

public abstract class AbstractRouter{

	/** The XDL object that holds the input design to route */
	protected Design design;
	/** This is the device database */
	protected Device dev;
	/** Utility class to convert to/from wire names and enumerations */
	protected WireEnumerator we;
	/** This keeps track of all the used nodes in the chip during routing */
	protected HashSet<Node> usedNodes;
	/** Keeps track for each used node by which net it is used by */
	protected HashMap<Node,LinkedList<Net>> usedNodesMap;
	/** This keeps track of all the visited nodes in the chip during routing */
	protected HashSet<Node> visitedNodes;
	/** The current working net list */
	public ArrayList<Net> netList;
	/** A Priority Queue for nodes to be processed */
	protected PriorityQueue<Node> queue;
	
	
	public AbstractRouter() {
		// Initialize variables
		usedNodes = new HashSet<Node>();
		usedNodesMap = new HashMap<Node, LinkedList<Net>>();
		// Create a compare function based on node's cost
		queue = new PriorityQueue<Node>(16, new Comparator<Node>() {
			public int compare(Node i, Node j) {return i.cost - j.cost;}});
	}
	
	/**
	 * Sets a wire as used for the given routing
	 * @param t The tile in the node that is used
	 * @param wire The wire in the node that is used
	 */
	protected Node setWireAsUsed(Tile t, int wire){
		Node n = new Node(t, wire, null, 0);
		usedNodes.add(n);
		return n;
	}
	
	/**
	 * Checks each node in a PIP to see if there are other nodes that should be
	 * marked as used. These are wires external to a tile such as
	 * doubles/pents/hexes/longlines.
	 * 
	 * @param wire
	 */
	protected void checkForIntermediateUsedNodes(PIP wire){
		HashMap<Integer, Wire[]> wireMap = wire.getTile().getWires();
		if(wireMap != null){
			Wire[] wires = wireMap.get(wire.getEndWire());
			if(wires != null && wires.length > 1){
				for(Wire w : wires){
					if(w.getRowOffset() != 0 || w.getColumnOffset() != 0){
						setWireAsUsed(w.getTile(dev, wire.getTile()), w.getWire());
					}
				}
			}
		}
	}
}
