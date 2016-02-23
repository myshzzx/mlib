package mysh.util;

import org.junit.Ignore;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class TimesTest {

	@Test
	@Ignore
	public void testIsTime() throws Exception {
		System.out.println(Times.isTime(13, 18));
	}

	@Test
	public void formatDate() throws Exception {
		Times.format("yyyy-MM", new Date());
	}

	@Test
	public void formatInstant() throws Exception {
		Times.format("yyyy-MM", Instant.now());
	}

	@Test
	public void formatZoned() throws Exception {
		Times.format("yyyy-MM", Instant.now().atZone(ZoneId.systemDefault()));
	}

	@Test
	public void formatLocal() throws Exception {
		Times.format("yyyy-MM", LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
	}

	@Test
	public void formatLocalDate() throws Exception {
		Times.format("yyyy-MM", LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()).toLocalDate());
	}

	@Test
	public void dayLightSaving() {
		dayLightSaving(LocalDateTime.of(2010, 8, 1, 10, 0, 0).atZone(Times.zoneBeijing));
		System.out.println(" ------------------------ ");
		dayLightSaving(LocalDateTime.of(2010, 1, 1, 10, 0, 0).atZone(Times.zoneBeijing));
	}

	private void dayLightSaving(ZonedDateTime time) {
		System.out.println(Times.format(Times.Formats.DayTime, time.withZoneSameInstant(ZoneId.of("UTC"))));
		System.out.println();

		System.out.println(Times.format(Times.Formats.DayTime, time.withZoneSameInstant(ZoneId.of("SystemV/EST5"))));
		System.out.println(Times.format(Times.Formats.DayTime, time.withZoneSameInstant(ZoneId.of("EST5EDT"))));
		System.out.println();

		System.out.println(Times.format(Times.Formats.DayTime, time.withZoneSameInstant(ZoneId.of("GMT"))));
		System.out.println(Times.format(Times.Formats.DayTime, time.withZoneSameInstant(ZoneId.of("Europe/London"))));
		System.out.println(Times.format(Times.Formats.DayTime, time.withZoneSameInstant(ZoneId.of("CET"))));
	}

	@Test
	public void parseDay() {
		System.out.println(Times.parseDay("yyyy-MM-dd", "2012-12-12"));
	}

	@Test
	public void parseDay2() {
		System.out.println(Times.parseDay("yyyy-MM-dd HH", "2012-12-12 12"));
	}

	@Test
	public void parseDayTime() {
		System.out.println(Times.parseDayTime("yyyy-MM-dd HH", "2012-12-12 12"));
	}

	@Test
	public void parseTime() {
		System.out.println(Times.parseTime("HH", "12"));
	}

	@Test
	public void parseTime2() {
		System.out.println(Times.parseTime("dd HH", "12 12"));
	}
}
