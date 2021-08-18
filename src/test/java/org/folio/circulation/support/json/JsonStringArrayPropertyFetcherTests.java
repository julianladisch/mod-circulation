package org.folio.circulation.support.json;

import static java.util.stream.Stream.of;
import static org.folio.circulation.support.StreamToListMapper.toList;
import static org.folio.circulation.support.json.JsonStringArrayPropertyFetcher.toStream;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JsonStringArrayPropertyFetcherTests {

  @Test
  public void StreamShouldContainSameContentsAsArray() {
    JsonObject json = objectWithJsonArrayOf("Foo", "Bar", "Lorem", "Ipsum");

    assertThat(toList(toStream(json, "array")), contains("Foo", "Bar", "Lorem", "Ipsum"));
  }

  @Test
  public void shouldMapEmptyArrayToEmptyStream() {
    JsonObject json = objectWithJsonArrayOf();

    assertThat(toList(toStream(json, "array")), is(empty()));
  }

  @Test
  public void shouldSkipNonStringElements() {
    JsonArray array = new JsonArray(toList(of("Foo", "Bar", new JsonObject(), "Lorem", "Ipsum")));

    JsonObject json = new JsonObject().put("array", array);

    assertThat(toList(toStream(json, "array")),
      contains("Foo", "Bar", "Lorem", "Ipsum"));
  }

  private JsonObject objectWithJsonArrayOf(String... strings) {
    JsonArray array = jsonArrayOf(strings);

    return new JsonObject().put("array", array);
  }

  private JsonArray jsonArrayOf(String... strings) {
    return new JsonArray(toList(of(strings)));
  }
}
