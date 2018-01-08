package com.jsoniter.extra;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
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
import com.jsoniter.spi.Config;
import com.jsoniter.spi.JsonException;

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
	private static final int ZERO = 0;
	private static final int ZERO1 = 0;
	private static final int ZERO2 = 0;
	private static final int ZERO3 = 0;
	private static final int ZERO4 = 0;

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
				return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, java.util.Locale.US);
			}
		};
		private FieldNamingStrategy fieldNamingStrategy;
		private Double version;
		private Set<ExclusionStrategy> serializationExclusionStrategies = new java.util.HashSet<ExclusionStrategy>();
		private Set<ExclusionStrategy> deserializationExclusionStrategies = new java.util.HashSet<ExclusionStrategy>();

		/**
		 * Builder.
		 */
		public Builder() {
			omitDefaultValue(true);
		}

		public Builder setExcludeFieldsWithoutExposeAnnotation() {
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
					return DateFormat.getDateTimeInstance(dateStyle, timeStyle, java.util.Locale.US);
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
				int y = 2014;
				int d = 1;
				int h = 10;
				int m = 45;
				LocalDateTime dateTime = LocalDateTime.of(y, java.time.Month.APRIL, d, h, m);
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
				String frenchDate = dateTime.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", new java.util.Locale("fr")));
				// using short german date/time formatting (01.04.14 10:45)
				/**
				 * 
				 */
				DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
						.withLocale(new java.util.Locale("de"));
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
			int n = 2;
			indentionStep(n);
			return this;
		}

		public Builder setDisableHtmlEscaping() {
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

		/**
		 * 
		 * @param b1
		 * @param b2
		 * @param b3
		 * @param flag
		 * @return
		 */
		protected boolean equalSupp2(boolean b1, boolean b2, boolean b3, boolean flag) {
			return b1 ? false : b2 ? false : b3 ? false : flag;
		}

		/**
		 * 
		 * @param bull
		 * @param flag
		 * @return
		 */
		private boolean equalSupp(Builder bull, boolean flag) {
			Builder builder = bull;
			boolean b1 = (excludeFieldsWithoutExposeAnnotation != builder.excludeFieldsWithoutExposeAnnotation);
			boolean b2 = (disableHtmlEscaping != builder.disableHtmlEscaping);
			boolean b3 = (!dateFormat.get().equals(builder.dateFormat.get()));

			boolean flag2 = equalSupp2(b1, b2, b3, flag);

			if (fieldNamingStrategy != null ? !fieldNamingStrategy.equals(builder.fieldNamingStrategy)
					: builder.fieldNamingStrategy != null) {
				flag2 = false;
			} else if (version != null ? (version.equals(builder.version) == false) : builder.version != null) {
				flag2 = false;
			} else if (serializationExclusionStrategies != null
					? !serializationExclusionStrategies.equals(builder.serializationExclusionStrategies)
					: builder.serializationExclusionStrategies != null) {
				flag2 = false;
			}
			builder.toString();
			return flag2;
		}

		@Override
		public boolean equals(Object o) {
			boolean flag = false;
			boolean supp = false;
			Builder builder = null;
			if (this == o) {
				flag = true;
				supp = true;
			} else if (o == null || getClass() != o.getClass()) {
				flag = false;
				supp = true;
			} else if (super.equals(o) == false) {
				flag = false;
				supp = true;
			} else if (o instanceof Builder) {
				builder = (Builder) o;
				supp = true;
			}

			if (supp == false) {
				flag = equalSupp(builder, flag);
			} else {
				flag = deserializationExclusionStrategies != null
						? deserializationExclusionStrategies.equals(builder.deserializationExclusionStrategies)
						: builder.deserializationExclusionStrategies == null;
			}
			return flag;
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
			copied.serializationExclusionStrategies = new java.util.HashSet<ExclusionStrategy>(serializationExclusionStrategies);
			copied.deserializationExclusionStrategies = new java.util.HashSet<ExclusionStrategy>(
					deserializationExclusionStrategies);
			return copied;
		}
	}

	@Override
	protected com.jsoniter.spi.OmitValue createOmitValue(Type valueType) {
		if (valueType instanceof Class) {
			Class clazz = (Class) valueType;
			if (clazz.isPrimitive()) {
				return null; // gson do not omit primitive zero
			}
		}
		return super.createOmitValue(valueType);
	}

	@Override
	public com.jsoniter.spi.Encoder createEncoder(String cacheKey, Type type) {
		final int[] v = { 0xc0, 6, 0x80, 0x3f, 0xe0, 12, 0xf0, 18 };
		if (Date.class == type) {
			return new com.jsoniter.spi.Encoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					DateFormat dateFormat = builder().dateFormat.get();
					stream.writeVal(dateFormat.format(obj));
				}
			};
		} else if (String.class == type) {
			final String[] replacements = encodeSupp6();
			return new com.jsoniter.spi.Encoder() {
				@Override
				public void encode(Object obj, JsonStream stream) throws IOException {
					String value = encodeSupp7(obj);
					stream.write('"');
					int n = value.length();
					for (int i = 0; i < n;i++) {
						int c = value.charAt(i);
						c = encodeSupp5(c, stream, replacements, i, value, v);
					}
					stream.write('"');
				}
			};
		}
		return super.createEncoder(cacheKey, type);
	}

	/**
	 * 
	 * @param c
	 * @param firstPart
	 * @param stream
	 * @param v
	 * @return
	 * @throws IOException
	 */
	private int encodeSupp(int c, int firstPart, JsonStream stream, int[] v) throws IOException {
		int ret = c;
		if (ret < SURR2_FIRST || ret > SURR2_LAST) {
			throw new JsonException("Broken surrogate pair: first char 0x" + Integer.toHexString(firstPart)
					+ ", second 0x" + Integer.toHexString(c) + "; illegal combination");
		}
		ret = 0x10000 + ((firstPart - SURR1_FIRST) << 10) + (c - SURR2_FIRST);
		if (ret > 0x10FFFF) {
			throw new JsonException("illegalSurrogate");
		}
		Integer n1 = Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[6])).longValue(),Long.getLong(Integer.toString(c >> v[7])).longValue(), '|'))).intValue());
		Integer n2 = Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[2])).longValue(),Long.getLong(Integer.toString(Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(c >> v[5])).longValue(),Long.getLong(Integer.toString(v[3])).longValue(),'&'))).intValue()))).longValue(),'|'))).intValue());
		Integer n3 = Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[2])).longValue(),Long.getLong(Integer.toString(Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(c >> v[1])).longValue(),Long.getLong(Integer.toString(v[3])).longValue(),'&'))).intValue()))),'|'))));
		Integer n4 = Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[2])).longValue(),Long.getLong(Integer.toString(Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(c)).longValue(),Long.getLong(Integer.toString(v[3])).longValue(),'&'))).intValue()))),'|'))));
		stream.write(n1.byteValue(), n2.byteValue(), n3.byteValue(), n4.byteValue());
		return ret;
	}
	
	/**
	 * 
	 * @param stream
	 * @param c
	 * @param v
	 * @throws IOException
	 */
	private void encodeSupp2(JsonStream stream, int c, int[] v) throws IOException {
		Integer n1 = Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[0])),Long.getLong(Integer.toString(c >> v[1])), '|'))).intValue());
		Integer n2 = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[2])),Long.getLong(Integer.toString(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(c)).longValue(),Long.getLong(Integer.toString(v[3])).longValue(),'&'))).intValue())).longValue(),'|'))).intValue();
		stream.write(n1.byteValue(), n2.byteValue());
	}

	/**
	 * 
	 * @param c
	 * @param stream
	 * @param v
	 * @return
	 * @throws IOException
	 */
	private int encodeSupp3(int c, JsonStream stream, int[] v) throws IOException {
		int ret = c;
		ret = gsonSupport(stream, ret, v);
		if (c > SURR1_LAST) {
			throw new JsonException("illegalSurrogate");
		}
		return ret;
	}

	/**
	 * 
	 * @param stream
	 * @param c
	 * @param replacements
	 * @throws IOException
	 */
	private void encodeSupp4(JsonStream stream, int c, String[] replacements) throws IOException {
		String ret = replacements[c];
		if (ret == null) {
			stream.write(c);
		} else {
			stream.writeRaw(ret);
		}
	}

	/**
	 * 
	 * @param c
	 * @param stream
	 * @param replacements
	 * @param i
	 * @param stringa
	 * @param v
	 * @return
	 * @throws IOException
	 */
	private int encodeSupp5(int c, JsonStream stream, String[] replacements, int i, String stringa, int[] v)
			throws IOException {
		int ret = c;
		int index = i;
		switch (ret) {
		case 128:
			encodeSupp4(stream, ret, replacements);
			break;
		case '\u2028':
			stream.writeRaw("\\u2028");
			break;
		case '\u2029':
			stream.writeRaw("\\u2029");
			break;
		default:
			if (ret < 0x800) {
				encodeSupp2(stream, ret, v);
			} else {
				ret = encodeSupp3(ret, stream, v);
				if (index >= stringa.length()) {
					break;
				}
				index++;
				ret = encodeSupp(stringa.charAt(index), ret, stream, v);
			}
		}
		return ret;
	}

	/**
	 * 
	 * @return
	 */
	private String[] encodeSupp6() {
		String[] ret = {""};
		if (builder().disableHtmlEscaping) {
			ret = REPLACEMENT_CHARS;
		} else {
			ret = HTML_SAFE_REPLACEMENT_CHARS;
		}
		return ret;
	}

	/**
	 * 
	 * @param obj
	 * @return
	 */
	private String encodeSupp7(Object obj) {
		String value = null;
		if (obj instanceof String) {
			value = (String) obj;
		}
		return value;
	}

	public int gsonSupport(JsonStream stream, int c, final int[] v) throws IOException {
		if (c < SURR1_FIRST || c > SURR2_LAST) {
			Integer n1 = Integer.valueOf(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[4])),Long.getLong(Integer.toString(c >> v[5])), '|'))).intValue());
			Integer n2 = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[2])), Long.getLong(Integer.toString(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(c >> v[1])).longValue(),Long.getLong(Integer.toString(v[3])).longValue(),'&'))).intValue())).longValue(),'|'))).intValue();
			Integer n3 = Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(v[2])).longValue(),Long.getLong(Integer.toString(Integer.getInteger(Long.toString(SupportBitwise.bitwise(Long.getLong(Integer.toString(c)).longValue(),Long.getLong(Integer.toString(v[3])).longValue(),'&'))).intValue())).longValue(),'|'))).intValue();
			stream.write(n1.byteValue(), n2.byteValue(), n3.byteValue());
		}
		return c;
	}

	public com.jsoniter.spi.Decoder createDecoder(String cacheKey, Type type) {
		if (Date.class == type) {
			return newDecDate();
		} else if (String.class == type) {
			return newDecString();
		} else if (boolean.class == type) {
			return newDecBool();
		} else if (long.class == type) {
			return newDecLong();
		} else if (int.class == type) {
			return newDecInt();
		} else if (float.class == type) {
			return newDecFloat();
		} else if (double.class == type) {
			return newDecDouble();
		}
		return super.createDecoder(cacheKey, type);
	}

	/**
	 * MaxiBon
	 * @throws IOException
	 * @return
	 */
	private com.jsoniter.spi.Decoder newDecDate() {
		return new com.jsoniter.spi.Decoder() {
			/**
			 * @throws IOException
			 */
			public Date decode(JsonIterator iter) throws IOException {
				DateFormat dateFormat = builder().dateFormat.get();
				try {
					String input = iter.readString();
					return dateFormat.parse(input);
				} catch (java.text.ParseException e) {
					throw new JsonException("Error: ParseException.");
				}
			}
		};
	}

	/**
	 * MaxiBon
	 * @throws IOException
	 * @return
	 */
	private com.jsoniter.spi.Decoder newDecString() {
		return new com.jsoniter.spi.Decoder() {
			/**
			 * @throws IOException
			 */
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
	}

	/**
	 * MaxiBon
	 * @throws IOException
	 * @return
	 */
	private com.jsoniter.spi.Decoder newDecBool() {
		return new com.jsoniter.spi.Decoder.BooleanDecoder() {
			/**
			 * @throws IOException
			 */
			public boolean decodeBoolean(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				switch (valueType) {
				case BOOLEAN:
					return iter.readBoolean();
				case NULL:
					iter.skip();
					return false;
				default:
					throw new JsonException("expect boolean, but found " + valueType);
				}
			}
		};
	}

	/**
	 * MaxiBon
	 * @throws IOException
	 * @return
	 */
	private com.jsoniter.spi.Decoder newDecLong() {
		return new com.jsoniter.spi.Decoder.LongDecoder() {
			/**
			 * @throws IOException
			 */
			public long decodeLong(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				switch (valueType) {
				case NUMBER:
					return iter.readLong();
				case NULL:
					iter.skip();
					return 0;
				default:
					throw new JsonException("expect long, but found " + valueType);
				}
			}
		};
	}

	/**
	 * MaxiBon
	 * @throws IOException
	 * @return
	 */
	private com.jsoniter.spi.Decoder newDecInt() {
		return new com.jsoniter.spi.Decoder.IntDecoder() {
			/**
			 * @throws IOException
			 */
			public int decodeInt(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				switch (valueType) {
				case NUMBER:
					return iter.readInt();
				case NULL:
					iter.skip();
					return 0;
				default:
					throw new JsonException("expect int, but found " + valueType);
				}
			}
		};
	}

	/**
	 * MaxiBon
	 * @throws IOException
	 * @return
	 */
	private com.jsoniter.spi.Decoder newDecFloat() {
		return new com.jsoniter.spi.Decoder.FloatDecoder() {
			/**
			 * @throws IOException
			 */
			public float decodeFloat(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				switch (valueType) {
				case NUMBER:
					return iter.readFloat();
				case NULL:
					iter.skip();
					final float n = 0.0f;
					return n;
				default:
					throw new JsonException("expect float, but found " + valueType);
				}
			}
		};
	}

	/**
	 * MaxiBon
	 * @throws IOException
	 * @return
	 */
	private com.jsoniter.spi.Decoder newDecDouble() {
		return new com.jsoniter.spi.Decoder.DoubleDecoder() {
			/**
			 * @throws IOException
			 */
			public double decodeDouble(JsonIterator iter) throws IOException {
				ValueType valueType = iter.whatIsNext();
				switch (valueType) {
				case NUMBER:
					return iter.readDouble();
				case NULL:
					iter.skip();
					final double n = 0.0d;
					return n;
				default:
					throw new JsonException("expect float, but found " + valueType);
				}
			}
		};
	}

	public com.jsoniter.spi.Binding updateClassDescriptorSupp3(com.jsoniter.spi.Binding binding, String translated) {
		com.jsoniter.spi.Binding bin = binding;
		final int one = 1;
		bin.toNames = newStringArray(1);
		bin.toNames[ZERO] = translated;
		bin.fromNames = newStringArray(one);
		bin.fromNames[ZERO] = translated;
		return bin;
	}
	
	private boolean ciclomatic(boolean b1, boolean b2){
		return b1 && b2;
	}

	public com.jsoniter.spi.Binding updateClassDescriptorSupp2(com.jsoniter.spi.Binding b, FieldNamingStrategy fNS) {
		FieldNamingStrategy f = fNS;
		com.jsoniter.spi.Binding bin = b;
		if (bin.method != null) {
			bin.toNames = newStringArray(ZERO);
			bin.fromNames = newStringArray(ZERO1);
		}
		boolean ciclomatic = ciclomatic(f != null, bin.field != null);
		if (ciclomatic) {
			String translated = f.translateName(bin.field);
			com.jsoniter.spi.Binding bind = updateClassDescriptorSupp3(bin, translated);
			bin = bind;
		}
		if (builder().version != null) {
			Since since = bin.getAnnotation(Since.class);
			if (since != null && builder().version < since.value()) {
				bin.toNames = newStringArray(ZERO2);
				bin.fromNames = newStringArray(ZERO3);
			}
			Until until = bin.getAnnotation(Until.class);
			if (until != null && builder().version >= until.value()) {
				bin.toNames = newStringArray(ZERO4);
				bin.fromNames = newStringArray(0);
			}
		}
		return bin;
	}

	public com.jsoniter.spi.Binding updateClassDescriptorSupp1(com.jsoniter.spi.Binding binding) {
		com.jsoniter.spi.Binding bin = binding;
		for (ExclusionStrategy strategy : builder().deserializationExclusionStrategies) {
			if (strategy.shouldSkipClass(bin.clazz)) {
				bin.fromNames = new String[0];
				continue;
			}
			if (strategy.shouldSkipField(new FieldAttributes(bin.field))) {
				bin.fromNames = new String[0];
			}
		}
		return bin;
	}

	@Override
	public void updateClassDescriptor(com.jsoniter.spi.ClassDescriptor desc) {
		FieldNamingStrategy fieldNamingStrategy = builder().fieldNamingStrategy;
		for (com.jsoniter.spi.Binding binding : desc.allBindings()) {
			com.jsoniter.spi.Binding bin = updateClassDescriptorSupp2(binding, fieldNamingStrategy);
			binding = bin;
			for (ExclusionStrategy strategy : builder().serializationExclusionStrategies) {
				if (strategy.shouldSkipClass(binding.clazz)) {
					binding.toNames = new String[0];
					continue;
				}
				if (strategy.shouldSkipField(new FieldAttributes(binding.field))) {
					binding.toNames = new String[0];
				}
			}
			com.jsoniter.spi.Binding bin1 = updateClassDescriptorSupp1(binding);
			binding = bin1;
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
			public Class<? extends com.jsoniter.spi.Decoder> decoder() {
				return com.jsoniter.spi.Decoder.class;
			}

			@Override
			public Class<?> implementation() {
				return Object.class;
			}

			@Override
			public Class<? extends com.jsoniter.spi.Encoder> encoder() {
				return com.jsoniter.spi.Encoder.class;
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
