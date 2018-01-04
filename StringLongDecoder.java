package com.jsoniter.fuzzy;

import java.io.IOException;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;


/**
 * Public Class StringLongDecoder.
 * 
 * @author MaxiBon
 *
 */
public class StringLongDecoder extends com.jsoniter.spi.Decoder.LongDecoder {

    @Override
    /**
     * decodeLong
     */
    public long decodeLong(JsonIterator iter) throws IOException {
    	/**
         * @throws IOException
         */
        byte c = CodegenAccess.nextToken(iter);
        if (c != '"') {
        	/**
             * @throws iter.reportError
             */
            throw iter.reportError("StringLongDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        long val = iter.readLong();
        c = CodegenAccess.nextToken(iter);
        if (c != '"') {
        	/**
             * @throws iter.reportError
             */
            throw iter.reportError("StringLongDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        return val;
    }
}
