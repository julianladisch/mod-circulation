package org.folio.circulation.domain.policy;

import static api.support.matchers.FailureMatcher.hasValidationFailure;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.folio.circulation.support.utils.DateTimeUtil.toDateTimeString;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.support.http.server.ValidationError;
import org.folio.circulation.support.results.Result;
import org.junit.Test;

import api.support.builders.FixedDueDateSchedule;
import api.support.builders.FixedDueDateSchedulesBuilder;
import api.support.builders.LoanBuilder;
import api.support.builders.LoanPolicyBuilder;

public class FixedLoanPolicyCheckOutDueDateCalculationTests {
  @Test
  public void shouldUseOnlyScheduleAvailableWhenLoanDateFits() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeYear(2018))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 3, 14, 11, 14, 54, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> calculationResult = loanPolicy
      .calculateInitialDueDate(loan, null);

    final ZonedDateTime desiredDueDate = ZonedDateTime.of(2018, 12, 31, 23, 59, 59, 0, UTC);

    assertThat(toDateTimeString(calculationResult.value()), is(toDateTimeString(desiredDueDate)));
  }

  @Test
  public void shouldUseOnlyScheduleAvailableWhenLoanDateTimeAfterMidnight() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(new FixedDueDateSchedule(ZonedDateTime.of(2020, 11, 1, 0, 0, 0, 0, UTC),
          ZonedDateTime.of(2020, 11, 2, 0, 0, 0, 0, UTC),
          ZonedDateTime.of(2020, 11, 2, 0, 0, 0, 0, UTC)))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2020, 11, 2, 12, 30, 30, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> calculationResult = loanPolicy
      .calculateInitialDueDate(loan, null);

    final var expectedInitialDueDate = ZonedDateTime.of(2020, 11, 2, 0, 0, 0, 0,
      UTC);

    assertThat(calculationResult.succeeded(), is(true));
    assertThat(toDateTimeString(calculationResult.value()), is(toDateTimeString(expectedInitialDueDate)));
  }

  @Test
  public void shouldUseOnlyScheduleAvailableWhenLoanDateTimeAfterMidnightAndTimeZoneIsNotUTC() {
    ZoneOffset timeZone = ZoneOffset.ofHours(4);
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(new FixedDueDateSchedule(ZonedDateTime.of(2020, 11, 1, 0, 0, 0, 0, timeZone),
          ZonedDateTime.of(2020, 11, 2, 0, 0, 0, 0, timeZone),
          ZonedDateTime.of(2020, 11, 2, 0, 0, 0, 0, timeZone)))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2020, 11, 2, 12, 30, 30, 0, timeZone);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> calculationResult = loanPolicy
      .calculateInitialDueDate(loan, null);
    
    final ZonedDateTime desiredDueDate = ZonedDateTime.of(2020, 11, 2, 0, 0, 0, 0, timeZone);

    assertThat(toDateTimeString(calculationResult.value()), is(toDateTimeString(desiredDueDate)));
  }

  @Test
  public void shouldFailWhenLoanDateIsBeforeOnlyScheduleAvailable() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .withName("Example Fixed Schedule Loan Policy")
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeYear(2018))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2017, 12, 30, 14, 32, 21, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = loanPolicy.calculateInitialDueDate(loan, null);

    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  @Test
  public void shouldFailWhenLoanDateIsAfterOnlyScheduleAvailable() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .withName("Example Fixed Schedule Loan Policy")
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeYear(2018))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2019, 1, 1, 8, 10, 45, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = loanPolicy.calculateInitialDueDate(loan, null);

    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  @Test
  public void shouldUseFirstScheduleAvailableWhenLoanDateFits() {
    final FixedDueDateSchedule expectedSchedule = FixedDueDateSchedule.wholeMonth(2018, 1);

    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(expectedSchedule)
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 2))
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 3))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 1, 8, 11, 14, 54, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> calculationResult = loanPolicy
      .calculateInitialDueDate(loan, null);

    assertThat(toDateTimeString(calculationResult.value()), is(toDateTimeString(expectedSchedule.due)));
  }

  @Test
  public void shouldUseMiddleScheduleAvailableWhenLoanDateFits() {
    final FixedDueDateSchedule expectedSchedule = FixedDueDateSchedule.wholeMonth(2018, 2);

    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 1))
        .addSchedule(expectedSchedule)
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 3))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 2, 27, 16, 23, 43, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> calculationResult = loanPolicy
      .calculateInitialDueDate(loan, null);

    assertThat(toDateTimeString(calculationResult.value()), is(toDateTimeString(expectedSchedule.due)));
  }

  @Test
  public void shouldUseLastScheduleAvailableWhenLoanDateFits() {
    final FixedDueDateSchedule expectedSchedule = FixedDueDateSchedule.wholeMonth(2018, 3);

    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 1))
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 2))
        .addSchedule(expectedSchedule)
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 3, 12, 7, 15, 23, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> calculationResult = loanPolicy
      .calculateInitialDueDate(loan, null);

    assertThat(toDateTimeString(calculationResult.value()), is(toDateTimeString(expectedSchedule.due)));
  }

  @Test
  public void shouldFailWhenLoanDateIsBeforeAllSchedules() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .withName("Example Fixed Schedule Loan Policy")
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 1))
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 2))
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 3))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2017, 12, 30, 14, 32, 21, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = loanPolicy.calculateInitialDueDate(loan, null);

    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  @Test
  public void shouldFailWhenLoanDateIsAfterAllSchedules() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .withName("Example Fixed Schedule Loan Policy")
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 1))
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 2))
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 3))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 4, 1, 6, 34, 21, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = loanPolicy.calculateInitialDueDate(loan, null);

    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  @Test
  public void shouldFailWhenLoanDateIsInBetweenSchedules() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .withName("Example Fixed Schedule Loan Policy")
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder()
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 1))
        .addSchedule(FixedDueDateSchedule.wholeMonth(2018, 3))
        .create());

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 2, 18, 6, 34, 21, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = loanPolicy.calculateInitialDueDate(loan, null);

    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  @Test
  public void shouldFailWhenNoSchedulesDefined() {
    LoanPolicy loanPolicy = LoanPolicy.from(new LoanPolicyBuilder()
      .fixed(UUID.randomUUID())
      .withName("Example Fixed Schedule Loan Policy")
      .create())
      .withDueDateSchedules(new FixedDueDateSchedulesBuilder().create());

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 3, 14, 11, 14, 54, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = loanPolicy.calculateInitialDueDate(loan, null);

    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  @Test
  public void shouldFailWhenSchedulesCollectionIsNull() {
    final FixedScheduleCheckOutDueDateStrategy calculator =
      new FixedScheduleCheckOutDueDateStrategy(UUID.randomUUID().toString(),
        "Example Fixed Schedule Loan Policy", null, s -> new ValidationError(s, null, null));

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 3, 14, 11, 14, 54, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = calculator.calculateDueDate(loan);

    assertThat(result.failed(), is(true));
    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  @Test
  public void shouldFailWhenNoSchedules() {
    final FixedScheduleCheckOutDueDateStrategy calculator =
      new FixedScheduleCheckOutDueDateStrategy(UUID.randomUUID().toString(),
        "Example Fixed Schedule Loan Policy", new NoFixedDueDateSchedules(),
        s -> new ValidationError(s, null, null));

    ZonedDateTime loanDate = ZonedDateTime.of(2018, 3, 14, 11, 14, 54, 0, UTC);

    Loan loan = loanFor(loanDate);

    final Result<ZonedDateTime> result = calculator.calculateDueDate(loan);

    assertThat(result, hasValidationFailure(
      "loan date falls outside of the date ranges in the loan policy"));
  }

  private Loan loanFor(ZonedDateTime loanDate) {
    return new LoanBuilder()
      .open()
      .withLoanDate(loanDate)
      .asDomainObject();
  }
}
