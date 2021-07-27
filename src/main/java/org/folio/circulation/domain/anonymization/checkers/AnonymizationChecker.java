package org.folio.circulation.domain.anonymization.checkers;

import org.folio.circulation.domain.Loan;

public interface AnonymizationChecker {
  boolean canBeAnonymized(Loan loan);
  CanBeAnonymizedDecision canBeAnonymizedEnum(Loan loan);
  String getReason();

  enum CanBeAnonymizedDecision {
    CAN_BE_ANONYMIZED,
    FEES_FINES_CLOSED_TOO_RECENTLY,
    HAS_FEES_FINES,
    HAS_OPEN_FEES_FINES,
    LOANS_ARE_NEVER_ANONYMIZED,
    LOAN_CLOSED_TOO_RECENTLY,
    LOAN_IS_OPEN
  }
}
