package org.folio.circulation.domain;

import static org.folio.circulation.support.utils.DateTimeUtil.TIME_MINUTES;
import static org.folio.circulation.support.utils.DateTimeUtil.parseTime;

import java.time.LocalTime;

import io.vertx.core.json.JsonObject;

public class OpeningHour {

  private static final String START_TIME_KEY = "startTime";
  private static final String END_TIME_KEY = "endTime";

  private LocalTime startTime;
  private LocalTime endTime;

  OpeningHour(JsonObject jsonObject) {
    this.startTime = parseTime(jsonObject.getString(START_TIME_KEY));
    this.endTime = parseTime(jsonObject.getString(END_TIME_KEY));
  }

  public OpeningHour(LocalTime startTime, LocalTime endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  JsonObject toJson() {
    return new JsonObject()
      .put(START_TIME_KEY, startTime.format(TIME_MINUTES))
      .put(END_TIME_KEY, endTime.format(TIME_MINUTES));
  }
}
