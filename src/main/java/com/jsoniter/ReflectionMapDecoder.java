package com.jsoniter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Map;

import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.MapKeyDecoder;
import com.jsoniter.spi.Slice;
import com.jsoniter.spi.TypeLiteral;

/**
 * class ReflectionMapDecoder
 * 
 * @author MaxiBon
 *
 */
class ReflectionMapDecoder implements Decoder {

	private final Constructor ctor;
	private final Decoder valueTypeDecoder;
	private final MapKeyDecoder mapKeyDecoder;

	/**
	 * ReflectionMapDecoder
	 * 
	 * @param clazz
	 * @param typeArgs
	 */
	ReflectionMapDecoder(Class clazz, Type[] typeArgs) {
		try {
			ctor = clazz.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new JsonException("Error: NoSuchMethodException.");
		}
		Type keyType = typeArgs[0];
		if (keyType == String.class) {
			mapKeyDecoder = null;
		} else {
			mapKeyDecoder = DefaultMapKeyDecoder.registerOrGetExisting(keyType);
		}
		TypeLiteral valueTypeLiteral = TypeLiteral.create(typeArgs[1]);
		valueTypeDecoder = Codegen.getDecoder(valueTypeLiteral.getDecoderCacheKey(), typeArgs[1]);
	}

	/**
	 * throws IOException
	 */
	public Object decode(JsonIterator iter) throws IOException {
		try {
			return decode_(iter);
		} catch (JsonException e) {
			throw e;
		} catch (Exception e) {
			throw new JsonException("Error: Exception.");
		}
	}

	/**
	 * 
	 * @param iter
	 * @return
	 * @throws Exception
	 */
	private Object decode_(JsonIterator iter) throws Exception {
		Object decodedMapKey = null;
		if (CodegenAccess.resetExistingObject(iter) instanceof Map) {
			Map map = (Map) CodegenAccess.resetExistingObject(iter);
			if (iter.readNull()) {
				return null;
			}
			if (map == null && (ctor.newInstance() instanceof Map)) {
				map = (Map) ctor.newInstance();
			}
			if (!CodegenAccess.readObjectStart(iter)) {
				return map;
			}
			decodedMapKey = readMapKey(iter);
			map.put(decodedMapKey, valueTypeDecoder.decode(iter));
			byte b = CodegenAccess.nextToken(iter);
			int intero = b;
			while (intero == ',') {
				decodedMapKey = readMapKey(iter);
				map.put(decodedMapKey, valueTypeDecoder.decode(iter));
				b = CodegenAccess.nextToken(iter);
				intero = b;
			}
			return map;
		}
		return decodedMapKey;
	}

	/**
	 * 
	 * @param iter
	 * @return
	 * @throws IOException
	 */
	private Object readMapKey(JsonIterator iter) throws IOException {
		if (mapKeyDecoder == null) {
			return CodegenAccess.readObjectFieldAsString(iter);
		} else {
			Slice mapKey = CodegenAccess.readObjectFieldAsSlice(iter);
			return mapKeyDecoder.decode(mapKey);
		}
	}
}
