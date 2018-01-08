package com.jsoniter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jsoniter.any.Any;
import com.jsoniter.spi.Binding;
import com.jsoniter.spi.ClassDescriptor;
import com.jsoniter.spi.ClassInfo;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.Slice;
import com.jsoniter.spi.WrapperDescriptor;

/**
 * class ReflectionObjectDecoder
 * 
 * @author MaxiBon
 *
 */
class ReflectionObjectDecoder {

	private final static Object NOT_SET = new Object() {
		@Override
		public String toString() {
			return "NOT_SET";
		}
	};
	/**
	 * 
	 */
	private Map<Slice, Binding> allBindings = new HashMap<Slice, Binding>();
	/**
	 * 
	 */
	private String tempCacheKey;
	/**
	 * 
	 */
	private String ctorArgsCacheKey;
	/**
	 * 
	 */
	private int tempCount;
	/**
	 * 
	 */
	private long expectedTracker;
	/**
	 * 
	 */
	private int requiredIdx;
	/**
	 * 
	 */
	private int tempIdx;
	/**
	 * Class Descriptor
	 */
	ClassDescriptor desc;
	/**
	 * String err
	 */
	final String err = "missing required properties: ";

	/**
	 * 
	 * @param classInfo
	 */
	ReflectionObjectDecoder(ClassInfo classInfo) {
		try {
			init(classInfo);
		} catch (JsonException e) {
			throw e;
		} catch (Exception e) {
			String err = "Error: IOException";
			throw new JsonException(err);
		}
	}

	private final void init(ClassInfo classInfo) {
		Class clazz = classInfo.clazz;
		ClassDescriptor descr = ClassDescriptor.getDecodingClassDescriptor(classInfo, true);
		for (Binding param : descr.ctor.parameters) {
			addBinding(classInfo, param);
		}
		this.desc = descr;
		subInit(descr, classInfo);
		if (requiredIdx > 63) {
			throw new JsonException("too many required properties to track");
		}
		expectedTracker = Long.MAX_VALUE >> (63 - requiredIdx);
		if (!descr.ctor.parameters.isEmpty() || !descr.bindingTypeWrappers.isEmpty()) {
			tempCount = tempIdx;
			tempCacheKey = "temp@" + clazz.getCanonicalName();
			ctorArgsCacheKey = "ctor@" + clazz.getCanonicalName();
		}
	}
	
	private void subInit(final ClassDescriptor descr, final ClassInfo classInfo) {
		if (descr.ctor.objectFactory == null && descr.ctor.ctor == null && descr.ctor.staticFactory == null) {
			throw new JsonException("no constructor for: " + descr.clazz);
		}
		for (Binding field : descr.fields) {
			addBinding(classInfo, field);
		}
		for (Binding setter : descr.setters) {
			addBinding(classInfo, setter);
		}
		for (WrapperDescriptor setter : descr.bindingTypeWrappers) {
			for (Binding param : setter.parameters) {
				addBinding(classInfo, param);
			}
		}
	}

	private void addBinding(ClassInfo classInfo, final Binding binding) {
		if (binding.fromNames.length == 0) {
			return;
		}
		subAddBinding(binding);
		binding.idx = tempIdx;
		for (String fromName : binding.fromNames) {
			Slice slice = Slice.make(fromName);
			if (allBindings.containsKey(slice)) {
				throw new JsonException("name conflict found in " + classInfo.clazz + ": " + fromName);
			}
			allBindings.put(slice, binding);
		}
		tempIdx++;
	}
	
	private void subAddBinding(final Binding binding) {
		if (binding.asMissingWhenNotPresent) {
			binding.mask = 1L << requiredIdx;
			requiredIdx++;
		}
		if (binding.asExtraWhenPresent) {
			binding.decoder = new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					throw new JsonException("found should not present property: " + binding.name);
				}
			};
		}
		if (binding.decoder == null) {
			// field decoder might be special customized
			binding.decoder = JsoniterSpi.getDecoder(binding.decoderCacheKey());
		}
		if (binding.decoder == null) {
			binding.decoder = Codegen.getDecoder(binding.valueTypeLiteral.getDecoderCacheKey(), binding.valueType);
		}
	}

	public Decoder create() {
		if (desc.ctor.parameters.isEmpty()) {
			if (desc.bindingTypeWrappers.isEmpty()) {
				return new OnlyField();
			} else {
				return new WithWrapper();
			}
		} else {
			return new WithCtor();
		}
	}

	/**
	 * Public Class OnlyField.
	 * 
	 * @author MaxiBon
	 *
	 */
	public class OnlyField implements Decoder {

		/**
		 * decode
		 * @throws IOException
		 */
		public Object decode(JsonIterator iter) throws IOException {
			 
			try {
				return decode_(iter);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new JsonException("Error: Exception");
			}
		}

		private Object decode_(JsonIterator iter) throws Exception {

			if (iter.readNull()) {
				CodegenAccess.resetExistingObject(iter);
				return null;
			}
			Object obj = CodegenAccess.existingObject(iter) == null ? createNewObject()
					: CodegenAccess.resetExistingObject(iter);
			if (!CodegenAccess.readObjectStart(iter)) {
				if (requiredIdx > 0) {
					if (desc.onMissingProperties == null) {
						throw new JsonException(err + collectMissingFields(0));
					} else {
						setToBinding(obj, desc.onMissingProperties, collectMissingFields(0));
					}
				}
				return obj;
			}
			subDecode1(iter, obj);			
			return obj;
		}
		
		private void subDecode1(JsonIterator iter, Object obj) throws ReflectiveOperationException, IllegalArgumentException, IOException {
			Map<String, Object> extra = null;
			long tracker = 0L;
			Slice fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
			Binding binding = allBindings.get(fieldName);
			ifSupport(tracker, obj, extra, binding, iter, fieldName);
			int intero = CodegenAccess.nextToken(iter);
			while (intero == ',') {
				fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
				binding = allBindings.get(fieldName);
				if (binding == null) {
					extra = onUnknownProperty(iter, fieldName, extra);
				} else {
					if (binding.asMissingWhenNotPresent) {
						tracker |= binding.mask;
					}
					setToBinding(obj, binding, decodeBinding(iter, obj, binding));
				}
				intero = CodegenAccess.nextToken(iter);
			}
			subDecode2(tracker, obj, extra);
		}
		
		private void subDecode2(long tracker, Object obj, Map<String, Object> extra) throws ReflectiveOperationException, IllegalArgumentException {
			if (tracker != expectedTracker) {
				if (desc.onMissingProperties == null) {
					throw new JsonException(err + collectMissingFields(tracker));
				} else {
					setToBinding(obj, desc.onMissingProperties, collectMissingFields(tracker));
				}
			}
			setExtra(obj, extra);
		}
		
		private void ifSupport(long tracker, Object obj, Map<String, Object> extra, Binding binding, JsonIterator iter, Slice fieldName) throws IOException, ReflectiveOperationException, IllegalArgumentException {
			if (binding == null) {
				extra = onUnknownProperty(iter, fieldName, extra);
			} else {
				if (binding.asMissingWhenNotPresent) {
					tracker |= binding.mask;
				}
				setToBinding(obj, binding, decodeBinding(iter, obj, binding));
			}
		}
	}

	/**
	 * Public Class WithCtor.
	 * 
	 * @author MaxiBon
	 *
	 */
	public class WithCtor implements Decoder {

		@Override
		/**
		 * decode
		 * @throws IOException
		 */
		public Object decode(JsonIterator iter) throws IOException {
			 
			try {
				return decode_(iter);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new JsonException("Error: Exception");
			}
		}

		private Object decode_(JsonIterator iter) throws Exception {
			if (iter.readNull()) {
				CodegenAccess.resetExistingObject(iter);
				return null;
			}
			if (iter.tempObjects == null) {
				iter.tempObjects = new HashMap<String, Object>();
			}
			if (iter.tempObjects.get(tempCacheKey) instanceof Object[]) {
				Object[] temp = (Object[]) iter.tempObjects.get(tempCacheKey);
				ifSupport(temp, iter);
				if (!CodegenAccess.readObjectStart(iter)) {
					if (requiredIdx > 0) {
						throw new JsonException(err + collectMissingFields(0));
					}
					return createNewObject(iter, temp);
				}
				Map<String, Object> extra = null;
				subDecode1(extra, iter, temp);
				Object obj = createNewObject(iter, temp);
				subDecode2(obj, extra, temp);
				return obj;
			}
			return null;
		}
		
		private void subDecode1(Map<String, Object> extra, JsonIterator iter, Object[] temp) throws IOException {
			long tracker = 0L;
			Slice fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
			Binding binding = allBindings.get(fieldName);
			ifSupport2(extra, iter, temp, fieldName, binding, tracker);
			int intero = CodegenAccess.nextToken(iter);
			while (intero == ',') {
				fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
				binding = allBindings.get(fieldName);
				if (binding == null) {
					extra = onUnknownProperty(iter, fieldName, extra);
				} else {
					if (binding.asMissingWhenNotPresent) {
						tracker |= binding.mask;
					}
					temp[binding.idx] = decodeBinding(iter, binding);
				}
				intero = CodegenAccess.nextToken(iter);
			}
			if (tracker != expectedTracker) {
				throw new JsonException(err + collectMissingFields(tracker));
			}
		}
		
		private void subDecode2(Object obj, Map<String, Object> extra, Object[] temp) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			setExtra(obj, extra);
			for (Binding field : desc.fields) {
				Object val = temp[field.idx];
				if (!val.equals(NOT_SET) && field.fromNames.length > 0) {
					field.field.set(obj, val);
				}
			}
			for (Binding setter : desc.setters) {
				Object val = temp[setter.idx];
				if (!val.equals(NOT_SET)) {
					setter.method.invoke(obj, val);
				}
			}
			applyWrappers(temp, obj);
		}
		
		private void ifSupport(Object[] temp, JsonIterator iter) {
			if (temp == null) {
				temp = new Object[tempCount];
				iter.tempObjects.put(tempCacheKey, temp);
			}
			Arrays.fill(temp, NOT_SET);
		}
		
		private void ifSupport2(Map<String, Object> extra, JsonIterator iter, Object[] temp, Slice fieldName, Binding binding, long tracker) throws IOException {
			if (binding == null) {
				extra = onUnknownProperty(iter, fieldName, extra);
			} else {
				if (binding.asMissingWhenNotPresent) {
					tracker |= binding.mask;
				}
				temp[binding.idx] = decodeBinding(iter, binding);
			}
		}
	}

	/**
	 * Public Class WithWrapper.
	 * 
	 * @author MaxiBon
	 *
	 */
	public class WithWrapper implements Decoder {

		@Override
		/**
		 * decode
		 * @throws IOException
		 */
		public Object decode(JsonIterator iter) throws IOException {
			 
			try {
				return decode_(iter);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new JsonException("Error: Exception");
			}
		}

		private Object decode_(JsonIterator iter) throws Exception {
			if (iter.readNull()) {
				CodegenAccess.resetExistingObject(iter);
				return null;
			}
			Object obj = createNewObject();
			if (!CodegenAccess.readObjectStart(iter)) {
				ifSupport(obj);
				return obj;
			}
			Map<String, Object> extra = null;
			if (iter.tempObjects == null) {
				iter.tempObjects = new HashMap<String, Object>();
			}
			
			if (!(iter.tempObjects.get(tempCacheKey) instanceof Object[])) {
				return null;
			} else {
				Object[] temp = (Object[]) iter.tempObjects.get(tempCacheKey);
				subDecode1(iter, extra, obj, temp);
				setExtra(obj, extra);
				applyWrappers(temp, obj);
			}
			return obj;
		}
	}
	
	private void subDecode1(JsonIterator iter, Map<String, Object> extra, Object obj, Object[] temp) throws IOException, IllegalArgumentException, ReflectiveOperationException {
		long tracker = 0L;
		if (temp == null) {
			temp = new Object[tempCount];
			iter.tempObjects.put(tempCacheKey, temp);
		}
		Arrays.fill(temp, NOT_SET);
		Slice fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
		Binding binding = allBindings.get(fieldName);
		if (binding == null) {
			extra = onUnknownProperty(iter, fieldName, extra);
		} else {
			if (binding.asMissingWhenNotPresent) {
				tracker |= binding.mask;
			}
			if (canNotSetDirectly(binding)) {
				temp[binding.idx] = decodeBinding(iter, obj, binding);
			} else {
				setToBinding(obj, binding, decodeBinding(iter, obj, binding));
			}
		}
		subDecode2(iter, extra, obj, tracker, temp, binding, fieldName);
	}
	
	private void subDecode2(JsonIterator iter, Map<String, Object> extra, Object obj, long tracker, Object[] temp, Binding binding, Slice fieldName) throws IOException, IllegalArgumentException, ReflectiveOperationException {
		int intero = CodegenAccess.nextToken(iter);
		while (intero == ',') {
			fieldName = CodegenAccess.readObjectFieldAsSlice(iter);
			binding = allBindings.get(fieldName);
			if (binding == null) {
				extra = onUnknownProperty(iter, fieldName, extra);
			} else {
				if (binding.asMissingWhenNotPresent) {
					tracker |= binding.mask;
				}
				if (canNotSetDirectly(binding)) {
					temp[binding.idx] = decodeBinding(iter, obj, binding);
				} else {
					setToBinding(obj, binding, decodeBinding(iter, obj, binding));
				}
			} 
			intero = CodegenAccess.nextToken(iter);
		}
		if (tracker != expectedTracker) {
			if (desc.onMissingProperties == null) {
				throw new JsonException(err + collectMissingFields(tracker));
			} else {
				setToBinding(obj, desc.onMissingProperties, collectMissingFields(tracker));
			}
		}
	}

	private void ifSupport(Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (requiredIdx > 0) {
			if (desc.onMissingProperties == null) {
				throw new JsonException(err + collectMissingFields(0));
			} else {
				setToBinding(obj, desc.onMissingProperties, collectMissingFields(0));
			}
		}
	}
	
	private static void setToBinding(Object obj, Binding binding, Object value)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (binding.field != null) {
			binding.field.set(obj, value);
		} else {
			binding.method.invoke(obj, value);
		}
	}

	private void setExtra(Object obj, Map<String, Object> extra)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (desc.asExtraForUnknownProperties) {
			if (desc.onExtraProperties == null) {
				for (String fieldName : extra.keySet()) {
					throw new JsonException("unknown property: " + fieldName);
				}
			} else {
				setToBinding(obj, desc.onExtraProperties, extra);
			}
		}
		for (Method wrapper : desc.keyValueTypeWrappers) {
			for (Map.Entry<String, Object> entry : extra.entrySet()) {
				if (entry.getValue() instanceof Any) {
					Any value = (Any) entry.getValue();
					wrapper.invoke(obj, entry.getKey(), value.object());
				}
			}
		}
	}

	private static boolean canNotSetDirectly(Binding binding) {
		return binding.field == null && binding.method == null;
	}

	private static Object decodeBinding(JsonIterator iter, Binding binding) throws IOException {
		Object value = binding.decoder.decode(iter);
		return value;
	}

	private Object decodeBinding(JsonIterator iter, Object obj, Binding binding)
			throws IllegalArgumentException, IllegalAccessException, IOException {
		if (binding.valueCanReuse) {
			CodegenAccess.setExistingObject(iter, binding.field.get(obj));
		}
		return decodeBinding(iter, binding);
	}

	private Map<String, Object> onUnknownProperty(JsonIterator iter, Slice fieldName, Map<String, Object> extra)
			throws IOException {
		boolean shouldReadValue = desc.asExtraForUnknownProperties || !desc.keyValueTypeWrappers.isEmpty();
		if (shouldReadValue) {
			Any value = iter.readAny();
			if (extra == null) {
				extra = new HashMap<String, Object>();
			}
			extra.put(fieldName.toString(), value);
		} else {
			iter.skip();
		}
		return extra;
	}

	private List<String> collectMissingFields(long tracker) {
		List<String> missingFields = new ArrayList<String>();
		for (Binding binding : allBindings.values()) {
			if (binding.asMissingWhenNotPresent) {
				long mask = binding.mask;
				CodegenAccess.addMissingField(missingFields, tracker, mask, binding.name);
			}
		}
		return missingFields;
	}

	private void applyWrappers(Object[] temp, Object obj)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		int size = 0;
		Object[] args = null;
		for (WrapperDescriptor wrapper : desc.bindingTypeWrappers) {
			size = wrapper.parameters.size();
			args = new Object[wrapper.parameters.size()];
			for (int i = 0; i < size; i++) {
				Object arg = temp[wrapper.parameters.get(i).idx];
				if (!arg.equals(NOT_SET)) {
					args[i] = arg;
				}
			}
			wrapper.method.invoke(obj, args);
		}
	}

	private Object createNewObject(JsonIterator iter, Object[] temp) throws Exception {
		int size = desc.ctor.parameters.size();
		if (iter.tempObjects == null) {
			iter.tempObjects = new HashMap<String, Object>();
		}
		if (iter.tempObjects.get(ctorArgsCacheKey) instanceof Object[]) {
			Object[] ctorArgs = (Object[]) iter.tempObjects.get(ctorArgsCacheKey);

			if (ctorArgs == null) {
				ctorArgs = new Object[size];
				iter.tempObjects.put(ctorArgsCacheKey, ctorArgs);
			}
			Arrays.fill(ctorArgs, null);
			for (int i = 0; i < size; i++) {
				Object arg = temp[desc.ctor.parameters.get(i).idx];
				if (!arg.equals(NOT_SET)) {
					ctorArgs[i] = arg;
				}
			}
			return createNewObject(ctorArgs);
		} else
			return null;

	}

	private Object createNewObject(Object... args) throws Exception {
		if (desc.ctor.objectFactory != null) {
			return desc.ctor.objectFactory.create(desc.clazz);
		}
		if (desc.ctor.staticFactory != null) {
			return desc.ctor.staticFactory.invoke(null, args);
		} else {
			return desc.ctor.ctor.newInstance(args);
		}
	}
}

