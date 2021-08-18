package org.folio.circulation.resources;

import static api.support.matchers.ResultMatchers.hasValidationError;
import static api.support.matchers.ResultMatchers.succeeded;
import static api.support.matchers.ValidationErrorMatchers.hasMessage;
import static org.folio.circulation.resources.RenewalValidator.errorWhenEarlierOrSameDueDate;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.ZonedDateTime;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.support.ClockManager;
import org.folio.circulation.support.results.Result;
import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

public class RenewalValidatorTest {

  @Test
  public void shouldDisallowRenewalWhenDueDateIsEarlierOrSame() {
    ZonedDateTime dueDate = ClockManager.getZonedDateTime();
    ZonedDateTime proposedDueDate = dueDate.minusWeeks(2);
    Loan loan = createLoan(dueDate);

    Result<ZonedDateTime> validationResult = errorWhenEarlierOrSameDueDate(loan, proposedDueDate);

    assertThat(validationResult, hasValidationError(
      hasMessage("renewal would not change the due date")));
  }

  @Test
  public void shouldAllowRenewalWhenDueDateAfterCurrentDueDate() {
    ZonedDateTime dueDate = ClockManager.getZonedDateTime();
    ZonedDateTime proposedDueDate = dueDate.plusWeeks(1);
    Loan loan = createLoan(dueDate);

    Result<ZonedDateTime> validationResult = errorWhenEarlierOrSameDueDate(loan, proposedDueDate);

    assertThat(validationResult, succeeded());
  }

  private Loan createLoan(ZonedDateTime dueDate) {
    return Loan.from(new JsonObject())
      .changeDueDate(dueDate);
  }
}
