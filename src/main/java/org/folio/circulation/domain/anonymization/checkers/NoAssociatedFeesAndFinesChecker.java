package org.folio.circulation.domain.anonymization.checkers;

import static org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker.CanBeAnonymizedDecision.CAN_BE_ANONYMIZED;
import static org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker.CanBeAnonymizedDecision.HAS_FEES_FINES;

import org.folio.circulation.domain.Loan;

public class NoAssociatedFeesAndFinesChecker implements AnonymizationChecker {
  @Override
  public boolean canBeAnonymized(Loan loan) {
    return !loan.hasAssociatedFeesAndFines();
  }

  @Override
  public CanBeAnonymizedDecision canBeAnonymizedEnum(Loan loan) {
    return canBeAnonymized(loan)
      ? CAN_BE_ANONYMIZED
      : HAS_FEES_FINES;
  }

  @Override
  public String getReason() {
    return "haveAssociatedFeesAndFines";
  }
}
