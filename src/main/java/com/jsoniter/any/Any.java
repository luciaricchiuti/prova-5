package com.jsoniter.any;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.jsoniter.ValueType;
import com.jsoniter.output.CodegenAccess;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.TypeLiteral;

/**
 * Abstract Class Any.
 * 
 * @author MaxiBon
 *
 */
public abstract class Any implements Iterable<Any> {
	
	private final static Character wildcard = '*';
	private final static int wildcardHashCode = Character.valueOf(wildcard).hashCode();

	/**
	 * final static Set<String> EMPTY_KEYS
	 */
	protected final static Set<String> EMPTY_KEYS = Collections.unmodifiableSet(new HashSet<String>());

	/**
	 * 
	 */
	protected final static Iterator<Any> EMPTY_ITERATOR = new Iterator<Any>() {
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Any next() {
			throw new NoSuchElementException();
		}
	};

	/**
	 * protected final static EntryIterator EMPTY_ENTRIES_ITERATOR = new
	 * EntryIterator()
	 */
	protected final static EntryIterator EMPTY_ENTRIES_ITERATOR = new EntryIterator() {
		/**
		 * 
		 */
		public boolean next() {
			return false;
		}

		/**
		 * 
		 */
		public String key() {
			throw new NoSuchElementException();
		}

		/**
		 * 
		 */
		public Any value() {
			throw new NoSuchElementException();
		}
	};

	static {
		com.jsoniter.spi.Encoder.ReflectionEncoder anyEncoder = new com.jsoniter.spi.Encoder.ReflectionEncoder() {
			@Override
			public void encode(Object obj, JsonStream stream) throws IOException {
				Any any = null;
				if (obj instanceof Any) {
					any = (Any) obj;
				}
				any.writeTo(stream);
			}

			@Override
			public Any wrap(Object obj) {
				Any any = null;
				try {
					if (obj instanceof Any) {
						any = (Any) obj;
					}
				} catch (Exception e) {
					System.out.print("Error: Exception.");
				} finally {
					System.out.print("");
				}
				return any;

			}
		};
		JsonStream.registerNativeEncoder(Any.class, anyEncoder);
		JsonStream.registerNativeEncoder(TrueAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(FalseAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(ArrayLazyAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(DoubleAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(FloatAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(IntAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(LongAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(NullAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(LongLazyAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(DoubleLazyAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(ObjectLazyAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(StringAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(StringLazyAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(ArrayAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(ObjectAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(ListWrapperAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(ArrayWrapperAny.class, anyEncoder);
		JsonStream.registerNativeEncoder(MapWrapperAny.class, anyEncoder);
	}

	/**
	 * wrapArray.
	 * 
	 * @param val
	 * @return
	 */
	public static Any wrapArray(Object val) {
		return new ArrayWrapperAny(val);
	}

	/**
	 * Interface EntryIterator
	 * 
	 * @author MaxiBon
	 *
	 */
	public interface EntryIterator {
		boolean next();

		String key();

		Any value();
	}

	/**
	 * 
	 * @return
	 */
	public abstract ValueType valueType();

	public <T> T bindTo(T obj, Object... keys_) {
		return get(keys_).bindTo(obj);
	}

	public <T> T bindTo(T obj) {
		T oggetto = null;
		if (object() instanceof Class<?>) {
			oggetto = obj;
		}
		return oggetto;
	}

	public <T> T bindTo(TypeLiteral<T> typeLiteral, T obj, Object... keys_) {
		return get(keys_).bindTo(typeLiteral, obj);
	}

	public <T> T bindTo(TypeLiteral<T> typeLiteral, T obj) {
		T oggetto = null;
		if (object() instanceof TypeLiteral<?>) {
			oggetto = bindTo(typeLiteral, obj);
		}
		return oggetto;
	}

	public Object object(Object... keys_) {
		return get(keys_).object();
	}

	public abstract Object object();

	public Map<String, Any> asMap() {
		Map<String, Any> map = null;
		if (object() instanceof Map<?, ?>) {
			map = (Map<String, Any>) object();
		}

		return map;
	}

	public List<Any> asList() {
		List<Any> list = null;
		if (object() instanceof List<?>) {
			list = (List<Any>) object();
		}

		return list;
	}

	public <T> T as(Class<T> clazz, Object... keys_) {
		return get(keys_).as(clazz);
	}

	public <T> T as(Class<T> clazz) {
		T oggetto = null;
		if (object() instanceof Class<?>) {
			oggetto = (T) clazz;
		}
		return oggetto;
	}

	public <T> T as(TypeLiteral<T> typeLiteral, Object... keys_) {
		return get(keys_).as(typeLiteral);
	}

	public <T> T as(TypeLiteral<T> typeLiteral) {
		T oggetto = null;
		if (object() instanceof TypeLiteral<?>) {
			oggetto = (T) typeLiteral;
		}
		return oggetto;
	}

	public final boolean toBoolean(Object... keys_) {
		return get(keys_).toBoolean();
	}

	public abstract boolean toBoolean();

	public final int toInt(Object... keys_) {
		return get(keys_).toInt();
	}

	public abstract int toInt();

	public final long toLong(Object... keys_) {
		return get(keys_).toLong();
	}

	public abstract long toLong();

	public final float toFloat(Object... keys_) {
		return get(keys_).toFloat();
	}

	public abstract float toFloat();

	public final double toDouble(Object... keys_) {
		return get(keys_).toDouble();
	}

	public abstract double toDouble();

	public final String toString(Object... keys_) {
		return get(keys_).toString();
	}

	public abstract String toString();

	public int size() {
		return 0;
	}

	public Any mustBeValid() {
		if (this instanceof NotFoundAny) {
			throw ((NotFoundAny) this).exception;
		} else {
			return this;
		}
	}

	public Set<String> keys() {
		return EMPTY_KEYS;
	}

	@Override
	public Iterator<Any> iterator() {
		return EMPTY_ITERATOR;
	}

	public EntryIterator entries() {
		return EMPTY_ENTRIES_ITERATOR;
	}

	public Any get(int index) {
		return new NotFoundAny(index, object());
	}

	public Any get(Object keyElement) {
		return new NotFoundAny(keyElement, object());
	}

	public final Any get(Object... keys_) {
		return get(keys_, 0);
	}

	public Any get(Object[] keysArray, int idx) {
		if (idx == keysArray.length) {
			return this;
		}
		return new NotFoundAny(keysArray, idx, object());
	}

	public Any set(int newVal) {
		return wrap(newVal);
	}

	public Any set(long newVal) {
		return wrap(newVal);
	}

	public Any set(float newVal) {
		return wrap(newVal);
	}

	public Any set(double newVal) {
		return wrap(newVal);
	}

	public Any set(String newVal) {
		return wrap(newVal);
	}

	public abstract void writeTo(JsonStream stream) throws IOException;

	protected JsonException reportUnexpectedType(ValueType toType) {
		throw new JsonException(String.format("can not convert %s to %s", valueType(), toType));
	}

	public static Any lazyString(byte[] data, int head, int tail) {
		return new StringLazyAny(data, head, tail);
	}

	public static Any lazyDouble(byte[] data, int head, int tail) {
		return new DoubleLazyAny(data, head, tail);
	}

	public static Any lazyLong(byte[] data, int head, int tail) {
		return new LongLazyAny(data, head, tail);
	}

	public static Any lazyArray(byte[] data, int head, int tail) {
		return new ArrayLazyAny(data, head, tail);
	}

	public static Any lazyObject(byte[] data, int head, int tail) {
		return new ObjectLazyAny(data, head, tail);
	}

	public static Any wrap(int val) {
		return new IntAny(val);
	}

	public static Any wrap(long val) {
		return new LongAny(val);
	}

	public static Any wrap(float val) {
		return new FloatAny(val);
	}

	public static Any wrap(double val) {
		return new DoubleAny(val);
	}

	public static Any wrap(boolean val) {
		if (val) {
			return TrueAny.INSTANCE;
		} else {
			return FalseAny.INSTANCE;
		}
	}

	public static Any wrap(String val) {
		if (val == null) {
			return NullAny.INSTANCE;
		}
		return new StringAny(val);
	}

	public static <T> Any wrap(Collection<T> val) {
		if (val == null) {
			return NullAny.INSTANCE;
		}
		return new ListWrapperAny(new ArrayList(val));
	}

	public static <T> Any wrap(List<T> val) {
		if (val == null) {
			return NullAny.INSTANCE;
		}
		return new ListWrapperAny(val);
	}

	public static <T> Any wrap(Map<String, T> val) {
		if (val == null) {
			return NullAny.INSTANCE;
		}
		return new MapWrapperAny(val);
	}

	public static Any wrap(Object val) {
		return CodegenAccess.wrap(val);
	}

	public static Any wrapNull() {
		return NullAny.INSTANCE;
	}

	public static Any rewrap(List<Any> val) {
		return new ArrayAny(val);
	}

	public static Any rewrap(Map<String, Any> val) {
		return new ObjectAny(val);
	}

	protected boolean isWildcard(Object keyElement) {
		return wildcardHashCode == keyElement.hashCode() && wildcard.equals(keyElement);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		if (o instanceof Any) {
			Any any = (Any) o;
			Object obj = this.object();
			Object thatObj = any.object();
			return obj != null ? obj.equals(thatObj) : thatObj == null;
		}
		return false;

	}

	@Override
	public int hashCode() {
		Object obj = this.object();
		return obj != null ? obj.hashCode() : 0;
	}
}
