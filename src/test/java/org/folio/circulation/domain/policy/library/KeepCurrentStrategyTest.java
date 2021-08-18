package org.folio.circulation.domain.policy.library;

import static java.time.ZoneOffset.UTC;
import static org.folio.circulation.domain.policy.library.ClosedLibraryStrategyUtils.END_OF_A_DAY;
import static org.junit.Assert.assertEquals;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;

import org.folio.circulation.support.results.Result;
import org.junit.jupiter.api.Test;

public class KeepCurrentStrategyTest {

  @Test
  public void testKeepCurrentDateStrategy() {
    ClosedLibraryStrategy keepCurrentStrategy =
      new KeepCurrentDateStrategy(UTC);
    ZonedDateTime requestDate = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, UTC);

    Result<ZonedDateTime> calculatedDateTime = keepCurrentStrategy
      .calculateDueDate(requestDate, null);

    ZonedDateTime expectedDate = ZonedDateTime.of(requestDate.toLocalDate(),
      END_OF_A_DAY, requestDate.getZone());
    assertEquals(expectedDate, calculatedDateTime.value());
  }

  @Test
  public void testKeepCurrentDateTimeStrategy() {
    ClosedLibraryStrategy keepCurrentStrategy = new KeepCurrentDateTimeStrategy();
     ZonedDateTime requestDate = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, UTC);

    Result<ZonedDateTime> calculatedDateTime = keepCurrentStrategy.calculateDueDate(requestDate, null);

    assertEquals(requestDate, calculatedDateTime.value());
  }

  @Test
  public void shouldAlwaysKeepCurrentDateWhenConvertingToTimeZone() {
    final int year = 2020;
    final int month = 11;
    final int dayOfMonth = 17;

    final ZonedDateTime now = ZonedDateTime.of(year, month, dayOfMonth, 9, 47, 0, 0, UTC);

    IntStream.rangeClosed(-12, 12)
      .forEach(zoneOffset -> {
        final ZoneOffset timeZone = ZoneOffset.ofHours(zoneOffset);
        final KeepCurrentDateStrategy strategy = new KeepCurrentDateStrategy(timeZone);

        final ZonedDateTime newDueDate = strategy.calculateDueDate(now, null).value();

        assertEquals(year, newDueDate.getYear());
        assertEquals(month, newDueDate.getMonth());
        assertEquals(dayOfMonth, newDueDate.getDayOfMonth());

        assertEquals(23, newDueDate.getHour());
        assertEquals(59, newDueDate.getMinute());
        assertEquals(59, newDueDate.getSecond());

        final int zoneOffsetInS = zoneOffset * 60 * 60;
        assertEquals(zoneOffsetInS, newDueDate.getOffset().getTotalSeconds());
      });
  }
}
