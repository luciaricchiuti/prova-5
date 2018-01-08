package com.jsoniter.output;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jsoniter.spi.ClassInfo;
import com.jsoniter.spi.Encoder;
import com.jsoniter.spi.Extension;
import com.jsoniter.spi.GenericsHelper;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;
import com.jsoniter.spi.TypeLiteral;

/**
 * class Codegen
 * 
 * @author MaxiBon
 *
 */
class Codegen {

	private Codegen() {
	}

	/**
	 * static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen
	 */
	static CodegenAccess.StaticCodegenTarget isDoingStaticCodegen = new CodegenAccess.StaticCodegenTarget("");
	// only read/write when generating code with synchronized protection
	private final static Map<String, CodegenResult> generatedSources = new HashMap<String, CodegenResult>();

	/**
	 * getReflectionEncoder
	 * 
	 * @param cacheKey
	 * @param type
	 * @return
	 */
	public static Encoder.ReflectionEncoder getReflectionEncoder(String cacheKey, Type type) {
		Map<String, Encoder.ReflectionEncoder> reflectionEncoders = new HashMap<String, Encoder.ReflectionEncoder>();
		Encoder.ReflectionEncoder encoder = CodegenImplNative.NATIVE_ENCODERS.get(type);
		if (encoder != null) {
			return encoder;
		}
		encoder = reflectionEncoders.get(cacheKey);
		if (encoder != null) {
			return encoder;
		}
		synchronized (Codegen.class) {
			encoder = reflectionEncoders.get(cacheKey);
			if (encoder != null) {
				return encoder;
			}
			ClassInfo classInfo = new ClassInfo(type);
			encoder = ReflectionEncoderFactory.create(classInfo);
			HashMap<String, Encoder.ReflectionEncoder> copy = new HashMap<String, Encoder.ReflectionEncoder>(
					reflectionEncoders);
			copy.put(cacheKey, encoder);
			reflectionEncoders = copy;
			return encoder;
		}
	}

	public static Encoder getEncoder(String cacheKey, Type type) {
		Encoder encoder = JsoniterSpi.getEncoder(cacheKey);
		if (encoder != null) {
			return encoder;
		}
		return gen(cacheKey, type);
	}

	/**
	 * primo metodo di supporto per errore "Follow the limit for number of
	 * statements in a method"
	 * 
	 * @param classInfo
	 */
	private static void primo(ClassInfo classInfo) {
		if (Map.class.isAssignableFrom(classInfo.clazz) && classInfo.typeArgs.length > 1) {
			DefaultMapKeyEncoder.registerOrGetExisting(classInfo.typeArgs[0]);
		}
	}

	/**
	 * secondo metodo di supporto per errore "Follow the limit for number of
	 * statements in a method"
	 * 
	 * @param encoder
	 * @param cacheKey
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	private static Encoder secondo(Encoder encoder, final String cacheKey)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		Encoder e = encoder;
		if (Class.forName(cacheKey).newInstance() instanceof Encoder) {
			e = (Encoder) Class.forName(cacheKey).newInstance();
		}
		return e;
	}

	/**
	 * terzo metodo di supporto per errore "Follow the limit for number of
	 * statements in a method"
	 * 
	 * @param encoder
	 * @param cacheKey
	 * @param classInfo
	 * @param source
	 * @return
	 */
	private static Encoder terzo(Encoder encoder, final String cacheKey, ClassInfo classInfo, CodegenResult source) {
		Encoder e = encoder;
		try {
			generatedSources.put(cacheKey, source);
			if (isDoingStaticCodegen.outputDir == "") {
				e = DynamicCodegen.gen(classInfo.clazz, cacheKey, source);
			} else {
				staticGen(classInfo.clazz, cacheKey, source);
			}
		} catch (Exception excpt) {
			System.out.print("");
		} finally {
			System.out.print("");
		}
		return e;
	}

	private static void quarto(EncodingMode mode) {
		if (mode == EncodingMode.STATIC_MODE) {
			throw new JsonException();
		}
	}

	private static Encoder quinto(EncodingMode mode, ClassInfo classInfo, Encoder encoder, final String cacheKey) {
		Encoder e = encoder;
		primo(classInfo);
		if (mode == EncodingMode.REFLECTION_MODE) {
			return ReflectionEncoderFactory.create(classInfo);
		}
		if (isDoingStaticCodegen.outputDir == "") {
			try {
				return secondo(e, cacheKey);
			} catch (Exception exc) {
				quarto(mode);
			}
		}
		CodegenResult source = genSource(cacheKey, classInfo);
		try {
			return terzo(e, cacheKey, classInfo, source);
		} catch (Exception exc) {
			throw new JsonException();
		}
	}

	private static Encoder gen(final String cacheKey, Type type) {
		synchronized (gen(cacheKey, type)) {
			Encoder encoder = (JsoniterSpi.getEncoder(cacheKey) != null) ? JsoniterSpi.getEncoder(cacheKey) : null;
			List<Extension> extensions = JsoniterSpi.getExtensions();
			for (Extension extension : extensions) {
				if (extension.createEncoder(cacheKey, type) != null) {
					JsoniterSpi.addNewEncoder(cacheKey, extension.createEncoder(cacheKey, type));
					encoder = extension.createEncoder(cacheKey, type);
				}
			}
			if (CodegenImplNative.NATIVE_ENCODERS.get(type) != null) {
				JsoniterSpi.addNewEncoder(cacheKey, CodegenImplNative.NATIVE_ENCODERS.get(type));
				encoder = CodegenImplNative.NATIVE_ENCODERS.get(type);
			}
			addPlaceholderEncoderToSupportRecursiveStructure(cacheKey);
			if (JsoniterSpi.getCurrentConfig().encodingMode() != EncodingMode.REFLECTION_MODE) {
				if (Object.class == chooseAccessibleSuper(type)) {
					throw new JsonException("dynamic code can not serialize private class: " + type);
				}
			}
			encoder = quinto(JsoniterSpi.getCurrentConfig().encodingMode(), new ClassInfo(chooseAccessibleSuper(type)),
					encoder, cacheKey);
			JsoniterSpi.addNewEncoder(cacheKey, encoder);
			return encoder;
		}
	}

	private static void addPlaceholderEncoderToSupportRecursiveStructure(final String cacheKey) {
		JsoniterSpi.addNewEncoder(cacheKey, new Encoder() {
			@Override
			public void encode(Object obj, JsonStream stream) throws IOException {
				Encoder encoder = JsoniterSpi.getEncoder(cacheKey);
				try {
					if (this == encoder) {
						for (int i = 0; i < 30; i++) {
							encoder = JsoniterSpi.getEncoder(cacheKey);

							if (this == encoder) {
								int n = 1000;
								Thread.sleep(n);
							} else {
								break;
							}
						}
						if (this == encoder) {
							throw new JsonException("internal error: placeholder is not replaced with real encoder");
						}
					}
				} catch (InterruptedException e) {
					throw new JsonException();
				}
				encoder.encode(obj, stream);
			}
		});
	}

	private static Type chooseAccessibleSuper(Type type) {
		Type[] typeArgs = new Type[0];
		Class clazz = null;
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			clazz = (Class) pType.getRawType();
			typeArgs = pType.getActualTypeArguments();
		} else {
			if (type instanceof Class) {
				clazz = (Class) type;
			}
		}
		if (Modifier.isPublic(clazz.getModifiers())) {
			return type;
		}
		clazz = walkSuperUntilPublic(clazz.getSuperclass());
		if (typeArgs.length == 0) {
			return clazz;
		} else {
			return GenericsHelper.createParameterizedType(typeArgs, null, clazz);
		}
	}

	private static Class walkSuperUntilPublic(Class clazz) {
		if (Modifier.isPublic(clazz.getModifiers())) {
			return clazz;
		}
		return walkSuperUntilPublic(clazz.getSuperclass());
	}

	public static CodegenResult getGeneratedSource(String cacheKey) {
		return generatedSources.get(cacheKey);
	}

	private static void staticGen(Class clazz, String cacheKey, CodegenResult source) throws IOException {
		createDir(cacheKey);
		String fileName = cacheKey.replace('.', '/') + ".java";
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(new File(isDoingStaticCodegen.outputDir, fileName));
			OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
			try {
				staticGen(clazz, cacheKey, writer, source);
			} finally {
				writer.close();
			}
		} finally {
			fileOutputStream.close();
		}
	}

	private static void staticGen(Class clazz, String cacheKey, OutputStreamWriter writer, CodegenResult source)
			throws IOException {
		String className = cacheKey.substring(cacheKey.lastIndexOf('.') + 1);
		String packageName = cacheKey.substring(0, cacheKey.lastIndexOf('.'));
		writer.write("package " + packageName + ";\n");
		writer.write("public class " + className + " implements com.jsoniter.spi.Encoder {\n");
		writer.write(source.generateWrapperCode(clazz));
		writer.write(source.toString());
		writer.write("}\n");
	}

	private static void createDir(String cacheKey) {
		String[] parts = cacheKey.split("\\.");
		File parent = new File(isDoingStaticCodegen.outputDir);
		File current = null;
		for (int i = 0; i < parts.length - 1; i++) {
			String part = parts[i];
			current = new File(parent, part);
			current.mkdir();
			parent = current;
		}
	}

	private static CodegenResult genSource(String cacheKey, ClassInfo classInfo) {
		Class clazz = classInfo.clazz;
		if (clazz.isArray()) {
			return CodegenImplArray.genArray(cacheKey, classInfo);
		}
		if (Map.class.isAssignableFrom(clazz)) {
			return CodegenImplMap.genMap(cacheKey, classInfo);
		}
		if (Collection.class.isAssignableFrom(clazz)) {
			return CodegenImplArray.genCollection(cacheKey, classInfo);
		}
		if (clazz.isEnum()) {
			return CodegenImplNative.genEnum(clazz);
		}
		return CodegenImplObject.genObject(classInfo);
	}

	public static void staticGenEncoders(TypeLiteral[] typeLiterals,
			CodegenAccess.StaticCodegenTarget staticCodegenTarget) {
		isDoingStaticCodegen = staticCodegenTarget;
		for (TypeLiteral typeLiteral : typeLiterals) {
			gen(typeLiteral.getEncoderCacheKey(), typeLiteral.getType());
		}
	}
}
