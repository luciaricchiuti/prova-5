package com.jsoniter.extra;

import com.jsoniter.spi.Binding;
import com.jsoniter.spi.ClassDescriptor;
import com.jsoniter.spi.EmptyExtension;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;

/**
 * Public Class NamingStrategy.
 * 
 * @author MaxiBon
 *
 */
public class NamingStrategySupport {
	/**
	 * NamingStrategySupport
	 */
	private NamingStrategySupport() {
	}

	/**
	 * Public Interface NamingStrategy.
	 * 
	 * @author MaxiBon
	 *
	 */
	public interface NamingStrategy {
		/**
		 * String translate(String input);
		 * 
		 * @author MaxiBon
		 *
		 */

		String translate(String input);
	}

	
	/**
	 * public NamingStrategy KEBAB_CASE = new NamingStrategy()
	 * 
	 * @author MaxiBon
	 *
	 */
	public NamingStrategy kEBAB_CASE = new NamingStrategy() {
		@Override
		public String translate(String string) {
			if (string == null) {
				return string;
			}

			int length = string.length();
			if (length == 0) {
				return string;
			}

			return translateSuppKebab(string, length);

		}
	};
	
	

	/**
	 * public static NamingStrategy SNAKE_CASE = new NamingStrategy()
	 * 
	 * @author MaxiBon
	 *
	 */
	public static NamingStrategy SNAKE_CASE = new NamingStrategy() {
		@Override
		public String translate(String inp) {
			if (inp == null) {
				return inp; // garbage in, garbage out
			}
			return translateSuppSnake(inp);

		}
	};

	/**
	 * public static NamingStrategy UPPER_CAMEL_CASE = new NamingStrategy()
	 * 
	 * @author MaxiBon
	 *
	 */
	public NamingStrategy upperCamelCase = new NamingStrategy() {
		@Override
		public String translate(String inpu) {
			if (inpu == null || inpu.length() == 0) {
				return inpu; // garbage in, garbage out
			}
			// Replace first lower-case letter with upper-case equivalent
			char c = inpu.charAt(0);
			char uc = Character.toUpperCase(c);
			if (c == uc) {
				return inpu;
			}
			StringBuilder sb = new StringBuilder(inpu);
			sb.setCharAt(0, uc);
			return sb.toString();
		}
	};

	/**
	 * public static NamingStrategy LOWER_CASE = new NamingStrategy()
	 * 
	 * @author MaxiBon
	 *
	 */
	public NamingStrategy lowerCase = new NamingStrategy() {
		@Override
		public String translate(String input) {
			return input.toLowerCase();
		}
	};

	/**
	 * enable
	 * 
	 */
	public static void enable(final NamingStrategy namingStrategy) {
		boolean enabled = false;
		synchronized (NamingStrategySupport.class) {
			if (enabled) {
				throw new JsonException("NamingStrategySupport.enable can only be called once");
			}
			enabled = true;
			JsoniterSpi.registerExtension(new EmptyExtension() {
				/**
				 * public void updateClassDescriptor(ClassDescriptor desc)
				 * 
				 * @author MaxiBon
				 *
				 */
				@Override
				public void updateClassDescriptor(ClassDescriptor desc) {
					for (Binding binding : desc.allBindings()) {
						String translated = namingStrategy.translate(binding.name);
						binding.toNames = new String[] { translated };
						binding.fromNames = new String[] { translated };
					}
				}
			});
		}
	}

	/**
	 * translateSuppKebab
	 * 
	 * @param stringa
	 * @param l
	 * @return
	 */
	public static String translateSuppKebab(String stringa, int l) {
		StringBuilder result = new StringBuilder(l + (l >> 1));

		int upperCount = 0;

		for (int i = 0; i < l; ++i) {
			char ch = stringa.charAt(i);
			char lc = Character.toLowerCase(ch);

			if (lc == ch) {
				if (upperCount > 1) {
					result.insert(result.length() - 1, '-');
				}
				upperCount = 0;
			} else {
				if ((upperCount == 0) && (i > 0)) {
					result.append('-');
				}
				++upperCount;
			}
			result.append(lc);
		}
		return result.toString();
	}
	
	/**
	 * translateSuppSnake
	 * 
	 * @param in
	 * @return
	 */
	public static String translateSuppSnake(String in) {
		int n = in.length() * 2;
		StringBuilder result = new StringBuilder(n);
		int resultLength = 0;
		boolean wasPrevTranslated = false;
		for (int i = 0; i < n / 2; i++) {
			char c = in.charAt(i);
			if (i > 0 || c != '_') {
				if (Character.isUpperCase(c)) {
					if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_') {
						result.append('_');
						resultLength++;
					}
					c = Character.toLowerCase(c);
					wasPrevTranslated = true;
				} else {
					wasPrevTranslated = false;
				}
				result.append(c);
				resultLength++;
			}
		}
		return resultLength > 0 ? result.toString() : in;
	}

}
