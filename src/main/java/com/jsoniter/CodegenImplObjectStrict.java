package com.jsoniter;

import static com.jsoniter.CodegenImplObjectHash.appendVarDef;
import static com.jsoniter.CodegenImplObjectHash.appendWrappers;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.jsoniter.spi.Binding;
import com.jsoniter.spi.ClassDescriptor;
import com.jsoniter.spi.ConstructorDescriptor;
import com.jsoniter.spi.GenericsHelper;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.WrapperDescriptor;

/**
 * class CodegenImplObjectStrict
 * 
 * @author MaxiBon
 *
 */
class CodegenImplObjectStrict {
	/**
	 * CONSTRUCTOR
	 */
	private CodegenImplObjectStrict() {
	}

	/**
	 * }
	 */
	private static final String PARENTESICHIUSA = "}";

	/**
	 * {
	 */
	private static final String PARENTESIAPERTA = "{";

	/**
	 * 0
	 */
	private static final String ZERO = "0";

	/**
	 * 128
	 */
	private final static int SBSIZE = 128;

	/**
	 * "throw new com.jsoniter.spi.JsonException('extra property: %s');"
	 */
	static final String QUARTO = "throw new com.jsoniter.spi.JsonException('extra property: %s');";

	/**
	 * %scom.jsoniter.CodegenAccess.setExistingObject(iter, obj.%s);obj.%s=%s%s
	 */
	static final String QUINTO = "%scom.jsoniter.CodegenAccess.setExistingObject(iter, obj.%s);obj.%s=%s%s";

	@SuppressWarnings("serial")
	/**
	 *  DEFAULT_VALUES
	 */
	static final Map<String, String> DEFAULT_VALUES = new HashMap<String, String>() {
		{
			put("float", "0.0f");
			put("double", "0.0d");
			put("boolean", "false");
			String byt = "byte";
			String num = ZERO;
			put(byt, num);
			put("short", ZERO);
			put("int", ZERO);
			put("char", ZERO);
			put("long", ZERO);
		}
	};

	/**
	 * 
	 * @param desc
	 * @param rendered
	 * @return
	 */
	private static String primo(ClassDescriptor desc, String rendered) {
		String toReturn = rendered;
		if (desc.ctor.parameters.isEmpty()) {
			// if not field or setter, the value will set to temp variable
			for (Binding field : desc.fields) {
				toReturn = updateBindingSetOp(rendered, field);
			}
			for (Binding setter : desc.setters) {
				toReturn = updateBindingSetOp(rendered, setter);
			}
		}
		return toReturn;
	}

	/**
	 * 
	 * @param desc
	 * @param lines
	 * @return
	 */
	private static void secondo(ClassDescriptor desc, StringBuilder lines) {
		String temp = "";
		if (!desc.ctor.parameters.isEmpty()) {
			temp = "%s obj = {{newInst}};";
			append(lines, String.format(temp, CodegenImplNative.getTypeName(desc.clazz)));
			for (Binding field : desc.fields) {
				temp = "obj.%s = _%s_;";
				append(lines, String.format(temp, field.field.getName(), field.name));
			}
			for (Binding setter : desc.setters) {
				temp = "obj.%s(_%s_);";
				append(lines, String.format(temp, setter.method.getName(), setter.name));
			}
		}
	}

	/**
	 * 24 LINES OF CODE
	 * 
	 * @param allBindings
	 * @param desc
	 * @param lines
	 * @param rendered
	 * @param hasRequiredBinding
	 * @param expectedTracker
	 * @return
	 */
	private static void multipleAppend(List<Binding> allBindings, ClassDescriptor desc, StringBuilder lines,
			String rendered, boolean hasRequiredBinding, long expectedTracker) {
		append(lines, "once = false;");
		if (hasAnythingToBindFrom(allBindings)) {
			append(lines, "switch (field.len()) {");
			append(lines, rendered);
			append(lines, PARENTESICHIUSA);
		}
		appendOnUnknownField(lines, desc);
		append(lines, PARENTESICHIUSA);
		append(lines, "while (com.jsoniter.CodegenAccess.nextToken(iter) == ',') {");
		append(lines, "field = com.jsoniter.CodegenAccess.readObjectFieldAsSlice(iter);");
		if (hasAnythingToBindFrom(allBindings)) {
			append(lines, "switch (field.len()) {");
			append(lines, rendered);
			append(lines, PARENTESICHIUSA); // end of switch
		}
		appendOnUnknownField(lines, desc);
		append(lines, PARENTESICHIUSA); // end of while
		if (hasRequiredBinding) {
			append(lines, "if (tracker != " + expectedTracker + "L) {");
			appendMissingRequiredProperties(lines, desc);
			append(lines, PARENTESICHIUSA);
		}
	}

	/**
	 * 19 LOC
	 * 
	 * @param desc
	 * @param lines
	 * @param b
	 * @return
	 */
	private static StringBuilder multipleAppend2(ClassDescriptor desc, StringBuilder lines, boolean b) {
		StringBuilder toReturn = lines;
		for (Binding parameter : desc.ctor.parameters) {
			appendVarDef(toReturn, parameter);
		}
		append(toReturn, "if (!com.jsoniter.CodegenAccess.readObjectStart(iter)) {");
		if (b) {
			appendMissingRequiredProperties(toReturn, desc);
		} else {
			append(toReturn, "return {{newInst}};");
		}
		append(toReturn, PARENTESICHIUSA);
		for (Binding field : desc.fields) {
			appendVarDef(toReturn, field);
		}
		for (Binding setter : desc.setters) {
			appendVarDef(toReturn, setter);
		}
		return toReturn;
	}

	/**
	 * 23 LOC
	 * 
	 * @param bin
	 * @param cD
	 * @param s
	 * @param b
	 * @param l
	 * @param m
	 * @return
	 */
	private static void terzo(List<Binding> bin, ClassDescriptor cD, StringBuilder lines, boolean b, long l,
			Map<Integer, Object> m) {
		for (WrapperDescriptor wrapper : cD.bindingTypeWrappers) {
			for (Binding param : wrapper.parameters) {
				appendVarDef(lines, param);
			}
		}
		if (cD.onExtraProperties != null || !cD.keyValueTypeWrappers.isEmpty()) {
			append(lines, "java.util.Map extra = null;");
		}
		append(lines, "com.jsoniter.spi.Slice field = com.jsoniter.CodegenAccess.readObjectFieldAsSlice(iter);");
		append(lines, "boolean once = true;");
		append(lines, "while (once) {");
		multipleAppend(bin, cD, lines, primo(cD, renderTriTree(m)), b, l);
		if (cD.onExtraProperties != null) {
			appendSetExtraProperteis(lines, cD);
		}
		if (!cD.keyValueTypeWrappers.isEmpty()) {
			appendSetExtraToKeyValueTypeWrappers(lines, cD);
		}
		secondo(cD, lines);
		appendWrappers(cD.bindingTypeWrappers, lines);
		append(lines, "return obj;");
	}

	/**
	 * genObjectUsingStrict 25 LOC
	 * 
	 * @param desc
	 * @return
	 */
	public static String genObjectUsingStrict(ClassDescriptor desc) {
		List<Binding> allBindings = desc.allDecoderBindings();
		int lastRequiredIdx = assignMaskForRequiredProperties(allBindings);
		boolean hasRequiredBinding = lastRequiredIdx > 0;
		long expectedTracker = Long.MAX_VALUE >> (63 - lastRequiredIdx);
		Map<Integer, Object> trieTree = buildTriTree(allBindings);
		StringBuilder lines = new StringBuilder(SBSIZE);
		lines.append("");
		append(lines, "java.lang.Object existingObj = com.jsoniter.CodegenAccess.resetExistingObject(iter);");
		append(lines, "if (iter.readNull()) { return null; }");
		if (hasRequiredBinding) {
			append(lines, "long tracker = 0;");
		}
		if (desc.ctor.parameters.isEmpty()) {
			append(lines, "{{clazz}} obj = {{newInst}};");
			append(lines, "if (!com.jsoniter.CodegenAccess.readObjectStart(iter)) {");
			if (hasRequiredBinding) {
				appendMissingRequiredProperties(lines, desc);
			}
			append(lines, "return obj;");
			append(lines, PARENTESIAPERTA);
		} else {
			lines = multipleAppend2(desc, lines, hasRequiredBinding);
		}
		terzo(allBindings, desc, lines, hasRequiredBinding, expectedTracker, trieTree);
		return lines.toString().replace("{{clazz}}", desc.clazz.getCanonicalName()).replace("{{newInst}}",
				CodegenImplObjectHash.genNewInstCode(desc.clazz, desc.ctor));
	}

	/**
	 * 
	 * @param lines
	 * @param desc
	 */
	private static void appendSetExtraToKeyValueTypeWrappers(StringBuilder lines, ClassDescriptor desc) {
		append(lines, "java.util.Iterator extraIter = extra.entrySet().iterator();");
		append(lines, "while(extraIter.hasNext()) {");
		for (Method wrapper : desc.keyValueTypeWrappers) {
			append(lines, "java.util.Map.Entry entry = (java.util.Map.Entry)extraIter.next();");
			append(lines, "String key = entry.getKey().toString();");
			append(lines, "com.jsoniter.any.Any value = (com.jsoniter.any.Any)entry.getValue();");
			append(lines, String.format("obj.%s(key, value.object());", wrapper.getName()));
		}
		append(lines, PARENTESICHIUSA);
	}

	/**
	 * 
	 * @param lines
	 * @param desc
	 */
	private static void appendSetExtraProperteis(StringBuilder lines, ClassDescriptor desc) {
		Binding onExtraProperties = desc.onExtraProperties;
		if (GenericsHelper.isSameClass(onExtraProperties.valueType, Map.class)) {
			if (onExtraProperties.field != null) {
				append(lines, String.format("obj.%s = extra;", onExtraProperties.field.getName()));
			} else {
				append(lines, String.format("obj.%s(extra);", onExtraProperties.method.getName()));
			}
			return;
		}
		throw new JsonException("extra properties can only be Map");
	}

	/**
	 * 
	 * @param allBindings
	 * @return
	 */
	private static boolean hasAnythingToBindFrom(List<Binding> allBindings) {
		boolean flag = false;
		for (Binding binding : allBindings) {
			if (binding.fromNames.length > 0) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	/**
	 * 
	 * @param allBindings
	 * @return
	 */
	private static int assignMaskForRequiredProperties(List<Binding> allBindings) {
		int requiredIdx = 0;
		for (Binding binding : allBindings) {
			if (binding.asMissingWhenNotPresent) {
				// one bit represent one field
				binding.mask = 1L << requiredIdx;
				requiredIdx++;
			}
		}
		if (requiredIdx > 63) {
			throw new JsonException("too many required properties to track");
		}
		return requiredIdx;
	}

	/**
	 * 
	 * @param rendered
	 * @param binding
	 * @param marker
	 * @param start
	 * @return
	 */
	private static String quinto(String rendered, Binding binding, int start) {
		String toReturn = "";
		int middle = rendered.indexOf('=', start);
		if (middle == -1) {
			throw new JsonException("can not find = in: " + rendered + " ,at " + start);
		}
		middle += 1;
		int end = rendered.indexOf(';', start);
		if (end == -1) {
			throw new JsonException("can not find ; in: " + rendered + " ,at " + start);
		}
		String op = rendered.substring(middle, end);
		if (binding.field != null) {
			if (binding.valueCanReuse) {
				// reuse; then field set
				toReturn = String.format(QUINTO, rendered.substring(0, start), binding.field.getName(),
						binding.field.getName(), op, rendered.substring(end));
			} else {
				// just field set
				toReturn = String.format("%sobj.%s=%s%s", rendered.substring(0, start), binding.field.getName(), op,
						rendered.substring(end));
			}
		} else {
			// method set
			toReturn = String.format("%sobj.%s(%s)%s", rendered.substring(0, start), binding.method.getName(), op,
					rendered.substring(end));
		}
		return toReturn;
	}

	/**
	 * 
	 * @param rendered
	 * @param binding
	 * @return
	 */
	private static String updateBindingSetOp(String rendered, Binding binding) {
		boolean flag = false;
		String toReturn = rendered;
		if (binding.fromNames.length == 0) {
			flag = false;
		} else {
			flag = true;
		}
		while (flag) {
			String marker = "_" + binding.name + "_";
			int start = toReturn.indexOf(marker);
			if (start == -1) {
				toReturn = rendered;
				flag = false;
				break;
			}
			toReturn = quinto(toReturn, binding, start);
		}
		return toReturn;
	}

	/**
	 * 
	 * @param lines
	 * @param desc
	 */
	private static void appendMissingRequiredProperties(StringBuilder lines, ClassDescriptor desc) {
		append(lines, "java.util.List missingFields = new java.util.ArrayList();");
		for (Binding binding : desc.allDecoderBindings()) {
			if (binding.asMissingWhenNotPresent) {
				long mask = binding.mask;
				append(lines,
						String.format(
								"com.jsoniter.CodegenAccess.addMissingField(missingFields, tracker, %sL, \"%s\");",
								mask, binding.name));
			}
		}
		if (desc.onMissingProperties == null || !desc.ctor.parameters.isEmpty()) {
			append(lines,
					"throw new com.jsoniter.spi.JsonException(\"missing required properties: \" + missingFields);");
		} else {
			if (desc.onMissingProperties.field != null) {
				append(lines, String.format("obj.%s = missingFields;", desc.onMissingProperties.field.getName()));
			} else {
				append(lines, String.format("obj.%s(missingFields);", desc.onMissingProperties.method.getName()));
			}
		}
	}

	/**
	 * 
	 * @param lines
	 * @param desc
	 */
	private static void appendOnUnknownField(StringBuilder lines, ClassDescriptor desc) {
		if (desc.asExtraForUnknownProperties && desc.onExtraProperties == null) {
			append(lines, "throw new com.jsoniter.spi.JsonException('extra property: ' + field.toString());"
					.replace('\'', '"'));
		} else {
			if (desc.asExtraForUnknownProperties || !desc.keyValueTypeWrappers.isEmpty()) {
				append(lines, "if (extra == null) { extra = new java.util.HashMap(); }");
				append(lines, "extra.put(field.toString(), iter.readAny());");
			} else {
				append(lines, "iter.skip();");
			}
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * buildTriTree
	 * @param allBindings
	 * @return
	 */
	private static Map<Integer, Object> buildTriTree(List<Binding> allBindings) {
		Map<Integer, Object> trieTree = new HashMap<Integer, Object>();
		for (Binding field : allBindings) {
			for (String fromName : field.fromNames) {
				byte[] fromNameBytes = fromName.getBytes();
				Map<Byte, Object> current = null;
				if (trieTree.get(fromNameBytes.length) instanceof Map<?, ?>) {
					current = (Map<Byte, Object>) trieTree.get(fromNameBytes.length);
				}
				if (current == null) {
					current = new HashMap<Byte, Object>();
					trieTree.put(fromNameBytes.length, current);
				}
				current = sesto(fromNameBytes, current);
				current.put(fromNameBytes[fromNameBytes.length - 1], field);
			}
		}
		return trieTree;
	}

	@SuppressWarnings("unchecked")
	/**
	 * 
	 * @param fromNameBytes
	 * @param current
	 * @return
	 */
	private static Map<Byte, Object> sesto(byte[] fromNameBytes, Map<Byte, Object> current) {
		Map<Byte, Object> newMap = current;
		for (int i = 0; i < fromNameBytes.length - 1; i++) {
			byte b = fromNameBytes[i];
			Map<Byte, Object> next = null;
			if (newMap.get(b) instanceof Map<?, ?>) {
				next = (Map<Byte, Object>) newMap.get(b);
			}
			if (next == null) {
				next = new HashMap<Byte, Object>();
				newMap.put(b, next);
			}
			newMap = next;
		}
		return newMap;
	}

	@SuppressWarnings("unchecked")
	/**
	 * 
	 * @param trieTree
	 * @return
	 */
	private static String renderTriTree(Map<Integer, Object> trieTree) {
		StringBuilder switchBody = new StringBuilder(SBSIZE);
		for (Map.Entry<Integer, Object> entry : trieTree.entrySet()) {
			Integer len = entry.getKey();
			switchBody.append("case " + len + ": \n");
			Map<Byte, Object> current = null;
			if (entry.getValue() instanceof Map<?, ?>) {
				current = (Map<Byte, Object>) entry.getValue();
			}
			addFieldDispatch(switchBody, len, 0, current, new ArrayList<Byte>());
			switchBody.append("break;");
		}
		return switchBody.toString();
	}

	/**
	 * 
	 * @param entry
	 * @param lines
	 * @return
	 */
	private static void quarto(Map.Entry<Byte, Object> entry, StringBuilder lines) {
		Binding field = null;
		boolean support = false;
		if (entry.getValue() instanceof Binding) {
			field = (Binding) entry.getValue();
		}
		if (field.asExtraWhenPresent) {
			support = true;
			append(lines, String.format(QUARTO.replace('\'', '"'), field.name));
		} else if (field.shouldSkip) {
			support = true;
			append(lines, "iter.skip();");
			append(lines, "continue;");
		} else if (!support) {
			support = true;
			append(lines, String.format("_%s_ = %s;", field.name, CodegenImplNative.genField(field)));
		}
		if (field.asMissingWhenNotPresent && support) {
			append(lines, "tracker = tracker | " + field.mask + "L;");
		}
		if (support) {
			append(lines, "continue;");
		}
	}

	/**
	 * 
	 * @param i
	 * @param len
	 * @param lines
	 * @param bytesToCompare
	 * @param entry
	 * @param b
	 * @return
	 */
	private static void settimo(int i, int len, StringBuilder lines, List<Byte> bytesToCompare,
			Map.Entry<Byte, Object> entry, Byte b) {
		int size = 0;
		if (i == len - 1) {
			append(lines, "if (");
			size = bytesToCompare.size();
			for (int j = 0; j < size; j++) {
				Byte a = bytesToCompare.get(j);
				append(lines, String.format("field.at(%d)==%s && ", i - bytesToCompare.size() + j, a));
			}
			append(lines, String.format("field.at(%d)==%s", i, b));
			append(lines, ") {");
			quarto(entry, lines);
			append(lines, PARENTESICHIUSA);
		}
	}

	/**
	 * 
	 * @param lines
	 * @param i
	 * @param len
	 * @param bytesToCompare
	 * @param b
	 * @param next
	 * @return
	 */
	private static void ottavo(StringBuilder lines, int i, int len, List<Byte> bytesToCompare, Byte b,
			Map<Byte, Object> next){
		append(lines, "if (");
		int size = bytesToCompare.size();
		for (int j = 0; j < size; j++) {
			Byte a = bytesToCompare.get(j);
			append(lines, String.format("field.at(%d)==%s && ", i - bytesToCompare.size() + j, a));
		}
		append(lines, String.format("field.at(%d)==%s", i, b));
		append(lines, ") {");
		addFieldDispatch(lines, len, i + 1, next, new ArrayList<Byte>());
		append(lines, PARENTESICHIUSA);
	}

	@SuppressWarnings("unchecked")
	/**
	 * 
	 * @param lines
	 * @param len
	 * @param i
	 * @param current
	 * @param bytesToCompare
	 */
	private static void addFieldDispatch(StringBuilder lines, int len, int i, Map<Byte, Object> current,
			List<Byte> bytesToCompare) {
		Set<Entry<Byte, Object>> setSize = current.entrySet();
		List<Byte> nextBytesToCompare = null;
		for (Map.Entry<Byte, Object> entry : setSize) {
			Map<Byte, Object> next = null;
			Byte b = entry.getKey();
			settimo(i, len, lines, bytesToCompare, entry, b);
			if (entry.getValue() instanceof Map<?, ?>) {
				next = (Map<Byte, Object>) entry.getValue();
			}
			if (next.size() == 1) {
				nextBytesToCompare = new ArrayList<Byte>(bytesToCompare);
				nextBytesToCompare.add(b);
				addFieldDispatch(lines, len, i + 1, next, nextBytesToCompare);
				continue;
			}
			ottavo(lines, i, len, bytesToCompare, b, next);
		}
	}

	@SuppressWarnings("rawtypes")
	/**
	 * genObjectUsingSkip
	 * @param clazz
	 * @param ctor
	 * @return
	 */
	public static String genObjectUsingSkip(Class clazz, ConstructorDescriptor ctor) {
		StringBuilder lines = new StringBuilder(SBSIZE);
		append(lines, "if (iter.readNull()) { return null; }");
		append(lines, "{{clazz}} obj = {{newInst}};");
		append(lines, "iter.skip();");
		append(lines, "return obj;");
		return lines.toString().replace("{{clazz}}", clazz.getCanonicalName()).replace("{{newInst}}",
				CodegenImplObjectHash.genNewInstCode(clazz, ctor));
	}

	/**
	 * 
	 * @param lines
	 * @param str
	 */
	static void append(StringBuilder lines, String str) {
		lines.append(str);
		lines.append("\n");
	}
}
