package org.folio.circulation.domain.anonymization.checkers;

import static org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker.CanBeAnonymizedDecision.CAN_BE_ANONYMIZED;
import static org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker.CanBeAnonymizedDecision.LOAN_IS_OPEN;

import org.folio.circulation.domain.Loan;

public class AnonymizeLoansImmediatelyChecker implements AnonymizationChecker {
  @Override
  public boolean canBeAnonymized(Loan loan) {
    return loan.isClosed();
  }

  @Override
  public CanBeAnonymizedDecision canBeAnonymizedEnum(Loan loan) {
    return canBeAnonymized(loan)
      ? CAN_BE_ANONYMIZED
      : LOAN_IS_OPEN;
  }

  @Override
  public String getReason() { return "anonymizeImmediately"; }
}
