package com.jsoniter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.MapKeyDecoder;
import com.jsoniter.spi.Slice;
import com.jsoniter.spi.TypeLiteral;

/**
 * Public class CodegenAccess. only uesd by generated code to access decoder.
 * 
 * @author MaxiBon
 *
 */
public class CodegenAccess {

	private CodegenAccess() {
	}

	/**
	 * addMissingField.
	 * 
	 * @param missingFields
	 * @param tracker
	 * @param mask
	 * @param fieldName
	 */
	public static void addMissingField(List missingFields, long tracker, long mask, String fieldName) {
		if (SupportBitwise.bitwise(Long.toBinaryString(tracker), Long.toBinaryString(mask))) {
			missingFields.add(fieldName);
		}
	}

	public static <T extends Collection> T reuseCollection(T col) {
		col.clear();
		return col;
	}

	public static Object existingObject(JsonIterator iter) {
		return iter.existingObject;
	}

	public static Object resetExistingObject(JsonIterator iter) {
		Object obj = iter.existingObject;
		iter.existingObject = null;
		return obj;
	}

	public static void setExistingObject(JsonIterator iter, Object obj) {
		iter.existingObject = obj;
	}

	public final static boolean nextTokenIsComma(final JsonIterator iter) throws IOException {
		byte c = readByte(iter);
		if (c == ',') {
			return true;
		}
		return nextTokenIsCommaSlowPath(iter, c);
	}

	private static boolean nextTokenIsCommaSlowPath(JsonIterator iter, byte c) throws IOException {
		switch (c) {
		case ' ':
			break;
		case '\n':
			break;
		case '\r':
			break;
		case '\t':
			break;
		default:
			return false;
		}
		return nextToken(iter) == ',';
	}

	public static byte nextToken(JsonIterator iter) throws IOException {
		return IterImpl.nextToken(iter);
	}

	public static final boolean readBoolean(String cacheKey, JsonIterator iter) throws IOException {
		if (JsoniterSpi.getDecoder(cacheKey) instanceof com.jsoniter.spi.Decoder.BooleanDecoder) {
			return ((com.jsoniter.spi.Decoder.BooleanDecoder) JsoniterSpi.getDecoder(cacheKey)).decodeBoolean(iter);
		} else {
			throw new IOException();
		}
	}

	public static final short readShort(String cacheKey, JsonIterator iter) throws IOException {
		if (JsoniterSpi.getDecoder(cacheKey) instanceof com.jsoniter.spi.Decoder.ShortDecoder) {
			return ((com.jsoniter.spi.Decoder.ShortDecoder) JsoniterSpi.getDecoder(cacheKey)).decodeShort(iter);
		} else {
			throw new IOException();
		}
	}

	public static final int readInt(String cacheKey, JsonIterator iter) throws IOException {
		if (JsoniterSpi.getDecoder(cacheKey) instanceof com.jsoniter.spi.Decoder.IntDecoder) {
			return ((com.jsoniter.spi.Decoder.IntDecoder) JsoniterSpi.getDecoder(cacheKey)).decodeInt(iter);
		} else {
			throw new IOException();
		}
	}

	public static final long readLong(String cacheKey, JsonIterator iter) throws IOException {
		if (JsoniterSpi.getDecoder(cacheKey) instanceof com.jsoniter.spi.Decoder.LongDecoder) {
			return ((com.jsoniter.spi.Decoder.LongDecoder) JsoniterSpi.getDecoder(cacheKey)).decodeLong(iter);
		} else {
			throw new IOException();
		}
	}

	public static final float readFloat(String cacheKey, JsonIterator iter) throws IOException {
		if (JsoniterSpi.getDecoder(cacheKey) instanceof com.jsoniter.spi.Decoder.FloatDecoder) {
			return ((com.jsoniter.spi.Decoder.FloatDecoder) JsoniterSpi.getDecoder(cacheKey)).decodeFloat(iter);
		} else {
			throw new IOException();
		}
	}

	public static final double readDouble(String cacheKey, JsonIterator iter) throws IOException {
		if (JsoniterSpi.getDecoder(cacheKey) instanceof com.jsoniter.spi.Decoder.DoubleDecoder) {
			return ((com.jsoniter.spi.Decoder.DoubleDecoder) JsoniterSpi.getDecoder(cacheKey)).decodeDouble(iter);
		} else {
			throw new IOException();
		}
	}

	public static boolean readArrayStart(JsonIterator iter) throws IOException {
		byte c = IterImpl.nextToken(iter);
		if (c == '[') {
			c = IterImpl.nextToken(iter);
			if (c == ']') {
				return false;
			}
			iter.unreadByte();
			return true;
		}
		throw iter.reportError("readArrayStart", "expect [ or n");
	}

	public static boolean readObjectStart(JsonIterator iter) throws IOException {
		byte c = IterImpl.nextToken(iter);
		if (c == '{') {
			c = IterImpl.nextToken(iter);
			if (c == '}') {
				return false;
			}
			iter.unreadByte();
			return true;
		}
		throw iter.reportError("readObjectStart", "expect { or n, found: " + Byte.toString(c).charAt(0));
	}

	public static void reportIncompleteObject(JsonIterator iter) {
		throw iter.reportError("genObject", "expect }");
	}

	public static void reportIncompleteArray(JsonIterator iter) {
		throw iter.reportError("genArray", "expect ]");
	}

	public static final String readObjectFieldAsString(JsonIterator iter) throws IOException {
		String field = iter.readString();
		if (IterImpl.nextToken(iter) != ':') {
			throw iter.reportError("readObjectFieldAsString", "expect :");
		}
		return field;
	}

	public static final int readObjectFieldAsHash(JsonIterator iter) throws IOException {
		return IterImpl.readObjectFieldAsHash(iter);
	}

	public static final Slice readObjectFieldAsSlice(JsonIterator iter) throws IOException {
		return IterImpl.readObjectFieldAsSlice(iter);
	}

	public static final Slice readSlice(JsonIterator iter) throws IOException {
		return IterImpl.readSlice(iter);
	}

	public static final Object readMapKey(String cacheKey, JsonIterator iter) throws IOException {
		Slice encodedMapKey = readObjectFieldAsSlice(iter);
		MapKeyDecoder mapKeyDecoder = JsoniterSpi.getMapKeyDecoder(cacheKey);
		return mapKeyDecoder.decode(encodedMapKey);
	}

	final static boolean skipWhitespacesWithoutLoadMore(JsonIterator iter) throws IOException {
		for (int i = iter.head; i < iter.tail; i++) {
			byte c = iter.buf[i];
			switch (c) {
			case ' ':
				continue;
			case '\n':
				continue;
			case '\t':
				continue;
			case '\r':
				continue;
			default:
				break;
			}
			iter.head = i;
			return false;
		}
		return true;
	}

	public static void staticGenDecoders(TypeLiteral[] typeLiterals, StaticCodegenTarget staticCodegenTarget) {
		Codegen.staticGenDecoders(typeLiterals, staticCodegenTarget);
	}

	public static int head(JsonIterator iter) {
		return iter.head;
	}

	public static void unreadByte(JsonIterator iter) throws IOException {
		iter.unreadByte();
	}

	public static byte readByte(JsonIterator iter) throws IOException {
		return IterImpl.readByte(iter);
	}

	public static int calcHash(String str) {
		return CodegenImplObjectHash.calcHash(str);
	}

	public static void skipFixedBytes(JsonIterator iter, int n) throws IOException {
		IterImpl.skipFixedBytes(iter, n);
	}

	/**
	 * Public class StaticCodegenTarget
	 * 
	 * @author MaxiBon
	 *
	 */
	public static class StaticCodegenTarget {
		/**
		 * Public String outputDir;
		 * 
		 * @author MaxiBon
		 *
		 */
		public String outputDir;

		public StaticCodegenTarget(String outputDir) {
			this.outputDir = outputDir;
		}
	}
}
