package org.folio.circulation.domain.policy.library;

import static org.folio.circulation.domain.policy.library.ClosedLibraryStrategyUtils.determineClosedLibraryStrategy;
import static org.folio.circulation.support.results.Result.succeeded;
import static org.folio.circulation.support.results.ResultBinding.mapResult;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.folio.circulation.AdjacentOpeningDays;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.LoanAndRelatedRecords;
import org.folio.circulation.domain.policy.LoanPolicy;
import org.folio.circulation.infrastructure.storage.CalendarRepository;
import org.folio.circulation.resources.context.RenewalContext;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.results.Result;

public class ClosedLibraryStrategyService {

  public static ClosedLibraryStrategyService using(
    Clients clients, ZonedDateTime currentTime, boolean isRenewal) {
    return new ClosedLibraryStrategyService(new CalendarRepository(clients), currentTime, isRenewal);
  }

  private final CalendarRepository calendarRepository;
  private final ZonedDateTime currentDateTime;
  private final boolean isRenewal;

  public ClosedLibraryStrategyService(
    CalendarRepository calendarRepository, ZonedDateTime currentDateTime, boolean isRenewal) {
    this.calendarRepository = calendarRepository;
    this.currentDateTime = currentDateTime;
    this.isRenewal = isRenewal;
  }

  public CompletableFuture<Result<LoanAndRelatedRecords>> applyClosedLibraryDueDateManagement(
    LoanAndRelatedRecords relatedRecords) {

    final Loan loan = relatedRecords.getLoan();

    return applyClosedLibraryDueDateManagement(loan, loan.getLoanPolicy(),
      relatedRecords.getTimeZone())
      .thenApply(mapResult(loan::changeDueDate))
      .thenApply(mapResult(relatedRecords::withLoan));
  }

  public CompletableFuture<Result<RenewalContext>> applyClosedLibraryDueDateManagement(
    RenewalContext renewalContext) {

    final Loan loan = renewalContext.getLoan();
    return applyClosedLibraryDueDateManagement(loan, loan.getLoanPolicy(),
      renewalContext.getTimeZone())
      .thenApply(mapResult(loan::changeDueDate))
      .thenApply(mapResult(renewalContext::withLoan));
  }

  private CompletableFuture<Result<ZonedDateTime>> applyClosedLibraryDueDateManagement(
    Loan loan, LoanPolicy loanPolicy, ZoneId zone) {

    LocalDate requestedDate = loan.getDueDate().withZoneSameInstant(zone).toLocalDate();
    return calendarRepository.lookupOpeningDays(requestedDate, loan.getCheckoutServicePointId())
      .thenApply(r -> r.next(openingDays -> applyStrategy(loan, loanPolicy, openingDays, zone)));
  }

  private Result<ZonedDateTime> applyStrategy(
    Loan loan, LoanPolicy loanPolicy, AdjacentOpeningDays openingDays, ZoneId zone) {
    ZonedDateTime initialDueDate = loan.getDueDate();

    ClosedLibraryStrategy strategy = determineClosedLibraryStrategy(loanPolicy, currentDateTime, zone);

    return strategy.calculateDueDate(initialDueDate, openingDays)
      .next(dateTime -> applyFixedDueDateLimit(dateTime, loan, loanPolicy, openingDays, zone));
  }

  private Result<ZonedDateTime> applyFixedDueDateLimit(
    ZonedDateTime dueDate, Loan loan, LoanPolicy loanPolicy, AdjacentOpeningDays openingDays,
    ZoneId zone) {

    Optional<ZonedDateTime> optionalDueDateLimit =
      loanPolicy.getScheduleLimit(loan.getLoanDate(), isRenewal, currentDateTime);
    if (!optionalDueDateLimit.isPresent()) {
      return succeeded(dueDate);
    }

    ZonedDateTime dueDateLimit = optionalDueDateLimit.get();
    Comparator<ZonedDateTime> dateComparator =
      Comparator.comparing(dateTime -> dateTime.withZoneSameInstant(zone).toLocalDate());
    if (dateComparator.compare(dueDate, dueDateLimit) <= 0) {
      return succeeded(dueDate);
    }

    ClosedLibraryStrategy strategy =
      ClosedLibraryStrategyUtils.determineStrategyForMovingBackward(
        loanPolicy, currentDateTime, zone);
    return strategy.calculateDueDate(dueDateLimit, openingDays);
  }
}
