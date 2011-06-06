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
package edu.byu.ece.rapidSmith.design;

import java.io.Serializable;

/**
 * This class represents the ports used to define the interfaces of modules
 * in XDL.  They consist of a unique name, the instance to which they are
 * connected and a pin name on the instance.
 * @author Chris Lavin
 * Created on: Jun 22, 2010
 */
public class Port implements Serializable, Cloneable{

	private static final long serialVersionUID = -8961782654770650827L;
	/** Name of the Port of the current module, this is the port of an instance in the module. */
	private String name;
	/** This is the pin that the port references. */
	private Pin pin;

	/**
	 * Default constructor, everything is null.
	 */
	public Port(){
		name = null;
		setPin(null);
	}
	

	/**
	 * @param name Name of the port.
	 * @param pin Pin which the port references
	 */
	public Port(String name, Pin pin){
		this.name = name;
		this.setPin(pin);
	}
	

	/**
	 * Gets and returns the name of the port.
	 * @return The name of the port.
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * Sets the name of the port.
	 * @param name The new name of the port.
	 */
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * Gets and returns the instance name.
	 * @return The name of the instance where this port resides.
	 */
	public String getInstanceName(){
		return pin.getInstanceName();
	}
	
	/**
	 *  Gets the pin name of the instance where the port resides.
	 * @return The pin name of the port.
	 */
	public String getPinName(){
		return pin.getName();
	}
	

	/**
	 * @param pin the pin to set
	 */
	public void setPin(Pin pin) {
		this.pin = pin;
	}

	/**
	 * @return the pin
	 */
	public Pin getPin() {
		return pin;
	}

	/**
	 * @return the instance
	 */
	public Instance getInstance() {
		return pin.getInstance();
	}

	/**
	 * Simply looks at the pin of the port to determine
	 * its direction.
	 * @return True if this port is an output, false otherwise.
	 */
	public boolean isOutPort(){
		return pin.isOutPin();
	}
	
	/**
	 * Generates hashCode for this port based on instance name, port name, and pin name.
	 */
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getInstanceName() == null) ? 0 : getInstanceName().hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((getPinName() == null) ? 0 : getPinName().hashCode());
		return result;
	}

	/**
	 * Checks if this and obj are equal ports by comparing port name,
	 * instance name and pin name.
	 */
	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		Port other = (Port) obj;
		if(getInstanceName() == null){
			if(other.getInstanceName() != null)
				return false;
		}
		else if(!getInstanceName().equals(other.getInstanceName()))
			return false;
		if(name == null){
			if(other.name != null)
				return false;
		}
		else if(!name.equals(other.name))
			return false;
		if(getPinName() == null){
			if(other.getPinName() != null)
				return false;
		}
		else if(!getPinName().equals(other.getPinName()))
			return false;
		return true;
	}
	
	@Override
	public String toString(){
		return "  port \"" + name + "\" \"" + pin.getInstanceName() + "\" \"" + pin.getName() +"\";";
	}
}
