package com.jsoniter.output;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jsoniter.spi.ClassInfo;
import com.jsoniter.spi.JsoniterSpi;

/**
 * class CodegenImplArray
 * 
 * @author MaxiBon
 *
 */
class CodegenImplArray {

	/**
	 * Stringa If
	 */
	final static String stringaIF =  "if (e == null) { stream.writeNull(); } else {";
	/**
	 * Stringa e
	 */
	final static String stringaE = "e";
	/**
	 * Stringa }
	 */
	final static String parentesi = "}";
	
	private CodegenImplArray() {
	}

	/**
	 * genCollection
	 * 
	 * @param cacheKey
	 * @param classInfo
	 * @return
	 */
	public static CodegenResult genCollection(String cacheKey, ClassInfo classInfo) {
		Type[] typeArgs = classInfo.typeArgs;
		Class clazz = classInfo.clazz;
		Type compType = Object.class;
		if (typeArgs.length == 1) {
			compType = typeArgs[0];
		} else {
			throw new IllegalArgumentException("can not bind to generic collection without argument types, "
					+ "try syntax like TypeLiteral<List<Integer>>{}");
		}
		if (clazz == List.class) {
			clazz = ArrayList.class;
		} else if (clazz == Set.class) {
			clazz = HashSet.class;
		}
		if (List.class.isAssignableFrom(clazz)) {
			return genList(cacheKey, compType);
		} else {
			return genCollection(cacheKey, compType);
		}
	}

	public static CodegenResult genArray(String cacheKey, ClassInfo classInfo) {
		boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
		Class clazz = classInfo.clazz;
		Class compType = clazz.getComponentType();
		if (compType.isArray()) {
			throw new IllegalArgumentException("nested array not supported: " + clazz.getCanonicalName());
		}
		boolean isCollectionValueNullable = true;
		if (cacheKey.endsWith("__value_not_nullable")) {
			isCollectionValueNullable = false;
		}
		if (compType.isPrimitive()) {
			isCollectionValueNullable = false;
		}
		CodegenResult ctx = new CodegenResult();
		ctx = subGenArray1(ctx, compType, noIndention, isCollectionValueNullable);
		return subGenArray2(ctx, compType, noIndention, isCollectionValueNullable);
	}
	
	static CodegenResult subGenArray1(CodegenResult ctx, Class compType, boolean noIndention, boolean isCollectionValueNullable) {
		ctx.append("public static void encode_(java.lang.Object obj, com.jsoniter.output.JsonStream stream) throws java.io.IOException {");
		ctx.append(String.format("%s[] arr = (%s[])obj;", compType.getCanonicalName(), compType.getCanonicalName()));
		if (noIndention) {
			ctx.append("if (arr.length == 0) { return; }");
			ctx.buffer('[');
		} else {
			ctx.append("if (arr.length == 0) { stream.write((byte)'[', (byte)']'); return; }");
			ctx.append("stream.writeArrayStart(); stream.writeIndention();");
		}
		ctx.append("int i = 0;");
		ctx.append(String.format("%s e = arr[i++];", compType.getCanonicalName()));
		if (isCollectionValueNullable) {
			ctx.append(stringaIF);
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, true);
			ctx.append(parentesi); // if
		} else {
			CodegenImplNative.genWriteOp(ctx, "e", compType, false);
		}
		ctx.append("while (i < arr.length) {");
		return ctx;
	}
	
	static CodegenResult subGenArray2(CodegenResult ctx, Class compType, boolean noIndention, boolean isCollectionValueNullable) {
		if (noIndention) {
			ctx.append("stream.write(',');");
		} else {
			ctx.append("stream.writeMore();");
		}
		ctx.append("e = arr[i++];");
		if (isCollectionValueNullable) {
			ctx.append(stringaIF);
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, true);
			ctx.append("}"); // if
		} else {
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, false);
		}
		ctx.append(parentesi); // while
		if (noIndention) {
			ctx.buffer(']');
		} else {
			ctx.append("stream.writeArrayEnd();");
		}
		ctx.append(parentesi); // public static void encode_
		return ctx;
	}

	private static CodegenResult genList(String cacheKey, Type compType) {
		boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
		boolean isCollectionValueNullable = true;
		if (cacheKey.endsWith("__value_not_nullable")) {
			isCollectionValueNullable = false;
		}
		CodegenResult ctx = new CodegenResult();
		subGenList1(ctx, noIndention);
		if (isCollectionValueNullable) {
			ctx.append(stringaIF);
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, true);
			ctx.append(parentesi);
		} else {
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, false);
		}
		ctx.append("for (int i = 1; i < size; i++) {");
		return subGenList2(ctx, compType, noIndention, isCollectionValueNullable);
	}
	
	private static void subGenList1(CodegenResult ctx, boolean noIndention) {
		ctx.append(
				"public static void encode_(java.lang.Object obj, com.jsoniter.output.JsonStream stream) throws java.io.IOException {");
		ctx.append("java.util.List list = (java.util.List)obj;");
		ctx.append("int size = list.size();");
		if (noIndention) {
			ctx.append("if (size == 0) { return; }");
			ctx.buffer('[');
		} else {
			ctx.append("if (size == 0) { stream.write((byte)'[', (byte)']'); return; }");
			ctx.append("stream.writeArrayStart(); stream.writeIndention();");
		}
		ctx.append("java.lang.Object e = list.get(0);");
		
	}
	
	private static CodegenResult subGenList2(CodegenResult ctx, Type compType, boolean noIndention, boolean isCollectionValueNullable) {
		if (noIndention) {
			ctx.append("stream.write(',');");
		} else {
			ctx.append("stream.writeMore();");
		}
		ctx.append("e = list.get(i);");
		if (isCollectionValueNullable) {
			ctx.append(stringaIF);
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, true);
			ctx.append(parentesi); // if
		} else {
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, false);
		}
		ctx.append(parentesi); // for
		if (noIndention) {
			ctx.buffer(']');
		} else {
			ctx.append("stream.writeArrayEnd();");
		}
		return ctx;
	}

	private static CodegenResult genCollection(String cacheKey, Type compType) {
		boolean noIndention = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
		boolean isCollectionValueNullable = true;
		if (cacheKey.endsWith("__value_not_nullable")) {
			isCollectionValueNullable = false;
		}
		CodegenResult ctx = new CodegenResult();
		subGenCollection1(ctx, noIndention);
		if (isCollectionValueNullable) {
			ctx.append(stringaIF);
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, true);
			ctx.append(parentesi); // if
		} else {
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, false);
		}
		ctx.append("while (iter.hasNext()) {");
		return subGenCollection2(ctx, compType, noIndention, isCollectionValueNullable);
	}
	
	private static void subGenCollection1(CodegenResult ctx, boolean noIndention) {
		ctx.append(
				"public static void encode_(java.lang.Object obj, com.jsoniter.output.JsonStream stream) throws java.io.IOException {");
		ctx.append("java.util.Iterator iter = ((java.util.Collection)obj).iterator();");
		if (noIndention) {
			ctx.append("if (!iter.hasNext()) { return; }");
			ctx.buffer('[');
		} else {
			ctx.append("if (!iter.hasNext()) { stream.write((byte)'[', (byte)']'); return; }");
			ctx.append("stream.writeArrayStart(); stream.writeIndention();");
		}
		ctx.append("java.lang.Object e = iter.next();");
		
	}
	
	private static CodegenResult subGenCollection2(CodegenResult ctx, Type compType, boolean noIndention, boolean isCollectionValueNullable) {
		if (noIndention) {
			ctx.append("stream.write(',');");
		} else {
			ctx.append("stream.writeMore();");
		}
		ctx.append("e = iter.next();");
		if (isCollectionValueNullable) {
			ctx.append(stringaIF);
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, true);
			ctx.append(parentesi); // if
		} else {
			CodegenImplNative.genWriteOp(ctx, stringaE, compType, false);
		}
		ctx.append(parentesi); // while
		if (noIndention) {
			ctx.buffer(']');
		} else {
			ctx.append("stream.writeArrayEnd();");
		}
		ctx.append(parentesi); // public static void encode_
		return ctx;
	}

}
