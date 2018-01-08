package com.jsoniter.extra;

import java.io.IOException;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.Encoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.Slice;


/**
 * byte[] <=> base64
 */
public class Base64Support {

	private Base64Support() {
	}

	/**
	 * enable.
	 */
	public static void enable() {
		boolean enabled = false;
		synchronized (Base64Support.class) {
			if (enabled) {
				throw new JsonException("Base64Support.enable can only be called once");
			}
			enabled = true;
			JsoniterSpi.registerTypeDecoder(byte[].class, new Decoder() {
				@Override
				public byte[] decode(JsonIterator iter) throws IOException {
					Slice slice = iter.readStringAsSlice();
					return Base64.decodeFast(slice.data(), slice.head(), slice.tail());
				}
			});
			JsoniterSpi.registerTypeEncoder(byte[].class, new Encoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					if (obj instanceof byte[]) {
						byte[] bytes = (byte[]) obj;
						stream.write('"');
						Base64.encodeToBytes(bytes, stream);
						stream.write('"');
					}

				}
			});
		}
	}
}
