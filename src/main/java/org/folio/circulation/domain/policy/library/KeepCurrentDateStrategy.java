package org.folio.circulation.domain.policy.library;

import static org.folio.circulation.domain.policy.library.ClosedLibraryStrategyUtils.END_OF_A_DAY;
import static org.folio.circulation.support.results.Result.succeeded;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.folio.circulation.AdjacentOpeningDays;
import org.folio.circulation.support.results.Result;

public class KeepCurrentDateStrategy implements ClosedLibraryStrategy {
  private final ZoneOffset zone;

  public KeepCurrentDateStrategy(ZoneOffset zone) {
    this.zone = zone;
  }

  @Override
  public Result<ZonedDateTime> calculateDueDate(ZonedDateTime requestedDate,
    AdjacentOpeningDays openingDays) {

    LocalDate date = LocalDate.ofInstant(requestedDate
      .withZoneSameLocal(zone).toInstant(), zone);
    return succeeded(ZonedDateTime.of(date, END_OF_A_DAY, zone));
  }
}
