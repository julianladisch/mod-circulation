package org.folio.circulation.domain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.folio.circulation.support.ClockManager;
import org.junit.Test;

import api.support.builders.UserBuilder;

public class ActiveUserTests {
  @Test
  public void userIsActiveWhenActivePropertyIsTrue() {
    final User activeUser = new User(new UserBuilder()
      .active()
      .create());

    assertThat(activeUser.isActive(), is(true));
    assertThat(activeUser.isInactive(), is(false));
  }

  @Test
  public void userIsInactiveWhenActivePropertyIsFalse() {
    final User activeUser = new User(new UserBuilder()
      .inactive()
      .create());

    assertThat(activeUser.isActive(), is(false));
    assertThat(activeUser.isInactive(), is(true));
  }

  @Test
  public void userIsInactiveWhenExpiredInThePast() {
    final User activeUser = new User(new UserBuilder()
      .active()
      .expires(ClockManager.getZonedDateTime().minusDays(10))
      .create());

    assertThat(activeUser.isActive(), is(false));
    assertThat(activeUser.isInactive(), is(true));
  }

  @Test
  public void userIsActiveWhenExpiresInTheFuture() {
    final User activeUser = new User(new UserBuilder()
      .active()
      .expires(ClockManager.getZonedDateTime().plusDays(30))
      .create());

    assertThat(activeUser.isActive(), is(true));
    assertThat(activeUser.isInactive(), is(false));
  }

  @Test
  public void userIsActiveWhenDoesNotExpire() {
    final User activeUser = new User(new UserBuilder()
      .active()
      .noExpiration()
      .create());

    assertThat(activeUser.isActive(), is(true));
    assertThat(activeUser.isInactive(), is(false));
  }
}
