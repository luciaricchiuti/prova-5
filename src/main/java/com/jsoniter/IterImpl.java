package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.Slice;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

/**
 * class IterImpl
 * 
 * @author MaxiBon
 *
 */
class IterImpl {
	/**
	 * "incomplete string";
	 */
	final static String INCOMPLETESTRING = "incomplete string";
	/**
	 * "readStringSlowPath";
	 */
	final static String READSTRINGSLOWPATH = "readStringSlowPath";

	/**
	 * DEFAULT PRIVATE CONSTRUCTOR
	 */
	private IterImpl() {
	}

	/**
	 * readObjectFieldAsHash
	 * 
	 * @param iter
	 * @return
	 * @throws IOException
	 */
	public static final int readObjectFieldAsHash(JsonIterator iter) throws IOException {
		if (readByte(iter) != '"') {
			if (nextToken(iter) != '"') {
				throw iter.reportError("readObjectFieldAsHash", "expect \"");
			}
		}
		long hash = 0x811c9dc5;
		int i = 0;
		for (i = iter.head; i < iter.tail; i++) {
			byte c = iter.buf[i];
			if (c == '"') {
				break;
			}
			hash ^= c;
			hash *= 0x1000193;
		}
		iter.head = i + 1;
		if (readByte(iter) != ':') {
			if (nextToken(iter) != ':') {
				throw iter.reportError("readObjectFieldAsHash", "expect :");
			}
		}
		Long intero = hash;
		return intero.intValue();
	}

	public static final Slice readObjectFieldAsSlice(JsonIterator iter) throws IOException {
		Slice field = readSlice(iter);
		if (nextToken(iter) != ':') {
			throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
		}
		return field;
	}

	final static void skipArray(JsonIterator iter) throws IOException {
		int level = 1;
		int i = iter.head;
		while (i < iter.tail) {
			switch (iter.buf[i]) {
			case '"': // If inside string, skip it
				iter.head = i + 1;
				skipString(iter);
				i = iter.head - 1; // it will be i++ soon
				break;
			case '[': // If open symbol, increase level
				level++;
				break;
			case ']': // If close symbol, increase level
				level--;

				// If we have returned to the original level, we're done
				if (level == 0) {
					iter.head = i + 1;
					return;
				}
				break;
			default:
				break;
			}
			i++;
		}
		throw iter.reportError("skipArray", "incomplete array");
	}

	final static void skipObject(JsonIterator iter) throws IOException {
		int level = 1;
		int i = iter.head;
		while (i < iter.tail) {
			switch (iter.buf[i]) {
			case '"': // If inside string, skip it
				iter.head = i + 1;
				skipString(iter);
				i = iter.head - 1; // it will be i++ soon
				break;
			case '{': // If open symbol, increase level
				level++;
				break;
			case '}': // If close symbol, increase level
				level--;

				// If we have returned to the original level, we're done
				if (level == 0) {
					iter.head = i + 1;
					return;
				}
				break;
			default:
				break;
			}
			i++;
		}
		throw iter.reportError("skipObject", "incomplete object");
	}

	final static void skipString(JsonIterator iter) throws IOException {
		int end = IterImplSkip.findStringEnd(iter);
		if (end == -1) {
			String stringa1 = "skipString";
			throw iter.reportError(stringa1, INCOMPLETESTRING);
		} else {
			iter.head = end;
		}
	}

	final static void skipUntilBreak(JsonIterator iter) throws IOException {
		// true, false, null, number
		for (int i = iter.head; i < iter.tail; i++) {
			byte c = iter.buf[i];
			if (IterImplSkip.breaks[c]) {
				iter.head = i;
				return;
			}
		}
		iter.head = iter.tail;
	}

	final static boolean skipNumber(JsonIterator iter) throws IOException {
		// true, false, null, number
		boolean dotFound = false;
		for (int i = iter.head; i < iter.tail; i++) {
			byte c = iter.buf[i];
			if (c == '.') {
				dotFound = true;
				continue;
			}
			if (IterImplSkip.breaks[c]) {
				iter.head = i;
				return dotFound;
			}
		}
		iter.head = iter.tail;
		return dotFound;
	}

	// read the bytes between " "
	public final static Slice readSlice(JsonIterator iter) throws IOException {
		if (IterImpl.nextToken(iter) != '"') {
			throw iter.reportError("readSlice", "expect \" for string");
		}
		int end = IterImplString.findSliceEnd(iter);
		if (end == -1) {
			throw iter.reportError("readSlice", INCOMPLETESTRING);
		} else {
			// reuse current buffer
			iter.reusableSlice.reset(iter.buf, iter.head, end - 1);
			iter.head = end;
			return iter.reusableSlice;
		}
	}

	final static byte nextToken(final JsonIterator iter) throws IOException {
		int i = iter.head;
		for (;;) {
			byte c = iter.buf[i++];
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
				iter.head = i;
				return c;
			}
		}
	}

	final static byte readByte(JsonIterator iter) throws IOException {
		return iter.buf[iter.head++];
	}

	public static Any readAny(JsonIterator iter) throws IOException {
		int start = iter.head;
		byte c = nextToken(iter);
		int n1 = 3;
		int n2 = 4;
		switch (c) {
		case '"':
			skipString(iter);
			return Any.lazyString(iter.buf, start, iter.head);
		case 't':
			skipFixedBytes(iter, n1);
			return Any.wrap(true);
		case 'f':
			skipFixedBytes(iter, n2);
			return Any.wrap(false);
		case 'n':
			skipFixedBytes(iter, n1);
			return Any.wrap(0);
		case '[':
			skipArray(iter);
			return Any.lazyArray(iter.buf, start, iter.head);
		case '{':
			skipObject(iter);
			return Any.lazyObject(iter.buf, start, iter.head);
		default:
			if (skipNumber(iter)) {
				return Any.lazyDouble(iter.buf, start, iter.head);
			} else {
				return Any.lazyLong(iter.buf, start, iter.head);
			}
		}
	}

	public static void skipFixedBytes(JsonIterator iter, int n) throws IOException {
		iter.head += n;
	}

	public final static boolean loadMore(JsonIterator iter) throws IOException {
		iter.toString();
		return false;
	}

	public final static int funReadStringSlowPath(JsonIterator iter, int j) throws IOException {
		final int[] n = { 0x80, 0xE0, 0x1F, 0x3F, 0xF0, 0x0F, 0xF8, 0x07, 0x3ff };
		String stringa2 = "invalid escape character: ";
		try {
			boolean isExpectingLowSurrogate = false;
			for (Integer i = iter.head; i < iter.tail;) {
				int bc = iter.buf[i++];
				if (bc == '"') {
					iter.head = i;
					return j;
				}
				if (bc == '\\') {
					bc = iter.buf[i++];
					bc = funReadStringSlowPathSupp5(bc, iter, stringa2, isExpectingLowSurrogate, i );
				} else if ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(bc)).longValue(),Long.getLong(Integer.toString(n[0])).longValue(), '&'))).intValue()) != 0) {
					bc = funReadStringSlowPathSupp4(iter, i, bc, j, n);
				}
				if (iter.reusableChars.length == j) {
					funReadStringSlowPathSupp2(iter);
				}
				iter.reusableChars[j++] = Integer.toString(bc).charAt(0);
			}
			throw iter.reportError(READSTRINGSLOWPATH, INCOMPLETESTRING);
		} catch (IndexOutOfBoundsException e) {
			throw iter.reportError("readString", INCOMPLETESTRING);
		}
	}
	
	/**
	 * 
	 * @param isExpectingLowSurrogate
	 * @param bc
	 * @return
	 */
	private static void funReadStringSlowPathSupp(boolean isExpectingLowSurrogate, char bc, boolean iselsANDbH) {
		boolean bool = isExpectingLowSurrogate;
		boolean bH = Character.isHighSurrogate(bc);
		boolean bL = Character.isLowSurrogate(bc);
		boolean b1 = iselsANDbH;
		boolean b2 = (!bool && bL);
		if (b1 || b2) {
			throw new JsonException("invalid surrogate");
		} else if (!bool && bH) {
			bool = true;
		} else if (bool && bL) {
			bool = false;
		} else {
			throw new JsonException("invalid surrogate");
		}
	}
	
	/**
	 * 
	 * @param iter
	 */
	private static void funReadStringSlowPathSupp2(JsonIterator iter) {
		char[] newBuf = new char[iter.reusableChars.length * 2];
		System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
		iter.reusableChars = newBuf;
	}
	
	/**
	 * 
	 * @param iter
	 * @param i
	 * @param bc
	 * @param u2
	 * @param j
	 * @param n
	 * @return
	 */
	private static int funReadStringSlowPathSupp3(JsonIterator iter, Integer i, int bc, int u2, int j, final int[] n) {
		final int u3 = iter.buf[i++];
		Map<JsonIterator, Integer> support = new TreeMap<JsonIterator, Integer>();
		support = iterImplSupp(bc, n, iter, u3, u2, i, j);
		for (JsonIterator je : support.keySet()) {
			iter = je;
		}
		return support.get(iter);
	}
	
	/**
	 * 
	 * @param iter
	 * @param i
	 * @param bc
	 * @param j
	 * @param n
	 * @return
	 */
	private static int funReadStringSlowPathSupp4 (JsonIterator iter, Integer i, int bc, int j, final int[] n ) {
		int toReturn = bc;
		final int u2 = iter.buf[i++];
		if ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(toReturn)).longValue(),Long.getLong(Integer.toString(n[2])).longValue(), '&'))).intValue()) == 0xC0) {
			toReturn = ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(toReturn)).longValue(),Long.getLong(Integer.toString(n[2])).longValue(), '&'))).intValue()) << 6) + (Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(u2)).longValue(),Long.getLong(Integer.toString(n[3])).longValue(), '&'))).intValue());
		} else {
			toReturn = funReadStringSlowPathSupp3(iter, i, toReturn, u2, j, n );
		}
		return toReturn;
	}

	/**
	 * 
	 * @param bc
	 * @param iter
	 * @param stringa2
	 * @param isExpectingLowSurrogate
	 * @param i
	 * @return
	 */
	private static int funReadStringSlowPathSupp5(int bc, JsonIterator iter, String stringa2, boolean isExpectingLowSurrogate, Integer i) {
		int bChar = bc;
		char bca = (char) bChar;
		if (bChar == 'b') {
			bChar = '\b';
		} else if (bChar == 't') {
			bChar = '\t';
		} else if (bChar == 'n') {
			bChar = '\n';
		} else if (bChar == 'f') {
			bChar = '\f';
		} else if (bChar == 'r') {
			bChar = '\r';
		} else if (bChar == 'u') {
			bChar = (IterImplString.translateHex(iter.buf[i++]) << 12) + (IterImplString.translateHex(iter.buf[i++]) << 8) + (IterImplString.translateHex(iter.buf[i++]) << 4) + IterImplString.translateHex(iter.buf[i++]);
			funReadStringSlowPathSupp(isExpectingLowSurrogate, bca, isExpectingLowSurrogate && Character.isHighSurrogate(bca)); 
		} else {
			throw iter.reportError(READSTRINGSLOWPATH, stringa2 + bChar);
		}
		return bChar;
	}

	private static Map<JsonIterator, Integer> iterImplSupp(int bc, final int[] n, JsonIterator iter, int u3, int u2, int i, int j) {
		Map<JsonIterator, Integer> support = new TreeMap<JsonIterator, Integer>();
		if ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(bc)).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()) == 0xE0) {
			bc = ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(bc)).longValue(),Long.getLong(Integer.toString(n[5])).longValue(), '&'))).intValue()) << 12) + ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(u2)).longValue(),Long.getLong(Integer.toString(n[3])).longValue(), '&'))).intValue()) << 6) + (Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(u3)).longValue(),Long.getLong(Integer.toString(n[3])).longValue(), '&'))).intValue());
			support.put(iter, bc);
			return support;
		} else {
			final int u4 = iter.buf[i++];
			if ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(bc)).longValue(),Long.getLong(Integer.toString(n[6])).longValue(), '&'))).intValue()) == 0xF0) {
				bc = ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(bc)).longValue(),Long.getLong(Integer.toString(n[7])).longValue(), '&'))).intValue()) << 18) + ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(u2)).longValue(),Long.getLong(Integer.toString(n[3])).longValue(), '&'))).intValue()) << 12) + ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(u3)).longValue(),Long.getLong(Integer.toString(n[3])).longValue(), '&'))).intValue()) << 6) + (Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(u4)).longValue(),Long.getLong(Integer.toString(n[3])).longValue(), '&'))).intValue());
			} else {
				throw iter.reportError(READSTRINGSLOWPATH, "invalid unicode character");
			}
			iter = iterImplSupp(iter, bc, n, j);
			support.put(iter, bc);
			return support;
		}
	}

	private static JsonIterator iterImplSupp(JsonIterator iter, int bc, final int[] n, int j) {
		if (bc >= 0x10000) {
			// check if valid unicode
			if (bc >= 0x110000) {
				throw iter.reportError(READSTRINGSLOWPATH, "invalid unicode character");
			}

			// split surrogates
			final int sup = bc - 0x10000;
			if (iter.reusableChars.length == j) {
				char[] newBuf = new char[iter.reusableChars.length * 2];
				System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
				iter.reusableChars = newBuf;
			}
			Integer a = (Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sup)).longValue(),Long.getLong(Integer.toString(n[8])).longValue(), '&'))).intValue() + +0xdc00);
			iter.reusableChars[j++] = a.toString().toCharArray()[0];
			if (iter.reusableChars.length == j) {
				char[] newBuf = new char[iter.reusableChars.length * 2];
				System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
				iter.reusableChars = newBuf;
			}
			Integer b = (Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sup)).longValue(),Long.getLong(Integer.toString(n[8])).longValue(), '&'))).intValue() + 0xdc00);
			iter.reusableChars[j++] = b.toString().toCharArray()[0];
		}
		return iter;
	}

	public static int updateStringCopyBound(final int bound) {
		return bound;
	}

	// funzione di appoggio creata da frappe
	/**
	 * @throws IOException
	 */
	static final int supportReadInt(final JsonIterator iter, int ind) throws IOException {
		if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			throw iter.reportError("readInt", "expect 0~9");
		}
		if (iter.tail - iter.head > 9) {
			int i = iter.head;
			return supportReadIntSupp(iter, i, ind);
		}
		return ind;
	}

	/**
	 * supportReadInt
	 * 
	 * @param iter
	 * @param i
	 * @param ind
	 * @return
	 */
	private static int supportReadIntSupp(final JsonIterator iter, int i, int ind) {
		int indTemp = ind;
		int ind2 = IterImplNumber.intDigits[iter.buf[i]];
		if (ind2 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			iter.head = i;
			return -indTemp;
		}
		int ind3 = IterImplNumber.intDigits[iter.buf[++i]];
		if (ind3 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			iter.head = i;
			indTemp = indTemp * 10 + ind2;
			return -indTemp;
		}
		int ind4 = IterImplNumber.intDigits[iter.buf[++i]];
		if (ind4 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			iter.head = i;
			indTemp = indTemp * 100 + ind2 * 10 + ind3;
			return -indTemp;
		}
		int ind5 = IterImplNumber.intDigits[iter.buf[++i]];
		if (ind5 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			iter.head = i;
			indTemp = indTemp * 1000 + ind2 * 100 + ind3 * 10 + ind4;
			return -indTemp;
		}
		return supportReadIntSupp2(iter, i, ind, ind2, ind3, ind4, ind5);
	}

	private static int supportReadIntSupp2(final JsonIterator iter, int i, int ind, int ind2, int ind3, int ind4, int ind5) {
		int indTemp = ind;
		int ind6 = IterImplNumber.intDigits[iter.buf[++i]];
		if (ind6 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			iter.head = i;
			indTemp = indTemp * 10000 + ind2 * 1000 + ind3 * 100 + ind4 * 10 + ind5;
			return -indTemp;
		}
		int ind7 = IterImplNumber.intDigits[iter.buf[++i]];
		if (ind7 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			iter.head = i;
			return -(indTemp * 100000 + ind2 * 10000 + ind3 * 1000 + ind4 * 100 + ind5 * 10 + ind6);
		}
		int ind8 = IterImplNumber.intDigits[iter.buf[++i]];
		if (ind8 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			iter.head = i;
			indTemp = indTemp * 1000000 + ind2 * 100000 + ind3 * 10000 + ind4 * 1000 + ind5 * 100 + ind6 * 10 + ind7;
			return -indTemp;
		}
		int ind9 = IterImplNumber.intDigits[iter.buf[++i]];
		indTemp = indTemp * 10000000 + ind2 * 1000000 + ind3 * 100000 + ind4 * 10000 + ind5 * 1000 + ind6 * 100 + ind7 * 10 + ind8;
		iter.head = i;
		if (ind9 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
			return -indTemp;
		}
		return indTemp;
	}

	static final int readInt(final JsonIterator iter, final byte c) throws IOException {
		int ind = IterImplNumber.intDigits[c];
		if (ind == 0) {
			IterImplForStreaming.assertNotLeadingZero(iter);
			return 0;
		}

		return IterImplForStreaming.readIntSlowPath(iter, supportReadInt(iter, ind));
	}

	static final long readLong(final JsonIterator iter, final byte c) throws IOException {
		long ind = IterImplNumber.intDigits[c];
		if (ind == 0) {
			IterImplForStreaming.assertNotLeadingZero(iter);
			return 0;
		}
		Long intero = ind;
		return IterImplForStreaming.readLongSlowPath(iter, supportReadInt(iter, intero.intValue()));
	}

	static final double readDouble(final JsonIterator iter) throws IOException {
		double toReturn = 0.0d;
		int oldHead = iter.head;
		JsonIterator iterCopy = iter;
		try {
			try {
				long value = IterImplNumber.readLong(iterCopy);
				if (iterCopy.head == iterCopy.tail) {
					return value;
				}
				byte c = iterCopy.buf[iterCopy.head];
				return supp(c , iterCopy, value, oldHead);
			} finally {
				if (iterCopy.head < iterCopy.tail && (iterCopy.buf[iterCopy.head] == 'e' || iterCopy.buf[iterCopy.head] == 'E')) {
					iterCopy.head = oldHead;
					toReturn = IterImplForStreaming.readDoubleSlowPath(iterCopy);
				}
			}
		} catch (JsonException e) {
			iter.head = oldHead;
			toReturn = IterImplForStreaming.readDoubleSlowPath(iterCopy);
		}
		return toReturn;
	}
	
	/**
	 * 
	 * @param c
	 * @param iter
	 * @param value
	 * @param oldHead
	 * @return
	 * @throws IOException
	 */
	private static double supp(byte c , JsonIterator iter, long value, int oldHead) throws IOException {
		double d = 0.0d;
		if (c == '.') {
			d = readDoubleSupp(c, iter, value, oldHead);
		} else {
			d =  value;
		}
		return d;
	}

	/**
	 * readDoubleSupp
	 * 
	 * @param c
	 * @param iter
	 * @param value
	 * @param oldHead
	 * @return
	 * @throws IOException
	 */
	private static double readDoubleSupp(byte c, final JsonIterator iter, long value, int oldHead) throws IOException {
		byte b = c;
		JsonIterator iterCopy = iter;
		iterCopy.head++;
		int start = iterCopy.head;
		b = iterCopy.buf[iterCopy.head++];
		long decimalPart = readLong(iterCopy, b);
		if (decimalPart == Long.MIN_VALUE) {
			return IterImplForStreaming.readDoubleSlowPath(iterCopy);
		}
		decimalPart = -decimalPart;
		int decimalPlaces = iterCopy.head - start;
		if (decimalPlaces > 0 && decimalPlaces < IterImplNumber.POW10.length && (iterCopy.head - oldHead) < 10) {
			BigDecimal number = new BigDecimal(IterImplNumber.POW10[decimalPlaces]);
			return value + (decimalPart / number.floatValue());
		} else {
			iterCopy.head = oldHead;
			return IterImplForStreaming.readDoubleSlowPath(iterCopy);
		}
	}
}
