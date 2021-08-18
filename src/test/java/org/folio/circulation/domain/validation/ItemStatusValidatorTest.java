package org.folio.circulation.domain.validation;

import static org.folio.circulation.support.ValidationErrorFailure.singleValidationError;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import org.folio.circulation.domain.Item;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.LoanAndRelatedRecords;
import org.folio.circulation.support.ValidationErrorFailure;
import org.folio.circulation.support.results.Result;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.vertx.core.json.JsonObject;

public class ItemStatusValidatorTest {

  @ParameterizedTest
  @ValueSource(strings = {
    "Long missing",
    "In process (non-requestable)",
    "Restricted",
    "Unavailable",
    "Unknown"
  })
  public void canCheckOutItemInAllowedStatus(String itemStatus) {
    ItemStatusValidator validator = new ItemStatusValidator(this::validationError);

    Result<LoanAndRelatedRecords> validationResult  = validator
      .refuseWhenItemIsNotAllowedForCheckOut(loanWithItemInStatus(itemStatus));

    assertTrue(validationResult.succeeded());
    assertThat(validationResult.value(), notNullValue());
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Declared lost",
    "Claimed returned",
    "Aged to lost",
    "Intellectual item"
  })
  public void cannotCheckOutItemInDisallowedStatus(String itemStatus) {
    ItemStatusValidator validator = new ItemStatusValidator(this::validationError);

    Result<LoanAndRelatedRecords> validationResult  = validator
      .refuseWhenItemIsNotAllowedForCheckOut(loanWithItemInStatus(itemStatus));

    assertTrue(validationResult.failed());
    assertThat(validationResult.cause(), instanceOf(ValidationErrorFailure.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "Declared lost",
    "Claimed returned",
    "Aged to lost"
  })
  public void cannotChangeDueDateForItemInDisallowedStatus(String itemStatus) {
    ItemStatusValidator validator = new ItemStatusValidator(this::validationError);

    Result<LoanAndRelatedRecords> validationResult  = validator
      .refuseWhenItemStatusDoesNotAllowDueDateChange(loanWithItemInStatus(itemStatus));

    assertTrue(validationResult.failed());
    assertThat(validationResult.cause(), instanceOf(ValidationErrorFailure.class));
  }

  private Result<LoanAndRelatedRecords> loanWithItemInStatus(String itemStatus) {
    JsonObject itemRepresentation = new JsonObject()
      .put("status", new JsonObject().put("name", itemStatus));
    Item item = Item.from(itemRepresentation);

    Loan loan = Loan.from(new JsonObject()).withItem(item);
    LoanAndRelatedRecords context = new LoanAndRelatedRecords(loan);

    return succeeded(context.withItem(item));
  }

  private ValidationErrorFailure validationError(Item item) {
    return singleValidationError("error", "barcode", "some-barcode");
  }
}
