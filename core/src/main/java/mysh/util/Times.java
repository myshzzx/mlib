package mysh.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * <a href='http://www.cs.berkeley.edu/CT/ag4.0/appendid.htm'>Time Zone Table</a>
 * <a href='http://www.timeanddate.com/time/map/'>Time Zone Map</a>
 *
 * @author Mysh
 * @since 2015/1/7 13:30
 */
public class Times {

	/**
	 * check time.
	 *
	 * @param hour   check hour(24-hour). ignore when negative
	 * @param minute check minute. ignore when negative
	 * @param second check second. ignore when negative
	 */
	public static boolean isNow(ZoneId zone, int hour, int minute, int second) {
		long now = nowSec(zone);

		if (second > -1 && now % 60 != second) return false;
		now /= 60;
		if (minute > -1 && now % 60 != minute) return false;
		now /= 60;
		return hour < 0 || now % 24 == hour;
	}

	private static long nowSec(ZoneId zone) {
		return (System.currentTimeMillis() + TimeZone.getTimeZone(zone).getOffset(System.currentTimeMillis())) / 1000;
	}

	/**
	 * check local time.
	 *
	 * @param hour   check hour(24-hour). ignore when negative
	 * @param minute check minute. ignore when negative
	 * @param second check second. ignore when negative
	 */
	public static boolean isNow(int hour, int minute, int second) {
		return isNow(zoneSysDefault, hour, minute, second);
	}

	private static boolean isNowBeforeAfter(boolean chkBefore, ZoneId zone, int hour, int minute, int second) {
		long now = nowSec(zone);
		now %= 24 * 3600;

		if (hour < 0 || hour > 23) throw new IllegalArgumentException("wrong hour: " + hour);
		if (minute < 0 || minute > 59) throw new IllegalArgumentException("wrong minute: " + minute);
		if (second < 0 || second > 59) throw new IllegalArgumentException("wrong second: " + second);
		long given = hour * 3600 + minute * 60 + second;

		return chkBefore ? now < given : now > given;
	}

	public static boolean isNowBefore(ZoneId zone, int hour, int minute, int second) {
		return isNowBeforeAfter(true, zone, hour, minute, second);
	}

	public static boolean isNowBefore(int hour, int minute, int second) {
		return isNowBeforeAfter(true, zoneSysDefault, hour, minute, second);
	}

	public static boolean isNowAfter(ZoneId zone, int hour, int minute, int second) {
		return isNowBeforeAfter(false, zone, hour, minute, second);
	}

	public static boolean isNowAfter(int hour, int minute, int second) {
		return isNowBeforeAfter(false, zoneSysDefault, hour, minute, second);
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
	public static final ZoneId zoneUTC = ZoneId.of("UTC");
	/**
	 * Beijing
	 */
	public static final ZoneId zoneBJ = ZoneId.of("Asia/Shanghai");
	/**
	 * Hongkong
	 */
	public static final ZoneId zoneHK = ZoneId.of("Hongkong");
	/**
	 * Japan Standard Time, Tokyo
	 */
	public static final ZoneId zoneJP = ZoneId.of("Asia/Tokyo");
	/**
	 * Eastern Standard Time, Canada/USA (Eastern)
	 */
	public static final ZoneId zoneEST = ZoneId.of("EST5EDT");
	/**
	 * Central Standard Time, Canada/USA (Central)
	 */
	public static final ZoneId zoneCST = ZoneId.of("CST6CDT");
	/**
	 * Greenwich Mean Time, London
	 */
	public static final ZoneId zoneGMT = ZoneId.of("Europe/London");
	/**
	 * Central Europe Time, most western european countries
	 */
	public static final ZoneId zoneCET = ZoneId.of("CET");
	/**
	 * Singapore Time
	 */
	public static final ZoneId zoneSG = ZoneId.of("Asia/Singapore");
	/**
	 * Australian Eastern Daylight Time, Sydney, Canberra
	 */
	public static final ZoneId zoneAE = ZoneId.of("Australia/Sydney");
	/**
	 * New Zealand Daylight Time, Auckland, Wellington
	 */
	public static final ZoneId zoneNZ = ZoneId.of("Pacific/Auckland");

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
