package org.folio.circulation.domain.anonymization.service;

import static org.folio.circulation.support.http.client.CqlQuery.exactMatch;
import static org.folio.circulation.support.http.client.PageLimit.limit;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.MultipleRecords;
import org.folio.circulation.infrastructure.storage.feesandfines.AccountRepository;
import org.folio.circulation.infrastructure.storage.loans.LoanRepository;
import org.folio.circulation.support.fetching.PageableFetcher;
import org.folio.circulation.support.http.client.CqlQuery;
import org.folio.circulation.support.results.Result;

public class LoansForTenantFinder extends DefaultLoansFinder {
  private final LoanRepository loanRepository;

  public LoansForTenantFinder(LoanRepository loanRepository,
    AccountRepository accountRepository) {

    super(accountRepository);
    this.loanRepository = loanRepository;
  }

  public CompletableFuture<Result<Collection<Loan>>> findLoansToAnonymize() {
    final var fetcher = new PageableFetcher<>(loanRepository::getMany, limit(1), 5000);

    final var cqlQuery = exactMatch("status.name", "Closed")
      .combine(CqlQuery.hasValue(LoanRepository.USER_ID), CqlQuery::and);

    AtomicReference<Collection<Loan>> accumulatedLoans = new AtomicReference<>(List.of());

    return cqlQuery.after(query -> fetcher.processPages(query, page -> accumulateLoans(page, accumulatedLoans)))
      .thenApply(r -> r.map(x -> accumulatedLoans.get()));
  }

  private CompletableFuture<Result<Void>> accumulateLoans(
    MultipleRecords<Loan> page,
    AtomicReference<Collection<Loan>> accumulatedLoans) {

    return accountRepository.findAccountsForLoans(page)
      .thenApply(r -> r.map(loans -> {
        accumulatedLoans.accumulateAndGet(loans.getRecords(), (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()));
        return null;
      }));
  }

}
