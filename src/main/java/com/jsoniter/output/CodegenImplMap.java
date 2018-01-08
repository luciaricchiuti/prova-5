package com.jsoniter.output;

import com.jsoniter.spi.ClassInfo;
import com.jsoniter.spi.JsoniterSpi;

import java.lang.reflect.Type;

/**
 * class CodegenImplMap
 * 
 * @author MaxiBon
 *
 */
class CodegenImplMap {

	/**
	 * Stringa 
	 */
	static final String stringa = "entry.getValue()";
	/**
	 * Parentesi
	 */
	static final String parentesi = "}";
	
	private CodegenImplMap() {
	}

	/**
	 * genMap
	 * 
	 * @param cacheKey
	 * @param classInfo
	 * @return
	 */
	public static CodegenResult genMap(String cacheKey, ClassInfo classInfo) {
		boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
		Type[] typeArgs = classInfo.typeArgs;
		boolean isCollectionValueNullable = true;
		if (cacheKey.endsWith("__value_not_nullable")) {
			isCollectionValueNullable = false;
		}
		Type keyType = String.class;
		Type valueType = Object.class;
		if (typeArgs.length == 2) {
			keyType = typeArgs[0];
			valueType = typeArgs[1];
		}
		String mapCacheKey = JsoniterSpi.getMapKeyEncoderCacheKey(keyType);
		CodegenResult ctx = new CodegenResult();
		
		subGenMap1(ctx, noIndention);
		if (keyType == String.class) {
			ctx.append("stream.writeVal((java.lang.String)entry.getKey());");
		} else {
			ctx.append(String.format("com.jsoniter.output.CodegenAccess.writeMapKey(\"%s\", entry.getKey(), stream);",
					mapCacheKey));
		}
		String keyTypeString = keyType.toString();
		subGenMap2(ctx, noIndention, keyTypeString, mapCacheKey, isCollectionValueNullable, valueType);
		subGenMap3(ctx, noIndention, isCollectionValueNullable, valueType);
		return ctx;
	}
	
	private static void subGenMap1(CodegenResult ctx, boolean noIndention) {
		ctx.append("public static void encode_(java.lang.Object obj, com.jsoniter.output.JsonStream stream) throws java.io.IOException {");
		ctx.append("if (obj == null) { stream.writeNull(); return; }");
		ctx.append("java.util.Map map = (java.util.Map)obj;");
		ctx.append("java.util.Iterator iter = map.entrySet().iterator();");
		if (noIndention) {
			ctx.append("if(!iter.hasNext()) { return; }");
		} else {
			ctx.append("if(!iter.hasNext()) { stream.write((byte)'{', (byte)'}'); return; }");
		}
		ctx.append("java.util.Map.Entry entry = (java.util.Map.Entry)iter.next();");
		if (noIndention) {
			ctx.buffer('{');
		} else {
			ctx.append("stream.writeObjectStart(); stream.writeIndention();");
		}
		
	}
	
	private static void subGenMap2(CodegenResult ctx, boolean noIndention, String keyTypeString, String mapCacheKey, boolean isCollectionValueNullable, Type valueType) {
		if (noIndention) {
			ctx.append("stream.write(':');");
		} else {
			ctx.append("stream.write((byte)':', (byte)' ');");
		}
		if (isCollectionValueNullable) {
			ctx.append("if (entry.getValue() == null) { stream.writeNull(); } else {");
			CodegenImplNative.genWriteOp(ctx, stringa, valueType, true);
			ctx.append(parentesi);
		} else {
			CodegenImplNative.genWriteOp(ctx, stringa, valueType, false);
		}
		ctx.append("while(iter.hasNext()) {");
		ctx.append("entry = (java.util.Map.Entry)iter.next();");
		if (noIndention) {
			ctx.append("stream.write(',');");
		} else {
			ctx.append("stream.writeMore();");
		}
		if (keyTypeString == String.class.toString()) {
			ctx.append("stream.writeVal((java.lang.String)entry.getKey());");
		} else {
			ctx.append(String.format("com.jsoniter.output.CodegenAccess.writeMapKey(\"%s\", entry.getKey(), stream);",
					mapCacheKey));
		}
	}
	
	private static void subGenMap3(CodegenResult ctx, boolean noIndention, boolean isCollectionValueNullable, Type valueType) {
		if (noIndention) {
			ctx.append("stream.write(':');");
		} else {
			ctx.append("stream.write((byte)':', (byte)' ');");
		}
		if (isCollectionValueNullable) {
			ctx.append("if (entry.getValue() == null) { stream.writeNull(); } else {");
			CodegenImplNative.genWriteOp(ctx, stringa, valueType, true);
			ctx.append(parentesi);
		} else {
			CodegenImplNative.genWriteOp(ctx, stringa, valueType, false);
		}
		ctx.append(parentesi);
		if (noIndention) {
			ctx.buffer('}');
		} else {
			ctx.append("stream.writeObjectEnd();");
		}
		ctx.append(parentesi);
	}
		
}
