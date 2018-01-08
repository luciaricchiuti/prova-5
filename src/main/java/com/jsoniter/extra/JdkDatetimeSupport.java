package com.jsoniter.extra;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Decoder;

import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.JsoniterSpi;

/**
 * there is no official way to encode/decode datetime, this is just an option
 * for you
 */
public class JdkDatetimeSupport {

	private static String pattern;
	// 2014-04-01 10:45
	/**
	 * LocalDateTime dateTime
	 */
	int y = 2014;
	int d = 1;
	int h = 10;
	int m = 45;
	LocalDateTime dateTime = LocalDateTime.of(y, Month.APRIL, d, h, m);
	// format as basic ISO date format (20140220)
	/**
	 * 
	 */
	String asBasicIsoDate = dateTime.format(DateTimeFormatter.BASIC_ISO_DATE);
	// format as ISO week date (2014-W08-4)
	/**
	 * 
	 */
	String asIsoWeekDate = dateTime.format(DateTimeFormatter.ISO_WEEK_DATE);
	// format ISO date time (2014-02-20T20:04:05.867)
	/**
	 * 
	 */
	String asIsoDateTime = dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
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
	DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(new Locale("de"));
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
	LocalDate fromCustomPattern = LocalDate.parse("20.01.2014", DateTimeFormatter.ofPattern("dd.MM.yyyy"));;

	/**
	 * enable.
	 * 
	 * @param patterns
	 * @throws JsonException 
	 */
	public static void enable(String patterns) throws JsonException {

		synchronized (JdkDatetimeSupport.class) {
			if (JdkDatetimeSupport.pattern != "") {

				throw new JsonException("JdkDatetimeSupport.enable can only be called once");
			}
			JdkDatetimeSupport.pattern = patterns;
			JsoniterSpi.registerTypeEncoder(Date.class, new com.jsoniter.spi.Encoder.ReflectionEncoder() {
				@Override

				/**
				 * encode.
				 * throws IOException
				 */
				public void encode(Object obj, JsonStream stream) throws IOException {
					stream.writeVal(get().format(obj));
				}

				@Override
				public Any wrap(Object obj) {
					return Any.wrap(get().format(obj));
				}
			});
			JsoniterSpi.registerTypeDecoder(Date.class, new Decoder() {
				@Override
				public Object decode(JsonIterator iter) throws IOException {
					try {
						return get().parse(iter.readString());
					} catch (ParseException e) {
						throw new JsonException("Error: ParseException.");
					}
				}
			});
		}
	}

	protected static DateFormat get() {
		String pattern;
		return null;
	}
}
