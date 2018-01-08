package com.jsoniter;

import java.io.IOException;
import java.lang.reflect.Type;

import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.MapKeyDecoder;
import com.jsoniter.spi.Slice;
import com.jsoniter.spi.TypeLiteral;

/**
 * class DefaultMapKeyDecoder
 * 
 * @author MaxiBon
 *
 */
class DefaultMapKeyDecoder implements MapKeyDecoder {
	/**
	 * 
	 */
	private final TypeLiteral mapKeyTypeLiteral;

	/**
	 * registerOrGetExisting.
	 * 
	 * @param mapKeyType
	 * @return
	 */
	public static MapKeyDecoder registerOrGetExisting(Type mapKeyType) {
		String cacheKey = JsoniterSpi.getMapKeyDecoderCacheKey(mapKeyType);
		MapKeyDecoder mapKeyDecoder = JsoniterSpi.getMapKeyDecoder(cacheKey);
		if (null != mapKeyDecoder) {
			return mapKeyDecoder;
		}
		mapKeyDecoder = new DefaultMapKeyDecoder(TypeLiteral.create(mapKeyType));
		JsoniterSpi.addNewMapDecoder(cacheKey, mapKeyDecoder);
		return mapKeyDecoder;
	}

	/**
	 * 
	 * @param mapKeyTypeLiteral
	 */
	private DefaultMapKeyDecoder(TypeLiteral mapKeyTypeLiteral) {
		this.mapKeyTypeLiteral = mapKeyTypeLiteral;
	}

	/**
	 * @return object
	 */
	public Object decode(Slice encodedMapKey) {
		JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
		iter.reset(encodedMapKey);
		try {
			return iter.read(mapKeyTypeLiteral);
		} catch (IOException e) {
			throw new JsonException("Error: IOException.");
		} finally {
			JsonIteratorPool.returnJsonIterator(iter);
		}
	}
}
