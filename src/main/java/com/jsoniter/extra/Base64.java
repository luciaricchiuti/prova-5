package com.jsoniter.extra;

import com.jsoniter.JsonIterator;
import com.jsoniter.SupportBitwise;
import com.jsoniter.spi.Slice;
import com.jsoniter.output.JsonStream;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

/**
 * A very fast and memory efficient class to encode and decode to and from
 * BASE64 in full accordance with RFC 2045.<br>
 * <br>
 * On Windows XP sp1 with 1.4.2_04 and later ;), this encoder and decoder is
 * about 10 times faster on small arrays (10 - 1000 bytes) and 2-3 times as fast
 * on larger arrays (10000 - 1000000 bytes) compared to
 * <code>sun.misc.Encoder()/Decoder()</code>.<br>
 * <br>
 *
 * On byte arrays the encoder is about 20% faster than Jakarta Commons Base64
 * Codec for encode and about 50% faster for decoding large arrays. This
 * implementation is about twice as fast on very small arrays (&lt 30 bytes). If
 * source/destination is a <code>String</code> this version is about three times
 * as fast due to the fact that the Commons Codec result has to be recoded to a
 * <code>String</code> from <code>byte[]</code>, which is very expensive.<br>
 * <br>
 *
 * This encode/decode algorithm doesn't create any temporary arrays as many
 * other codecs do, it only allocates the resulting array. This produces less
 * garbage and it is possible to handle arrays twice as large as algorithms that
 * create a temporary array. (E.g. Jakarta Commons Codec). It is unknown whether
 * Sun's <code>sun.misc.Encoder()/Decoder()</code> produce temporary arrays but
 * since performance is quite low it probably does.<br>
 * <br>
 *
 * The encoder produces the same output as the Sun one except that the Sun's
 * encoder appends a trailing line separator if the last character isn't a pad.
 * Unclear why but it only adds to the length and is probably a side effect.
 * Both are in conformance with RFC 2045 though.<br>
 * Commons codec seem to always att a trailing line separator.<br>
 * <br>
 *
 * <b>Note!</b> The encode/decode method pairs (types) come in three versions
 * with the <b>exact</b> same algorithm and thus a lot of code redundancy. This
 * is to not create any temporary arrays for transcoding to/from different
 * format types. The methods not used can simply be commented out.<br>
 * <br>
 *
 * There is also a "fast" version of all decode methods that works the same way
 * as the normal ones, but har a few demands on the decoded input. Normally
 * though, these fast verions should be used if the source if the input is known
 * and it hasn't bee tampered with.<br>
 * <br>
 *
 * If you find the code useful or you find a bug, please send me a note at
 * base64 @ miginfocom . com.
 *
 * Licence (BSD): ==============
 *
 * Copyright (c) 2004, Mikael Grev, MiG InfoCom AB. (base64 @ miginfocom . com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither the name of the MiG InfoCom AB nor the names
 * of its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @version 2.2
 * @author Mikael Grev Date: 2004-aug-02 Time: 11:31:11
 */

abstract class Base64 {

	/**
	 * 
	 */
	private final static byte[] EMPTY_ARRAY = new byte[0];
	/**
	 * final static int SIXTEEN
	 */
	private final static int SIXTEEN = 16;
	/**
	 * final static int EIGHT
	 */
	private final static int EIGHT = 16;
	/**
	 * final static int 0xff
	 */
	private final static int N = 0xff;
	/**
	 * INT 0
	 */
	private final static int ZERO = 0;
	/**
	 * INT 2
	 */
	private final static int DUE = 2;
	/**
	 *  INT 19
	 */
	private final static int DIC = 19;

	/**
	 * costruttore di default, dovrebbe essere protected per classi abstract meglio
	 * privato a causa di utilizzo di variabili e metodi statici
	 */
	private Base64() {
	}

	/**
	 * static final char[] ca
	 */
	private static final char[] CA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();
	/**
	 * static final byte[] BA;
	 */
	static final byte[] BA;
	/**
	 * static final int[] IA = new int[256];
	 */
	static final int[] IA = new int[256];
	static {
		Arrays.fill(IA, -1);
		int iS = CA.length;
		for (int i = 0; i < iS; i++) {
			IA[CA[i]] = i;
		}
		IA['='] = 0;
		BA = new byte[CA.length];
		for (int i = 0; i < CA.length; i++) {
			Character c = CA[i];
			BA[i] = c.toString().getBytes().clone()[0];
		}
	}

	/**
	 * 
	 * @param sArr
	 * @param dArr
	 * @param start
	 * @return
	 */
	static int encodeToChar(byte[] sArr, char[] dArr, final int start) {
		final int sLen = sArr.length;
		final int[] n = { 0xff, 16, 8, 18, 0x3f, 12, 6 };
		final int eLen = (sLen / 3) * 3; // Length of even 24-bits.
		final int dLen = ((sLen - 1) / 3 + 1) << 2; // Returned character count

		// Encode even 24-bits
		int d = start;
		for (int s = 0; s < eLen; s++) {
			// Copy next three bytes into lower 24 bits of int, paying attension to sign.
			int bitwise = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sArr[s])), Long.getLong(Integer.toString(n[0])), '&'))).intValue();
			int i = Integer.getInteger(Long.toString(SupportBitwise.bitwise(SupportBitwise.bitwise(Long.getLong(Integer.toString((bitwise) << n[1])).longValue(),Long.getLong(Integer.toString((bitwise) << n[2])).longValue(), '|'),Long.getLong(Integer.toString(bitwise)).longValue(), '|'))).intValue();
			// Encode the int into four chars
			dArr[d++] = CA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i >>> n[3])).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()];
			dArr[d++] = CA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i >>> n[5])).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()];
			dArr[d++] = CA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i >>> n[6])).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()];
			dArr[d++] = CA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i)).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()];
		}

		// Pad and encode last bits if source isn't even 24 bits.
		int left = sLen - eLen; // 0 - 2.
		if (left > 0) {
			// Prepare the int
			int i = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sArr[eLen])).longValue(),Long.getLong(Integer.toString(n[0])).longValue(),'&'))).intValue() << 10))).longValue(),Long.getLong(Integer.toString((left == 2 ? (Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sArr[sLen - 1])).longValue(),Long.getLong(Integer.toString(n[0])).longValue(),'&'))).intValue() << 2) : 0))).longValue(),'|'))).intValue();
			// Set last four chars
			dArr[start + dLen - 4] = CA[i >> 12];
			dArr[start + dLen - 3] = CA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i >>> n[6])).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()];
			dArr[start + dLen - 2] = left == 2 ? CA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i)).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()] : '=';
			dArr[start + dLen - 1] = '=';
		}
		return dLen;
	}

	/**
	 * 
	 * @param sArr
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	static int encodeToBytes(byte[] sArr, JsonStream stream) throws IOException {
		final int sLen = sArr.length;
		final int[] n = { 0xff, 16, 8, 18, 0x3f, 12, 6, 10, 2 };
		final int eLen = (sLen / 3) * 3; // Length of even 24-bits.
		final int dLen = ((sLen - 1) / 3 + 1) << 2; // Returned character count
		int s = 0;
		// Encode even 24-bits
		while(s < eLen) {
			// Copy next three bytes into lower 24 bits of int, paying attension to sign.
			int bitwise = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sArr[s])), Long.getLong(Integer.toString(n[0])), '&'))).intValue();
			s++;
			int i = Integer.getInteger(Long.toString(SupportBitwise.bitwise(SupportBitwise.bitwise(Long.getLong(Integer.toString((bitwise) << n[1])).longValue(),Long.getLong(Integer.toString((bitwise) << n[2])).longValue(), '|'),Long.getLong(Integer.toString(bitwise)).longValue(), '|'))).intValue();
			// Encode the int into four chars
			stream.write(BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i >>> n[3])),Long.getLong(Integer.toString(n[4])), '&'))).intValue()],BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i >>> n[5])),Long.getLong(Integer.toString(n[4])), '&'))).intValue()],BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i >>> n[6])),Long.getLong(Integer.toString(n[4])), '&'))).intValue()],BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i)),Long.getLong(Integer.toString(n[4])), '&'))).intValue()]);
		}

		// Pad and encode last bits if source isn't even 24 bits.
		int left = sLen - eLen; // 0 - 2.
		if (left > 0) {
			// Prepare the int
			int i = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((sArr[eLen]))).longValue(),Long.getLong(Integer.toString(n[0])).longValue(),'&'))).intValue() << n[7])).longValue(),Long.getLong(Integer.toString(left == n[8] ? ((Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((sArr[sLen - 1]))).longValue(),Long.getLong(Integer.toString(n[0])).longValue(),'&'))).intValue()) << 2) : 0)).longValue(),'|'))).intValue();
			// Set last four chars
			Character c = '=';
			byte ch = c.toString().getBytes().clone()[0];
			stream.write(BA[i >> 12], BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 6))).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()],left == n[8]? BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i))).longValue(),Long.getLong(Integer.toString(n[4])).longValue(), '&'))).intValue()]: ch,ch);
		}

		return dLen;
	}

	/**
	 * 
	 * @param bits
	 * @param stream
	 * @throws IOException
	 */
	static void encodeLongBits(long bits, JsonStream stream) throws IOException {
		int n = 0x3f;
		Long l = bits;
		int i = l.intValue();
		byte b1 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 18))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		byte b2 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 12))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		byte b3 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 6))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		byte b4 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		Character c = '"';
		stream.write(c.toString().getBytes().clone()[0], b1, b2, b3, b4);

		bits = bits >>> 24;
		i = l.intValue();
		b1 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 18))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		b2 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 12))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		b3 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 6))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		b4 = BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i))).longValue(),
				Long.getLong(Integer.toString(n)).longValue(), '&'))).intValue()];
		stream.write(b1, b2, b3, b4);
		bits = (bits >>> 24) << DUE;
		i = BigDecimal.valueOf(bits).intValue();
		b1 = BA[i >> 12];
		b2 = BA[Integer
				.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString((i >>> 6))).longValue(),
						Long.getLong(Integer.toString(n)).longValue(), '&')))
				.intValue()];
		b3 = BA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(i)).longValue(),
				Long.getLong(Integer.toString(n)).longValue(), '&'))).intValue()];
		stream.write(b1, b2, b3, c.toString().getBytes().clone()[0]);
	}

	/**
	 * 
	 * @param iter
	 * @return
	 * @throws IOException
	 */
	static long decodeLongBits(JsonIterator iter) throws IOException {
		int[] n = { 46, 6, 12, 24, 18 };
		Slice slice = iter.readStringAsSlice();
		if (slice.len() != 11) {
			throw iter.reportError("decodeLongBits", "must be 11 bytes for long bits encoded double");
		}
		byte[] encoded = slice.data();
		int sIx = slice.head();
		long i = Integer.getInteger(Long.toString(SupportBitwise.bitwise(
				SupportBitwise.bitwise(
						SupportBitwise.bitwise(Long.getLong(Integer.toString(IA[encoded[sIx++]] << n[4])).longValue(),
								Long.getLong(Integer.toString(IA[encoded[sIx++]] << n[2])).longValue(), '|'),
						Long.getLong(Integer.toString(IA[encoded[sIx++]] << n[1])).longValue(), '|'),
				Long.getLong(Integer.toString(IA[encoded[sIx++]])).longValue(), '|'))).intValue();
		long bits = SupportBitwise.bitwise(i << n[3], i, '|');
		i = SupportBitwise.bitwise(
				SupportBitwise.bitwise(Long.getLong(Integer.toString(IA[encoded[sIx++]] << n[2])).longValue(),
						Long.getLong(Integer.toString(IA[encoded[sIx++]] << n[1])).longValue(), '|'),
				Long.getLong(Integer.toString(IA[encoded[sIx]])).longValue(), '|');
		bits = SupportBitwise.bitwise(i << n[0], bits, '|');
		return bits;
	}

	/**
	 * 
	 * @param sArr
	 * @param start
	 * @return
	 */
	static int findEnd(final byte[] sArr, final int start) {
		int n = 0xff;
		int i = 0;
		int ret = sArr.length;
		for (i = start; i < sArr.length; i++) {
			if (IA[Integer.getInteger(
					Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sArr[i])).longValue(),
							Long.getLong(Integer.toString(n)).longValue(), '&')))
					.intValue()] < 0) {
				ret = sArr.length +1;
				break;
			}
		}
		if(ret == sArr.length +1) {
			ret = i;
		}
		return ret;
	}

	// Follow the limit for number of statements in a method
	/**
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private static byte[] limitStatements(final int start, final int end) {
		int sLen = end - start;
		byte[] byteArr = null;
		if (sLen == 0) {
			byteArr = EMPTY_ARRAY;
		}
		return byteArr;
	}

	/**
	 * 
	 * @param sArr
	 * @param sIx
	 * @return
	 */
	private static Integer limitStatements2(final byte[] sArr, int sIx) {
		int n0 = 18;
		int n1 = 12;
		int n2 = 6;
		int index = sIx;
		return Integer.getInteger(Long.toString(SupportBitwise.bitwise(SupportBitwise.bitwise(SupportBitwise.bitwise(Long.getLong(Integer.toString(IA[sArr[index++]] << n0)).longValue(),Long.getLong(Integer.toString(IA[sArr[index++]] << n1)).longValue(), '|'),Long.getLong(Integer.toString(IA[sArr[index++]] << n2)).longValue(), '|'),Long.getLong(Integer.toString(IA[sArr[index++]])).longValue(), '|'))).intValue();
	}

	/**
	 * 
	 * @param int1
	 * @param int2
	 * @return
	 */
	private static Integer limitStatements3Bitwise(int int1, int int2) {
		int intReturn = int1 >> int2;
		return intReturn;
	}

	/**
	 * 
	 * @param int1
	 * @param int2
	 * @param int3
	 * @return
	 */
	private static int limitStatements4While(int int1, int int2, int int3) {
		int toReturn = int2;
		while (int2 < int3 && int1 < 0) {
			toReturn++;
		}
		return toReturn;
	}

	/**
	 * 
	 * @param int1
	 * @param int2
	 * @param int3
	 * @param sArr
	 * @return
	 */
	private static int limitStatements5For(int int1, int int2, int int3, final byte[] sArr) {
		int toReturn = 0;
		int size = int2 - int3;
		int support = int1;
		for (int j = 0; int1 <= size; j++) {
			toReturn |= IA[sArr[support++]] << (18 - j * 6);
		}
		return toReturn;
	}

	/**
	 * 
	 * @param int1
	 * @param int2
	 * @param int3
	 * @param dArr
	 * @return
	 */
	private static byte[] limitStatements6For2(int int1, int int2, int int3, final byte[] dArr) {
		byte[] toReturn = dArr;
		int index = int1;
		for (int r = 16; int1 < int2; r -= 8) {
			toReturn[index++] = limitStatements3Bitwise(int3, r).toString().getBytes().clone()[0];
		}
		return toReturn;
	}

	/**
	 * 
	 * @param byteArr
	 * @param dArr
	 * @return
	 */
	private static byte[] limitStatements7(final byte[] byteArr, final byte[] dArr) {
		byte[] toReturn = null;
		if (byteArr.equals(toReturn)) {
			toReturn = null;
		} else {
			toReturn = dArr;
		}
		return toReturn;
	}
	
	/**
	 * 
	 * @param dArr
	 * @param d
	 * @param sArr
	 * @param sIx
	 * @return
	 */
	private static byte[] limitStatements8(byte[] dArr, Integer d, final byte[] sArr, int sIx) {
		byte[] copyOfDARR = dArr;
		int i = d;
		copyOfDARR[i++] = limitStatements3Bitwise(limitStatements2(sArr, sIx), SIXTEEN).toString().getBytes()
				.clone()[0];
		copyOfDARR[i++] = limitStatements3Bitwise(limitStatements2(sArr, sIx), EIGHT).toString().getBytes().clone()[0];
		copyOfDARR[i++] = limitStatements2(sArr, sIx).byteValue();
		d=i;
		return copyOfDARR;
	}

	/**
	 * 
	 * @param sArr
	 * @param start
	 * @param end
	 * @return
	 */
	static byte[] decodeFast(final byte[] sArr, final int start, final int end) {
		// Check special case
		byte[] byteArr = limitStatements(start, end);
		// Start and end index after trimming.
		int sIx = start;
		int eIx = end - 1;
		// Trim illegal chars from start
		int noMethodInWhile = IA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sArr[sIx])).longValue(),Long.getLong(Integer.toString(N)).longValue(), '&'))).intValue()];
		sIx = limitStatements4While(noMethodInWhile, sIx, eIx);
		// Trim illegal chars from end
		noMethodInWhile = IA[Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(sArr[eIx])).longValue(),Long.getLong(Integer.toString(N)).longValue(), '&'))).intValue()];
		eIx = limitStatements4While(noMethodInWhile, eIx, 0);
		// get the padding count (=) (0, 1 or 2)
		int pad = sArr[eIx] == '=' ? (sArr[eIx - 1] == '=' ? 2 : 1) : 0; // Count '=' at end.
		int cCnt = eIx - sIx + 1; // Content count including possible separators
		int sepCnt = (end - start) > 76 ? (sArr[76] == '\r' ? cCnt / 78 : 0) << 1 : 0;
		int len = ((cCnt - sepCnt) * 6 >> 3) - pad; // The number of decoded bytes
		byte[] dArr = new byte[len]; // Preallocate byte[] of exact length
		// Decode all but the last 0 - 2 bytes.
		Integer d = 0;
		int eLen = (len / 3) * 3;
		Integer cc=0;
		while(d<eLen) {
			// Assemble three bytes into an int from four "valid" characters. // Add the bytes
			dArr = limitStatements8(dArr, d, sArr, sIx);
			// If line separator, jump over it.
			sIx = sIxReturn (sepCnt, ++cc, sIx);
			cc = (cyclomaticAND(sepCnt > ZERO, cc == DIC)) ? ZERO : cc;
		}
		if (d < len){
			dArr = limitStatements6For2(d, len, limitStatements5For(sIx, eIx, pad, sArr), dArr);
		}
		return limitStatements7(byteArr, dArr);
	}
	
	/**
	 * 
	 * @param b1
	 * @param b2
	 * @return
	 */
	private static boolean cyclomaticAND (boolean b1, boolean b2) {
		return b1 && b2;
	}
	/**
	 * 
	 * @param sepCnt
	 * @param cc
	 * @param sIx
	 * @return
	 */
	private static int sIxReturn (int sepCnt, Integer cc, int sIx) {
		return (cyclomaticAND(sepCnt > ZERO, cc == DIC)) ? sIx + DUE : sIx;
	}
}
