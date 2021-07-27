package org.folio.circulation.domain.anonymization.checkers;

import static org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker.CanBeAnonymizedDecision.LOANS_ARE_NEVER_ANONYMIZED;

import org.folio.circulation.domain.Loan;

public class NeverAnonymizeLoansChecker implements AnonymizationChecker {
  @Override
  public boolean canBeAnonymized(Loan loan) {
    return false;
  }


  @Override
  public CanBeAnonymizedDecision canBeAnonymizedEnum(Loan loan) {
    return LOANS_ARE_NEVER_ANONYMIZED;
  }
  @Override
  public String getReason() { return "neverAnonymizeLoans"; }
}
