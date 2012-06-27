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

package edu.byu.ece.rapidSmith.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class parses command-line arguments in a mostly-Posix compliant form.
 * The class reads in an array of strings, each representing an option.  parse()
 * then parses the passed in arguments based off of the previously provided rules.
 * Options can then be checked to see if they were set and attributed values with
 * the optionDefined() and getOption() methods.
 *
 * Rule syntax
 * OPTION... [+$?] [default-value]
 *
 * OPTION :
 *   -<character> specifies a short-form option
 *   --<string> specifies a long-form option, separate word with '-'
 *   Options are separated by a space
 *
 * $ and ? characters indicate if the options expect accompanying values
 * If a $ or ? + exists, the parser will look for a value to accompany the option
 * If no $ or ? exists in the rule, no value accompanies the rule
 *
 * $ indicates that the option expects an associated value with it.
 *    Another occurrence of the option will result in an InvalidArgumentException
 *    getOption() will return a String object of the value
 * ? indicates that the option may optionally have an associated value.
 *    getOption() will return a String object of the value or null if
 *    it is not specified
 *
 * Examples :
 * "-a" - an short-option with no value
 * "-a --aardvark" - a short and long version of the same option
 * "-a $" - a short-optoin with a single accompanying value
 * Created on: Jun 23, 2012
 */
public class ArgsParser {
	HashMap<Object, Option> optionMap;
	ArrayList<String> arguments;

	/**
	 * Contructor for ArgsParser.  Takes in an array of Strings specifying the
	 * option rules.  See above for details"
	 * @param usage - the array of rules represented as strings
	 */
	public ArgsParser(String[] usage) {
		optionMap = new HashMap<Object, Option> ();
		arguments = new ArrayList<String> ();
		setParameters(usage);
	}

	/**
	 * Handles the parsing of the rules.
	 */
	private void setParameters(String[] usage) {
		for (String definition : usage) {
			// Create and set the defaults for the new option
			Option option = new Option();

			definition = definition.trim();

			// make sure the string starts with an option
			if (definition.length() > 0 && definition.charAt(0) != '-')
				throw new InvalidOptionException("No option specified");

			// parse option identifiers
			do {
				// ensure the string is valid
				if (definition.length() < 2)
					throw new InvalidOptionException("Improperly formatted option");

				// check if the option name is of a short or long option style
				if (definition.charAt(1) == '-') {
					// long option
					if (definition.length() < 3)
						throw new InvalidOptionException("Improperly formatted option");

					String optionName = definition.substring(2, indexOfOptionEnd(definition));
					definition = definition.substring(indexOfOptionEnd(definition));
					if (optionMap.put(optionName, option) != null)
						throw new InvalidOptionException("Duplicate option " + optionName);
				} else {
					// short option
					char optionName = definition.charAt(1);
					definition = definition.substring(2);
					if (optionMap.put(optionName, option) != null)
						throw new InvalidOptionException("Duplicate option " + optionName);

					if (definition.length() > 0 && !Character.isWhitespace(definition.charAt(0)))
						throw new InvalidOptionException("Improperly formatted option");
				}

				definition = definition.trim();
			} while(definition.length() > 0 && definition.charAt(0) == '-');

			// is this a flag option
			if (definition.length() == 0) {
				option.argType = Option.FLAG;
				continue;
			}

			// check if option expects accompanying value
			switch(definition.charAt(0)) {
				case '$' :
					option.argType = Option.REQUIRED;
					option.value = new ArrayList<String> ();
					break;
				case '?' :
					option.argType = Option.OPTIONAL;
					option.value = new ArrayList<String> ();
					break;
				default :
					throw new InvalidOptionException("Unknown value option " + definition.charAt(0));
			}

			definition = definition.substring(1).trim();

			// check for any superfluous info
			if (definition.length() > 0)
				option.defaultArg = definition;
		}
	}

	/**
	 * Parses the command-line arguments args
	 * @param args - array of command-line arguments
	 */
	public void parse(String[] args) {
		boolean argsEnabled = true;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			if (argsEnabled && arg.startsWith("--")) {
				if (arg.length() > 2)
					i = parseLong(i, args);
				else
					argsEnabled = false;
			} else if (argsEnabled && arg.startsWith("-")) {
				i = parseShort(i, args);
			} else {
				arguments.add(arg);
			}
		}
	}

	private int parseLong(int index, String[] args) {
		String arg = args[index];
		String optionName = arg.substring(2);
		String value = null;

		if (optionName.length() == 0)
			throw new InvalidArgumentException("Syntax error");

		// check if a value is associated with the argument
		int eqIndex = arg.indexOf('=');
		if (eqIndex != -1) {
			optionName = arg.substring(2, eqIndex);
			value = arg.substring(eqIndex+1);
		}

		// check if the option is valid
		Option option = optionMap.get(optionName);
		if (option == null)
			throw new InvalidArgumentException("Unknown option --" + optionName + ".");

		option.defined = true;

		// set the argument
		switch(option.argType) {
			case Option.FLAG :
				// arguments are not allow for FLAG option
				if (value != null)
					throw new InvalidArgumentException("Option '--" + optionName + "' does not take a value.");
				break;
			case Option.OPTIONAL :
				// check if an arguments accompanies this option
				if (value != null) {
					option.value.add(value);
					break;
				}

				// if the next argument does not start with a '-', take it
				// as an argument to this option
				if (args.length > index + 1) {
					value = args[index+1];
					if (value.length() == 0 || value.charAt(0) != '-') {
						option.value.add(value);
						index++;
					}
				} else {
					option.value.add(null);
				}
				break;
			case Option.REQUIRED :
				// These must have an argument
				if (value == null) {
					if (args.length <= index + 1)
						throw new InvalidArgumentException("No argument associated with option '--" + optionName + "'");
					value = args[++index];
				}

				option.value.add(value);
				break;
		}

		return index;
	}

	private int parseShort(int index, String[] args) {
		String arg = args[index];

		if (arg.length() < 2)
			throw new InvalidArgumentException("Syntax error: Unexpected -");

		// iterate through all characters to handle chains ie. "ls -al"
		ARG_FOR : for (int j = 1; j < arg.length(); j++) {
			char optionName = arg.charAt(j);

			// check if the option is valid
			Option option = optionMap.get(optionName);
			if (option == null)
				throw new InvalidArgumentException("Unknown option -" + optionName);

			option.defined = true;

			switch (option.argType) {
				case Option.FLAG :
					// Never have arguments
					break;
				case Option.OPTIONAL :
					// if more characters exist on this chain, engulf them
					if (arg.length() > j + 1) {
						option.value.add(arg.substring(j + 1));
						break ARG_FOR; // exit iteration over arg
					}

					// check if the next arguments is not an option
					// if not, then include it as the argument
					if (args.length > index + 1) {
						String value = args[index+1];
						if (value.length() == 0 || value.charAt(0) != '-') {
							option.value.add(value);
							index++;
						}
					} else {
						option.value.add(null);
					}
					break ARG_FOR; // exit iteration over arg
				case Option.REQUIRED :
					// If more characters exist on the chain, engulf them
					if (arg.length() > j + 1) {
						option.value.add(arg.substring(j+1));
						break ARG_FOR;
					}

					// the next argument is the value for this option
					if (args.length <= index + 1)
						throw new InvalidArgumentException("No argument associated with option '--" + optionName + "'");
					option.value.add(args[++index]);
					break ARG_FOR; // exit iteration over arg
			}
		}

		return index;
	}

	/**
	 * Returns whether the user specified this option.
	 * @param option - the option to check
	 * @return true if the user specified this option
	 */
	public boolean optionSpecified(char option) {
		return optionSpecified_work(option);
	}

	/**
	 * Long-option form of optionDefined.  See short form for details.
	 */
	public boolean optionSpecified(String option) {
		return optionSpecified_work(option);
	}

	private boolean optionSpecified_work(Object option) {
		Option o = optionMap.get(option);
		if (o == null)
			throw new InvalidOptionException("No option " + option);
		return o.defined;
	}

	/**
	 * Returns whether the option had an associated argument.
	 * @param option - the option to check
	 * @return true if the option had an associated argument
	 */
	public boolean argumentSet(char option) {
		return argumentSet_work(option);		
	}

	/**
	 * Long-option form of argumentSet.  See short form for details
	 * @param option
	 * @return
	 */
	public boolean argumentSet(String option) {
		return argumentSet_work(option);
	}
	
	private boolean argumentSet_work(Object option) {
		Option o = optionMap.get(option);
		if (o == null)
			throw new InvalidOptionException("No option " + option);

		if (o.argType == Option.FLAG)
			return false;
		if (o.argType == Option.REQUIRED)
			return true;
		if (o.value.isEmpty())
			return false;
		
		if (o.value.isEmpty())
			return false;
		
		// if the last option specified had a value, return true
		return o.value.get(o.value.size() - 1) != null;
	}

	/**
	 * Returns the arguments associated with the option.  If no arguments 
	 * were set, this returns an empty list.  If an optional argument was 
	 * set without an argument, the list will contain a null at that index. 
	 * A list with more than one element means the option was included more
	 * than once.
	 * @param option - the option to get arguments for
	 * @return the list of arguments for the option
	 */
	public ArrayList<String> getArguments(char option) {
		return getArguments_work(option);
	}

	/**
	 * The long-option form of getArguments.  See the short form for details
	 */
	public ArrayList<String> getArguments(String option) {
		return getArguments_work(option);
	}

	private ArrayList<String> getArguments_work(Object option) {
		Option o = optionMap.get(option);
		if (o == null)
			throw new InvalidOptionException("No option " + option);

		if (o.value == null)
			o.value = new ArrayList<String> ();

		return o.value;
	}

	/**
	 * Returns the argument for the indexed occurrence of the option.  0 will
	 * return the first occurrence of the option.  A negative index will return
	 * the occurrence from the last, ie. -1 will return the last occurrence.
	 * If the user never specifies the option, this method will return the
	 * default argument if one was specified, else null.
	 * 
	 * @param option - the option to check
	 * @param index - the occurrence to return
	 * @return the argument associated the option occurring index-th time
	 */
	public String getArgument(char option, int index) {
		return getArgument_work(option, index);
	}

	/**
	 * Long-option form of getArgument.  See short form for details.
	 * @param option
	 * @param index
	 * @return
	 */
	public String getArgument(String option, int index) {
		return getArgument_work(option, index);
	}

	public String getArgument_work(Object option, int index) {
		Option o = optionMap.get(option);
		if (o == null)
			throw new InvalidOptionException("No option " + option);

		if (o.value == null)
			return null;
		if (o.value.isEmpty())
			return o.defaultArg;

		if (index < 0)
			index = o.value.size() + index;

		return o.value.get(index);
	}

	/**
	 * Returns all non-option arguments in order of occurrence.
	 * @return a list of all non-option arguments
	 */
	public ArrayList<String> getOtherArguments() {
		return arguments;
	}

	private class Option {
		public static final int FLAG = 0;
		public static final int REQUIRED = 1;
		public static final int OPTIONAL = 3;

		int argType;
		boolean defined = false;
		String defaultArg = null;
		ArrayList<String> value = null;

		Option() { }
	}

	public class InvalidOptionException extends RuntimeException {
		private static final long serialVersionUID = 7618704076543193655L;

		public InvalidOptionException(String msg) {
			super(msg);
		}
	}

	public class InvalidArgumentException extends RuntimeException {
		private static final long serialVersionUID = 8961467564518579222L;

		public InvalidArgumentException(String msg) {
			super(msg);
		}
	}

	private static int indexOfOptionEnd(String s) {
		final int length = s.length();
		for (int i = 0; i < length; i++) {
			switch ( s.charAt( i ) ) {
				case ' ':
				case '\n':
				case '\r':
				case '\t':
					return i;
				default:
					// keep looking
				}
		   }
		return length;
	}

	public static void main(String[] args) {
		String[] rules = {"-v --verbose-c +", "-a"};
		ArgsParser ap = new ArgsParser(rules);
		ap.parse(args);
	}
}
