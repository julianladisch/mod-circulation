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
import org.folio.circulation.domain.anonymization.config.ClosingType;
import org.folio.circulation.domain.anonymization.config.LoanAnonymizationConfiguration;

public class AnonymizationCheckersService {
  private final LoanAnonymizationConfiguration config;
  private final AnonymizationChecker manualChecker;
  private AnonymizationChecker feesAndFinesCheckersFromLoanHistory;
  private AnonymizationChecker closedLoansCheckersFromLoanHistory;

  public static AnonymizationCheckersService manual(Clock clock) {
    return new AnonymizationCheckersService(null, clock,
      getManualAnonymizationChecker());
  }

  public static AnonymizationCheckersService scheduled(LoanAnonymizationConfiguration config, Clock clock) {
    return new AnonymizationCheckersService(config, clock, null);
  }

  private AnonymizationCheckersService(LoanAnonymizationConfiguration config,
    Clock clock, AnonymizationChecker manualChecker) {
    this.config = config;
    this.manualChecker = manualChecker;

    if ( config != null) {
      feesAndFinesCheckersFromLoanHistory = getFeesAndFinesCheckersFromLoanHistory(
        config, clock);
      closedLoansCheckersFromLoanHistory = getClosedLoansCheckersFromLoanHistory(
        config, clock);
    }
  }

  public boolean neverAnonymizeLoans() {
    // Without config, this cannot be determined
    if (config == null) {
      return false;
    }

    return config.getLoanClosingType() == ClosingType.NEVER &&
      !config.treatLoansWithFeesAndFinesDifferently();
  }

  public Map<String, Set<String>> segregateLoans(Collection<Loan> loans) {
    return loans.stream()
      .collect(Collectors.groupingBy(applyCheckersForLoanAndLoanHistoryConfig(),
        Collectors.mapping(Loan::getId, Collectors.toSet())));
  }

  private Function<Loan, String> applyCheckersForLoanAndLoanHistoryConfig() {
    return loan -> {
      AnonymizationChecker checker;
      if (config == null) {
        checker = manualChecker;
      } else if (loan.hasAssociatedFeesAndFines() && config.treatLoansWithFeesAndFinesDifferently()) {
        checker = feesAndFinesCheckersFromLoanHistory;
      } else {
        checker = closedLoansCheckersFromLoanHistory;
      }

      if (!checker.canBeAnonymized(loan)) {
        return checker.getReason();
      } else {
        return CAN_BE_ANONYMIZED_KEY;
      }
    };
  }

  private static AnonymizationChecker getManualAnonymizationChecker() {
    return new NoAssociatedFeesAndFinesChecker();
  }

  private static AnonymizationChecker getClosedLoansCheckersFromLoanHistory(
    LoanAnonymizationConfiguration config, Clock clock) {

    if (config == null) {
      return null;
    }

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

    if (config == null) {
      return null;
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
