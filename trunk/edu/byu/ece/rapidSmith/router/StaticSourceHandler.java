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
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;

import edu.byu.ece.rapidSmith.design.Attribute;
import edu.byu.ece.rapidSmith.design.Instance;
import edu.byu.ece.rapidSmith.design.Net;
import edu.byu.ece.rapidSmith.design.NetType;
import edu.byu.ece.rapidSmith.design.PIP;
import edu.byu.ece.rapidSmith.design.Pin;
import edu.byu.ece.rapidSmith.device.Device;
import edu.byu.ece.rapidSmith.device.PrimitiveSite;
import edu.byu.ece.rapidSmith.device.PrimitiveType;
import edu.byu.ece.rapidSmith.device.Tile;
import edu.byu.ece.rapidSmith.device.Wire;
import edu.byu.ece.rapidSmith.device.WireDirection;
import edu.byu.ece.rapidSmith.device.WireEnumerator;
import edu.byu.ece.rapidSmith.device.WireType;
import edu.byu.ece.rapidSmith.router.PinSorter.StaticSink;
import edu.byu.ece.rapidSmith.util.MessageGenerator;

/**
 * This class separates the static nets in to separate nets so they can be routed
 * more easily by the router.  This same process happens in Xilinx PAR during the routing
 * phase.
 * @author Chris Lavin
 * Created on: Jun 10, 2010
 */
public class StaticSourceHandler{

	/** Current Router */
	private AbstractRouter router;
	/** Current Device */
	private Device dev;
	/** Current WireEnumerator */
	private WireEnumerator we;
	/** Number of nets created and makes them unique by name */
	private int netCount;
	/** Final set of static nets */
	private ArrayList<Net> finalStaticNets;
	/** Special switch matrix sink wires that need HARD1 */
	HashSet<Integer> needsHard1;
	/** Special switch matrix sink wires that need a SLICE as a static source */
	HashSet<Integer> needsNonTIEOFFSource;
	/** Default pin on SLICE to be used as static output source */
	String slicePin;
	/** Just a node that is used so it doesn't have to be created over and over */
	private Node tempNode;
	/** Map to help retrieve the FAN and BOUNCE connections in Virtex 5 */
	private static HashMap<String,String[]> fanBounceMap;
	/** List of OMUX Top Wires in Virtex 4 Switch Box */
	private static String[] v4TopOmuxs = {"OMUX14","OMUX9","OMUX8","OMUX13","OMUX11","OMUX15","OMUX12","OMUX10"};
	/** List of OMUX Bottom Wires in Virtex 4 Switch Box */
	private static String[] v4BottomOmuxs = {"OMUX0","OMUX4","OMUX3","OMUX5","OMUX2","OMUX7","OMUX6","OMUX1"};
	
	// Attributes used in creating TIEOFFs
	private Attribute noUserLogicAttr;
	private Attribute hard1Attr;
	private Attribute keep1Attr;
	private Attribute keep0Attr;
	
	/**
	 * Constructor
	 * @param router The router to be used with this static source handler.
	 */
	public StaticSourceHandler(AbstractRouter router){
		this.router = router;
		dev = router.dev;
		we = router.we;
		netCount = 0;
		finalStaticNets = new ArrayList<Net>();
		needsHard1 = getPinsNeedingHardPowerSource(dev.getPartName(), we);
		needsNonTIEOFFSource = getPinsNeedingNonTIEOFFSource(dev.getPartName(), we);
		tempNode = new Node();
		
		if(router.dev.getPartName().startsWith("xc5v")){
			slicePin = "B";
		}
		else if(router.dev.getPartName().startsWith("xc4v")){
			slicePin = "Y";
		}
		else{
			System.out.println("Sorry, this architecture is not supported.");
			System.exit(1);
		}
		
		// Initialize attributes
		noUserLogicAttr = new Attribute("_NO_USER_LOGIC","","");
		hard1Attr = new Attribute("_VCC_SOURCE","","HARD1");
		keep1Attr = new Attribute("_VCC_SOURCE","","KEEP1");
		keep0Attr = new Attribute("_GND_SOURCE","","KEEP0");
	}
	
	private void addReservedNode(Node node, Net net){
		ArrayList<Node> nodes = router.reservedNodes.get(net);
		if(nodes == null){
			nodes = new ArrayList<Node>();
			router.reservedNodes.put(net, nodes);
		}
		nodes.add(node);
		router.usedNodes.add(node);
	}
	
	public Node getSwitchBoxWire(Net net){
		Pin source = net.getSource();
		
		if(source.getName().contains("COUT") && net.getPins().size() == 2){
			Pin sink = net.getPins().get(1).equals(source) ? net.getPins().get(0) : net.getPins().get(1);
			if(sink.getName().contains("CIN")){
				return null;
			}
		}
		
		Node curr = new Node(source.getTile(), dev.getPrimitiveExternalPin(source), null, 0);
		while(!we.getWireDirection(curr.getWire()).equals(WireDirection.CLK) && !we.getWireType(curr.getWire()).equals(WireType.INT_SOURCE)){
			Wire[] wires = curr.getWires();
			if(wires == null) return null;
			Wire w = wires[0];
			if(we.getWireName(w.getWire()).contains("COUT") && wires.length > 1 ){
				
				w = wires[1];
			}
			curr = new Node(w.getTile(dev, curr.tile),w.getWire(), null, 0);
		}
		
		return curr;
	}
	
	/**
	 * This function will remove the static source'd nets from inputDesign 
	 * and return them in their own list.  
	 */
	public void separateStaticSourceNets(){
		ArrayList<Net> netList = router.netList;
		ArrayList<Net> staticNetList = new ArrayList<Net>();
		HashMap<Tile, ArrayList<Net>> sourceCount = new HashMap<Tile, ArrayList<Net>>();
		
		// Normalize static source nets type name and separate them from design
		for(int i = 0; i < netList.size(); i++){			
			Net net = netList.get(i);
			NetType netType = net.getType();
			
			
			// Do re-entrant routing, if a net already has PIPs
			// on it we are going to assume it is routed
			if(net.getPIPs().size() > 0){
				for(PIP p : net.getPIPs()){
					router.setWireAsUsed(p.getTile(), p.getStartWire());
					router.setWireAsUsed(p.getTile(), p.getEndWire());
					router.checkForIntermediateUsedNodes(p);
				}
			}
			
			if(net.getSource() == null){
				// Sorts the nets into static sourced and wire nets
				if(netType == NetType.WIRE){
					if(!net.hasAttributes() && net.getModuleInstance() != null){
						MessageGenerator.briefErrorAndExit("This case has not yet been coded for" +
								" handling.  I believe it is a bad file. Net: " + net.getName());
					}
				}
				else if(netType.equals(NetType.VCC)){
					staticNetList.add(netList.get(i));
				}
				else if(netType.equals(NetType.GND)){
					staticNetList.add(netList.get(i));
				}
				else if(net.hasAttributes()){
					netType.equals(NetType.WIRE);
				}
				else{
					MessageGenerator.briefErrorAndExit(net.toString(we) + "The Net type: " + netType +
														" does not have a driver (outpin).");
				}
			}
			else{ // Let's look at the other nets, reserve nodes that they will need
				if(net.isStaticNet()){
					MessageGenerator.briefErrorAndExit("ERROR: Static Net found with a source: " + net.getName());
				}
				
				ArrayList<Node> rNodes = new ArrayList<Node>();
				for(Pin p : net.getPins()){
					if(!p.isOutPin()){
						int extPin = dev.getPrimitiveExternalPin(p);
						tempNode.setTileAndWire(p.getInstance().getTile(), extPin);
						Node reserved = tempNode.getSwitchBoxSink(dev);
						if(reserved.wire == -1) continue;
						if(router.usedNodes.contains(reserved)) continue;
						String name = we.getWireName(reserved.wire); 
						if(name.startsWith("BYP_INT_B")){
							rNodes.add(reserved);
						}
						else if(name.startsWith("BYP_B")){
							reserved.setWire(we.getWireEnum("BYP" + name.charAt(name.length()-1)));
							rNodes.add(reserved);
						}
						else if(name.startsWith("CTRL_B")){
							reserved.setWire(we.getWireEnum("CTRL" + name.charAt(name.length()-1)));
							rNodes.add(reserved);
						}
						else if(name.startsWith("FAN_B")){
							reserved.setWire(we.getWireEnum("FAN" + name.charAt(name.length()-1)));
							rNodes.add(reserved);
						}
					}
					else{
						ArrayList<Net> nets = sourceCount.get(p.getTile());
						if(nets == null) {
							nets = new ArrayList<Net>();
							sourceCount.put(p.getTile(), nets);
						}
						nets.add(net);
					}
				}
				if(rNodes.size() > 0){
					ArrayList<Node> nodes = router.reservedNodes.get(net);
					if(nodes == null){
						nodes = new ArrayList<Node>();
						router.reservedNodes.put(net,rNodes);
					}
					nodes.addAll(rNodes);

					router.usedNodes.addAll(rNodes);
				}
			}
		}
		
		/*
		 * Reserve OMUX wires for congested Virtex 4 switch boxes 
		 */
		if(dev.getPartName().startsWith("xc4v")){
			HashMap<Tile, ArrayList<Net>> switchMatrixSources = new HashMap<Tile, ArrayList<Net>>();
			for(Tile t : sourceCount.keySet()){
				ArrayList<Net> nets = sourceCount.get(t);
				for(Net n : nets){
					Node node = getSwitchBoxWire(n);
					if(node == null) continue;
					ArrayList<Net> tmp = switchMatrixSources.get(node.tile);
					if(tmp == null){
						tmp = new ArrayList<Net>();
						switchMatrixSources.put(node.tile,tmp);
					}
					tmp.add(n);
				}
			}
			for(Tile t : switchMatrixSources.keySet()){
				ArrayList<Net> nets = switchMatrixSources.get(t);
				boolean debug = false;
				if(nets.size() > 8){

					int reservedTop = 0;
					int reservedBot = 0;
					
					ArrayList<Net> secondaryNets = new ArrayList<Net>();
					for(Net n : nets){
						if(n.getPIPs().size() > 0) continue;
						Node node = getSwitchBoxWire(n);
						if(debug) System.out.println("Debugging " + t.getName() + " net: " + n.getName() + " node: " + node.toString(we));
						if(debug) System.out.println("NET: " + n.toString(we));
						if(node == null) continue;
						String wireName = we.getWireName(node.getWire()); 
						if(wireName.startsWith("HALF")){
							if(wireName.contains("TOP")){
								if(reservedTop > 7){
									break;
								}
								Node newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
								while(router.usedNodes.contains(newNode)){
									if(debug) System.out.println("HALF Reserved Top "+ reservedTop + " for " + v4TopOmuxs[reservedTop] + " net: " + n.getName() + " " + n.getSource().getName());
									reservedTop++;
									if(reservedTop > 7) {
										break;
									}
									newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
								}
								addReservedNode(newNode, n);
								if(debug) System.out.println("HALF Reserved Top "+ reservedTop + " for " + v4TopOmuxs[reservedTop] + " net: " + n.getName() + " " + n.getSource().getName());
								reservedTop++;									
							}
							else if(wireName.contains("BOT")){
								if(reservedBot > 7){
									break;
								}
								Node newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
								while(router.usedNodes.contains(newNode)){
									if(debug) System.out.println("HALF Reserved Bot "+ reservedTop + " for " + v4BottomOmuxs[reservedBot] + " net: " + n.getName() + " " + n.getSource().getName());
									reservedBot++;
									if(reservedBot > 7){
										break;
									}
									newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
								}
								addReservedNode(newNode, n);
								if(debug) System.out.println("HALF Reserved Bot "+ reservedTop + " for " + v4BottomOmuxs[reservedBot] + " net: " + n.getName() + " " + n.getSource().getName());
								reservedBot++;
							}
						}
						else if(wireName.startsWith("SECONDARY")){
							secondaryNets.add(n);
						}
					}
					for(Net n : secondaryNets){
						Node node = getSwitchBoxWire(n);
						if(node == null) continue;
						if(reservedTop < 8){
							Node newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
							while(router.usedNodes.contains(newNode)){
								if(debug) System.out.println("SEC Reserved Top "+ reservedTop + " for " + v4TopOmuxs[reservedTop] + " net: " + n.getName() + " " + n.getSource().getName());
								reservedTop++;
								if(reservedTop > 7) break;
								newNode = new Node(node.tile, we.getWireEnum(v4TopOmuxs[reservedTop]), null, 0);
							}
							addReservedNode(newNode, n);
							if(debug) System.out.println("SEC Reserved Top "+ reservedTop + " for " + v4TopOmuxs[reservedTop] + " net: " + n.getName() + " " + n.getSource().getName());
							reservedTop++;						
						}
						else if(reservedBot < 8){
							Node newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
							while(router.usedNodes.contains(newNode)){
								if(debug) System.out.println("SEC Reserved Bot "+ reservedBot + " for " + v4BottomOmuxs[reservedBot] + " net: " + n.getName() + " " + n.getSource().getName());
								reservedBot++;
								if(reservedBot > 7) {
									break;
								}
								newNode = new Node(node.tile, we.getWireEnum(v4BottomOmuxs[reservedBot]), null, 0);
							}
							addReservedNode(newNode, n);							
							if(debug) System.out.println("SEC Reserved Bot "+ reservedBot + " for " + v4BottomOmuxs[reservedBot] + " net: " + n.getName() + " " + n.getSource().getName());
							reservedBot++;												
						}
						else{
							break;
						}
					}
				}
			}			
		}		
		
		// Remove all static source'd nets afterwards from original design
		netList.removeAll(staticNetList);
		
		HashMap<Tile,PinSorter> tileMap = new HashMap<Tile, PinSorter>();
		
		// Iterate through all static nets and create mapping of all sinks to their
		// respective switch matrix tile, each pin is separated into groups of how their
		// sources should be created. There are 3 groups:
		// 1. High priority TIEOFF sinks - Do the best you can to attach these sinks to the TIEOFF
		// 2. Attempt TIEOFF sinks - Attempt to connect them to a TIEOFF, but not critical
		// 3. SLICE Source - Instance a nearby slice to supply GND/VCC
		for(Net net : staticNetList){
			for(Pin pin : net.getPins()){
				// Switch matrix sink, where the route has to connect through
				Node switchMatrixSink  = dev.getSwitchMatrixSink(pin);
				PinSorter tmp = tileMap.get(switchMatrixSink.tile);
				if(tmp == null){
					tmp = new PinSorter();
					tileMap.put(switchMatrixSink.tile, tmp);
				}
				tmp.addPin(switchMatrixSink, pin, net, needsHard1, needsNonTIEOFFSource);
			}
		}
		
		// For every switch matrix tile we have found that requires static sink connections
		for(Tile tile : tileMap.keySet()){
			PinSorter ps = tileMap.get(tile);
			tempNode.setTile(tile);
			ArrayList<StaticSink> removeThese = new ArrayList<StaticSink>();
			
			// Virtex 5 has some special pins that we should reserve
			if(dev.getPartName().startsWith("xc5v")){
				for(StaticSink ss : ps.highPriorityForTIEOFF){
					String[] fans = fanBounceMap.get(we.getWireName(ss.node.wire));
					Node newNode = null;
					for(String fan : fans){
						tempNode.setWire(we.getWireEnum(fan));
						// Add this to reserved
						if(!router.usedNodes.contains(tempNode)){
							newNode = new Node(tile, we.getWireEnum(fan), null, 0);
							String wireName = we.getWireName(newNode.wire);
							if(wireName.equals("FAN0") && !we.getWireName(ss.node.wire).equals("FAN_B0")){
								ss.node.tile = dev.getTile(ss.node.tile.getRow()-1, ss.node.tile.getColumn());
								newNode.tile = ss.node.tile;
							}
							else if(wireName.equals("FAN7") && !we.getWireName(ss.node.wire).equals("FAN_B7")){
								ss.node.tile = dev.getTile(ss.node.tile.getRow()+1, ss.node.tile.getColumn());
								newNode.tile = ss.node.tile;
							}
							break;
						}
					}
					if(newNode == null){
						removeThese.add(ss);
						ps.useSLICE.add(ss);
					}
				}
				ps.highPriorityForTIEOFF.removeAll(removeThese);				
			
				removeThese = new ArrayList<StaticSink>();
				for(StaticSink ss : ps.attemptTIEOFF){
					if(ss.net.getType().equals(NetType.GND)){
						String[] fans = fanBounceMap.get(we.getWireName(ss.node.wire));
						boolean useSLICE = true;
						for(String fan : fans){
							tempNode.setWire(we.getWireEnum(fan));
							// Add this to reserved
							if(!router.usedNodes.contains(tempNode)){
								useSLICE = false;
								break;
							}
						}
						if(useSLICE){
							ps.useSLICE.add(ss);
							removeThese.add(ss);
						}
					}
				}
				ps.attemptTIEOFF.removeAll(removeThese);
			}
		}
		
		// Handle each group of sinks separately, allocating TIEOFF to those sinks according
		// to priority
		for(PinSorter ps : tileMap.values()){
			for(StaticSink ss : ps.highPriorityForTIEOFF){
				Instance inst = updateTIEOFF(ss.node.tile, ss.net, true);
				// Find the correct net corresponding to this TIEOFF if it exists
				Net matchingNet = null;
				for(Net net : inst.getNetList()){
					if(net.getType().equals(ss.net.getType()) && !net.getSource().getName().equals("KEEP1")){
						matchingNet = net;
						break;
					}
				}
				if(matchingNet == null){
					Net newNet = createNewNet(ss.net, ss.pin);
					finalStaticNets.add(newNet);
					inst.addToNetList(newNet);
					createAndAddPin(newNet, inst, true);
				}
				else{
					matchingNet.addPin(ss.pin);
					ss.pin.getInstance().addToNetList(matchingNet);
				}
			}
			
			for(StaticSink ss : ps.attemptTIEOFF){
				Instance inst = updateTIEOFF(ss.node.tile, ss.net, false);
				Net matchingNet = null;
				// Find the correct net corresponding to this TIEOFF if it exists
				for(Net net : inst.getNetList()){
					if(net.getType().equals(ss.net.getType()) && !net.getSource().getName().equals("HARD1")){
						matchingNet = net;
						break;
					}
				}
				if(matchingNet == null){
					Net newNet = createNewNet(ss.net, ss.pin);
					finalStaticNets.add(newNet);
					inst.addToNetList(newNet);
					createAndAddPin(newNet, inst, false);
				}
				else{
					matchingNet.addPin(ss.pin);
					ss.pin.getInstance().addToNetList(matchingNet);
				}
			}
			
			if(ps.useSLICE.size() > 0){
				ArrayList<Pin> gnds = new ArrayList<Pin>();
				ArrayList<Pin> vccs = new ArrayList<Pin>();
				for(StaticSink ss : ps.useSLICE){
					if(ss.net.getType().equals(NetType.GND)){
						gnds.add(ss.pin);
					}
					else if(ss.net.getType().equals(NetType.VCC)){
						vccs.add(ss.pin);
					}
				}		
				
				if(gnds.size() > 0){
					// Create the new net
					Net newNet = createNewNet(ps.useSLICE.get(0).net, gnds);
					finalStaticNets.add(newNet);

					// Create new instance of SLICE primitive to get source
					Instance currInst = findClosestAvailableSLICE(ps.useSLICE.get(0).node.tile, ps.useSLICE.get(0).net.getType());
					
					router.design.addInstance(currInst);

					currInst.addToNetList(newNet);
					
					Pin source = new Pin(true, slicePin, currInst); 
					newNet.addPin(source);
					source.getInstance().addToNetList(newNet);
				}
				if(vccs.size() > 0){
					// Create the new net
					Net newNet = createNewNet(ps.useSLICE.get(0).net, vccs);
					finalStaticNets.add(newNet);

					// Create new instance of SLICE primitive to get source
					Instance currInst = findClosestAvailableSLICE(ps.useSLICE.get(0).node.tile, ps.useSLICE.get(0).net.getType());
					
					router.design.addInstance(currInst);

					currInst.addToNetList(newNet);
					
					Pin source = new Pin(true, slicePin, currInst); 
					newNet.addPin(source);
					source.getInstance().addToNetList(newNet);
				}
			}
		}
		
		// Re order and assemble nets for router
		ArrayList<Net> tmpList = new ArrayList<Net>();
		
		finalStaticNets = orderGNDNetsFirst(finalStaticNets);
		
		tmpList.addAll(finalStaticNets);
		tmpList.addAll(netList);
		router.netList = tmpList;
	}
	
	/**
	 * Helper method that creates and adds a pin to a net
	 * @param net The net to add the pin to.
	 * @param inst The instance the pin belongs to.
	 */
	private void createAndAddPin(Net net, Instance inst, boolean needHard1){
		String pinName;
		if(net.getType().equals(NetType.GND)){
			pinName = "HARD0";
		}
		else if(needHard1){
			pinName = "HARD1";
		}
		else{
			pinName = "KEEP1";
		}
		Pin source = new Pin(true,pinName, inst); 
		net.addPin(source);
		inst.addToNetList(net);
	}
	
	/**
	 * Creates or updates the appropriate TIEOFF to act as a source for a given net.
	 * @param net The net driven by the TIEOFF.
	 * @param needHard1 Determines if the source should be a HARD1.
	 * @return The created/updated TIEOFF.
	 */
	private Instance updateTIEOFF(Tile tile, Net net, boolean needHard1){
		String tileSuffix = tile.getTileNameSuffix();
		String instName = "XDL_DUMMY_INT" + tileSuffix + "_TIEOFF" + tileSuffix;
		Instance currInst = router.design.getInstance(instName);
		Attribute vccAttr = needHard1 ? hard1Attr : keep1Attr;
		// Add the appropriate attribute if instance already exists
		if(currInst != null){
			if(net.getType().equals(NetType.VCC)){
				// Add HARD1
				if(!currInst.hasAttribute(vccAttr.getPhysicalName())){
					currInst.addAttribute(vccAttr);
				}
			}
			else if(net.getType().equals(NetType.GND)){
				if(!currInst.hasAttribute(keep0Attr.getPhysicalName())){
					currInst.addAttribute(keep0Attr);
				}
			}
		}
		// Add the instance (it doesn't exist yet)
		else{
			currInst = new Instance();
			currInst.place(router.dev.getPrimitiveSite("TIEOFF" + tileSuffix));
			currInst.setType(PrimitiveType.TIEOFF);
			currInst.setName(instName);
			currInst.addAttribute(noUserLogicAttr);
			if(net.getType().equals(NetType.VCC)){
				// Add HARD1
				currInst.addAttribute(vccAttr);
			}
			else if(net.getType().equals(NetType.GND)){
				currInst.addAttribute(keep0Attr);
			}
			router.design.addInstance(currInst);
		}

		return currInst;
	}

	/**
	 * Creates a new net based on the staticNet and will contain the newPinList
	 * @param staticNet Parent net to create new net from
	 * @param newPinList The new set of pins for the new net
	 * @return The newly created net
	 */
	private Net createNewNet(Net staticNet, ArrayList<Pin> newPinList){
		Net newNet = new Net();
		newNet.setPins(newPinList);
		newNet.setName(staticNet.getName() + "_" + netCount);
		newNet.setType(staticNet.getType());
		netCount++;
		return newNet;
	}
	
	/**
	 * Creates a new net based on the staticNet and will contain the newPin
	 * @param staticNet Parent net to create new net from
	 * @param newPin The new pin of the new net
	 * @return The newly created net
	 */
	private Net createNewNet(Net staticNet, Pin newPin){
		Net newNet = new Net();
		newNet.addPin(newPin);
		newPin.getInstance().addToNetList(newNet);
		newNet.setName(staticNet.getName() + "_" + netCount);
		newNet.setType(staticNet.getType());
		netCount++;
		return newNet;
	}
		
	/**
	 * Finds an available SLICE to be used as a static source.  
	 * @param tile The tile where the sink to be driven is located
	 * @return The newly created instance of the SLICE to source the sink
	 */
	private Instance findClosestAvailableSLICE(Tile tile, NetType sourceType) {
		PrimitiveSite p = null;
		final Tile sink = router.dev.getTile(tile.toString());
		
		// Create a Priority Queue that prioritizes by the closest Manhattan distance
		PriorityQueue<PrimitiveSite> queueSLICEs =  new PriorityQueue<PrimitiveSite>(16, new Comparator<PrimitiveSite>(){
				public int compare(PrimitiveSite i, PrimitiveSite j){
					int iDist = Math.abs(sink.getRow() - i.getTile().getRow()) + 
								Math.abs(sink.getColumn() - i.getTile().getColumn());
					int jDist = Math.abs(sink.getRow() - j.getTile().getRow()) + 
								Math.abs(sink.getColumn() - j.getTile().getColumn());
					
					return iDist-jDist;
				}
			}
		);
		
		// Add all SLICEs on the chip to the queue
		Set<String> set = router.dev.getPrimitiveSites().keySet();
		Iterator<String> itr = set.iterator();
		while(itr.hasNext()){
			p = router.dev.getPrimitiveSite(itr.next());
			if(p.getType() == PrimitiveType.SLICEL || p.getType() == PrimitiveType.SLICEM){
				if(!router.design.getUsedPrimitiveSites().contains(p)){
					queueSLICEs.add(p);
				}
			}
		}
		
		// Iterate through the SLICEs looking for a SLICE that is unused
		// If one is found, create the instance and return it
		while((p = queueSLICEs.poll()) != null){
			Instance returnMe = new Instance();
			ArrayList<Attribute> list = new ArrayList<Attribute>();
			list.add(new Attribute("_NO_USER_LOGIC","",""));
			if(sourceType.equals(NetType.VCC)){
				list.add(new Attribute("_VCC_SOURCE","",slicePin));	
			}
			else{
				list.add(new Attribute("_GND_SOURCE","",slicePin));
			}
			
			returnMe.place(p);
			returnMe.setType(p.getType());
			returnMe.setAttributes(list);
			returnMe.setName("XDL_DUMMY_" + returnMe.getTile() + "_" + p.getName());
			
			return returnMe;
		}
		
		System.out.println("Could not find a free SLICE for a static source, I am giving up.");
		System.exit(1);
		return null;
	}
	
	/**
	 * This method generates a set of wires that will require the TIEOFF HARD1 pin rather than
	 * the KEEP1 pin.  
	 * @param partName Name of the part to generate the set for.
	 * @param we The wire enumerator corresponding to the supplied part.
	 * @return A set of switch box wires the require a TIEOFF HARD1 pin connection based on the supplied part. 
	 */
	public static HashSet<Integer> getPinsNeedingHardPowerSource(String partName, WireEnumerator we){
		HashSet<Integer> set = new HashSet<Integer>();
		if(partName.startsWith("xc4v")){
			for(int i = 0; i < 4; i++){
				set.add(we.getWireEnum("SR_B" + i));
				set.add(we.getWireEnum("CE_B" + i));
			}
			return set;
		}
		else if(partName.startsWith("xc5v")){
			set.add(we.getWireEnum("CLK_B0"));
			set.add(we.getWireEnum("CLK_B1"));
			for(int i = 0; i < 8; i++){
				set.add(we.getWireEnum("FAN_B" + i));				
			}
			return set;
		}
		else{
			MessageGenerator.briefErrorAndExit("Sorry, this architecture is not yet supported.");
			return null;
		}
	}
	
	/**
	 * This method generates a set of wires in a particular part switch box that require
	 * a SLICE to supply the static source.  This is true of CLB_B wires in Virtex 4.
	 * @param partName Name of the part to generate the set for.
	 * @param we The wire enumerator corresponding to the supplied part.
	 * @return The set of all switch matrix wires that require a SLICE to supply a static source.
	 */
	public static HashSet<Integer> getPinsNeedingNonTIEOFFSource(String partName, WireEnumerator we){
		HashSet<Integer> set = new HashSet<Integer>();
		if(partName.startsWith("xc4v")){
			for(int i = 0; i < 4; i++){
				set.add(we.getWireEnum("CLK_B" + i));
			}
			return set;
		}
		else if(partName.startsWith("xc5v")){
			return set;
		}
		else {
			MessageGenerator.briefErrorAndExit("Sorry, this architecture is not yet supported.");
			return null;
		}
	}
	
	/**
	 * This function makes sure that all the GND and VCC sources are grouped together
	 * @param inputList The input static source net list
	 * @return The grouped net list with GND nets first
	 */
	public ArrayList<Net> orderGNDNetsFirst(ArrayList<Net> inputList){
		ArrayList<Net> gndNets = new ArrayList<Net>();
		ArrayList<Net> vccNets = new ArrayList<Net>();
		
		for(Net net : inputList){
			if(net.getType().equals(NetType.GND)){
				gndNets.add(net);
			}
			else if(net.getType().equals(NetType.VCC)){
				vccNets.add(net);
			}
			else{
				MessageGenerator.briefErrorAndExit("Error: found non-static net in static netlist.");
			}
		}
		gndNets.addAll(vccNets);
		return gndNets;
	}
	
	// This is to help remove routing conflicts
	static {
		fanBounceMap = new HashMap<String, String[]>();
		String[] array0 = {"FAN2", "FAN7", };
		fanBounceMap.put("BYP_B0", array0);
		String[] array1 = {"FAN2", "FAN7"};
		fanBounceMap.put("BYP_B1", array1);
		String[] array2 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B2", array2);
		String[] array3 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B3", array3);
		String[] array4 = {"FAN2", "FAN7"};
		fanBounceMap.put("BYP_B4", array4);
		String[] array5 = {"FAN2", "FAN7"};
		fanBounceMap.put("BYP_B5", array5);
		String[] array6 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B6", array6);
		String[] array7 = {"FAN5", "FAN0"};
		fanBounceMap.put("BYP_B7", array7);
		String[] array8 = {"FAN4", "FAN1"};
		fanBounceMap.put("CLK_B0", array8);
		String[] array9 = {"FAN6", "FAN3"};
		fanBounceMap.put("CLK_B1", array9);
		String[] array10 = {"FAN4", "FAN1"};
		fanBounceMap.put("CTRL_B0", array10);
		String[] array11 = {"FAN4", "FAN1"};
		fanBounceMap.put("CTRL_B1", array11);
		String[] array12 = {"FAN6", "FAN3"};
		fanBounceMap.put("CTRL_B2", array12);
		String[] array13 = {"FAN6", "FAN3"};
		fanBounceMap.put("CTRL_B3", array13);
		String[] array14 = {"FAN4", "FAN1"};
		fanBounceMap.put("GFAN0", array14);
		String[] array15 = {"FAN6", "FAN3"};
		fanBounceMap.put("GFAN1", array15);
		String[] array16 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B0", array16);
		String[] array17 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B1", array17);
		String[] array18 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B10", array18);
		String[] array19 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B11", array19);
		String[] array20 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B12", array20);
		String[] array21 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B13", array21);
		String[] array22 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B14", array22);
		String[] array23 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B15", array23);
		String[] array24 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B16", array24);
		String[] array25 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B17", array25);
		String[] array26 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B18", array26);
		String[] array27 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B19", array27);
		String[] array28 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B2", array28);
		String[] array29 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B20", array29);
		String[] array30 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B21", array30);
		String[] array31 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B22", array31);
		String[] array32 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B23", array32);
		String[] array33 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B24", array33);
		String[] array34 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B25", array34);
		String[] array35 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B26", array35);
		String[] array36 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B27", array36);
		String[] array37 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B28", array37);
		String[] array38 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B29", array38);
		String[] array39 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B3", array39);
		String[] array40 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B30", array40);
		String[] array41 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B31", array41);
		String[] array42 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B32", array42);
		String[] array43 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B33", array43);
		String[] array44 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B34", array44);
		String[] array45 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B35", array45);
		String[] array46 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B36", array46);
		String[] array47 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B37", array47);
		String[] array48 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B38", array48);
		String[] array49 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B39", array49);
		String[] array50 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B4", array50);
		String[] array51 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B40", array51);
		String[] array52 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B41", array52);
		String[] array53 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B42", array53);
		String[] array54 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B43", array54);
		String[] array55 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B44", array55);
		String[] array56 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B45", array56);
		String[] array57 = {"FAN5", "FAN3", "FAN0"};
		fanBounceMap.put("IMUX_B46", array57);
		String[] array58 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B47", array58);
		String[] array59 = {"FAN5", "FAN0"};
		fanBounceMap.put("IMUX_B5", array59);
		String[] array60 = {"FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B6", array60);
		String[] array61 = {"FAN4", "FAN2", "FAN7"};
		fanBounceMap.put("IMUX_B7", array61);
		String[] array62 = {"FAN6", "FAN4", "FAN1"};
		fanBounceMap.put("IMUX_B8", array62);
		String[] array63 = {"FAN6", "FAN3", "FAN1"};
		fanBounceMap.put("IMUX_B9", array63);
		String[] array64 = {"FAN0"};
		fanBounceMap.put("FAN_B0", array64);
		String[] array65 = {"FAN1"};
		fanBounceMap.put("FAN_B1", array65);
		String[] array66 = {"FAN2"};
		fanBounceMap.put("FAN_B2", array66);
		String[] array67 = {"FAN3"};
		fanBounceMap.put("FAN_B3", array67);
		String[] array68 = {"FAN4"};
		fanBounceMap.put("FAN_B4", array68);
		String[] array69 = {"FAN5"};
		fanBounceMap.put("FAN_B5", array69);
		String[] array70 = {"FAN6"};
		fanBounceMap.put("FAN_B6", array70);
		String[] array71 = {"FAN7"};
		fanBounceMap.put("FAN_B7", array71);
	}
}
