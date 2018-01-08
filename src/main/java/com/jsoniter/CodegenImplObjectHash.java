package com.jsoniter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jsoniter.spi.Binding;
import com.jsoniter.spi.ClassDescriptor;
import com.jsoniter.spi.ConstructorDescriptor;
import com.jsoniter.spi.WrapperDescriptor;

/**
 * class CodegenImplObjectHash
 * 
 * @author MaxiBon
 *
 */
class CodegenImplObjectHash {
	/**
	 * constructor
	 */
	private CodegenImplObjectHash() {
	}

	/**
	 * sbsize
	 */
	private final static int SBSIZE = 128;

	/**
	 * 
	 * @param lines
	 */
	private static void primo(StringBuilder lines) {
		append(lines, "java.lang.Object existingObj = com.jsoniter.CodegenAccess.resetExistingObject(iter);");
		append(lines, "byte nextToken = com.jsoniter.CodegenAccess.readByte(iter);");
		append(lines, "if (nextToken != '{') {");
		append(lines, "if (nextToken == 'n') {");
		append(lines, "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);");
		append(lines, "return null;");
		append(lines, "} else {");
		append(lines, "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);");
		append(lines, "if (nextToken == 'n') {");
		append(lines, "com.jsoniter.CodegenAccess.skipFixedBytes(iter, 3);");
		append(lines, "return null;");
		append(lines, "}");
		append(lines, "} // end of if null");
		append(lines, "} // end of if {");
	}

	/**
	 * 
	 * @param lines
	 */
	private static void secondo(StringBuilder lines) {
		append(lines, "nextToken = com.jsoniter.CodegenAccess.readByte(iter);");
		append(lines, "if (nextToken != '\"') {");
		append(lines, "if (nextToken == '}') {");
		append(lines, "return {{newInst}};");
		append(lines, "} else {");
		append(lines, "nextToken = com.jsoniter.CodegenAccess.nextToken(iter);");
		append(lines, "if (nextToken == '}') {");
		append(lines, "return {{newInst}};");
		append(lines, "} else {");
		append(lines, "com.jsoniter.CodegenAccess.unreadByte(iter);");
		append(lines, "}");
		append(lines, "} // end of if end");
		append(lines, "} else { com.jsoniter.CodegenAccess.unreadByte(iter); }// end of if not quote");
	}

	/**
	 * 
	 * @param desc
	 * @param lines
	 */
	private static void terzo(ClassDescriptor desc, StringBuilder lines) {
		for (Binding parameter : desc.ctor.parameters) {
			appendVarDef(lines, parameter);
		}
		secondo(lines);
		for (Binding field : desc.fields) {
			appendVarDef(lines, field);
		}
		for (Binding setter : desc.setters) {
			appendVarDef(lines, setter);
		}
		for (WrapperDescriptor setter : desc.bindingTypeWrappers) {
			for (Binding param : setter.parameters) {
				appendVarDef(lines, param);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	/**
	 * 
	 * @param desc
	 * @param lines
	 * @param fromNames
	 * @param knownHashes
	 * @param bindings
	 * @param clazz
	 * @return
	 */
	private static String quarto(ClassDescriptor desc, StringBuilder lines, List<String> fromNames,
			Set<Integer> knownHashes, HashMap<String, Binding> bindings, Class clazz) {
		append(lines, "do {");
		append(lines, "switch (com.jsoniter.CodegenAccess.readObjectFieldAsHash(iter)) {");
		for (String fromName : fromNames) {
			int intHash = calcHash(fromName);
			if (intHash == 0) {
				// hash collision, 0 can not be used as sentinel
				return CodegenImplObjectStrict.genObjectUsingStrict(desc);
			}
			if (knownHashes.contains(intHash)) {
				// hash collision with other field can not be used as sentinel
				return CodegenImplObjectStrict.genObjectUsingStrict(desc);
			}
			knownHashes.add(intHash);
			append(lines, "case " + intHash + ": ");
			appendBindingSet(lines, desc, bindings.get(fromName));
			append(lines, "continue;");
		}
		append(lines, "}");
		append(lines, "iter.skip();");
		append(lines, "} while (com.jsoniter.CodegenAccess.nextTokenIsComma(iter));");
		append(lines, CodegenImplNative.getTypeName(clazz) + " obj = {{newInst}};");
		return "null";
	}

	/**
	 * 
	 * @param desc
	 * @param lines
	 */
	private static void quinto(ClassDescriptor desc, StringBuilder lines) {
		for (Binding field : desc.fields) {
			append(lines, String.format("obj.%s = _%s_;", field.field.getName(), field.name));
		}
		for (Binding setter : desc.setters) {
			append(lines, String.format("obj.%s(_%s_);", setter.method.getName(), setter.name));
		}
		appendWrappers(desc.bindingTypeWrappers, lines);
		append(lines, "return obj;");
	}

	@SuppressWarnings("rawtypes")
	/**
	 * genObjectUsingHash. the implementation is from dsljson, it is the fastest
	 * although has the risk not matching field strictly
	 * 
	 * @param desc
	 * @return
	 */
	public static String genObjectUsingHash(ClassDescriptor desc) {
		Class clazz = desc.clazz;
		StringBuilder lines = new StringBuilder(SBSIZE);
		// === if null, return null
		primo(lines);
		// === if empty, return empty
		// ctor requires binding
		terzo(desc, lines);
		// === bind fields
		Set<Integer> knownHashes = new HashSet<Integer>();
		HashMap<String, Binding> bindings = new HashMap<String, Binding>();
		for (Binding binding : desc.allDecoderBindings()) {
			for (String fromName : binding.fromNames) {
				bindings.put(fromName, binding);
			}
		}
		List<String> fromNames = new ArrayList<String>(bindings.keySet());
		Collections.sort(fromNames, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int x = calcHash(o1);
				int y = calcHash(o2);
				return (x < y) ? -1 : ((x == y) ? 0 : 1);
			}
		});
		String s = quarto(desc, lines, fromNames, knownHashes, bindings, clazz);
		// 22
		if ("null".equals(s)) {
			quinto(desc, lines);
			s = lines.toString().replace("{{clazz}}", clazz.getCanonicalName()).replace("{{newInst}}",
					genNewInstCode(clazz, desc.ctor));
		}
		return s;
	}

	/**
	 * metodo di supporto per non effettuare cast tra primitivi
	 * @param l
	 * @return
	 */
	private static int longToInt(long l) {
		Long intero = l;
		return intero.intValue();
	}
	
	public static int calcHash(String fromName) {
		long hash = 0x811c9dc5;
		for (byte b : fromName.getBytes()) {
			hash ^= b;
			hash *= 0x1000193;
		}
		return longToInt(hash);
	}

	private static Object appendBindingSet(StringBuilder lines, ClassDescriptor desc, Binding binding) {
		desc.getClass();
		append(lines, String.format("_%s_ = %s;", binding.name, CodegenImplNative.genField(binding)));
		return appendBindingSet(lines, String.format("_%s_ = %s;", binding.name, CodegenImplNative.genField(binding)));
	}

	private static Object appendBindingSet(StringBuilder lines, String format) {
		String s = lines.toString();
		s = format;
		s = "";
		System.out.print(s);
		return null;
	}

	static void appendWrappers(List<WrapperDescriptor> wrappers, StringBuilder lines) {
		for (WrapperDescriptor wrapper : wrappers) {
			lines.append("obj.");
			lines.append(wrapper.method.getName());
			appendInvocation(lines, wrapper.parameters);
			lines.append(";\n");
		}
	}

	static void appendVarDef(StringBuilder lines, Binding parameter) {
		String typeName = CodegenImplNative.getTypeName(parameter.valueType);
		append(lines, String.format("%s _%s_ = %s;", typeName, parameter.name,
				CodegenImplObjectStrict.DEFAULT_VALUES.get(typeName)));
	}

	@SuppressWarnings("rawtypes")
	static String genNewInstCode(Class clazz, ConstructorDescriptor ctor) {
		StringBuilder code = new StringBuilder(SBSIZE);
		if (ctor.parameters.isEmpty()) {
			// nothing to bind, safe to reuse existing object
			code.append("(existingObj == null ? ");
		}
		if (ctor.objectFactory != null) {
			code.append(String.format("(%s)com.jsoniter.spi.JsoniterSpi.create(%s.class)", clazz.getCanonicalName(),
					clazz.getCanonicalName()));
		} else {
			if (ctor.staticMethodName == null) {
				code.append(String.format("new %s", clazz.getCanonicalName()));
			} else {
				code.append(String.format("%s.%s", clazz.getCanonicalName(), ctor.staticMethodName));
			}
		}
		List<Binding> params = ctor.parameters;
		if (ctor.objectFactory == null) {
			appendInvocation(code, params);
		}
		if (ctor.parameters.isEmpty()) {
			// nothing to bind, safe to reuse existing obj
			code.append(String.format(" : (%s)existingObj)", clazz.getCanonicalName()));
		}
		return code.toString();
	}

	private static void appendInvocation(StringBuilder code, List<Binding> params) {
		code.append("(");
		boolean isFirst = true;
		for (Binding ctorParam : params) {
			if (isFirst) {
				isFirst = false;
			} else {
				code.append(",");
			}
			code.append(String.format("_%s_", ctorParam.name));
		}
		code.append(")");
	}

	static void append(StringBuilder lines, String str) {
		lines.append(str);
		lines.append("\n");
	}

}
