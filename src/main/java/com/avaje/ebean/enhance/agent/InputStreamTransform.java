package com.avaje.ebean.enhance.agent;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.IllegalClassFormatException;


/**
 * Utility object that handles input streams for reading and writing.
 */
public class InputStreamTransform {

	final Transformer transformer;
	final ClassLoader classLoader;
	
	public InputStreamTransform(Transformer transformer, ClassLoader classLoader){
		this.transformer = transformer;
		this.classLoader = classLoader;
	}
	
	public void log(int level, String msg){
		transformer.log(level, msg);
	}
	
	/**
	 * Transform a file.
	 */
	public byte[] transform(String className, File file) throws IOException, IllegalClassFormatException {
		try {
			return transform(className, new FileInputStream(file));
			
		} catch (FileNotFoundException e){
			throw new RuntimeException(e);
		}
	}

	/**
	 * Transform a input stream.
	 */
	public byte[] transform(String className, InputStream is) throws IOException, IllegalClassFormatException {

		try {
			
			byte[] classBytes = readBytes(is);
			
			return transformer.transform(classLoader, className, null, null, classBytes);
			
		} finally {
			if (is != null){
				is.close();
			}
		}
	}
	
	/**
	 * Helper method to write bytes to a file.
	 */
	public static void writeBytes(byte[] bytes, File file) throws IOException {
		writeBytes(bytes, new FileOutputStream(file));
	}

	/**
	 * Helper method to write bytes to a OutputStream.
	 */
	public static void writeBytes(byte[] bytes, OutputStream os) throws IOException {
		
		BufferedOutputStream bos = new BufferedOutputStream(os);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		
		byte[] buf = new byte[1028];
		
		int len = 0;
		while ((len = bis.read(buf, 0, buf.length)) > -1){
			bos.write(buf, 0, len);
		}
		
		bos.flush();
		bos.close();
		
		bis.close();
	}
	
	
	public static byte[] readBytes(InputStream is) throws IOException {
		
		BufferedInputStream bis = new BufferedInputStream(is);

		ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);

		byte[] buf = new byte[1028];
		
		int len = 0;
		while ((len = bis.read(buf, 0, buf.length)) > -1){
			baos.write(buf, 0, len);
		}
		baos.flush();
		baos.close();
		return baos.toByteArray();
	}
}
