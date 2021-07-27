package org.folio.circulation;

import org.folio.circulation.support.ClockManager;
import org.joda.time.DateTime;

@FunctionalInterface
public interface Clock {
  static Clock systemClock() {
    return () -> ClockManager.getClockManager().getDateTime();
  }

  DateTime now();
}
