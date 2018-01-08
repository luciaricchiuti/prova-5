package com.jsoniter.any;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;
import com.jsoniter.JsonIteratorPool;
import com.jsoniter.ValueType;
import com.jsoniter.spi.JsonException;

import java.io.IOException;

/**
 * 
 * @author MaxiBon
 *
 */
class StringLazyAny extends LazyAny {
	/**
	 * false
	 */
	private final static String FALSE = "false";
	/**
	 * cache
	 */
	private String cache;
	/**
	 * err
	 */
	private final String err = "Error: IOException";

	/**
	 * StringLazyAny.
	 * 
	 * @param data
	 * @param head
	 * @param tail
	 */
	StringLazyAny(byte[] data, int head, int tail) {
		super(data, head, tail);
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
		fillCache();
		return cache;
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
			switch (cache.charAt(i)) {
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
		fillCache();
		boolean supp = true;
		int len = cache.length();
		if (len == 0) {
			supp = false;
		}
		if (len == 5 && FALSE.equals(cache)) {
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
		JsonIterator iter = parse();
		try {
			CodegenAccess.nextToken(iter);
			return iter.readInt();
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	/**
	 * toLong
	 */
	@Override
	public long toLong() {
		JsonIterator iter = parse();
		try {
			CodegenAccess.nextToken(iter);
			return iter.readLong();
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	/**
	 * toFloat
	 */
	@Override
	public float toFloat() {
		JsonIterator iter = parse();
		try {
			CodegenAccess.nextToken(iter);
			return iter.readFloat();
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	/**
	 * toDouble
	 */
	@Override
	public double toDouble() {
		JsonIterator iter = parse();
		try {
			CodegenAccess.nextToken(iter);
			return iter.readDouble();
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	/**
	 * toString
	 */
	@Override
	public String toString() {
		fillCache();
		return cache;
	}

	/**
	 * fillCache
	 */
	private void fillCache() {
		if (cache == null) {
			JsonIterator iter = parse();
			try {
				cache = iter.readString();
			} catch (IOException e) {
				throw new JsonException();
			} finally {
				JsonIteratorPool.returnJsonIterator(iter);
			}
		}
	}
}
