package org.folio.circulation.domain.anonymization.checkers;

import java.time.ZonedDateTime;
import static org.folio.circulation.support.utils.DateTimeUtil.isAfterMillis;

import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.ClockManager;


abstract class TimePeriodChecker implements AnonymizationChecker {

  private Period period;

  TimePeriodChecker(Period period) {
    this.period = period;
  }

  boolean checkTimePeriodPassed(ZonedDateTime startDate) {
    return startDate != null && isAfterMillis(
      ClockManager.getClockManager().getZonedDateTime(),
      period.plusDate(startDate));
  }

}
