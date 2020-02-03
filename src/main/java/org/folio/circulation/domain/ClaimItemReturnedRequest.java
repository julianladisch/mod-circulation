package org.folio.circulation.domain;

import static org.folio.circulation.domain.representations.ClaimItemReturnedProperties.COMMENT;
import static org.folio.circulation.domain.representations.ClaimItemReturnedProperties.ITEM_CLAIMED_RETURNED_DATE;

import org.folio.circulation.domain.loan.LoanClaimedReturned;
import org.folio.circulation.support.http.server.WebContext;
import org.joda.time.DateTime;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ClaimItemReturnedRequest {
  private final String loanId;
  private final String comment;
  private final LoanClaimedReturned loanClaimedReturned;

  private ClaimItemReturnedRequest(String loanId, DateTime dateTime, String comment,
                                   String staffMemberId) {

    this.loanId = loanId;
    this.comment = comment;
    this.loanClaimedReturned = new LoanClaimedReturned(dateTime, staffMemberId);
  }

  public String getLoanId() {
    return loanId;
  }

  public String getComment() {
    return comment;
  }

  public LoanClaimedReturned getLoanClaimedReturned() {
    return loanClaimedReturned;
  }

  public static ClaimItemReturnedRequest from(WebContext webContext) {
    final RoutingContext routingContext = webContext.getRoutingContext();

    final String loanId = routingContext.pathParam("id");
    final JsonObject body = routingContext.getBodyAsJson();

    return new ClaimItemReturnedRequest(
      loanId,
      DateTime.parse(body.getString(ITEM_CLAIMED_RETURNED_DATE)),
      body.getString(COMMENT),
      webContext.getUserId());
  }
}
