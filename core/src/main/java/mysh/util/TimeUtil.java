package mysh.util;

import java.util.TimeZone;

/**
 * @author Mysh
 * @since 2015/1/7 13:30
 */
public class TimeUtil {

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
}
