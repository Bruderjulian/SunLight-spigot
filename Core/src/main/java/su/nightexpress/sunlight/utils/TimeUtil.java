package su.nightexpress.sunlight.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

public class TimeUtil {

  private static TimeZone timeZone = TimeZone.getDefault();

  public static ZoneId getZoneId() {
    return timeZone.toZoneId();
  }

  public static boolean isPassed(long timestamp) {
    return timestamp >= 0 && System.currentTimeMillis() > timestamp;
  }

  public static long createFutureTimestamp(double seconds) {
    return seconds < 0 ? -1L : System.currentTimeMillis() + (long) (Math.abs(seconds) * 1000D);
  }

  public static LocalDateTime getCurrentDateTime() {
    return LocalDateTime.now(timeZone.toZoneId());
  }

  public static LocalDate getCurrentDate() {
    return LocalDate.now(timeZone.toZoneId());
  }

  public static LocalTime getCurrentTime() {
    return LocalTime.now(timeZone.toZoneId());
  }

  public static LocalDateTime getLocalDateTimeOf(long ms) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), timeZone.toZoneId());
  }

  public static long toEpochMillis(LocalDateTime dateTime) {
    return dateTime.atZone(getZoneId()).toInstant().toEpochMilli();
  }
}
