package org.folio.circulation.domain;

import java.time.ZoneId;
import java.time.ZoneOffset;

import io.vertx.core.json.JsonObject;

public class LoanAndRelatedRecords implements UserRelatedRecord {
  public static final String REASON_TO_OVERRIDE = "reasonToOverride";

  private final Loan loan;
  private final RequestQueue requestQueue;
  private final ZoneId timeZone;
  private final JsonObject logContextProperties;

  private LoanAndRelatedRecords(Loan loan, RequestQueue requestQueue, ZoneId timeZone, JsonObject logContextProperties) {
    this.loan = loan;
    this.requestQueue = requestQueue;
    this.timeZone = timeZone;
    this.logContextProperties = logContextProperties;
  }

  public LoanAndRelatedRecords(Loan loan) {
    this(loan, ZoneOffset.UTC);
  }

  public LoanAndRelatedRecords(Loan loan, ZoneId timeZone) {
    this(loan, null, timeZone, new JsonObject());
  }

  public LoanAndRelatedRecords changeItemStatus(ItemStatus status) {
    return withItem(getItem().changeStatus(status));
  }


  public LoanAndRelatedRecords withLoan(Loan newLoan) {
    return new LoanAndRelatedRecords(newLoan, requestQueue, timeZone, logContextProperties);
  }

  public LoanAndRelatedRecords withRequestingUser(User newUser) {
    return withLoan(loan.withUser(newUser));
  }

  public LoanAndRelatedRecords withProxyingUser(User newProxy) {
    return withLoan(loan.withProxy(newProxy));
  }

  public LoanAndRelatedRecords withRequestQueue(RequestQueue newRequestQueue) {
    return new LoanAndRelatedRecords(loan, newRequestQueue,
      timeZone, logContextProperties);
  }

  public LoanAndRelatedRecords withItem(Item newItem) {
    return withLoan(loan.withItem(newItem));
  }

  public LoanAndRelatedRecords withItemEffectiveLocationIdAtCheckOut() {
    return withLoan(loan.changeItemEffectiveLocationIdAtCheckOut(getItem().getLocationId()));
  }

  public LoanAndRelatedRecords withTimeZone(ZoneId newTimeZone) {
    return new LoanAndRelatedRecords(loan, requestQueue, newTimeZone, logContextProperties);
  }

  public Loan getLoan() {
    return loan;
  }

  public Item getItem() {
    return getLoan().getItem();
  }

  public RequestQueue getRequestQueue() {
    return requestQueue;
  }

  public User getProxy() {
    return loan.getProxy();
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }

  @Override
  public String getUserId() {
    return loan.getUserId();
  }

  @Override
  public String getProxyUserId() {
    return loan.getProxyUserId();
  }

  public JsonObject getLogContextProperties() {
    return logContextProperties;
  }
}
