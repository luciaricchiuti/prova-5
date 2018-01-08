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
	 * numberVector
	 */
	final int[]  nV = {2, 3, 8,};
	/**
	 * subDecode
	 */
	final int[] sD = {1, 2, 3, 4};

	/**
	 * ReflectionArrayDecoder
	 * 
	 * @param clazz
	 */
	ReflectionArrayDecoder(Class clazz) {
		componentType = clazz.getComponentType();
		compTypeDecoder = Codegen.getDecoder(TypeLiteral.create(componentType).getDecoderCacheKey(), componentType);
	}

	/**
	 * @see com.jsoniter.spi.Decoder#decode(com.jsoniter.JsonIterator)
	 */
	@Override
	public Object decode(JsonIterator iter) throws IOException {
		CodegenAccess.resetExistingObject(iter);
		if (iter.readNull()) {
			return null;
		}
		if (!CodegenAccess.readArrayStart(iter)) {
			return Array.newInstance(componentType, 0);
		}
		
		if (CodegenAccess.nextToken(iter) != ',') {
			return subDecode(sD[0], iter);
		}
		
		if (CodegenAccess.nextToken(iter) != ',') {
			return subDecode(sD[1], iter);
		}
		
		if (CodegenAccess.nextToken(iter) != ',') {
			return subDecode(sD[2], iter);
		}
		
		Object arr = subDecode(sD[3], iter);		
		int[] cond = whileSupport(arr, iter);
		if (cond[0] == cond[1]) {
			return arr;
		}
		Object newArr = Array.newInstance(componentType, cond[0]);
		System.arraycopy(arr, 0, newArr, 0, cond[0]);
		return newArr;
	}
	
	Object subDecode(int i, JsonIterator iter) throws IOException {
		
		Object a1 = compTypeDecoder.decode(iter);
		Object a2 = compTypeDecoder.decode(iter);
		Object a3 = compTypeDecoder.decode(iter);
		Object a4 = compTypeDecoder.decode(iter);		
		Object arr = null;
		
		switch(i) {
		case 1:
			arr = Array.newInstance(componentType, 1);
			Array.set(arr, 0, a1);
			break;
		case 2:
			arr = Array.newInstance(componentType, nV[0]);
			Array.set(arr, 0, a1);
			Array.set(arr, 1, a2);
			break;
		case 3:
			arr = Array.newInstance(componentType, nV[1]);
			Array.set(arr, 0, a1);
			Array.set(arr, 1, a2);
			Array.set(arr, nV[0], a3);
			break;
		default:
			arr = Array.newInstance(componentType, nV[2]);
			Array.set(arr, 0, a1);
			Array.set(arr, 1, a2);
			Array.set(arr, nV[0], a3);
			Array.set(arr, nV[1], a4);
			break;
		}
		return arr;
	}
	
	int[] whileSupport(Object arr, JsonIterator iter) throws IOException {
		
		int i = 4;
		int arrLen = 8;
		byte b = CodegenAccess.nextToken(iter);
		int intero = b;
		while (intero == ',') {
			if (i == arrLen) {
				int n = 2 * arrLen;
				Object newArr = Array.newInstance(componentType, n);
				System.arraycopy(arr, 0, newArr, 0, arrLen);
				arr = newArr;
				arrLen = 2 * arrLen;
			}
			Array.set(arr, i++, compTypeDecoder.decode(iter));
			b = CodegenAccess.nextToken(iter);
			intero = b;
		}
		
		int[] result = {i, arrLen};
		return result;
	}
}
