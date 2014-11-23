package com.avaje.ebean.enhance.agent;

/**
 * Interface for reading class bytes as part of enhancement.
 * <p>
 * Used to parse inheritance objects when enhancing a given class.
 * </p>
 */
public interface ClassBytesReader {

	/**
	 * Return the class bytes for a given class.
	 */
	public byte[] getClassBytes(String className, ClassLoader classLoader);
}
