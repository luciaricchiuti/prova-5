package com.jsoniter.fuzzy;

import java.io.IOException;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;


/**
 * Public Class StringFloatDecoder.
 * 
 * @author MaxiBon
 *
 */
public class StringFloatDecoder extends com.jsoniter.spi.Decoder.FloatDecoder {

    @Override
    /**
     * decodeFloat
     * @throws IOException
     */
    public float decodeFloat(JsonIterator iter) throws IOException {
        byte c = CodegenAccess.nextToken(iter);
        if (c != '"') {
            throw iter.reportError("StringFloatDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        float val = iter.readFloat();
        c = CodegenAccess.nextToken(iter);
        if (c != '"') {
            throw iter.reportError("StringFloatDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        return val;
    }
}
