package org.folio.circulation.domain.policy.library;

import static org.folio.circulation.domain.policy.library.ClosedLibraryStrategyUtils.END_OF_A_DAY;
import static org.folio.circulation.support.results.Result.succeeded;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.folio.circulation.AdjacentOpeningDays;
import org.folio.circulation.support.results.Result;

public class KeepCurrentDateStrategy implements ClosedLibraryStrategy {
  private final ZoneId zone;

  public KeepCurrentDateStrategy(ZoneId zone) {
    this.zone = zone;
  }

  @Override
  public Result<ZonedDateTime> calculateDueDate(ZonedDateTime requestedDate,
    AdjacentOpeningDays openingDays) {

    return succeeded(ZonedDateTime.of(
      requestedDate.toLocalDate().atTime(END_OF_A_DAY), zone));
  }
}
