package api.support.matchers;

import static org.folio.circulation.support.utils.DateTimeUtil.toOffsetDateTime;
import static org.hamcrest.CoreMatchers.is;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.hamcrest.Matcher;

public class DateTimeMatchers {
  private DateTimeMatchers() { }

  public static Matcher<OffsetDateTime> isEquivalentTo(ZonedDateTime expected) {
    // All date times produced by the APIs should be in UTC
    return is(toOffsetDateTime(expected.withZoneSameInstant(ZoneOffset.UTC)));
  }
}
