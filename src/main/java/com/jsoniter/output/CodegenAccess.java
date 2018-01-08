package com.jsoniter.output;

import java.io.IOException;

import com.jsoniter.any.Any;

import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.TypeLiteral;

/**
 * Public Class CodegenAccess.
 * 
 * @author MaxiBon
 *
 */
public class CodegenAccess {

	private CodegenAccess() {
	}

	/**
	 * writeVal
	 * 
	 * @param cacheKey
	 * @param obj
	 * @param stream
	 * @throws IOException
	 */
	public static void writeVal(String cacheKey, Object obj, JsonStream stream) throws IOException {
		JsoniterSpi.getEncoder(cacheKey).encode(obj, stream);
	}

	public static void writeVal(String cacheKey, boolean obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.BooleanEncoder) {
			com.jsoniter.spi.Encoder.BooleanEncoder encoder = (com.jsoniter.spi.Encoder.BooleanEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeBoolean(obj, stream);
		}
	}

	public static void writeVal(String cacheKey, byte obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.ShortEncoder) {
			com.jsoniter.spi.Encoder.ShortEncoder encoder = (com.jsoniter.spi.Encoder.ShortEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeShort(obj, stream);
		}
	}

	public static void writeVal(String cacheKey, short obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.ShortEncoder) {
			com.jsoniter.spi.Encoder.ShortEncoder encoder = (com.jsoniter.spi.Encoder.ShortEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeShort(obj, stream);
		}
	}

	public static void writeVal(String cacheKey, int obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.IntEncoder) {
			com.jsoniter.spi.Encoder.IntEncoder encoder = (com.jsoniter.spi.Encoder.IntEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeInt(obj, stream);
		}
	}

	public static void writeVal(String cacheKey, char obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.IntEncoder) {
			com.jsoniter.spi.Encoder.IntEncoder encoder = (com.jsoniter.spi.Encoder.IntEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeInt(obj, stream);
		}
	}

	public static void writeVal(String cacheKey, long obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.LongEncoder) {
			com.jsoniter.spi.Encoder.LongEncoder encoder = (com.jsoniter.spi.Encoder.LongEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeLong(obj, stream);
		}
	}

	public static void writeVal(String cacheKey, float obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.FloatEncoder) {
			com.jsoniter.spi.Encoder.FloatEncoder encoder = (com.jsoniter.spi.Encoder.FloatEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeFloat(obj, stream);
		}
	}

	public static void writeVal(String cacheKey, double obj, JsonStream stream) throws IOException {
		if (JsoniterSpi.getEncoder(cacheKey) instanceof com.jsoniter.spi.Encoder.DoubleEncoder) {
			com.jsoniter.spi.Encoder.DoubleEncoder encoder = (com.jsoniter.spi.Encoder.DoubleEncoder) JsoniterSpi.getEncoder(cacheKey);
			encoder.encodeDouble(obj, stream);
		}
	}

	public static void writeMapKey(String cacheKey, Object mapKey, JsonStream stream) throws IOException {
		String encodedMapKey = JsoniterSpi.getMapKeyEncoder(cacheKey).encode(mapKey);
		stream.writeVal(encodedMapKey);
	}

	public static void writeStringWithoutQuote(String obj, JsonStream stream) throws IOException {
		StreamImplString.writeStringWithoutQuote(stream, obj);
	}

	public static void staticGenEncoders(TypeLiteral[] typeLiterals, StaticCodegenTarget staticCodegenTarget) {
		Codegen.staticGenEncoders(typeLiterals, staticCodegenTarget);
	}

	public static Any wrap(Object val) {
		if (val == null) {
			return Any.wrapNull();
		}
		Class<?> clazz = val.getClass();
		String cacheKey = TypeLiteral.create(clazz).getEncoderCacheKey();
		return Codegen.getReflectionEncoder(cacheKey, clazz).wrap(val);
	}

	/**
	 * Public Class StaticCodegenTarget.
	 * 
	 * @author MaxiBon
	 *
	 */
	public static class StaticCodegenTarget {
		/**
		 * public final String outputDir;
		 * 
		 * @author MaxiBon
		 *
		 */

		public final String outputDir;

		/**
		 * StaticCodegenTarget
		 * 
		 * @param outputDir
		 */
		public StaticCodegenTarget(String outputDir) {
			this.outputDir = outputDir;
		}
	}
}
