package org.folio.circulation.resources;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.folio.circulation.domain.*;
import org.folio.circulation.domain.representations.RequestProperties;
import org.folio.circulation.domain.validation.ProxyRelationshipValidator;
import org.folio.circulation.support.*;
import org.folio.circulation.support.http.server.ClientErrorResponse;
import org.folio.circulation.support.http.server.ForwardResponse;
import org.folio.circulation.support.http.server.SuccessResponse;
import org.folio.circulation.support.http.server.WebContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.folio.circulation.domain.ItemStatus.*;
import static org.folio.circulation.support.JsonPropertyWriter.write;
import static org.folio.circulation.support.ValidationErrorFailure.failure;

public class RequestCollectionResource extends CollectionResource {
  public RequestCollectionResource(HttpClient client) {
    super(client, "/circulation/requests");
  }

  void create(RoutingContext routingContext) {
    final WebContext context = new WebContext(routingContext);

    JsonObject representation = routingContext.getBodyAsJson();

    RequestStatus status = RequestStatus.from(representation);

    HttpServerResponse response = routingContext.response();
    if(!status.isValid()) {
      ClientErrorResponse.badRequest(response,
        RequestStatus.invalidStatusErrorMessage());
      return;
    }
    else {
      status.writeTo(representation);
    }

    removeRelatedRecordInformation(representation);

    final Request request = new Request(representation);

    final Clients clients = Clients.create(context, client);

    final ItemRepository itemRepository = new ItemRepository(clients, true, false);
    final RequestQueueFetcher requestQueueFetcher = new RequestQueueFetcher(clients);
    final UserRepository userRepository = new UserRepository(clients);
    final UpdateItem updateItem = new UpdateItem(clients);
    final UpdateLoanActionHistory updateLoanActionHistory = new UpdateLoanActionHistory(clients);
    final ProxyRelationshipValidator proxyRelationshipValidator = new ProxyRelationshipValidator(
      clients, () -> failure(
      "proxyUserId is not valid", RequestProperties.PROXY_USER_ID,
      request.getProxyUserId()));

    completedFuture(HttpResult.succeeded(new RequestAndRelatedRecords(request)))
      .thenCombineAsync(itemRepository.fetchFor(request), this::addInventoryRecords)
      .thenApply(this::refuseWhenItemDoesNotExist)
      .thenApply(this::refuseWhenItemIsNotValid)
      .thenComposeAsync(r -> r.after(proxyRelationshipValidator::refuseWhenInvalid))
      .thenCombineAsync(requestQueueFetcher.get(request.getItemId()), this::addRequestQueue)
      .thenCombineAsync(userRepository.getUser(request.getUserId(), false), this::addUser)
      .thenCombineAsync(userRepository.getUser(request.getProxyUserId(), false), this::addProxyUser)
      .thenComposeAsync(r -> r.after(updateItem::onRequestCreation))
      .thenComposeAsync(r -> r.after(updateLoanActionHistory::onRequestCreation))
      .thenComposeAsync(r -> r.after(records -> createRequest(records, clients)))
      .thenApply(r -> r.map(this::extendedRequest))
      .thenApply(CreatedJsonHttpResult::from)
      .thenAccept(result -> result.writeTo(routingContext.response()));
  }

  void replace(RoutingContext routingContext) {
    final WebContext context = new WebContext(routingContext);

    String id = routingContext.request().getParam("id");
    JsonObject representation = routingContext.getBodyAsJson();

    removeRelatedRecordInformation(representation);

    final Request request = new Request(representation);

    final Clients clients = Clients.create(context, client);

    final ItemRepository itemRepository = new ItemRepository(clients, false, false);
    final UserRepository userRepository = new UserRepository(clients);

    final ProxyRelationshipValidator proxyRelationshipValidator = new ProxyRelationshipValidator(
      clients, () -> failure(
      "proxyUserId is not valid", RequestProperties.PROXY_USER_ID,
      request.getProxyUserId()));

    completedFuture(HttpResult.succeeded(new RequestAndRelatedRecords(request)))
      .thenCombineAsync(itemRepository.fetchFor(request), this::addInventoryRecords)
      .thenCombineAsync(userRepository.getUser(request.getUserId(), false), this::addUser)
      .thenCombineAsync(userRepository.getUser(request.getProxyUserId(), false), this::addProxyUser)
      .thenComposeAsync(r -> r.after(proxyRelationshipValidator::refuseWhenInvalid))
      .thenAcceptAsync(result -> {
        if(result.failed()) {
          result.cause().writeTo(routingContext.response());
          return;
        }

        final Item item = result.value().getInventoryRecords();
        final User requester = result.value().getRequestingUser();
        final User proxy = result.value().getProxyUser();

        addStoredItemProperties(representation, item);
        addStoredRequesterProperties(representation, requester);
        addStoredProxyProperties(representation, proxy);

        clients.requestsStorage().put(id, representation, response -> {
          if(response.getStatusCode() == 204) {
            SuccessResponse.noContent(routingContext.response());
          }
          else {
            ForwardResponse.forward(routingContext.response(), response);
          }
        });
      });
  }

  void get(RoutingContext routingContext) {
    WebContext context = new WebContext(routingContext);
    Clients clients = Clients.create(context, client);

    String id = routingContext.request().getParam("id");

    clients.requestsStorage().get(id)
      .thenAccept(requestResponse -> {
        if(requestResponse.getStatusCode() == 200) {
          Request request = new Request(requestResponse.getJson());

          ItemRepository itemRepository = new ItemRepository(clients, true, false);

          CompletableFuture<HttpResult<Item>> inventoryRecordsCompleted =
            itemRepository.fetchFor(request);

          inventoryRecordsCompleted.thenAccept(r -> {
            if(r.failed()) {
              r.cause().writeTo(routingContext.response());
              return;
            }

            final JsonObject representation = request.asJson();

            addAdditionalItemProperties(representation, r.value());

            new OkJsonHttpResult(representation)
              .writeTo(routingContext.response());
          });
        }
        else {
          ForwardResponse.forward(routingContext.response(), requestResponse);
        }
      });
  }

  void delete(RoutingContext routingContext) {
    WebContext context = new WebContext(routingContext);
    Clients clients = Clients.create(context, client);

    String id = routingContext.request().getParam("id");

    clients.requestsStorage().delete(id, response -> {
      if(response.getStatusCode() == 204) {
        SuccessResponse.noContent(routingContext.response());
      }
      else {
        ForwardResponse.forward(routingContext.response(), response);
      }
    });
  }

  void getMany(RoutingContext routingContext) {
    WebContext context = new WebContext(routingContext);
    Clients clients = Clients.create(context, client);

    final RequestRepository requestRepository = RequestRepository.using(clients);

    requestRepository.findBy(routingContext.request().query())
      .thenAccept(requestsResult -> {
          if (requestsResult.failed()) {
            requestsResult.cause().writeTo(routingContext.response());
          }

        final MultipleRecords<Request> requests = requestsResult.value();

        final List<JsonObject> mappedRequests = requests.getRecords().stream()
          .map(request -> {
            final JsonObject requestRepresentation = request.asJson();
            addAdditionalItemProperties(requestRepresentation, request.getItem());

            return requestRepresentation;

          }).collect(Collectors.toList());

        new OkJsonHttpResult(
          new MultipleRecordsWrapper(mappedRequests, "requests", requests.getTotalRecords())
            .toJson()).writeTo(routingContext.response());
      });
  }

  void empty(RoutingContext routingContext) {
    WebContext context = new WebContext(routingContext);
    Clients clients = Clients.create(context, client);

    clients.requestsStorage().delete(response -> {
      if(response.getStatusCode() == 204) {
        SuccessResponse.noContent(routingContext.response());
      }
      else {
        ForwardResponse.forward(routingContext.response(), response);
      }
    });
  }

  private void addStoredItemProperties(
    JsonObject request,
    Item item) {

    if(item == null || item.isNotFound()) {
      return;
    }

    JsonObject itemSummary = new JsonObject();

    write(itemSummary, "title", item.getTitle());
    write(itemSummary, "barcode", item.getBarcode());

    request.put("item", itemSummary);
  }

  private void addAdditionalItemProperties(
    JsonObject request,
    Item item) {

    if(item == null || item.isNotFound()) {
      return;
    }

    JsonObject itemSummary = request.containsKey("item")
      ? request.getJsonObject("item")
      : new JsonObject();

    write(itemSummary, "holdingsRecordId", item.getHoldingsRecordId());
    write(itemSummary, "instanceId", item.getInstanceId());

    final JsonObject location = item.getLocation();

    if(location != null && location.containsKey("name")) {
      itemSummary.put("location", new JsonObject()
        .put("name", location.getString("name")));
    }

    request.put("item", itemSummary);
  }

  private void addStoredRequesterProperties
    (JsonObject requestWithAdditionalInformation,
     User requester) {

    if(requester == null) {
      return;
    }

    JsonObject requesterSummary = requester.createUserSummary();

    requestWithAdditionalInformation.put("requester", requesterSummary);
  }

  private void addStoredProxyProperties
    (JsonObject requestWithAdditionalInformation,
     User proxy) {

    if(proxy == null) {
      return;
    }

    requestWithAdditionalInformation.put("proxy", proxy.createUserSummary());
  }

  private void removeRelatedRecordInformation(JsonObject request) {
    request.remove("item");
    request.remove("requester");
    request.remove("proxy");
  }

  private HttpResult<RequestAndRelatedRecords> addInventoryRecords(
    HttpResult<RequestAndRelatedRecords> loanResult,
    HttpResult<Item> inventoryRecordsResult) {

    return HttpResult.combine(loanResult, inventoryRecordsResult,
      RequestAndRelatedRecords::withInventoryRecords);
  }

  private HttpResult<RequestAndRelatedRecords> addRequestQueue(
    HttpResult<RequestAndRelatedRecords> loanResult,
    HttpResult<RequestQueue> requestQueueResult) {

    return HttpResult.combine(loanResult, requestQueueResult,
      RequestAndRelatedRecords::withRequestQueue);
  }

  private HttpResult<RequestAndRelatedRecords> addUser(
    HttpResult<RequestAndRelatedRecords> loanResult,
    HttpResult<User> getUserResult) {

    return HttpResult.combine(loanResult, getUserResult,
      RequestAndRelatedRecords::withRequestingUser);
  }

  private HttpResult<RequestAndRelatedRecords> addProxyUser(
    HttpResult<RequestAndRelatedRecords> loanResult,
    HttpResult<User> getUserResult) {

    return HttpResult.combine(loanResult, getUserResult,
      RequestAndRelatedRecords::withProxyUser);
  }

  private HttpResult<RequestAndRelatedRecords> refuseWhenItemDoesNotExist(
    HttpResult<RequestAndRelatedRecords> result) {

    return result.next(requestAndRelatedRecords -> {
      if(requestAndRelatedRecords.getInventoryRecords().isNotFound()) {
        return HttpResult.failed(failure(
          "Item does not exist", "itemId",
          requestAndRelatedRecords.getRequest().getItemId()));
      }
      else {
        return result;
      }
    });
  }

  private HttpResult<RequestAndRelatedRecords> refuseWhenItemIsNotValid(
    HttpResult<RequestAndRelatedRecords> result) {

    return result.next(requestAndRelatedRecords -> {
      Request request = requestAndRelatedRecords.getRequest();

      RequestType requestType = RequestType.from(request);

      if (!requestType.canCreateRequestForItem(requestAndRelatedRecords.getInventoryRecords())) {
        return HttpResult.failed(failure(
          String.format("Item is not %s, %s or %s", CHECKED_OUT,
            CHECKED_OUT_HELD, CHECKED_OUT_RECALLED),
          "itemId", request.getItemId()
        ));
      }
      else {
        return result;
      }
    });
  }

  private CompletableFuture<HttpResult<RequestAndRelatedRecords>> createRequest(
    RequestAndRelatedRecords requestAndRelatedRecords,
    Clients clients) {

    CompletableFuture<HttpResult<RequestAndRelatedRecords>> onCreated = new CompletableFuture<>();

    JsonObject request = requestAndRelatedRecords.getRequest().asJson();

    User requestingUser = requestAndRelatedRecords.getRequestingUser();
    User proxyUser = requestAndRelatedRecords.getProxyUser();

    addStoredItemProperties(request, requestAndRelatedRecords.getInventoryRecords());
    addStoredRequesterProperties(request, requestingUser);
    addStoredProxyProperties(request, proxyUser);

    clients.requestsStorage().post(request, response -> {
      if (response.getStatusCode() == 201) {
        onCreated.complete(HttpResult.succeeded(
          requestAndRelatedRecords.withRequest(new Request(response.getJson()))));
      } else {
        onCreated.complete(HttpResult.failed(new ForwardOnFailure(response)));
      }
    });

    return onCreated;
  }

  private JsonObject extendedRequest(RequestAndRelatedRecords requestAndRelatedRecords) {
    final JsonObject representation = requestAndRelatedRecords.getRequest().asJson();

    addAdditionalItemProperties(representation,
      requestAndRelatedRecords.getInventoryRecords());

    return representation;
  }
}
