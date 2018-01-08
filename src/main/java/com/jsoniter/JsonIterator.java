package com.jsoniter;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jsoniter.any.Any;
import com.jsoniter.spi.Config;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.Slice;
import com.jsoniter.spi.TypeLiteral;

/**
 * Public Class JsonIterator.
 * 
 * @author MaxiBon
 *
 */
public class JsonIterator implements Closeable {
	/**
	 * public Config configCache;
	 * 
	 * @author MaxiBon
	 *
	 */
	protected Config configCache;
	/**
	 * final static ValueType[] valueTypes
	 */
	final static ValueType[] valueTypes = new ValueType[256];
	/**
	 * in
	 */
	InputStream in;
	/**
	 * buf
	 */
	byte[] buf;
	/**
	 * head
	 */
	int head;
	/**
	 * tail
	 */
	int tail;
	/**
	 * skipStartedAt
	 */
	int skipStartedAt = -1; // skip should keep bytes starting at this pos
	/**
	 * tempObjects
	 */
	Map<String, Object> tempObjects = null; // used in reflection object decoder
	/**
	 * reusableSlice
	 */
	final Slice reusableSlice = new Slice(null, 0, 0);
	/**
	 * reusableChars
	 */
	char[] reusableChars = new char[32];
	/**
	 * existingObject
	 */
	Object existingObject = null; // the object should be bind to next

	/**
	 * fillArray
	 */
	private final static ReadArrayCallback fillArray = new ReadArrayCallback() {
		/**
		 * @throws IOException
		 */
		public boolean handle(JsonIterator iter, Object attachment) throws IOException {

			if (attachment instanceof List) {
				List list = (List) attachment;
				list.add(iter.read());
				return true;
			}
			return false;

		}
	};

	/**
	 * readToken
	 */
	final static String readToken = "read";

	/**
	 * prematureEnd
	 */
	final static String prematureEnd = "premature end";

	/**
	 * deserializeToken
	 */
	final static String deserializeToken = "deserialize";
	/**
	 * fillObject
	 */
	private final static ReadObjectCallback fillObject = new ReadObjectCallback() {
		/**
		 * @throws IOException
		 */
		public boolean handle(JsonIterator iter, String field, Object attachment) throws IOException {
			if (attachment instanceof Map) {
				Map map = (Map) attachment;
				map.put(field, iter.read());
				return true;
			}
			return false;

		}
	};

	static {
		for (int i = 0; i < valueTypes.length; i++) {
			valueTypes[i] = ValueType.INVALID;
		}
		valueTypes['"'] = ValueType.STRING;
		valueTypes['-'] = ValueType.NUMBER;
		valueTypes['0'] = ValueType.NUMBER;
		valueTypes['1'] = ValueType.NUMBER;
		valueTypes['2'] = ValueType.NUMBER;
		valueTypes['3'] = ValueType.NUMBER;
		valueTypes['4'] = ValueType.NUMBER;
		valueTypes['5'] = ValueType.NUMBER;
		valueTypes['6'] = ValueType.NUMBER;
		valueTypes['7'] = ValueType.NUMBER;
		valueTypes['8'] = ValueType.NUMBER;
		valueTypes['9'] = ValueType.NUMBER;
		valueTypes['t'] = ValueType.BOOLEAN;
		valueTypes['f'] = ValueType.BOOLEAN;
		valueTypes['n'] = ValueType.NULL;
		valueTypes['['] = ValueType.ARRAY;
		valueTypes['{'] = ValueType.OBJECT;
	}

	/**
	 * JsonIterator
	 * 
	 * @param in
	 * @param buf
	 * @param head
	 * @param tail
	 */
	private JsonIterator(InputStream in, byte[] buf, int head, int tail) {
		this.in = in;
		this.buf = buf;
		this.head = head;
		this.tail = tail;
	}

	/**
	 * JsonIterator
	 */
	public JsonIterator() {
		this(null, new byte[0], 0, 0);
	}

	/**
	 * parse
	 * 
	 * @param inn
	 * @param bufSizee
	 * @return
	 */
	public static JsonIterator parse(InputStream inn, int bufSizee) {
		enableStreamingSupport();
		return new JsonIterator(inn, new byte[bufSizee], 0, 0);
	}

	/**
	 * parse
	 * 
	 * @param buff
	 * @return
	 */
	public static JsonIterator parse(byte[] buff) {
		return new JsonIterator(null, buff, 0, buff.length);
	}

	/**
	 * parse
	 * 
	 * @param buff
	 * @param head1
	 * @param tail1
	 * @return
	 */
	public static JsonIterator parse(byte[] buff, int head1, int tail1) {
		return new JsonIterator(null, buff, head1, tail1);
	}

	/**
	 * parse
	 * 
	 * @param str
	 * @return
	 */
	public static JsonIterator parse(String str) {
		return parse(str.getBytes());
	}

	/**
	 * parse
	 * 
	 * @param slice
	 * @return
	 */
	public static JsonIterator parse(Slice slice) {
		return new JsonIterator(null, slice.data(), slice.head(), slice.tail());
	}

	/**
	 * reset
	 * 
	 * @param buff
	 */
	public final void reset(byte[] buff) {
		this.buf = buff;
		this.head = 0;
		this.tail = buff.length;
	}

	/**
	 * reset
	 * 
	 * @param buff
	 * @param head1
	 * @param tail1
	 */
	public final void reset(byte[] buff, int head1, int tail1) {
		this.buf = buff;
		this.head = head1;
		this.tail = tail1;
	}

	/**
	 * reset
	 * 
	 * @param value
	 */
	protected final void reset(Slice value) {
		this.buf = value.data();
		this.head = value.head();
		this.tail = value.tail();
	}

	/**
	 * reset
	 * 
	 * @param inn
	 */
	public final void reset(InputStream inn) {
		JsonIterator.enableStreamingSupport();
		this.in = inn;
		this.head = 0;
		this.tail = 0;
	}

	/**
	 * close
	 * 
	 * @throws IOException
	 */
	public final void close() throws IOException {
		if (in != null) {
			in.close();
		}
	}

	/**
	 * unreadByte
	 */
	protected final void unreadByte() {
		if (head == 0) {
			throw reportError("unreadByte", "unread too many bytes");
		}
		head--;
	}

	/**
	 * reportError
	 * 
	 * @param op
	 * @param msg
	 * @return
	 */
	public final JsonException reportError(String op, String msg) {
		int peekStart = head - 10;
		if (peekStart < 0) {
			peekStart = 0;
		}
		int peekSize = head - peekStart;
		if (head > tail) {
			peekSize = tail - peekStart;
		}
		String peek = new String(buf, peekStart, peekSize);
		throw new JsonException(op + ": " + msg + ", head: " + head + ", peek: " + peek + ", buf: " + new String(buf));
	}

	/**
	 * currentBuffer
	 * 
	 * @return
	 */
	protected final String currentBuffer() {
		int peekStart = head - 10;
		if (peekStart < 0) {
			peekStart = 0;
		}
		String peek = new String(buf, peekStart, head - peekStart);
		return "head: " + head + ", peek: " + peek + ", buf: " + new String(buf);
	}

	/**
	 * readNull
	 * 
	 * @throws IOException
	 */
	protected final boolean readNull() throws IOException {
		byte c = IterImpl.nextToken(this);
		if (c != 'n') {
			unreadByte();
			return false;
		}
		int n = 3;
		IterImpl.skipFixedBytes(this, n); // null
		return true;
	}

	/**
	 * readBoolean
	 * 
	 * @throws IOException
	 */
	public final boolean readBoolean() throws IOException {
		byte c = IterImpl.nextToken(this);
		if ('t' == c) {
			int n = 3;
			IterImpl.skipFixedBytes(this, n); // true
			return true;
		}
		if ('f' == c) {
			int n = 4;
			IterImpl.skipFixedBytes(this, n); // false
			return false;
		}
		throw reportError("readBoolean", "expect t or f, found: " + c);
	}

	/**
	 * readShort
	 * 
	 * @throws IOException
	 */
	public final short readShort() throws IOException {
		int v = readInt();
		if (Short.MIN_VALUE <= v && v <= Short.MAX_VALUE) {
			return intToShort(v);
		} else {
			throw reportError("readShort", "short overflow: " + v);
		}
	}
	
	/**
	 * 
	 * @param intero
	 * @return
	 */
	private short intToShort (int intero){
		Integer shortI = intero;
		return shortI.shortValue();
	}

	/**
	 * readInt
	 * 
	 * @throws IOException
	 */
	public final int readInt() throws IOException {
		return IterImplNumber.readInt(this);
	}

	/**
	 * readLong
	 * 
	 * @throws IOException
	 */
	public final long readLong() throws IOException {
		return IterImplNumber.readLong(this);
	}

	/**
	 * readArray
	 * 
	 * @throws IOException
	 */
	public final boolean readArray() throws IOException {
		return IterImplArray.readArray(this);
	}

	/**
	 * readNumberAsString
	 * 
	 * @throws IOException
	 */
	public String readNumberAsString() throws IOException {
		return IterImplForStreaming.readNumber(this);
	}

	/**
	 * Public Interface ReadObjectCallback.
	 * 
	 * @author MaxiBon boolean handle(JsonIterator iter, String field, Object
	 *         attachment) throws IOException;
	 * @author MaxiBon
	 * @throws IOException
	 */
	public static interface ReadArrayCallback {
		/**
		 * handle
		 * 
		 * @param iter
		 * @param attachment
		 * @return
		 * @throws IOException
		 */
		boolean handle(JsonIterator iter, Object attachment) throws IOException;
	}

	/**
	 * readArrayCB
	 * 
	 * @param callback
	 * @param attachment
	 * @return
	 * @throws IOException
	 */
	public final boolean readArrayCB(ReadArrayCallback callback, Object attachment) throws IOException {
		return IterImplArray.readArrayCB(this, callback, attachment);
	}

	/**
	 * readString
	 * 
	 * @return
	 * @throws IOException
	 */
	public final String readString() throws IOException {
		return IterImplString.readString(this);
	}

	/**
	 * readStringAsSlice
	 * 
	 * @return
	 * @throws IOException
	 */
	public final Slice readStringAsSlice() throws IOException {
		return IterImpl.readSlice(this);
	}

	/**
	 * readObject
	 * 
	 * @return
	 * @throws IOException
	 */
	public final String readObject() throws IOException {
		return IterImplObject.funReadObject(this);
	}

	/**
	 * Public Interface ReadObjectCallback.
	 * 
	 * @author MaxiBon
	 *
	 */
	public static interface ReadObjectCallback {
		/**
		 * boolean handle(JsonIterator iter, String field, Object attachment)
		 * throws IOException;
		 * 
		 * @author MaxiBon
		 * @throws IOException
		 *
		 */
		boolean handle(JsonIterator iter, String field, Object attachment) throws IOException;

	}

	/**
	 * readObjectCB
	 * 
	 * @param cb
	 * @param attachment
	 * @throws IOException
	 */
	public final void readObjectCB(ReadObjectCallback cb, Object attachment) throws IOException {
		IterImplObject.readObjectCB(this, cb, attachment);
	}

	/**
	 * readFloat
	 * 
	 * @return
	 * @throws IOException
	 */
	public final float readFloat() throws IOException {
		return IterImplNumber.readFloat(this);
	}

	/**
	 * readDouble
	 * 
	 * @return
	 * @throws IOException
	 */
	public final double readDouble() throws IOException {
		return IterImplNumber.readDouble(this);
	}

	/**
	 * readBigDecimal
	 * 
	 * @return
	 * @throws IOException
	 */
	public final BigDecimal readBigDecimal() throws IOException {
		// skip whitespace by read next
		ValueType valueType = whatIsNext();
		if (valueType == ValueType.NULL) {
			skip();
			return null;
		}
		if (valueType != ValueType.NUMBER) {
			throw reportError("readBigDecimal", "not number");
		}
		return new BigDecimal(IterImplForStreaming.readNumber(this));
	}

	/**
	 * readBigInteger
	 * 
	 * @return
	 * @throws IOException
	 */
	public final BigInteger readBigInteger() throws IOException {
		// skip whitespace by read next
		ValueType valueType = whatIsNext();
		if (valueType == ValueType.NULL) {
			skip();
			return null;
		}
		if (valueType != ValueType.NUMBER) {
			throw reportError("readBigDecimal", "not number");
		}
		return new BigInteger(IterImplForStreaming.readNumber(this));
	}

	/**
	 * readAny
	 * 
	 * @return
	 * @throws IOException
	 */
	public final Any readAny() throws IOException {
		try {
			return IterImpl.readAny(this);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw reportError(readToken, prematureEnd);
		}
	}

	/**
	 * read
	 * 
	 * @return
	 * @throws IOException
	 */
	public final Object read() throws IOException {
		try {
			ValueType valueType = whatIsNext();
			int n = 4;
			switch (valueType) {
			case STRING:
				return readString();
			case NUMBER:
				return readDouble();
			case NULL:
				IterImpl.skipFixedBytes(this, n);
				return null;
			case BOOLEAN:
				return readBoolean();
			case ARRAY:
				List list = new ArrayList(n);
				readArrayCB(fillArray, list);
				return list;
			case OBJECT:
				Map map = new HashMap(n);
				readObjectCB(fillObject, map);
				return map;
			default:
				throw reportError(readToken, "unexpected value type: " + valueType);
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			throw reportError(readToken, prematureEnd);
		}
	}

	/**
	 * try to bind to existing object, returned object might not the same
	 * instance
	 *
	 * @param existingObjects
	 *            the object instance to reuse
	 * @param <T>
	 *            object type
	 * @return data binding result, might not be the same object
	 * @throws IOException
	 *             if I/O went wrong
	 */
	public final <T> T read(T existingObjects) throws IOException {
		Object o = null;
		T typeT = null;
		try {
			this.existingObject = existingObjects;
			Class<?> clazz = existingObjects.getClass();
			String cacheKey = currentConfig().getDecoderCacheKey(clazz);
			o = Codegen.getDecoder(cacheKey, clazz).decode(this);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw reportError(readToken, prematureEnd);
		} finally {
			typeT = (T) o;
		}
		return typeT;
	}

	/**
	 * currentConfig
	 * 
	 * @return
	 */
	private Config currentConfig() {
		if (configCache == null) {
			configCache = JsoniterSpi.getCurrentConfig();
		}
		return configCache;
	}

	/**
	 * try to bind to existing object, returned object might not the same
	 * instance
	 *
	 * @param typeLiteral1
	 *            the type object
	 * @param existingObject1
	 *            the object instance to reuse
	 * @param <T>
	 *            object type
	 * @return data binding result, might not be the same object
	 * @throws IOException
	 *             if I/O went wrong
	 */
	public final <T> T read(TypeLiteral<T> typeLiteral1, T existingObject1) throws IOException {
		T typeT = null;
		Object o = null;
		try {
			this.existingObject = existingObject1;
			String cacheKey = currentConfig().getDecoderCacheKey(typeLiteral1.getType());
			o = Codegen.getDecoder(cacheKey, typeLiteral1.getType()).decode(this);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw reportError(readToken, prematureEnd);
		} finally {
			if (o instanceof Class<?>) {
				typeT = (T) o;
			}

		}
		return typeT;
	}

	/**
	 * read
	 * 
	 * @param clazz
	 * @return
	 * @throws IOException
	 */
	public final <T> T read(Class<T> clazz) throws IOException {
		Object o = read((Type) clazz);
		T typeT = null;
		if (o instanceof Class<?>) {
			typeT = (T) o;
		}
		return typeT;

	}

	/**
	 * read
	 * 
	 * @param typeLiteral
	 * @return
	 * @throws IOException
	 */
	public final <T> T read(TypeLiteral<T> typeLiteral) throws IOException {
		Object o = null;
		T typeT = null;
		o = read(typeLiteral.getType());
		if (o instanceof Class<?>) {
			typeT = (T) o;
		}
		return typeT;

	}

	/**
	 * read
	 * 
	 * @param type
	 * @return
	 * @throws IOException
	 */
	public final Object read(Type type) throws IOException {
		try {

			String cacheKey = currentConfig().getDecoderCacheKey(type);
			return Codegen.getDecoder(cacheKey, type).decode(this);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw reportError(readToken, prematureEnd);
		}
	}

	/**
	 * whatIsNext
	 * 
	 * @return
	 * @throws IOException
	 */
	public ValueType whatIsNext() throws IOException {
		ValueType valueType = valueTypes[IterImpl.nextToken(this)];
		unreadByte();
		return valueType;
	}

	/**
	 * skip
	 * 
	 * @throws IOException
	 */
	public void skip() throws IOException {
		IterImplSkip.skip(this);
	}

	/**
	 * deserialize
	 * 
	 * @param config
	 * @param input
	 * @param clazz
	 * @return
	 */
	protected static final <T> T deserialize(Config config, String input, Class<T> clazz) {
		JsoniterSpi.setCurrentConfig(config);
		try {
			return deserialize(input.getBytes(), clazz);
		} finally {
			JsoniterSpi.clearCurrentConfig();
		}
	}

	/**
	 * deserialize
	 * 
	 * @param input
	 * @param clazz
	 * @return
	 */
	public static final <T> T deserialize(String input, Class<T> clazz) {
		return deserialize(input.getBytes(), clazz);
	}

	/**
	 * deserialize
	 * 
	 * @param config
	 * @param input
	 * @param typeLiteral
	 * @return
	 */
	protected static final <T> T deserialize(Config config, String input, TypeLiteral<T> typeLiteral) {
		JsoniterSpi.setCurrentConfig(config);
		try {
			return deserialize(input.getBytes(), typeLiteral);
		} finally {
			JsoniterSpi.clearCurrentConfig();
		}
	}

	/**
	 * deserialize
	 * 
	 * @param input
	 * @param typeLiteral
	 * @return
	 */
	protected static final <T> T deserialize(String input, TypeLiteral<T> typeLiteral) {
		return deserialize(input.getBytes(), typeLiteral);
	}

	/**
	 * deserialize
	 * 
	 * @param config
	 * @param input
	 * @param clazz
	 * @return
	 */
	protected static final <T> T deserialize(Config config, byte[] input, Class<T> clazz) {
		JsoniterSpi.setCurrentConfig(config);
		try {
			return deserialize(input, clazz);
		} finally {
			JsoniterSpi.clearCurrentConfig();
		}
	}

	/**
	 * deserialize
	 * 
	 * @param input
	 * @param clazz
	 * @return
	 */
	protected static final <T> T deserialize(byte[] input, Class<T> clazz) {
		int lastNotSpacePos = findLastNotSpacePos(input);
		JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
		iter.reset(input, 0, lastNotSpacePos);
		try {
			T val = iter.read(clazz);
			if (iter.head != lastNotSpacePos) {

				String stringa2 = "trailing garbage found";
				throw iter.reportError(deserializeToken, stringa2);
			}
			return val;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw iter.reportError(deserializeToken, prematureEnd);
		} catch (IOException e) {
			throw new JsonException("Error: IOException");
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	/**
	 * deserialize
	 * 
	 * @param config
	 * @param input
	 * @param typeLiteral
	 * @return
	 */
	public static final <T> T deserialize(Config config, byte[] input, TypeLiteral<T> typeLiteral) {
		JsoniterSpi.setCurrentConfig(config);
		try {
			return deserialize(input, typeLiteral);
		} finally {
			JsoniterSpi.clearCurrentConfig();
		}
	}

	/**
	 * deserialize
	 * 
	 * @param input
	 * @param typeLiteral
	 * @return
	 */
	public static final <T> T deserialize(byte[] input, TypeLiteral<T> typeLiteral) {
		int lastNotSpacePos = findLastNotSpacePos(input);
		JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
		iter.reset(input, 0, lastNotSpacePos);
		try {
			T val = iter.read(typeLiteral);
			if (iter.head != lastNotSpacePos) {
				throw iter.reportError(deserializeToken, "trailing garbage found");
			}
			return val;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw iter.reportError(deserializeToken, prematureEnd);
		} catch (IOException e) {
			throw new JsonException("Error: IOException");
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	/**
	 * deserialize
	 * 
	 * @param config
	 * @param input
	 * @return
	 */
	public static final Any deserialize(Config config, String input) {
		JsoniterSpi.setCurrentConfig(config);
		try {
			return deserialize(input.getBytes());
		} finally {
			JsoniterSpi.clearCurrentConfig();
		}
	}

	/**
	 * deserialize
	 * 
	 * @param input
	 * @return
	 */
	public static final Any deserialize(String input) {
		return deserialize(input.getBytes());
	}

	/**
	 * deserialize
	 * 
	 * @param config
	 * @param input
	 * @return
	 */
	public static final Any deserialize(Config config, byte[] input) {
		JsoniterSpi.setCurrentConfig(config);
		try {
			return deserialize(input);
		} finally {
			JsoniterSpi.clearCurrentConfig();
		}
	}

	/**
	 * deserialize
	 * 
	 * @param input
	 * @return
	 */
	public static final Any deserialize(byte[] input) {
		int lastNotSpacePos = findLastNotSpacePos(input);
		JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
		iter.reset(input, 0, lastNotSpacePos);
		try {
			Any val = iter.readAny();
			if (iter.head != lastNotSpacePos) {
				throw iter.reportError(deserializeToken, "trailing garbage found");
			}
			return val;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw iter.reportError(deserializeToken, prematureEnd);
		} catch (IOException e) {
			throw new JsonException("Error: IOException");
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}

	/**
	 * findLastNotSpacePos
	 * 
	 * @param input
	 * @return
	 */
	private static int findLastNotSpacePos(byte[] input) {
		for (int i = input.length - 1; i >= 0; i--) {
			byte c = input[i];
			if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
				return i + 1;
			}
		}
		return 0;
	}

	/**
	 * setMode
	 * 
	 * @param mode
	 */
	public static void setMode(DecodingMode mode) {
		Config newConfig = JsoniterSpi.getDefaultConfig().copyBuilder().decodingMode(mode).build();
		JsoniterSpi.setDefaultConfig(newConfig);
		JsoniterSpi.setCurrentConfig(newConfig);
	}

	/**
	 * enableStreamingSupport
	 */
	public static void enableStreamingSupport() {
		boolean isStreamingEnabled = false;

		if (isStreamingEnabled) {
			return;
		}
		isStreamingEnabled = true;
		try {
			DynamicCodegen.enableStreamingSupport();
		} catch (JsonException e) {
			throw e;
		} catch (Exception e) {
			throw new JsonException("Error: Exception");
		}
	}
}
