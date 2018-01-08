package com.jsoniter.any;

import com.jsoniter.JsonIteratorPool;
import com.jsoniter.spi.JsonException;
import com.jsoniter.JsonIterator;
import com.jsoniter.ValueType;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.TypeLiteral;

import java.io.IOException;
/**
 * @author Maxibon
 *
 */
abstract class LazyAny extends Any {

	/**
	 * data
	 */
	protected final byte[] data;
	/**
	 * head
	 */
	protected final int head;
	/**
	 * tail
	 */
	protected final int tail;
	/**
	 * string
	 */
	private final String err = "Error: IOException";

	/**
	 * LazyAny.
	 * 
	 * @param data
	 * @param head
	 * @param tail
	 */
	protected LazyAny(byte[] data, int head, int tail) {
		this.data = data;
		this.head = head;
		this.tail = tail;
	}

	public abstract ValueType valueType();

	public final <T> T bindTo(T obj) {
		JsonIterator iter = parse();
		try {
			return iter.read(obj);
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	public final <T> T bindTo(TypeLiteral<T> typeLiteral, T obj) {
		JsonIterator iter = parse();
		try {
			return iter.read(typeLiteral, obj);
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	public final <T> T as(Class<T> clazz) {
		JsonIterator iter = parse();
		try {
			return iter.read(clazz);
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	public final <T> T as(TypeLiteral<T> typeLiteral) {
		JsonIterator iter = parse();
		try {
			return iter.read(typeLiteral);
		} catch (IOException e) {
			throw new JsonException(err);
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	public String toString() {
		return new String(data, head, tail - head);
	}

	protected final JsonIterator parse() {
		JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
		iter.reset(data, head, tail);
		return iter;
	}

	@Override
	public void writeTo(JsonStream stream) throws IOException {
		stream.write(data, head, tail - head);
	}
}
