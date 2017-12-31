package com.jsoniter.extra;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Since;
import com.google.gson.annotations.Until;
import com.jsoniter.JsonIterator;
import com.jsoniter.SupportBitwise;
import com.jsoniter.ValueType;
import com.jsoniter.annotation.JsonIgnore;
import com.jsoniter.annotation.JsonProperty;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Binding;
import com.jsoniter.spi.ClassDescriptor;
import com.jsoniter.spi.Config;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.Encoder;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.OmitValue;

/**
 * Public Class GsonCompatibilityMode.
 * 
 * @author MaxiBon
 *
 */
public class GsonCompatibilityMode extends Config {

	private final static int SURR1_FIRST = 0xD800;
	private final static int SURR1_LAST = 0xDBFF;
	private final static int SURR2_FIRST = 0xDC00;
	private final static int SURR2_LAST = 0xDFFF;
	private static final String[] REPLACEMENT_CHARS;
	private static final String[] HTML_SAFE_REPLACEMENT_CHARS;

	static {
		REPLACEMENT_CHARS = new String[128];
		for (int i = 0; i <= 0x1f; i++) {
			REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
		}
		REPLACEMENT_CHARS['"'] = "\\\"";
		REPLACEMENT_CHARS['\\'] = "\\\\";
		REPLACEMENT_CHARS['\t'] = "\\t";
		REPLACEMENT_CHARS['\b'] = "\\b";
		REPLACEMENT_CHARS['\n'] = "\\n";
		REPLACEMENT_CHARS['\r'] = "\\r";
		REPLACEMENT_CHARS['\f'] = "\\f";
		HTML_SAFE_REPLACEMENT_CHARS = REPLACEMENT_CHARS.clone();
		HTML_SAFE_REPLACEMENT_CHARS['<'] = "\\u003c";
		HTML_SAFE_REPLACEMENT_CHARS['>'] = "\\u003e";
		HTML_SAFE_REPLACEMENT_CHARS['&'] = "\\u0026";
		HTML_SAFE_REPLACEMENT_CHARS['='] = "\\u003d";
		HTML_SAFE_REPLACEMENT_CHARS['\''] = "\\u0027";
	}

	private GsonCompatibilityMode(String configName, Builder builder) {
		super(configName, builder);
	}

	protected Builder builder() {
		Builder b = null;
		if (super.builder() instanceof Builder) {
			b = (Builder) super.builder();
		}
		return b;
	}

	/**
	 * Public Class Builder.
	 *
	 * @author MaxiBon
	 *
	 */
	public static class Builder extends Config.Builder {
		private boolean excludeFieldsWithoutExposeAnnotation = false;
		private boolean disableHtmlEscaping = false;
		private ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
			@Override
			protected DateFormat initialValue() {
				return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US);
			}
		};
		private FieldNamingStrategy fieldNamingStrategy;
		private Double version;
		private Set<ExclusionStrategy> serializationExclusionStrategies = new HashSet<ExclusionStrategy>();
		private Set<ExclusionStrategy> deserializationExclusionStrategies = new HashSet<ExclusionStrategy>();

		/**
		 * Builder.
		 */
		public Builder() {
			omitDefaultValue(true);
		}

		public Builder excludeFieldsWithoutExposeAnnotation() {
			excludeFieldsWithoutExposeAnnotation = true;
			return this;
		}

		public Builder serializeNulls() {
			omitDefaultValue(false);
			return this;
		}

		public Builder setDateFormat(final int dateStyle, final int timeStyle) {
			dateFormat = new ThreadLocal<DateFormat>() {
				@Override
				protected DateFormat initialValue() {
					return DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.US);
				}
			};
			return this;
		}

		public Builder setDateFormat() {
			/**
			 * Class JdkDatetimeSupport.
			 * 
			 * @author MaxiBon
			 *
			 */
			class JdkDatetimeSupport {
				// 2014-04-01 10:45
				/**
				 * LocalDateTime dateTime
				 */
				LocalDateTime dateTime = LocalDateTime.of(2014, Month.APRIL, 1, 10, 45);
				// format as ISO week date (2014-W08-4)
				/**
				 * 
				 */
				String asIsoWeekDate = dateTime.format(DateTimeFormatter.ISO_WEEK_DATE);
				// using a custom pattern (01/04/2014)
				/**
				 * 
				 */
				String asCustomPattern = dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
				// french date formatting (1. avril 2014)
				/**
				 * 
				 */
				String frenchDate = dateTime.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", new Locale("fr")));
				// using short german date/time formatting (01.04.14 10:45)
				/**
				 * 
				 */
				DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
						.withLocale(new Locale("de"));
				/**
				 * 
				 */
				String germanDateTime = dateTime.format(formatter);
				// parsing date strings
				/**
				 * 
				 */
				LocalDate fromIsoDate = LocalDate.parse("2014-01-20");
				/**
				 * 
				 */
				LocalDate fromIsoWeekDate = LocalDate.parse("2014-W14-2", DateTimeFormatter.ISO_WEEK_DATE);
				/**
				 * 
				 */
				LocalDate fromCustomPattern = LocalDate.parse("20.01.2014", DateTimeFormatter.ofPattern("dd.MM.yyyy"));

			}

			return this;
		}

		public Builder setFieldNamingStrategy(FieldNamingStrategy fieldNameStrategy) {
			this.fieldNamingStrategy = fieldNameStrategy;
			return this;
		}

		public Builder setFieldNamingPolicy(FieldNamingPolicy namingConvention) {
			this.fieldNamingStrategy = namingConvention;
			return this;
		}

		public Builder setPrettyPrinting() {
			indentionStep(2);
			return this;
		}

		public Builder disableHtmlEscaping() {
			disableHtmlEscaping = true;
			return this;
		}

		public Builder setVersion(double versions) {
			this.version = versions;
			return this;
		}

		public Builder setExclusionStrategies(ExclusionStrategy... strategies) {
			for (ExclusionStrategy strategy : strategies) {
				addSerializationExclusionStrategy(strategy);
			}
			return this;
		}

		public Builder addSerializationExclusionStrategy(ExclusionStrategy exclusionStrategy) {
			serializationExclusionStrategies.add(exclusionStrategy);
			return this;
		}

		public Builder addDeserializationExclusionStrategy(ExclusionStrategy exclusionStrategy) {
			deserializationExclusionStrategies.add(exclusionStrategy);
			return this;
		}

		public GsonCompatibilityMode build() {
			escapeUnicode(false);
			GsonCompatibilityMode g = null;
			if (super.build() instanceof GsonCompatibilityMode) {
				g = (GsonCompatibilityMode) super.build();
			}
			return g;
		}

		@Override
		protected Config doBuild(String configName) {
			return new GsonCompatibilityMode(configName, this);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			if (!super.equals(o)) {
				return false;
			}

			Builder builder = null;
			if (o instanceof Builder) {
				builder = (Builder) o;
			}

			if (excludeFieldsWithoutExposeAnnotation != builder.excludeFieldsWithoutExposeAnnotation) {
				return false;
			}

			if (disableHtmlEscaping != builder.disableHtmlEscaping) {
				return false;
			}

			if (!dateFormat.get().equals(builder.dateFormat.get())) {
				return false;
			}

			if (fieldNamingStrategy != null ? !fieldNamingStrategy.equals(builder.fieldNamingStrategy)
					: builder.fieldNamingStrategy != null) {
				return false;
			}

			if (version != null ? !version.equals(builder.version) : builder.version != null) {
				return false;
			}

			if (serializationExclusionStrategies != null
					? !serializationExclusionStrategies.equals(builder.serializationExclusionStrategies)
					: builder.serializationExclusionStrategies != null) {
				return false;
			}

			return deserializationExclusionStrategies != null
					? deserializationExclusionStrategies.equals(builder.deserializationExclusionStrategies)
					: builder.deserializationExclusionStrategies == null;
		}

		@Override
		/**
		 * hashCode.
		 */
		public int hashCode() {
			int result = super.hashCode();
			result = 31 * result + (excludeFieldsWithoutExposeAnnotation ? 1 : 0);
			result = 31 * result + (disableHtmlEscaping ? 1 : 0);
			result = 31 * result + dateFormat.get().hashCode();
			result = 31 * result + (fieldNamingStrategy != null ? fieldNamingStrategy.hashCode() : 0);
			result = 31 * result + (version != null ? version.hashCode() : 0);
			result = 31 * result
					+ (serializationExclusionStrategies != null ? serializationExclusionStrategies.hashCode() : 0);
			result = 31 * result
					+ (deserializationExclusionStrategies != null ? deserializationExclusionStrategies.hashCode() : 0);
			return result;
		}

		@Override
		public Config.Builder copy() {
			Builder copied = null;
			if (super.copy() instanceof Builder) {
				copied = (Builder) super.copy();
			}
			copied.excludeFieldsWithoutExposeAnnotation = excludeFieldsWithoutExposeAnnotation;
			copied.disableHtmlEscaping = disableHtmlEscaping;
			copied.dateFormat = dateFormat;
			copied.fieldNamingStrategy = fieldNamingStrategy;
			copied.version = version;
			copied.serializationExclusionStrategies = new HashSet<ExclusionStrategy>(serializationExclusionStrategies);
			copied.deserializationExclusionStrategies = new HashSet<ExclusionStrategy>(
					deserializationExclusionStrategies);
			return copied;
		}
	}

	@Override
	protected OmitValue createOmitValue(Type valueType) {
		if (valueType instanceof Class) {
			Class clazz = (Class) valueType;
			if (clazz.isPrimitive()) {
				return null; // gson do not omit primitive zero
			}
		}
		return super.createOmitValue(valueType);
	}

	@Override
	public Encoder createEncoder(String cacheKey, Type type) {
		if (Date.class == type) {
			return new Encoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					DateFormat dateFormat = builder().dateFormat.get();
					stream.writeVal(dateFormat.format(obj));
				}
			};
		} else if (String.class == type) {
			final String[] replacements;
			if (builder().disableHtmlEscaping) {
				replacements = REPLACEMENT_CHARS;
			} else {
				replacements = HTML_SAFE_REPLACEMENT_CHARS;
			}
			return new Encoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					String value = null;
					if (obj instanceof String) {
						value = (String) obj;
					}
					stream.write('"');
					int _surrogate = 0;
					int i = 0;
					int n = value.length();
					while (i < n) {
						int c = value.charAt(i);
						String replacement = null;
						if (c < 128) {
							replacement = replacements[c];
							if (replacement == null) {
								stream.write(c);
							} else {
								stream.writeRaw(replacement);
							}
						} else if (c == '\u2028') {
							stream.writeRaw("\\u2028");
						} else if (c == '\u2029') {
							stream.writeRaw("\\u2029");
						} else {
							if (c < 0x800) { // 2-byte
								Integer n1 = Integer
										.valueOf(Integer
												.getInteger(Long.toString(
														SupportBitwise.bitwise(Long.getLong(Integer.toString(0xc0)),
																Long.getLong(Integer.toString(c >> 6)), '|')))
												.intValue());
								Integer n2 = Integer
										.getInteger(Long
												.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(0x80)),
														Long.getLong(Integer.toString(Integer
																.getInteger(
																		Long.toString(SupportBitwise.bitwise(
																				Long.getLong(Integer.toString(c))
																						.longValue(),
																				Long.getLong(Integer.toString(0x3f))
																						.longValue(),
																				'&')))
																.intValue())).longValue(),
														'|')))
										.intValue();
								stream.write(n1.byteValue(), n2.byteValue());
							} else { // 3 or 4 bytes
								// Surrogates?
								if (c < SURR1_FIRST || c > SURR2_LAST) {
									Integer n1 = Integer
											.valueOf(Integer
													.getInteger(Long.toString(
															SupportBitwise.bitwise(Long.getLong(Integer.toString(0xe0)),
																	Long.getLong(Integer.toString(c >> 12)), '|')))
													.intValue());
									Integer n2 = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(
											Integer.toString(0x80)),
											Long
													.getLong(Integer
															.toString(Integer
																	.getInteger(Long.toString(SupportBitwise.bitwise(
																			Long.getLong(Integer.toString(c >> 6))
																					.longValue(),
																			Long.getLong(Integer.toString(0x3f))
																					.longValue(),
																			'&')))
																	.intValue()))
													.longValue(),
											'|'))).intValue();
									Integer n3 = Integer
											.getInteger(
													Long.toString(SupportBitwise.bitwise(Long
															.getLong(Integer.toString(0x80)).longValue(),
															Long
																	.getLong(Integer.toString(Integer
																			.getInteger(Long.toString(
																					SupportBitwise.bitwise(Long
																							.getLong(
																									Integer.toString(c))
																							.longValue(),
																							Long
																									.getLong(
																											Integer.toString(
																													0x3f))
																									.longValue(),
																							'&')))
																			.intValue()))
																	.longValue(),
															'|')))
											.intValue();
									stream.write(n1.byteValue(), n2.byteValue(), n3.byteValue());
									continue;
								}
								// Yup, a surrogate:
								if (c > SURR1_LAST) { // must be from first
									// range
									throw new JsonException("illegalSurrogate");
								}
								_surrogate = c;
								// and if so, followed by another from next
								// range
								if (i >= value.length()) { // unless we hit the
									// end?
									break;
								}
								i++;
								c = value.charAt(i);
								int firstPart = _surrogate;
								_surrogate = 0;
								// Ok, then, is the second part valid?
								if (c < SURR2_FIRST || c > SURR2_LAST) {
									throw new JsonException(
											"Broken surrogate pair: first char 0x" + Integer.toHexString(firstPart)
													+ ", second 0x" + Integer.toHexString(c) + "; illegal combination");
								}
								c = 0x10000 + ((firstPart - SURR1_FIRST) << 10) + (c - SURR2_FIRST);
								if (c > 0x10FFFF) { // illegal in JSON as well
									// as in XML
									throw new JsonException("illegalSurrogate");
								}

								Integer n1 = Integer.valueOf(Integer
										.getInteger(Long.toString(
												SupportBitwise.bitwise(Long.getLong(Integer.toString(0xf0)).longValue(),
														Long.getLong(Integer.toString(c >> 18)).longValue(), '|')))
										.intValue());
								Integer n2 = Integer
										.valueOf(Integer
												.getInteger(Long.toString(SupportBitwise
														.bitwise(Long.getLong(Integer.toString(0x80)).longValue(),
																Long.getLong(Integer.toString(Integer.valueOf(Integer
																		.getInteger(Long.toString(
																				SupportBitwise.bitwise(Long
																						.getLong(Integer
																								.toString(c >> 12))
																						.longValue(),
																						Long
																								.getLong(Integer
																										.toString(0x3f))
																								.longValue(),
																						'&')))
																		.intValue()))).longValue(),
																'|')))
												.intValue());
								Integer n3 = Integer
										.valueOf(
												Integer.getInteger(Long.toString(SupportBitwise.bitwise(
														Long.getLong(Integer.toString(0x80)).longValue(), Long.getLong(
																Integer.toString(Integer.valueOf(Integer
																		.getInteger(Long.toString(
																				SupportBitwise.bitwise(
																						Long.getLong(Integer
																								.toString(c >> 6))
																								.longValue(),
																						Long.getLong(
																								Integer.toString(0x3f))
																								.longValue(),
																						'&')))
																		.intValue()))),
														'|'))));
								Integer n4 = Integer.valueOf(Integer.getInteger(Long.toString(
										SupportBitwise.bitwise(Long.getLong(Integer.toString(0x80)).longValue(),
												Long.getLong(
														Integer.toString(Integer.valueOf(Integer
																.getInteger(Long.toString(SupportBitwise
																		.bitwise(
																				Long.getLong(Integer.toString(c))
																						.longValue(),
																				Long.getLong(Integer.toString(0x3f))
																						.longValue(),
																				'&')))
																.intValue()))),
												'|'))));
								stream.write(n1.byteValue(), n2.byteValue(), n3.byteValue(), n4.byteValue());
							}
						}
						i++;
					}
					stream.write('"');
				}
			};
		}
		return super.createEncoder(cacheKey, type);
	}

	@Override
	public Decoder createDecoder(String cacheKey, Type type) {
		if (Date.class == type) {
			return new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					DateFormat dateFormat = builder().dateFormat.get();
					try {
						String input = iter.readString();
						return dateFormat.parse(input);
					} catch (ParseException e) {
						throw new JsonException("Error: ParseException.");
					}
				}
			};
		} else if (String.class == type) {
			return new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					ValueType valueType = iter.whatIsNext();
					if (valueType == ValueType.STRING) {
						return iter.readString();
					} else if (valueType == ValueType.NUMBER) {
						return iter.readNumberAsString();
					} else if (valueType == ValueType.BOOLEAN) {
						return iter.readBoolean() ? "true" : "false";
					} else if (valueType == ValueType.NULL) {
						iter.skip();
						return null;
					} else {
						throw new JsonException("expect string, but found " + valueType);
					}
				}
			};
		} else if (boolean.class == type) {
			return new Decoder.BooleanDecoder() {
				@Override
				public boolean decodeBoolean(JsonIterator iter) throws IOException {
					ValueType valueType = iter.whatIsNext();
					if (valueType == ValueType.BOOLEAN) {
						return iter.readBoolean();
					} else if (valueType == ValueType.NULL) {
						iter.skip();
						return false;
					} else {
						throw new JsonException("expect boolean, but found " + valueType);
					}
				}
			};
		} else if (long.class == type) {
			return new Decoder.LongDecoder() {
				@Override
				public long decodeLong(JsonIterator iter) throws IOException {
					ValueType valueType = iter.whatIsNext();
					if (valueType == ValueType.NUMBER) {
						return iter.readLong();
					} else if (valueType == ValueType.NULL) {
						iter.skip();
						return 0;
					} else {
						throw new JsonException("expect long, but found " + valueType);
					}
				}
			};
		} else if (int.class == type) {
			return new Decoder.IntDecoder() {
				@Override
				public int decodeInt(JsonIterator iter) throws IOException {
					ValueType valueType = iter.whatIsNext();
					if (valueType == ValueType.NUMBER) {
						return iter.readInt();
					} else if (valueType == ValueType.NULL) {
						iter.skip();
						return 0;
					} else {
						throw new JsonException("expect int, but found " + valueType);
					}
				}
			};
		} else if (float.class == type) {
			return new Decoder.FloatDecoder() {
				@Override
				public float decodeFloat(JsonIterator iter) throws IOException {
					ValueType valueType = iter.whatIsNext();
					if (valueType == ValueType.NUMBER) {
						return iter.readFloat();
					} else if (valueType == ValueType.NULL) {
						iter.skip();
						return 0.0f;
					} else {
						throw new JsonException("expect float, but found " + valueType);
					}
				}
			};
		} else if (double.class == type) {
			return new Decoder.DoubleDecoder() {
				@Override
				public double decodeDouble(JsonIterator iter) throws IOException {
					ValueType valueType = iter.whatIsNext();
					if (valueType == ValueType.NUMBER) {
						return iter.readDouble();
					} else if (valueType == ValueType.NULL) {
						iter.skip();
						return 0.0d;
					} else {
						throw new JsonException("expect float, but found " + valueType);
					}
				}
			};
		}
		return super.createDecoder(cacheKey, type);
	}

	@Override
	public void updateClassDescriptor(ClassDescriptor desc) {
		FieldNamingStrategy fieldNamingStrategy = builder().fieldNamingStrategy;
		for (Binding binding : desc.allBindings()) {
			if (binding.method != null) {
				binding.toNames = newStringArray(0);
				binding.fromNames = newStringArray(0);
			}
			if (fieldNamingStrategy != null && binding.field != null) {
				String translated = fieldNamingStrategy.translateName(binding.field);
				binding.toNames = newStringArray(1);
				binding.toNames[0] = translated;
				binding.fromNames = newStringArray(1);
				binding.fromNames[0] = translated;
			}
			if (builder().version != null) {
				Since since = binding.getAnnotation(Since.class);
				if (since != null && builder().version < since.value()) {
					binding.toNames = newStringArray(0);
					binding.fromNames = newStringArray(0);
				}
				Until until = binding.getAnnotation(Until.class);
				if (until != null && builder().version >= until.value()) {
					binding.toNames = newStringArray(0);
					binding.fromNames = newStringArray(0);
				}
			}
			for (ExclusionStrategy strategy : builder().serializationExclusionStrategies) {
				if (strategy.shouldSkipClass(binding.clazz)) {
					binding.toNames = new String[0];
					continue;
				}
				if (strategy.shouldSkipField(new FieldAttributes(binding.field))) {
					binding.toNames = new String[0];
				}
			}
			for (ExclusionStrategy strategy : builder().deserializationExclusionStrategies) {
				if (strategy.shouldSkipClass(binding.clazz)) {
					binding.fromNames = new String[0];
					continue;
				}
				if (strategy.shouldSkipField(new FieldAttributes(binding.field))) {
					binding.fromNames = new String[0];
				}
			}
		}
		super.updateClassDescriptor(desc);
	}

	@Override
	protected JsonProperty getJsonProperty(Annotation[] annotations) {
		JsonProperty jsoniterObj = super.getJsonProperty(annotations);
		if (jsoniterObj != null) {
			return jsoniterObj;
		}
		final SerializedName gsonObj = getAnnotation(annotations, SerializedName.class);
		if (gsonObj == null) {
			return null;
		}
		return new JsonProperty() {

			@Override
			public String value() {
				return "";
			}

			@Override
			public String[] from() {
				return new String[] { gsonObj.value() };
			}

			@Override
			public String[] to() {
				return new String[] { gsonObj.value() };
			}

			@Override
			public boolean required() {
				return false;
			}

			@Override
			public Class<? extends Decoder> decoder() {
				return Decoder.class;
			}

			@Override
			public Class<?> implementation() {
				return Object.class;
			}

			@Override
			public Class<? extends Encoder> encoder() {
				return Encoder.class;
			}

			@Override
			public boolean nullable() {
				return true;
			}

			@Override
			public boolean collectionValueNullable() {
				return true;
			}

			@Override
			public String defaultValueToOmit() {
				return "";
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return JsonProperty.class;
			}
		};
	}

	@Override
	protected JsonIgnore getJsonIgnore(Annotation[] annotations) {

		JsonIgnore jsoniterObj = super.getJsonIgnore(annotations);
		if (jsoniterObj != null) {
			return jsoniterObj;
		}
		if (builder().excludeFieldsWithoutExposeAnnotation) {
			final Expose gsonObj = getAnnotation(annotations, Expose.class);
			if (gsonObj != null) {
				return new JsonIgnore() {
					@Override
					public boolean ignoreDecoding() {
						return !gsonObj.deserialize();
					}

					@Override
					public boolean ignoreEncoding() {
						return !gsonObj.serialize();
					}

					@Override
					public Class<? extends Annotation> annotationType() {
						return JsonIgnore.class;
					}
				};
			}
			return new JsonIgnore() {
				@Override
				public boolean ignoreDecoding() {
					return true;
				}

				@Override
				public boolean ignoreEncoding() {
					return true;
				}

				@Override
				public Class<? extends Annotation> annotationType() {
					return JsonIgnore.class;
				}
			};
		}
		return null;
	}

	// CREATA DA ENRICO
	String[] newStringArray(int n) {
		return new String[n];
	}
}