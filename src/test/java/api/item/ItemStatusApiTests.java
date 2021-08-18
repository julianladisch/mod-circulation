package api.item;

import static api.support.matchers.TextDateTimeMatcher.withinSecondsAfter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.ZonedDateTime;

import org.folio.circulation.support.ClockManager;
import org.junit.jupiter.api.Test;

import api.support.APITests;
import api.support.http.IndividualResource;
import io.vertx.core.json.JsonObject;

public class ItemStatusApiTests extends APITests {
  private static final String ITEM_STATUS = "status";
  private static final String ITEM_STATUS_DATE = "date";

  @Test
  public void itemStatusDateShouldExistsAfterCheckout() {

    IndividualResource item = itemsFixture.basedUponSmallAngryPlanet();
    IndividualResource user = usersFixture.jessica();
    final ZonedDateTime beforeCheckOutDatetime = ClockManager.getZonedDateTime();

    checkOutFixture.checkOutByBarcode(item, user, ClockManager.getZonedDateTime());

    JsonObject checkedOutItem = itemsClient.get(item.getId()).getJson();

    assertThat(checkedOutItem.getJsonObject(ITEM_STATUS).getString(ITEM_STATUS_DATE),
      is(notNullValue()));
    assertThat(checkedOutItem.getJsonObject(ITEM_STATUS).getString(ITEM_STATUS_DATE),
      withinSecondsAfter(2, beforeCheckOutDatetime)
    );
  }
}
