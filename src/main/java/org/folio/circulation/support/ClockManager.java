package org.folio.circulation.support;

import java.time.Clock;
import java.time.ZonedDateTime;

// This class allows for unit tests to replace the clock used by the module.
// Ideally, we'd use dependency injection for this.
public class ClockManager {
  private static final ClockManager INSTANCE = new ClockManager();

  private Clock clock = Clock.systemUTC();

  private ClockManager() {
    super();
  }

  public static ClockManager getClockManager() {
    return INSTANCE;
  }

  public void setClock(Clock clock) {
    if (clock == null) {
      throw new IllegalArgumentException("clock cannot be null");
    }

    this.clock = clock;
  }

  public void setDefaultClock() {
    clock = Clock.systemUTC();
  }

  public Clock getClock() {
    return clock;
  }

  public ZonedDateTime getZonedDateTime() {
    return ZonedDateTime.now(clock);
  }
}
