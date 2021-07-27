package org.folio.circulation.domain.anonymization.checkers;

import static org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker.CanBeAnonymizedDecision.CAN_BE_ANONYMIZED;
import static org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker.CanBeAnonymizedDecision.LOAN_CLOSED_TOO_RECENTLY;

import org.folio.circulation.Clock;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.policy.Period;
import org.joda.time.DateTime;

public class LoanClosePeriodChecker implements AnonymizationChecker {
  private final Period period;
  private final Clock clock;

  public LoanClosePeriodChecker(Period period, Clock clock) {
    this.period = period;
    this.clock = clock;
  }

  @Override
  public boolean canBeAnonymized(Loan loan) {
    return loan.isClosed() && itemReturnedEarlierThanPeriod(loan.getSystemReturnDate());
  }

  @Override
  public CanBeAnonymizedDecision canBeAnonymizedEnum(Loan loan) {
    return canBeAnonymized(loan)
      ? CAN_BE_ANONYMIZED
      : LOAN_CLOSED_TOO_RECENTLY;
  }

  @Override
  public String getReason() {
    return "loanClosedPeriodNotPassed";
  }

  private boolean itemReturnedEarlierThanPeriod(DateTime returnDate) {
    if (returnDate == null) {
      return false;
    }

    return clock.now().isAfter(returnDate.plus(period.timePeriod()));
  }
}
