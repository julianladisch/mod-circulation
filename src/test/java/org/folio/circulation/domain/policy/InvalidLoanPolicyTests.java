package org.folio.circulation.domain.policy;

import static api.support.matchers.FailureMatcher.hasValidationFailure;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collections;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.RequestQueue;
import org.folio.circulation.resources.renewal.RegularRenewalStrategy;
import org.folio.circulation.support.results.Result;
import org.junit.Test;

import api.support.builders.LoanBuilder;
import api.support.builders.LoanPolicyBuilder;
import io.vertx.core.json.JsonObject;

public class InvalidLoanPolicyTests {
  @Test
  public void shouldFailCheckOutCalculationWhenNoLoanPolicyProvided() {
    final JsonObject representation = new LoanPolicyBuilder()
      .rolling(Period.from(5, "Unknown"))
      .withName("Invalid Loan Policy")
      .create();

    representation.remove("loansPolicy");

    LoanPolicy loanPolicy = LoanPolicy.from(representation);

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 3, 14, 11, 14, 54, 0, UTC);

    Loan loan = new LoanBuilder()
      .open()
      .withLoanDate(loanDate)
      .asDomainObject();

    final Result<ZonedDateTime> result = loanPolicy.calculateInitialDueDate(loan, null);

    //TODO: This is fairly ugly, replace with a better message
    assertThat(result, hasValidationFailure(
      "profile \"\" in the loan policy is not recognised"));
  }

  @Test
  public void shouldFailRenewalWhenNoLoanPolicyProvided() {
    final JsonObject representation = new LoanPolicyBuilder()
      .rolling(Period.from(5, "Unknown"))
      .withName("Invalid Loan Policy")
      .create();

    representation.remove("loansPolicy");

    RegularRenewalStrategy regularRenewalStrategy = new RegularRenewalStrategy();
    LoanPolicy loanPolicy = LoanPolicy.from(representation);

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 3, 14, 11, 14, 54, 0, UTC);

    Loan loan = new LoanBuilder()
      .open()
      .withLoanDate(loanDate)
      .asDomainObject()
      .withLoanPolicy(loanPolicy);

    final Result<Loan> result = regularRenewalStrategy.renew(
      loan, ZonedDateTime.now(Clock.systemUTC()),
      new RequestQueue(Collections.emptyList()));

    //TODO: This is fairly ugly, replace with a better message
    assertThat(result, hasValidationFailure(
      "profile \"\" in the loan policy is not recognised"));
  }
}
