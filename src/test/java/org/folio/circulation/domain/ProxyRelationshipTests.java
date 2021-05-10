package org.folio.circulation.domain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import api.support.builders.ProxyRelationshipBuilder;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class ProxyRelationshipTests {
  @Test
  @Parameters({
    "false",
    "true"
  })
  public void shouldBeActiveWhenActiveAndDoesNotExpire(boolean useMetaObject) {
    final ProxyRelationship relationship = new ProxyRelationship(
      new ProxyRelationshipBuilder()
        .proxy(UUID.randomUUID())
        .sponsor(UUID.randomUUID())
        .active()
        .doesNotExpire()
        .useMetaObject(useMetaObject)
        .create());

    assertThat(relationship.isActive(), is(true));
  }

  @Test
  @Parameters({
    "false",
    "true"
  })
  public void shouldBeActiveWhenActiveAndNotExpired(boolean useMetaObject) {
    final ProxyRelationship relationship = new ProxyRelationship(
      new ProxyRelationshipBuilder()
        .proxy(UUID.randomUUID())
        .sponsor(UUID.randomUUID())
        .active()
        .expires(ZonedDateTime.now(Clock.systemUTC()).plusWeeks(3))
        .useMetaObject(useMetaObject)
        .create());

    assertThat(relationship.isActive(), is(true));
  }

  @Test
  @Parameters({
    "false",
    "true"
  })
  public void shouldBeInactiveWhenActiveAndExpired(boolean useMetaObject) {
    final ProxyRelationship relationship = new ProxyRelationship(
      new ProxyRelationshipBuilder()
        .proxy(UUID.randomUUID())
        .sponsor(UUID.randomUUID())
        .active()
        .expires(ZonedDateTime.now(Clock.systemUTC()).minusMonths(2))
        .useMetaObject(useMetaObject)
        .create());

    assertThat(relationship.isActive(), is(false));
  }

  @Test
  @Parameters({
    "false",
    "true"
  })
  public void shouldBeInactiveWhenInactiveAndDoesNotExpire(boolean useMetaObject) {
    final ProxyRelationship relationship = new ProxyRelationship(
      new ProxyRelationshipBuilder()
        .proxy(UUID.randomUUID())
        .sponsor(UUID.randomUUID())
        .inactive()
        .doesNotExpire()
        .useMetaObject(useMetaObject)
        .create());

    assertThat(relationship.isActive(), is(false));
  }


  @Test
  @Parameters({
    "false",
    "true"
  })
  public void shouldBeInactiveWhenInactiveAndNotExpired(boolean useMetaObject) {
    final ProxyRelationship relationship = new ProxyRelationship(
      new ProxyRelationshipBuilder()
        .proxy(UUID.randomUUID())
        .sponsor(UUID.randomUUID())
        .inactive()
        .expires(ZonedDateTime.now(Clock.systemUTC()).plusWeeks(3))
        .useMetaObject(useMetaObject)
        .create());

    assertThat(relationship.isActive(), is(false));
  }


  @Test
  @Parameters({
    "false",
    "true"
  })
  public void shouldBeInactiveWhenInactiveAndExpired(boolean useMetaObject) {
    final ProxyRelationship relationship = new ProxyRelationship(
      new ProxyRelationshipBuilder()
        .proxy(UUID.randomUUID())
        .sponsor(UUID.randomUUID())
        .inactive()
        .expires(ZonedDateTime.now(Clock.systemUTC()).minusMonths(2))
        .useMetaObject(useMetaObject)
        .create());

    assertThat(relationship.isActive(), is(false));
  }
}
