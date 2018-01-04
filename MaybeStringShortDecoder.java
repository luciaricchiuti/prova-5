package com.jsoniter.fuzzy;

import java.io.IOException;

import com.jsoniter.CodegenAccess;
import com.jsoniter.JsonIterator;


/**
 * Public Class MaybeStringShortDecoder.
 * 
 * @author MaxiBon
 *
 */
public class MaybeStringShortDecoder extends com.jsoniter.spi.Decoder.ShortDecoder {

    @Override
    /**
     * decodeShort.
     */
    public short decodeShort(JsonIterator iter) throws IOException {
    	/**
         * @throws IOException
         */
        byte c = CodegenAccess.nextToken(iter);
        if (c != '"') {
            CodegenAccess.unreadByte(iter);
            return iter.readShort();
        }
        short val = iter.readShort();
        c = CodegenAccess.nextToken(iter);
        if (c != '"') {
        	/**
             * @throws iter.reportError
             */
            throw iter.reportError("StringShortDecoder", "expect \", but found: " + Byte.toString(c).charAt(0));
        }
        return val;
    }
}
