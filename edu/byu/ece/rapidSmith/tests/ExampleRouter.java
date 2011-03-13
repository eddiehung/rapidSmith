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
package edu.byu.ece.rapidSmith.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

import edu.byu.ece.rapidSmith.design.Design;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.WireConnection;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.router.Node;

public class ExampleRouter {

	public static ArrayList<Node> route(Pin src, Pin snk){
		Device dev = Device.getInstance("xc4vfx12ff668");
		PriorityQueue<Node> pq=new PriorityQueue<Node>();
		Node snkNode = dev.getNodeFromPin(snk);
		Node currNode = dev.getNodeFromPin(src);
		
		// Loop on queue output nodes to find the sink
		while(!currNode.equals(snkNode)){
			if(currNode.getConnections() != null)
				for(WireConnection w : currNode.getConnections()){
					Node n = w.getNodeFromWire(dev, currNode);
					n.setCost(n.getManhattanDistance(snkNode));
					if(!pq.contains(n)) pq.add(n);
				}				
			if(pq.isEmpty()) return null;
			currNode = pq.remove();
		}
		
		// When we have found the sink, reconstruct path
		ArrayList<Node> path = new ArrayList<Node>();
		while(currNode.getParent() != null){
			path.add(currNode);
			currNode = currNode.getParent();
		}
		return path;
	}
	
	public static void main(String[] args) {
		Design d = new Design();
		d.loadXDLFile(args[0]);
		Net n = d.getNet(args[1]);
		for(Node node : route(n.getSource(), n.getPins().get(1))){
			System.out.println(node.toString(d.getWireEnumerator()));
		}
	}
}
