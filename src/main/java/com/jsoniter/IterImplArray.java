package com.jsoniter;

import java.io.IOException;
/**
 * class IterImplArray
 * @author MaxiBon
 *
 */
class IterImplArray {
	
	private IterImplArray() {}
/**
 * readArray
 * @param iter
 * @return
 * @throws IOException
 */
	public static final boolean readArray(final JsonIterator iter) throws IOException {
		byte c = IterImpl.nextToken(iter);
		switch (c) {
		case '[':
			c = IterImpl.nextToken(iter);
			if (c != ']') {
				iter.unreadByte();
				return true;
			}
			return false;
		case ']':
			return false;
		case ',':
			return true;
		case 'n':
			return false;
		default:
			throw iter.reportError("readArray", "expect [ or , or n or ], but found: " + Byte.toString(c).charAt(0));
		}
	}

	public static final boolean readArrayCB(final JsonIterator iter, final JsonIterator.ReadArrayCallback callback,
			Object attachment) throws IOException {
		byte c = IterImpl.nextToken(iter);
		if (c == '[') {
			c = IterImpl.nextToken(iter);
			if (c != ']') {
				iter.unreadByte();
				if (!callback.handle(iter, attachment)) {
					return false;
				}
				byte b = IterImpl.nextToken(iter);
				int intero = b;
				while (intero == ',') {
					if (!callback.handle(iter, attachment)) {
						return false;
					}
					b = IterImpl.nextToken(iter);
					intero = b;
				}
				return true;
			}
			return true;
		}
		if (c == 'n') {
			return true;
		}
		throw iter.reportError("readArrayCB", "expect [ or n, but found: " + Byte.toString(c).charAt(0));
	}
}
