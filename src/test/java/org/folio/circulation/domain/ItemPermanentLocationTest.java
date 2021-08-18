package org.folio.circulation.domain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import api.support.builders.HoldingBuilder;
import api.support.builders.ItemBuilder;
import io.vertx.core.json.JsonObject;

public class ItemPermanentLocationTest {

  @Test
  public void itemLocationTakesPriorityOverHoldings() {
    UUID itemLocation = UUID.randomUUID();
    UUID holdingsLocation = UUID.randomUUID();

    JsonObject itemJson = new ItemBuilder().withPermanentLocation(itemLocation).create();
    JsonObject holdingsJson = new HoldingBuilder().withPermanentLocation(holdingsLocation).create();

    Item item = Item.from(itemJson).withHoldingsRecord(holdingsJson);

    assertThat(item.getPermanentLocationId(), is(itemLocation.toString()));
  }

  @Test
  public void holdingsLocationIsReturnedByDefault() {
    UUID holdingsLocation = UUID.randomUUID();

    JsonObject itemJson = new ItemBuilder().withPermanentLocation((UUID) null).create();
    JsonObject holdingsJson = new HoldingBuilder().withPermanentLocation(holdingsLocation).create();

    Item item = Item.from(itemJson).withHoldingsRecord(holdingsJson);

    assertThat(item.getPermanentLocationId(), is(holdingsLocation.toString()));
  }

  @Test
  public void nullIsReturnedWhenNoHoldingsJsonPresent() {
    JsonObject itemJson = new ItemBuilder().withPermanentLocation((UUID) null).create();

    Item item = Item.from(itemJson).withHoldingsRecord(null);

    assertThat(item.getPermanentLocationId(), nullValue());
  }
}
