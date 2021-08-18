package api.requests.scenarios;

import static api.support.builders.ItemBuilder.AWAITING_PICKUP;
import static api.support.builders.RequestBuilder.OPEN_AWAITING_PICKUP;
import static api.support.builders.RequestBuilder.OPEN_NOT_YET_FILLED;
import static api.support.matchers.ItemStatusCodeMatcher.hasItemStatus;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import api.support.APITests;
import api.support.http.IndividualResource;

public class MultipleOutOfOrderRequestsTests extends APITests {
  @Disabled("Disabled since introducing position in queue, " +
    "need to decide if will support this, in the interim before allowing changing of position")
  @Test
  public void statusOfOldestRequestCreatedOutOfOrderChangesToAwaitingPickupWhenItemCheckedIn() {

    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    IndividualResource james = usersFixture.james();
    IndividualResource jessica = usersFixture.jessica();
    IndividualResource steve = usersFixture.steve();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, james);

    IndividualResource requestBySteve = requestsFixture.placeHoldShelfRequest(
      smallAngryPlanet, steve, ZonedDateTime.of(2018, 1, 10, 15, 34, 21, 0, UTC));

    IndividualResource oldestRequestByJessica = requestsFixture.placeHoldShelfRequest(
      smallAngryPlanet, jessica, ZonedDateTime.of(2017, 7, 22, 10, 22, 54, 0, UTC));

      checkInFixture.checkInByBarcode(smallAngryPlanet);

    oldestRequestByJessica = requestsClient.get(oldestRequestByJessica);

    assertThat(oldestRequestByJessica.getJson().getString("status"), is(OPEN_AWAITING_PICKUP));

    requestBySteve = requestsClient.get(requestBySteve);

    assertThat(requestBySteve.getJson().getString("status"), is(OPEN_NOT_YET_FILLED));

    smallAngryPlanet = itemsClient.get(smallAngryPlanet);

    assertThat(smallAngryPlanet, hasItemStatus(AWAITING_PICKUP));
  }
}
