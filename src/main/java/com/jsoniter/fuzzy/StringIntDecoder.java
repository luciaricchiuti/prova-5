package com.jsoniter.fuzzy;

import java.io.IOException;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;


/**
 * Public Class StringIntDecoder.
 * 
 * @author MaxiBon
 *
 */
public class StringIntDecoder extends com.jsoniter.spi.Decoder.IntDecoder {

    @Override
    /**
     * decodeInt
     * @throws IOException
     */
    public int decodeInt(JsonIterator iter) throws IOException {
        byte c = CodegenAccess.nextToken(iter);
        if (c != '"') {
            throw iter.reportError("StringIntDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        int val = iter.readInt();
        c = CodegenAccess.nextToken(iter);
        if (c != '"') {
            throw iter.reportError("StringIntDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        return val;
    }
}
