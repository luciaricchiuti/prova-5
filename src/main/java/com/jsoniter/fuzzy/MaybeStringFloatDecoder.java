package com.jsoniter.fuzzy;

import java.io.IOException;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;


/**
 * Public Class MaybeStringFloatDecoder.
 * 
 * @author MaxiBon
 *
 */
public class MaybeStringFloatDecoder extends com.jsoniter.spi.Decoder.FloatDecoder {

	@Override
	/**
	 * decodeFloat.
	 * @throws IOException
	 */
	public float decodeFloat(JsonIterator iter) throws IOException {
		byte c = CodegenAccess.nextToken(iter);
		if (c != '"') {
			CodegenAccess.unreadByte(iter);
			return iter.readFloat();
		}
		float val = iter.readFloat();
		c = CodegenAccess.nextToken(iter);
		if (c != '"') {
			throw iter.reportError("StringFloatDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
		}
		return val;
	}
}
