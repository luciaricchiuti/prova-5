package com.jsoniter.extra;

import java.io.IOException;

import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;

import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;

/**
 * default float/double encoding will keep 6 decimal places enable precise
 * encoding will use JDK toString to be precise
 */
public class PreciseFloatSupport {

	private PreciseFloatSupport() {
	}

	/**
	 * enable.
	 */
	public static void enable() {
		boolean enabled = false;
		synchronized (PreciseFloatSupport.class) {
			if (enabled) {
				throw new JsonException("PreciseFloatSupport.enable can only be called once");
			}
			enabled = true;
			JsoniterSpi.registerTypeEncoder(Double.class, new com.jsoniter.spi.Encoder.ReflectionEncoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					stream.writeRaw(obj.toString());
				}

				@Override
				public Any wrap(Object obj) {
					if (obj instanceof Double) {
						Double number = (Double) obj;
						return Any.wrap(number.doubleValue());
					}
					return null;

				}
			});
			JsoniterSpi.registerTypeEncoder(double.class, new com.jsoniter.spi.Encoder.DoubleEncoder() {
				@Override
				public void encodeDouble(double obj, JsonStream stream) throws IOException {
					stream.writeRaw(Double.toString(obj));
				}
			});
			JsoniterSpi.registerTypeEncoder(Float.class, new com.jsoniter.spi.Encoder.ReflectionEncoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					stream.writeRaw(obj.toString());
				}

				@Override
				public Any wrap(Object obj) {
					if (obj instanceof Float) {
						Float number = (Float) obj;
						return Any.wrap(number.floatValue());
					}
					return null;

				}
			});
			JsoniterSpi.registerTypeEncoder(float.class, new com.jsoniter.spi.Encoder.FloatEncoder() {
				@Override
				public void encodeFloat(float obj, JsonStream stream) throws IOException {
					stream.writeRaw(Float.toString(obj));
				}
			});
		}
	}
}
