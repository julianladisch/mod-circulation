package org.folio.circulation.resources.renewal;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.circulation.domain.ItemStatus.CHECKED_OUT;
import static org.folio.circulation.resources.RenewalValidator.errorForDueDate;
import static org.folio.circulation.resources.RenewalValidator.errorForNotMatchingOverrideCases;
import static org.folio.circulation.resources.RenewalValidator.errorWhenEarlierOrSameDueDate;
import static org.folio.circulation.resources.RenewalValidator.overrideDueDateIsRequiredError;
import static org.folio.circulation.support.ValidationErrorFailure.failedValidation;
import static org.folio.circulation.support.json.JsonPropertyFetcher.getDateTimeProperty;
import static org.folio.circulation.support.json.JsonPropertyFetcher.getProperty;
import static org.folio.circulation.support.results.CommonFailures.failedDueToServerError;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.folio.circulation.support.results.ResultBinding.mapResult;
import static org.folio.circulation.support.utils.DateTimeUtil.isAfterMillis;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.RequestType;
import org.folio.circulation.domain.policy.LoanPolicy;
import org.folio.circulation.resources.context.RenewalContext;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.ClockManager;
import org.folio.circulation.support.results.Result;

import io.vertx.core.json.JsonObject;

public class OverrideRenewalStrategy implements RenewalStrategy {

  private static final String COMMENT = "comment";
  private static final String DUE_DATE = "dueDate";

  @Override
  public CompletableFuture<Result<RenewalContext>> renew(RenewalContext context,
    Clients clients) {

    final JsonObject requestBody = context.getRenewalRequest();
    final String comment = getProperty(requestBody, COMMENT);
    if (StringUtils.isBlank(comment)) {
      return completedFuture(failedValidation("Override renewal request must have a comment",
        COMMENT, null));
    }
    final ZonedDateTime overrideDueDate = getDateTimeProperty(requestBody, DUE_DATE);

    Loan loan = context.getLoan();
    boolean hasRecallRequest =
    context.getRequestQueue().getRequests().stream().findFirst()
      .map(r -> r.getRequestType() == RequestType.RECALL)
      .orElse(false);

    return completedFuture(overrideRenewal(loan, ClockManager.getZonedDateTime(),
      overrideDueDate, comment, hasRecallRequest))
      .thenApply(mapResult(context::withLoan));
  }

  private Result<Loan> overrideRenewal(Loan loan, ZonedDateTime systemDate,
    ZonedDateTime overrideDueDate, String comment, boolean hasRecallRequest) {

    try {
      final LoanPolicy loanPolicy = loan.getLoanPolicy();

      if (loanPolicy.isNotLoanable() || loanPolicy.isNotRenewable()) {
        return overrideRenewalForDueDate(loan, overrideDueDate, comment);
      }

      if (unableToCalculateProposedDueDate(loan, systemDate)) {
        return overrideRenewalForDueDate(loan, overrideDueDate, comment);
      }

      final Result<ZonedDateTime> newDueDateResult = calculateNewDueDate(overrideDueDate, loan, systemDate);

      if (loanPolicy.hasReachedRenewalLimit(loan)) {
        return processRenewal(newDueDateResult, loan, comment);
      }

      if (hasRecallRequest) {
        return processRenewal(newDueDateResult, loan, comment);
      }

      if (loan.isItemLost()) {
        return processRenewal(newDueDateResult, loan, comment);
      }

      if (proposedDueDateIsSameOrEarlier(loan, systemDate)) {
        return processRenewal(newDueDateResult, loan, comment);
      }

      return failedValidation(errorForNotMatchingOverrideCases(loanPolicy));

    } catch (Exception e) {
      return failedDueToServerError(e);
    }
  }

  private Result<Loan> overrideRenewalForDueDate(Loan loan, ZonedDateTime overrideDueDate, String comment) {
    if (overrideDueDate == null) {
      return failedValidation(errorForDueDate());
    }
    return succeeded(overrideRenewLoan(overrideDueDate, loan, comment));
  }

  private Result<Loan> processRenewal(Result<ZonedDateTime> calculatedDueDate, Loan loan, String comment) {
    return calculatedDueDate
      .next(dueDate -> {
        return errorWhenEarlierOrSameDueDate(loan, dueDate);
      })
      .map(dueDate -> overrideRenewLoan(dueDate, loan, comment));
  }

  private Loan overrideRenewLoan(ZonedDateTime dueDate, Loan loan, String comment) {
    if (loan.isAgedToLost()) {
      loan.removeAgedToLostBillingInfo();
    }

    return loan.overrideRenewal(dueDate, loan.getLoanPolicyId(), comment)
      .changeItemStatusForItemAndLoan(CHECKED_OUT);
  }

  private Result<ZonedDateTime> calculateNewDueDate(ZonedDateTime overrideDueDate, Loan loan, ZonedDateTime systemDate) {
    final Result<ZonedDateTime> proposedDateTimeResult = calculateProposedDueDate(loan, systemDate);

    if (newDueDateAfterCurrentDueDate(loan, proposedDateTimeResult)) {
      return proposedDateTimeResult;
    }

    if (overrideDueDate == null && proposedDueDateIsSameOrEarlier(loan, systemDate)) {
      return failedValidation(overrideDueDateIsRequiredError());
    }

    return succeeded(overrideDueDate);
  }

  private Result<ZonedDateTime> calculateProposedDueDate(Loan loan, ZonedDateTime systemDate) {
    return loan.getLoanPolicy()
      .determineStrategy(null, true, false, systemDate).calculateDueDate(loan);
  }

  private boolean newDueDateAfterCurrentDueDate(Loan loan, Result<ZonedDateTime> proposedDueDateResult) {
    return proposedDueDateResult.map(proposedDueDate ->
      isAfterMillis(proposedDueDate, loan.getDueDate())).orElse(false);
  }

  private boolean unableToCalculateProposedDueDate(Loan loan, ZonedDateTime systemDate) {
    return calculateProposedDueDate(loan, systemDate).failed();
  }

  private boolean proposedDueDateIsSameOrEarlier(Loan loan, ZonedDateTime systemDate) {
    return !newDueDateAfterCurrentDueDate(loan, calculateProposedDueDate(loan, systemDate));
  }
}
