package com.jsoniter.output;


import com.jsoniter.spi.JsoniterSpi;

/**
 * class CodegenResult
 * 
 * @author MaxiBon
 *
 */
class CodegenResult {

	private final boolean supportBuffer;
	/**
	 * String prelude
	 */
	String prelude = null; // first
	/**
	 * String epilogue
	 */
	String epilogue = null; // last
	private static int SBSIZE = 128;
	private StringBuilder lines = new StringBuilder(SBSIZE);
	private StringBuilder buffered = new StringBuilder(SBSIZE);

	/**
	 * CodegenResult
	 */
	CodegenResult() {
		supportBuffer = JsoniterSpi.getCurrentConfig().indentionStep() == 0;
	}

	public void append(String str) {
		if (str.contains("stream")) {
			// maintain the order of write op
			// must flush now
			appendBuffer();
		}
		lines.append(str);
		String linea = "\n";
		lines.append(linea);
	}

	public void buffer(char c) {
		if (supportBuffer) {
			buffered.append(c);
		} else {
			throw new UnsupportedOperationException("internal error: should not call buffer when indention step > 0");
		}
	}

	public void buffer(String s) {
		if (s == null) {
			return;
		}
		if (supportBuffer) {
			buffered.append(s);
		} else {
			throw new UnsupportedOperationException("internal error: should not call buffer when indention step > 0");

		}
	}

	public void flushBuffer() {
		if (buffered.length() == 0) {
			return;
		}
		if (prelude == null) {
			prelude = buffered.toString();
		} else {
			epilogue = buffered.toString();
		}
		buffered.setLength(0);
	}

	public String toString() {
		return lines.toString();
	}

	public void appendBuffer() {
		flushBuffer();
		if (epilogue != null) {
			lines.append(bufferToWriteOp(epilogue));
			lines.append("\n");
			epilogue = null;
		}
	}

	public String generateWrapperCode(Class clazz) {
		flushBuffer();
		StringBuilder linea = new StringBuilder(SBSIZE);
		linea.append(
				"public void encode(Object obj, com.jsoniter.output.JsonStream stream) throws java.io.IOException {\n");
		linea.append("if (obj == null) { stream.writeNull(); return; }\n");
		if (prelude != null) {
			append(linea, CodegenResult.bufferToWriteOp(prelude));
		}
		linea.append(String.format("encode_((%s)obj, stream);", clazz.getCanonicalName()) + "\n");
		if (epilogue != null) {
			linea.append(CodegenResult.bufferToWriteOp(epilogue) + "\n");
		}
		linea.append("}\n");
		return linea.toString();
	}

	private static void append(StringBuilder linea, String linea1) {
		linea.append(linea1);
		linea.append('\n');
	}

	public static String bufferToWriteOp(String buff) {
		if (buff == null) {
			return "";
		}
		if (buff.length() == 1) {
			return String.format("stream.write((byte)'%s');", escape(buff.charAt(0)));
		} else if (buff.length() == 2) {
			return String.format("stream.write((byte)'%s', (byte)'%s');", escape(buff.charAt(0)),
					escape(buff.charAt(1)));
		} else if (buff.length() == 3) {
			return String.format("stream.write((byte)'%s', (byte)'%s', (byte)'%s');", escape(buff.charAt(0)),
					escape(buff.charAt(1)), escape(buff.charAt(2)));
		} else if (buff.length() == 4) {
			return String.format("stream.write((byte)'%s', (byte)'%s', (byte)'%s', (byte)'%s');",
					escape(buff.charAt(0)), escape(buff.charAt(1)), escape(buff.charAt(2)),
					escape(buff.charAt(3)));
		} else {
			StringBuilder escape = new StringBuilder(SBSIZE);
			int size1 = buff.length();
			for (int i = 0; i < size1; i++) {
				char cBuff = buff.charAt(i);
				if (cBuff == '"') {
					escape.append('\\');
				}
				escape.append(cBuff);
			}
			return String.format("stream.writeRaw(\"%s\", %s);", escape.toString(), buff.length());
		}
	}

	private static String escape(char c) {
		if (c == '"') {
			return "\\\"";
		}
		if (c == '\\') {
			return "\\\\";
		}
		return String.valueOf(c);
	}
}
