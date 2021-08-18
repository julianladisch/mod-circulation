package org.folio.circulation.domain.validation;

import static org.folio.circulation.support.results.Result.failed;
import static org.folio.circulation.support.results.Result.ofAsync;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.folio.circulation.domain.Item;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.LoanAndRelatedRecords;
import org.folio.circulation.infrastructure.storage.loans.LoanRepository;
import org.folio.circulation.support.ClockManager;
import org.folio.circulation.support.ServerErrorFailure;
import org.folio.circulation.support.ValidationErrorFailure;
import org.folio.circulation.support.results.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.vertx.core.json.JsonObject;
import lombok.val;

public class ChangeDueDateValidatorTest {
  private ChangeDueDateValidator changeDueDateValidator;

  @BeforeEach
  public void mockRepository() {
    final LoanRepository loanRepository = mock(LoanRepository.class);

    when(loanRepository.getById(anyString()))
      .thenReturn(ofAsync(() -> createLoan("", ClockManager.getZonedDateTime().minusHours(2))));

    changeDueDateValidator = new ChangeDueDateValidator(loanRepository);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Declared lost",
    "Claimed returned",
    "Aged to lost"
  })
  public void cannotChangeDueDateForItemInDisallowedStatus(String itemStatus) {
    val validationResult  = changeDueDateValidator
      .refuseChangeDueDateForItemInDisallowedStatus(loanAndRelatedRecords(itemStatus))
      .getNow(failed(new ServerErrorFailure("timed out")));

    assertTrue(validationResult.failed());
    assertThat(validationResult.cause(), instanceOf(ValidationErrorFailure.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Declared lost",
    "Claimed returned",
    "Aged to lost"
  })
  public void canChangeLoanWhenDueDateIsNotChanged(String itemStatus) {
    Loan existingLoan = createLoan(itemStatus, ClockManager.getZonedDateTime());

    final LoanRepository loanRepository = mock(LoanRepository.class);
    when(loanRepository.getById(anyString())).thenReturn(ofAsync(() -> existingLoan));

    changeDueDateValidator = new ChangeDueDateValidator(loanRepository);

    Result<LoanAndRelatedRecords> changedLoan = loanAndRelatedRecords(Loan.from(existingLoan.asJson()
      .put("action", "checkedOut")));

    Result<LoanAndRelatedRecords> validationResult  = changeDueDateValidator
      .refuseChangeDueDateForItemInDisallowedStatus(changedLoan)
      .getNow(failed(new ServerErrorFailure("timed out")));

    assertTrue(validationResult.succeeded());
  }

  private Result<LoanAndRelatedRecords> loanAndRelatedRecords(String itemStatus) {
    Loan loan = createLoan(itemStatus, ClockManager.getZonedDateTime());
    return succeeded(new LoanAndRelatedRecords(loan));
  }

  private Result<LoanAndRelatedRecords> loanAndRelatedRecords(Loan loan) {
    return succeeded(new LoanAndRelatedRecords(loan));
  }

  private Loan createLoan(String itemStatus, ZonedDateTime dueDate) {
    final JsonObject loanRepresentation = new JsonObject()
      .put("id", UUID.randomUUID().toString())
      .put("dueDate", dueDate.toString());

    JsonObject itemRepresentation = new JsonObject()
      .put("status", new JsonObject().put("name", itemStatus));

    Item item = Item.from(itemRepresentation);
    return Loan.from(loanRepresentation).withItem(item);
  }
}
