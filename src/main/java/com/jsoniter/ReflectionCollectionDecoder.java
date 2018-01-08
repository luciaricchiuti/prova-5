package com.jsoniter;

import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.TypeLiteral;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * class ReflectionCollectionDecoder
 * 
 * @author MaxiBon
 *
 */
class ReflectionCollectionDecoder implements Decoder {
	/**
	 * private final Constructor ctor;
	 */
	private final Constructor ctor;
	/**
	 * private final Decoder compTypeDecoder;
	 */
	private final Decoder compTypeDecoder;

	/**
	 * ReflectionCollectionDecoder
	 * 
	 * @param clazz
	 * @param typeArgs
	 */
	ReflectionCollectionDecoder(Class clazz, Type[] typeArgs) {
		try {
			ctor = clazz.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new JsonException("Error: NoSuchMethodException.");
		}
		compTypeDecoder = Codegen.getDecoder(TypeLiteral.create(typeArgs[0]).getDecoderCacheKey(), typeArgs[0]);
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
		Object obj = CodegenAccess.resetExistingObject(iter);
		if (obj instanceof Collection) {
			Collection col = (Collection) obj;
			if (iter.readNull()) {
				return null;
			}
			obj = this.ctor.newInstance();
			if (col == null && (obj instanceof Collection)) {
				col = (Collection) obj;
			} else {
				col.clear();
			}
			boolean flag = iter.readArray();
			while (flag) {
				col.add(compTypeDecoder.decode(iter));
				flag = iter.readArray();
			}
			return col;
		}
		obj = null;
		return obj;
	}
}
