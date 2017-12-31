package com.jsoniter;

import java.io.IOException;
import java.lang.reflect.Array;

import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.TypeLiteral;

/**
 * class ReflectionArrayDecoder
 * 
 * @author MaxiBon
 *
 */
class ReflectionArrayDecoder implements Decoder {

	private final Class componentType;
	private final Decoder compTypeDecoder;

	/**
	 * ReflectionArrayDecoder
	 * 
	 * @param clazz
	 */
	ReflectionArrayDecoder(Class clazz) {
		componentType = clazz.getComponentType();
		compTypeDecoder = Codegen.getDecoder(TypeLiteral.create(componentType).getDecoderCacheKey(), componentType);
	}

	@Override
	public Object decode(JsonIterator iter) throws IOException {
		CodegenAccess.resetExistingObject(iter);
		if (iter.readNull()) {
			return null;
		}
		if (!CodegenAccess.readArrayStart(iter)) {
			return Array.newInstance(componentType, 0);
		}
		Object a1 = compTypeDecoder.decode(iter);
		if (CodegenAccess.nextToken(iter) != ',') {
			Object arr = Array.newInstance(componentType, 1);
			Array.set(arr, 0, a1);
			return arr;
		}
		Object a2 = compTypeDecoder.decode(iter);
		if (CodegenAccess.nextToken(iter) != ',') {
			Object arr = Array.newInstance(componentType, 2);
			Array.set(arr, 0, a1);
			Array.set(arr, 1, a2);
			return arr;
		}
		Object a3 = compTypeDecoder.decode(iter);
		if (CodegenAccess.nextToken(iter) != ',') {
			Object arr = Array.newInstance(componentType, 3);
			Array.set(arr, 0, a1);
			Array.set(arr, 1, a2);
			Array.set(arr, 2, a3);
			return arr;
		}
		Object a4 = compTypeDecoder.decode(iter);
		Object arr = Array.newInstance(componentType, 8);
		Array.set(arr, 0, a1);
		Array.set(arr, 1, a2);
		Array.set(arr, 2, a3);
		Array.set(arr, 3, a4);
		int i = 4;
		int arrLen = 8;
		byte b = CodegenAccess.nextToken(iter);
		int intero = b;
		while (intero == ',') {
			if (i == arrLen) {
				Object newArr = Array.newInstance(componentType, 2 * arrLen);
				System.arraycopy(arr, 0, newArr, 0, arrLen);
				arr = newArr;
				arrLen = 2 * arrLen;
			}
			Array.set(arr, i++, compTypeDecoder.decode(iter));
			b = CodegenAccess.nextToken(iter);
			intero = b;
		}
		if (i == arrLen) {
			return arr;
		}
		Object newArr = Array.newInstance(componentType, i);
		System.arraycopy(arr, 0, newArr, 0, i);
		return newArr;
	}
}
