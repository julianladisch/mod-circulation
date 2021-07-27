package org.folio.circulation.domain.anonymization.service;

import static org.folio.circulation.domain.anonymization.LoanAnonymizationRecords.CAN_BE_ANONYMIZED_KEY;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.folio.circulation.Clock;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.anonymization.checkers.AnonymizationChecker;
import org.folio.circulation.domain.anonymization.checkers.AnonymizeLoansImmediatelyChecker;
import org.folio.circulation.domain.anonymization.checkers.AnonymizeLoansWithFeeFinesImmediatelyChecker;
import org.folio.circulation.domain.anonymization.checkers.FeesAndFinesClosePeriodChecker;
import org.folio.circulation.domain.anonymization.checkers.LoanClosePeriodChecker;
import org.folio.circulation.domain.anonymization.checkers.NeverAnonymizeLoansChecker;
import org.folio.circulation.domain.anonymization.checkers.NeverAnonymizeLoansWithFeeFinesChecker;
import org.folio.circulation.domain.anonymization.checkers.NoAssociatedFeesAndFinesChecker;
import org.folio.circulation.domain.anonymization.config.LoanAnonymizationConfiguration;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access=AccessLevel.PRIVATE)
public class AnonymizationCheckersService {
  private final boolean neverAnonymizeAnyLoans;
  private final AnonymizationChecker loansWithoutFeesChecker;
  private final AnonymizationChecker loansWithFeesChecker;

  public static AnonymizationCheckersService manual() {
    return new AnonymizationCheckersService(false,
      new NoAssociatedFeesAndFinesChecker(), new NoAssociatedFeesAndFinesChecker());
  }

  public static AnonymizationCheckersService scheduled(LoanAnonymizationConfiguration config, Clock clock) {
    if (config == null) {
      throw new IllegalArgumentException("Loan anonymization configuration cannot be null");
    }

    return new AnonymizationCheckersService(config.neverAnonymizeAnyLoans(),
      getClosedLoansCheckersFromLoanHistory(config, clock),
      getFeesAndFinesCheckersFromLoanHistory(config, clock));
  }

  public boolean neverAnonymizeAnyLoans() {
    return neverAnonymizeAnyLoans;
  }

  public Map<String, Set<String>> segregateLoans(Collection<Loan> loans) {
    return loans.stream()
      .collect(Collectors.groupingBy(applyCheckersForLoanAndLoanHistoryConfig(),
        Collectors.mapping(Loan::getId, Collectors.toSet())));
  }

  private Function<Loan, String> applyCheckersForLoanAndLoanHistoryConfig() {
    return loan -> {
      final var checker = checkerForLoan(loan);

      if (!checker.canBeAnonymized(loan)) {
        return checker.getReason();
      } else {
        return CAN_BE_ANONYMIZED_KEY;
      }
    };
  }

  private AnonymizationChecker checkerForLoan(Loan loan) {
    if (loan.hasAssociatedFeesAndFines()) {
      return loansWithFeesChecker;
    } else {
      return loansWithoutFeesChecker;
    }
  }

  private static AnonymizationChecker getClosedLoansCheckersFromLoanHistory(
    LoanAnonymizationConfiguration config, Clock clock) {

    switch (config.getLoanClosingType()) {
      case IMMEDIATELY:
        return new AnonymizeLoansImmediatelyChecker();
      case INTERVAL:
        return new LoanClosePeriodChecker(config.getLoanClosePeriod(), clock);
      case UNKNOWN:
      case NEVER:
        return new NeverAnonymizeLoansChecker();
      default:
        return null;
    }
  }

  private static AnonymizationChecker getFeesAndFinesCheckersFromLoanHistory(
    LoanAnonymizationConfiguration config, Clock clock) {

    if (!config.treatLoansWithFeesAndFinesDifferently()) {
      return getClosedLoansCheckersFromLoanHistory(config, clock);
    }

    switch (config.getFeesAndFinesClosingType()) {
      case IMMEDIATELY:
        return new AnonymizeLoansWithFeeFinesImmediatelyChecker();
      case INTERVAL:
        return new FeesAndFinesClosePeriodChecker(config.getFeeFineClosePeriod(), clock);
      case UNKNOWN:
      case NEVER:
        return new NeverAnonymizeLoansWithFeeFinesChecker();
      default:
        return null;
    }
  }
}
