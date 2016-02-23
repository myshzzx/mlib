package mysh.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * @author Mysh
 * @since 2015/1/7 13:30
 */
public class Times {

	private static int timeOffset = TimeZone.getDefault().getRawOffset();

	/**
	 * check local time.
	 *
	 * @param hour   check hour, 0-23.
	 * @param minute check minute, 0-59.
	 * @param second check second, 0-59.
	 */
	public static boolean isTime(int hour, int minute, int second) {
		long now = (System.currentTimeMillis() + timeOffset) / 1000;

		if (now % 60 == second) {
			now /= 60;
			if (now % 60 == minute) {
				now /= 60;
				if (now % 24 == hour)
					return true;
			}
		}

		return false;
	}

	/**
	 * check local time.
	 *
	 * @param hour   check hour, 0-23.
	 * @param minute check minute, 0-59.
	 */
	public static boolean isTime(int hour, int minute) {
		long now = (System.currentTimeMillis() + timeOffset) / 1000;

		now /= 60;
		if (now % 60 == minute) {
			now /= 60;
			if (now % 24 == hour)
				return true;
		}

		return false;
	}

	public enum Formats {
		/**
		 * yyyy-MM-dd
		 */
		Day(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
		/**
		 * HH:mm:ss
		 */
		Time(DateTimeFormatter.ofPattern("HH:mm:ss")),
		/**
		 * HH:mm
		 */
		HourMin(DateTimeFormatter.ofPattern("HH:mm")),
		/**
		 * yyyy-MM-dd HH:mm:ss
		 */
		DayTime(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
		/**
		 * yyyy-MM-dd HH:mm
		 */
		DayHourMin(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

		private DateTimeFormatter formatter;

		Formats(DateTimeFormatter formatter) {
			this.formatter = formatter;
		}
	}

	private static final ZoneId zoneSysDefault = ZoneId.systemDefault();
	public static final ZoneId zoneBeijing = ZoneId.of("Asia/Shanghai");
	public static final ZoneId zoneUTC = ZoneId.of("UTC");
	public static final ZoneId zoneTokyo = ZoneId.of("Asia/Tokyo");
	/**
	 * Eastern Standard Time with Day Light Saving Time, used in Canada/USA (Eastern)
	 * <p>
	 * <a href='http://www.cs.berkeley.edu/CT/ag4.0/appendid.htm'>Time Zone Table</a>
	 */
	public static final ZoneId zoneEST5EDT = ZoneId.of("EST5EDT");
	/**
	 * Central Standard Time with Day Light Saving Time, used in Canada/USA (Central)
	 */
	public static final ZoneId zoneCST6CDT = ZoneId.of("CST6CDT");
	public static final ZoneId zoneLondon = ZoneId.of("Europe/London");
	/**
	 * Central Europe Time, used by most western european countries
	 */
	public static final ZoneId zoneCET = ZoneId.of("CET");

	/**
	 * @see #format(Object, Object, ZoneId)
	 */
	public static String formatNow(Object format) {
		return format(format, Instant.ofEpochMilli(System.currentTimeMillis()), zoneSysDefault);
	}

	/**
	 * @see #format(Object, Object, ZoneId)
	 */
	public static String formatNow(Object format, ZoneId zone) {
		return format(format, Instant.ofEpochMilli(System.currentTimeMillis()), zone);
	}

	/**
	 * @see #format(Object, Object, ZoneId)
	 */
	public static String format(Object format, Object time) {
		return format(format, time, zoneSysDefault);
	}

	private static final ConcurrentHashMap<String, DateTimeFormatter> formatters = new ConcurrentHashMap<>();

	/**
	 * @param format can be {@link Formats}, {@link DateTimeFormatter}, {@link String}
	 * @param time   can be {@link Date}, {@link TemporalAccessor}
	 * @param zone   add zone info to time if time has no zone but need a zone to format.
	 */
	public static String format(Object format, Object time, ZoneId zone) {
		requireNonNull(time, "need time");
		requireNonNull(zone, "need zone");
		DateTimeFormatter formatter = getProperFormatter(format);

		TemporalAccessor formatTime;

		if (time instanceof Date)
			formatTime = ((Date) time).toInstant().atZone(zone);
		else if (time instanceof Instant)
			formatTime = ((Instant) time).atZone(zone);
		else if (time instanceof TemporalAccessor)
			formatTime = (TemporalAccessor) time;
		else
			throw new IllegalArgumentException("unsupported time type: " + time.getClass());

		return formatter.format(formatTime);
	}

	private static DateTimeFormatter getProperFormatter(Object format) {
		requireNonNull(format, "need format");
		if (format instanceof Formats)
			return ((Formats) format).formatter;
		else if (format instanceof DateTimeFormatter)
			return (DateTimeFormatter) format;
		else if (format instanceof String)
			return formatters.computeIfAbsent((String) format, DateTimeFormatter::ofPattern);
		else
			throw new IllegalArgumentException("unsupported format type: " + format.getClass());
	}

	public static LocalDate parseDay(Object format, String time) {
		requireNonNull(time, "need time");
		DateTimeFormatter formatter = getProperFormatter(format);
		return LocalDate.parse(time, formatter);
	}

	public static LocalTime parseTime(Object format, String time) {
		requireNonNull(time, "need time");
		DateTimeFormatter formatter = getProperFormatter(format);
		return LocalTime.parse(time, formatter);
	}

	public static LocalDateTime parseDayTime(Object format, String time) {
		requireNonNull(time, "need time");
		DateTimeFormatter formatter = getProperFormatter(format);
		return LocalDateTime.parse(time, formatter);
	}
}
