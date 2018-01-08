package com.jsoniter.fuzzy;

import java.io.IOException;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;


/**
 * Public Class StringDoubleDecoder.
 * 
 * @author MaxiBon
 *
 */
public class StringDoubleDecoder extends com.jsoniter.spi.Decoder.DoubleDecoder {

    @Override
    /**
     * decodeDouble.
     * @throws IOException
     */
    public double decodeDouble(JsonIterator iter) throws IOException {
        byte c = CodegenAccess.nextToken(iter);
        if (c != '"') {
            throw iter.reportError("StringDoubleDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        double val = iter.readDouble();
        c = CodegenAccess.nextToken(iter);
        if (c != '"') {
            throw iter.reportError("StringDoubleDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        return val;
    }
}
