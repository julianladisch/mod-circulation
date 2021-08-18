package org.folio.circulation.domain;

import static api.support.matchers.JsonObjectMatcher.hasJsonPath;
import static org.folio.circulation.support.utils.DateTimeUtil.isSameMillis;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.folio.circulation.support.ClockManager;
import org.junit.jupiter.api.Test;

import api.support.builders.LoanBuilder;

public class LoanLostDateTest {

  @Test
  public void declaredLostDateReturnedWhenSet() {
    final var declaredLostDate = ClockManager.getZonedDateTime();
    final var loan = new LoanBuilder().asDomainObject()
      .declareItemLost("Lost", declaredLostDate);

    assertTrue(isSameMillis(declaredLostDate, loan.getLostDate()));
  }

  @Test
  public void agedToLostDateReturnedWhenSet() {
    final var agedToLostDate = ClockManager.getZonedDateTime();
    final var loan = new LoanBuilder().asDomainObject()
      .ageOverdueItemToLost(agedToLostDate);

    assertTrue(isSameMillis(agedToLostDate, loan.getLostDate()));
  }

  @Test
  public void declaredLostDateReturnedWhenIsAfterAgedToLostDate() {
    final var agedToLostDate = ClockManager.getZonedDateTime().minusDays(2);
    final var declaredLostDate = ClockManager.getZonedDateTime();

    final var loan = new LoanBuilder().asDomainObject()
      .ageOverdueItemToLost(agedToLostDate)
      .declareItemLost("Lost", declaredLostDate);

    assertTrue(isSameMillis(declaredLostDate, loan.getLostDate()));
    // make sure properties are not cleared
    assertThat(loan.asJson(), allOf(
      hasJsonPath("declaredLostDate", declaredLostDate.toString()),
      hasJsonPath("agedToLostDelayedBilling.agedToLostDate", agedToLostDate.toString())
    ));
  }

  @Test
  public void agedToLostDateReturnedWhenIsAfterDeclaredLostDate() {
    final var declaredLostDate = ClockManager.getZonedDateTime().minusDays(3);
    final var agedToLostDate = ClockManager.getZonedDateTime();

    final var loan = new LoanBuilder().asDomainObject()
      .ageOverdueItemToLost(agedToLostDate)
      .declareItemLost("Lost", declaredLostDate);

    assertTrue(isSameMillis(agedToLostDate, loan.getLostDate()));
    // make sure properties are not cleared
    assertThat(loan.asJson(), allOf(
      hasJsonPath("declaredLostDate", declaredLostDate.toString()),
      hasJsonPath("agedToLostDelayedBilling.agedToLostDate", agedToLostDate.toString())
    ));
  }

  @Test
  public void lostDateIsNotNullWhenBothLostDatesAreEqual() {
    final var lostDate = ClockManager.getZonedDateTime();

    final var loan = new LoanBuilder().asDomainObject()
      .ageOverdueItemToLost(lostDate)
      .declareItemLost("Lost", lostDate);

    assertTrue(isSameMillis(lostDate, loan.getLostDate()));
  }

  @Test
  public void lostDateIsNullWhenLoanWasNeverLost() {
    final var loan = new LoanBuilder().asDomainObject();

    assertNull(loan.getLostDate());
  }
}
