package com.jsoniter;

import java.io.IOException;

/**
 * 
 * @author MaxiBon
 *
 */
class IterImplSkip {

	private IterImplSkip() {
	}

	/**
	 * 
	 */
	static final boolean[] breaks = new boolean[127];

	static {
		breaks[' '] = true;
		breaks['\t'] = true;
		breaks['\n'] = true;
		breaks['\r'] = true;
		breaks[','] = true;
		breaks['}'] = true;
		breaks[']'] = true;
	}

	/**
	 * skip
	 * 
	 * @param iter
	 * @throws IOException
	 */
	public static final void skip(JsonIterator iter) throws IOException {
		byte c = IterImpl.nextToken(iter);
		switch (c) {
		case '"':
			IterImpl.skipString(iter);
			return;
		case '-':
			IterImpl.skipUntilBreak(iter);
			return;
		case '0':
			IterImpl.skipUntilBreak(iter);
			return;
		case '1':
			IterImpl.skipUntilBreak(iter);
			return;
		case '2':
			IterImpl.skipUntilBreak(iter);
			return;
		case '3':
			IterImpl.skipUntilBreak(iter);
			return;
		case '4':
			IterImpl.skipUntilBreak(iter);
			return;
		case '5':
			IterImpl.skipUntilBreak(iter);
			return;
		case '6':
			IterImpl.skipUntilBreak(iter);
			return;
		case '7':
			IterImpl.skipUntilBreak(iter);
			return;
		case '8':
			IterImpl.skipUntilBreak(iter);
			return;
		case '9':
			IterImpl.skipUntilBreak(iter);
			return;
		case 't':
		case 'n':
			IterImpl.skipFixedBytes(iter, 3); // true or null
			return;
		case 'f':
			IterImpl.skipFixedBytes(iter, 4); // false
			return;
		case '[':
			IterImpl.skipArray(iter);
			return;
		case '{':
			IterImpl.skipObject(iter);
			return;
		default:
			throw iter.reportError("IterImplSkip", "do not know how to skip: " + c);
		}
	}

	// adapted from: https://github.com/buger/jsonparser/blob/master/parser.go
	// Tries to find the end of string
	// Support if string contains escaped quote symbols.
	final static int findStringEnd(JsonIterator iter) {
		boolean escaped = false;
		for (int i = iter.head; i < iter.tail; i++) {
			byte c = iter.buf[i];
			if (c == '"') {
				if (!escaped) {
					return i + 1;
				} else {
					int j = i - 1;
					for (;;) {
						if (j < iter.head || iter.buf[j] != '\\') {
							// even number of backslashes
							// either end of buffer, or " found
							return i + 1;
						}
						j--;
						if (j < iter.head || iter.buf[j] != '\\') {
							// odd number of backslashes
							// it is \" or \\\"
							break;
						}
						j--;
					}
				}
			} else if (c == '\\') {
				escaped = true;
			}
		}
		return -1;
	}
}
