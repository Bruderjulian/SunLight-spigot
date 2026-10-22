package su.nightexpress.sunlight.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.TimeZone;

import su.nightexpress.nightcore.util.time.TimeFormatType;
import su.nightexpress.nightcore.util.time.TimeFormats;

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

  public static String formatMillis(final long ms) {
    if (ms <= 0L)
      return TimeFormats.formatAmount(0L, TimeFormatType.LITERAL);
    return TimeFormats.formatAmount(ms, TimeFormatType.LITERAL);
  }

  /**
   * Parses a duration typed by a player into seconds.
   * <p>
   * Every configured duration in SunLight is a plain number of seconds, so that
   * stays valid here
   * and means nothing at all. On top of that a suffix may be used — {@code 30s},
   * {@code 5m},
   * {@code 1h}, {@code 7d} — and several may be combined as {@code 1h30m}.
   * <p>
   * 
   * @return the duration in seconds, or {@code -1} when the input is not a
   *         duration at all. The
   *         caller turns that into a usage message rather than a parse error,
   *         because a player
   *         who mistyped needs to be told what is expected.
   */
  public static long parseDurationSeconds(String input) {
    if (input == null) {
      return -1L;
    }

    String text = input.trim().toLowerCase(java.util.Locale.ROOT);
    if (text.isEmpty()) {
      return -1L;
    }

    if (!text.chars().anyMatch(Character::isLetter)) {
      try {
        return Long.parseLong(text);
      } catch (NumberFormatException exception) {
        return -1L;
      }
    }

    long total = 0L;
    long number = 0L;
    boolean sawDigit = false;
    boolean sawUnit = false;

    try {
      for (int i = 0; i < text.length(); i++) {
        char symbol = text.charAt(i);
        if (symbol >= '0' && symbol <= '9') {
          number = Math.addExact(Math.multiplyExact(number, 10L), (symbol - '0'));
          sawDigit = true;
          continue;
        }

        if (!sawDigit) {
          return -1L;
        }

        long unit;
        switch (symbol) {
          case 's' -> unit = 1L;
          case 'm' -> unit = 60L;
          case 'h' -> unit = 3600L;
          case 'd' -> unit = 86400L;
          case 'w' -> unit = 604800L;
          default -> {
            return -1L;
          }
        }

        total = Math.addExact(total, Math.multiplyExact(number, unit));
        number = 0L;
        sawDigit = false;
        sawUnit = true;
      }
    } catch (ArithmeticException overflow) {
      // A value that does not fit in a long is not a usable duration, whatever it was
      // meant as.
      // Without this a huge input wraps around to a small positive number and creates
      // a
      // garbage-duration poll instead of being rejected.
      return -1L;
    }

    // A trailing number with no unit, as in "1h30", is a typo rather than a bare
    // value: mixing
    // the two notations in one string is far more likely to be a mistake than an
    // intention.
    if (sawDigit) {
      return -1L;
    }
    return sawUnit ? total : -1L;
  }
}