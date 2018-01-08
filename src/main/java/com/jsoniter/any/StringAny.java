package com.jsoniter.any;

import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;

import java.io.IOException;

/**
 * 
 * @author MaxiBon
 *
 */
class StringAny extends Any {
	/**
	 * FALSE
	 */
	private final static String FALSE = "false";
	/**
	 * val
	 */
	private String val;

	/**
	 * StringAny.
	 * 
	 * @param val
	 */
	StringAny(String val) {
		this.val = val;
	}

	/**
	 * valueType
	 */
	@Override
	public ValueType valueType() {
		return ValueType.STRING;
	}

	/**
	 * object
	 */
	@Override
	public Object object() {
		return val;
	}

	/**
	 * set
	 */
	public Any set(String newVal) {
		val = newVal;
		return this;
	}

	/**
	 * writeTo
	 * @param stream
	 * @throws java.io.IOException
	 */
	@Override
	public void writeTo(JsonStream stream) throws IOException {
		stream.writeVal(val);
	}

	/**
	 * toBooleanSupp
	 * 
	 * @param len
	 * @return
	 */
	public boolean toBooleanSupp(int len) {
		boolean supp = false;
		for (int i = 0; i < len; i++) {
			switch (val.charAt(i)) {
			case ' ':
				continue;
			case '\t':
				continue;
			case '\n':
				continue;
			case '\r':
				continue;
			default:
				supp = true;
			}
		}
		return supp;
	}

	/**
	 * toBoolean
	 */
	@Override
	public boolean toBoolean() {
		int len = val.length();
		boolean supp = true;
		if (len == 0) {
			supp = false;
		}
		if (len == 5 && FALSE.equals(val)) {
			supp = false;
		}
		if (supp) {
			supp = toBooleanSupp(len);
		}
		return supp;
	}

	/**
	 * toInt
	 */
	@Override
	public int toInt() {
		return Integer.valueOf(val);
	}

	/**
	 * toLong
	 */
	@Override
	public long toLong() {
		return Long.valueOf(val);
	}

	/**
	 * toFloat
	 */
	@Override
	public float toFloat() {
		return Float.valueOf(val);
	}

	/**
	 * toDouble
	 */
	@Override
	public double toDouble() {
		return Double.valueOf(val);
	}

	/**
	 * toString
	 */
	@Override
	public String toString() {
		return val;
	}
}
