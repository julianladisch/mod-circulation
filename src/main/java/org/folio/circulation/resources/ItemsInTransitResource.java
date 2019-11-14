package org.folio.circulation.resources;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.circulation.domain.ItemStatus.IN_TRANSIT;
import static org.folio.circulation.support.CqlQuery.exactMatch;
import static org.folio.circulation.support.CqlQuery.exactMatchAny;
import static org.folio.circulation.support.CqlSortBy.ascending;
import static org.folio.circulation.support.Result.of;
import static org.folio.circulation.support.Result.succeeded;

import io.vertx.core.http.HttpClient;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.folio.circulation.domain.InTransitReportEntry;
import org.folio.circulation.domain.Item;
import org.folio.circulation.domain.ItemsReportFetcher;
import org.folio.circulation.domain.Loan;
import org.folio.circulation.domain.MultipleRecords;
import org.folio.circulation.domain.PatronGroupRepository;
import org.folio.circulation.domain.ReportRepository;
import org.folio.circulation.domain.Request;
import org.folio.circulation.domain.RequestRepository;
import org.folio.circulation.domain.RequestStatus;
import org.folio.circulation.domain.ServicePointRepository;
import org.folio.circulation.domain.UserRepository;
import org.folio.circulation.domain.representations.ItemReportRepresentation;
import org.folio.circulation.support.Clients;
import org.folio.circulation.support.CollectionResourceClient;
import org.folio.circulation.support.CqlQuery;
import org.folio.circulation.support.ItemRepository;
import org.folio.circulation.support.MultipleRecordFetcher;
import org.folio.circulation.support.OkJsonResponseResult;
import org.folio.circulation.support.Result;
import org.folio.circulation.support.RouteRegistration;
import org.folio.circulation.support.http.client.Response;
import org.folio.circulation.support.http.server.WebContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collector;
import java.util.stream.Collectors;


public class ItemsInTransitResource extends Resource {

  private static final String ITEM_ID = "itemId";
  private final String rootPath;

  public ItemsInTransitResource(String rootPath, HttpClient client) {
    super(client);
    this.rootPath = rootPath;
  }

  @Override
  public void register(Router router) {
    RouteRegistration routeRegistration = new RouteRegistration(rootPath, router);
    routeRegistration.getMany(this::getMany);
  }

  private void getMany(RoutingContext routingContext) {
    final WebContext context = new WebContext(routingContext);
    final Clients clients = Clients.create(context, client);

    final CollectionResourceClient loansStorageClient = clients.loansStorage();
    final CollectionResourceClient requestsStorageClient = clients.requestsStorage();
    final ItemRepository itemRepository = new ItemRepository(clients, true, true, true);
    final ServicePointRepository servicePointRepository = new ServicePointRepository(clients);
    final ReportRepository reportRepository = new ReportRepository(clients);
    final UserRepository userRepository = new UserRepository(clients);
    final PatronGroupRepository patronGroupRepository = new PatronGroupRepository(clients);

    reportRepository.getAllItemsByField("status.name", IN_TRANSIT.getValue())
      .thenComposeAsync(r -> r.after(itemsReportFetcher ->
        fetchInTransitReportEntry(itemsReportFetcher, itemRepository, servicePointRepository)))
      .thenComposeAsync(r -> r.after(inTransitReportEntries ->
        fetchLoans(loansStorageClient, servicePointRepository, inTransitReportEntries)))
      .thenComposeAsync(r -> findRequestsByItemsIds(requestsStorageClient, itemRepository,
        servicePointRepository, userRepository, patronGroupRepository, r.value()))
      .thenApply(this::mapResultToJson)
      .thenApply(OkJsonResponseResult::from)
      .thenAccept(r -> r.writeTo(routingContext.response()));
  }

  public CompletableFuture<Result<List<InTransitReportEntry>>> fetchInTransitReportEntry(ItemsReportFetcher itemsReportFetcher,
                                                                                         ItemRepository itemRepository,
                                                                                         ServicePointRepository servicePointRepository) {
    List<InTransitReportEntry> inTransitReportEntries = itemsReportFetcher.getResultListOfItems().stream()
      .flatMap(records -> records.value().getRecords()
        .stream()).map(item -> fetchRelatedRecords(itemRepository, servicePointRepository, item))
      .map(CompletableFuture::join)
      .map(item -> new InTransitReportEntry(item.value()))
      .collect(Collectors.toList());

    return CompletableFuture.completedFuture(Result.succeeded(inTransitReportEntries));
  }

  private CompletableFuture<Result<Item>> fetchRelatedRecords(ItemRepository itemRepository,
                                                              ServicePointRepository servicePointRepository,
                                                              Item item) {
    return CompletableFuture.completedFuture(Result.succeeded(item))
      .thenComposeAsync(itemRepository::fetchItemRelatedRecords)
      .thenComposeAsync(result -> result
        .combineAfter(it -> servicePointRepository
          .getServicePointById(it.getInTransitDestinationServicePointId()), Item::updateDestinationServicePoint));
  }

  private CompletableFuture<Result<List<InTransitReportEntry>>> findRequestsByItemsIds(CollectionResourceClient requestsStorageClient,
                                                                                       ItemRepository itemRepository,
                                                                                       ServicePointRepository servicePointRepository,
                                                                                       UserRepository userRepository,
                                                                                       PatronGroupRepository patronGroupRepository,
                                                                                       List<InTransitReportEntry> inTransitReportEntryList) {

    MultipleRecordFetcher<Request> fetcher = new MultipleRecordFetcher<>(requestsStorageClient, "requests", Request::from);

    final Result<CqlQuery> statusQuery = exactMatchAny("status", RequestStatus.openStates());
    Result<CqlQuery> cqlQueryResult = statusQuery.combine(statusQuery, CqlQuery::and)
      .map(q -> q.sortBy(ascending("position")));

    return fetcher.findByIndexNameAndQuery(mapToItemIdList(inTransitReportEntryList), ITEM_ID, cqlQueryResult)
      .thenComposeAsync(requests ->
        itemRepository.fetchItemsFor(requests, Request::withItem))
      .thenComposeAsync(result -> result.after(servicePointRepository::findServicePointsForRequests))
      .thenComposeAsync(result -> result.after(userRepository::findUsersForRequests))
      .thenComposeAsync(result -> result.after(patronGroupRepository::findPatronGroupsForRequestsUsers))
      .thenComposeAsync(r -> r.after(multipleRecords -> completedFuture(succeeded(
        multipleRecords.getRecords().stream().collect(
          Collectors.groupingBy(Request::getItemId))))))
      .thenComposeAsync(r -> mapRequestToInTransitReportEntry(inTransitReportEntryList, r.value()));
  }

  private CompletableFuture<Result<List<InTransitReportEntry>>> fetchLoans(
    CollectionResourceClient loansStorageClient,
    ServicePointRepository servicePointRepository,
    List<InTransitReportEntry> inTransitReportEntries) {
    final List<String> itemsToFetchLoansFor = inTransitReportEntries.stream()
      .filter(Objects::nonNull)
      .map(inTransitReportEntry -> inTransitReportEntry.getItem().getItemId())
      .filter(Objects::nonNull)
      .distinct()
      .collect(Collectors.toList());

    if (itemsToFetchLoansFor.isEmpty()) {
      return completedFuture(succeeded(inTransitReportEntries));
    }

    final Result<CqlQuery> statusQuery = exactMatch("itemStatus", IN_TRANSIT.getValue());
    final Result<CqlQuery> itemIdQuery = exactMatchAny(ITEM_ID, itemsToFetchLoansFor);

    CompletableFuture<Result<MultipleRecords<Loan>>> multipleRecordsLoans =
      statusQuery.combine(
        itemIdQuery, CqlQuery::and)
        .after(q -> loansStorageClient.getMany(q, inTransitReportEntries.size()))
        .thenApply(result -> result.next(this::mapResponseToLoans));

    return multipleRecordsLoans.thenCompose(multiLoanRecordsResult ->
      multiLoanRecordsResult.after(servicePointRepository::findServicePointsForLoans))
      .thenApply(multipleLoansResult -> multipleLoansResult.next(
        loans -> matchLoansToInTransitReportEntry(inTransitReportEntries, loans)));
  }

  private Result<List<InTransitReportEntry>> matchLoansToInTransitReportEntry(
    List<InTransitReportEntry> inTransitReportEntries,
    MultipleRecords<Loan> loans) {

    return of(() ->
      inTransitReportEntries.stream()
        .map(inTransitReportEntry -> matchLoansToInTransitReportEntry(inTransitReportEntry, loans))
        .collect(Collectors.toList()));
  }

  private InTransitReportEntry matchLoansToInTransitReportEntry(
    InTransitReportEntry inTransitReportEntry,
    MultipleRecords<Loan> loans) {

    final Map<String, Loan> loanMap = loans.toMap(Loan::getItemId);
    inTransitReportEntry
      .setLoan(loanMap.getOrDefault(inTransitReportEntry.getItem().getItemId(), null));
    return inTransitReportEntry;
  }

  private Result<MultipleRecords<Loan>> mapResponseToLoans(Response response) {
    return MultipleRecords.from(response, Loan::from, "loans");
  }

  private CompletableFuture<Result<List<Result<MultipleRecords<Request>>>>> getInTransitRequestByItemsIds(RequestRepository requestRepository,
                                                                                                          List<List<String>> batchItemIds) {
    List<Result<MultipleRecords<Request>>> inTransitRequest = getInTransitRequest(requestRepository, batchItemIds);
    return CompletableFuture.completedFuture(Result.succeeded(inTransitRequest));
  }

  private List<Result<MultipleRecords<Request>>> getInTransitRequest(RequestRepository requestRepository,
                                                                     List<List<String>> batchItemIds) {
    return batchItemIds.stream()
      .map(itemIds -> {
        final Result<CqlQuery> statusQuery = exactMatchAny("status", RequestStatus.openStates());
        final Result<CqlQuery> itemIdsQuery = exactMatchAny(ITEM_ID, itemIds);

        Result<CqlQuery> cqlQueryResult = statusQuery.combine(itemIdsQuery, CqlQuery::and)
          .map(q -> q.sortBy(ascending("position")));

        return requestRepository.findBy(cqlQueryResult, itemIds.size());
      })
      .map(CompletableFuture::join)
      .collect(Collectors.toList());
  }

  private List<String> mapToItemIdList(List<InTransitReportEntry> inTransitReportEntryList) {
    return inTransitReportEntryList.stream()
      .map(InTransitReportEntry::getItem)
      .filter(Objects::nonNull)
      .map(Item::getItemId)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

  }

  private CompletableFuture<Result<List<InTransitReportEntry>>> mapRequestToInTransitReportEntry(
    List<InTransitReportEntry> inTransitReportEntryList,
    Map<String, List<Request>> itemRequestsMap) {

    inTransitReportEntryList.stream().filter(inTransitReportEntry -> itemRequestsMap
      .containsKey(inTransitReportEntry.getItem().getItemId()))
      .forEach(inTransitReportEntry -> inTransitReportEntry.setRequest(itemRequestsMap
        .get(inTransitReportEntry.getItem().getItemId()).stream().findFirst()
        .orElse(null)));

    return CompletableFuture.completedFuture(Result.succeeded(inTransitReportEntryList));
  }

  private Result<JsonObject> mapResultToJson
    (Result<List<InTransitReportEntry>> inTransitReportEntry) {

    return inTransitReportEntry.map(resultList -> resultList
      .stream().map(itemAndRelatedRecord -> new ItemReportRepresentation()
        .createItemReport(itemAndRelatedRecord))
      .collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add)))
      .next(jsonArray -> Result.succeeded(new JsonObject()
        .put("items", jsonArray)
        .put("totalRecords", jsonArray.size())));
  }

}
