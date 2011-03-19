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
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.WireConnection;
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
	/** Some nodes are reserved for particular routes to minimize routing conflicts later */
	HashMap<Net,ArrayList<Node>> reservedNodes;

	/** PIPs that are part of the most recently routed connection */
	protected ArrayList<PIP> pipList;
	
	/** Keeps track of all current sources for a given net (to avoid the RUG CREATION PROBLEM) */
	protected HashSet<Node> currSources;
	/** Current sink node to be routed */
	protected Node currSink;
	/** Current net to be routed */
	protected Net currNet;
	/** Current sink pin to be routed */ 
	protected Pin currSinkPin;
	/** PIPs of the current net being routed */
	protected ArrayList<PIP> netPIPs;

	protected Node tempNode;
	
	/** A flag indicating if the current connection was routed successfully */
	protected boolean successfulRoute;
	/** A flag which determines if the current sink is a clock wire */
	protected boolean isCurrSinkAClkWire;
	
	// Statistic variables
	/** Total number of connections in design */
	protected int totalConnections;
	/** Counts the total number of nodes that were examined in routing */
	protected int totalNodesProcessed;
	/** Counts number of nodes processed during a route */
	protected int nodesProcessed;
	/** Counts the number of times the router failed to route a connection */
	protected int failedConnections;
	
	public AbstractRouter() {
		// Initialize variables
		tempNode = new Node();
		usedNodes = new HashSet<Node>();
		usedNodesMap = new HashMap<Node, LinkedList<Net>>();
		reservedNodes = new HashMap<Net, ArrayList<Node>>();
		// Create a compare function based on node's cost
		queue = new PriorityQueue<Node>(16, new Comparator<Node>() {
			public int compare(Node i, Node j) {return i.cost - j.cost;}});

		totalConnections = 0;
		totalNodesProcessed = 0;
		nodesProcessed = 0;
		failedConnections = 0;
		currSink = new Node();
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
	
	public boolean isNodeUsed(Tile tile, int wire){
		tempNode.setTileAndWire(tile, wire);
		return usedNodes.contains(tempNode);
	}
	
	public boolean isNodeUsed(Node node){
		return usedNodes.contains(node);
	}
	
	/**
	 * Checks each node in a PIP to see if there are other nodes that should be
	 * marked as used. These are wires external to a tile such as
	 * doubles/pents/hexes/longlines.
	 * 
	 * @param wire
	 */
	protected void checkForIntermediateUsedNodes(PIP wire){
		WireConnection[] wires = wire.getTile().getWireConnections(wire.getEndWire());
		if(wires != null && wires.length > 1){
			for(WireConnection w : wires){
				if(w.getRowOffset() != 0 || w.getColumnOffset() != 0){
					setWireAsUsed(w.getTile(dev, wire.getTile()), w.getWire());
				}
			}
		}
	}
}
