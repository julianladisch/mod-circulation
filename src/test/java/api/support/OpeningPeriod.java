package api.support;

import static org.folio.circulation.support.utils.DateTimeUtil.parseDateTime;
import static org.folio.circulation.support.utils.DateTimeUtil.formatDateTime;

import java.time.LocalDate;

import org.folio.circulation.domain.OpeningDay;

import io.vertx.core.json.JsonObject;

public class OpeningPeriod {
  private static final String OPENING_DAY_KEY = "openingDay";
  private static final String DATE_KEY = "date";

  private final OpeningDay openingDay;
  private final LocalDate date;

  public OpeningPeriod(LocalDate date, OpeningDay openingDay) {
    this.openingDay = openingDay;
    this.date = date;
  }

  public static OpeningPeriod from(JsonObject jsonObject) {
    return new OpeningPeriod(parseDateTime(jsonObject.getString(DATE_KEY)).toLocalDate(),
      OpeningDay.fromJsonByDefaultKey(jsonObject));
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put(OPENING_DAY_KEY, openingDay.toJson())
      .put(DATE_KEY, formatDateTime(date));
  }

  public OpeningDay getOpeningDay() {
    return openingDay;
  }

  public LocalDate getDate() {
    return date;
  }
}
