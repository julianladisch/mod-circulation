package api.loans.scenarios;

import java.time.ZonedDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import api.support.fixtures.AgeToLostFixture;
import api.support.fixtures.OverrideRenewalFixture;

public class OverrideRenewAgedToLostItemTest extends RefundAgedToLostFeesTestBase {
  @Autowired
  private OverrideRenewalFixture overrideRenewalFixture;

  public OverrideRenewAgedToLostItemTest() {
    super("Cancelled item renewed");
  }

  @Override
  protected void performActionThatRequiresRefund(AgeToLostFixture.AgeToLostResult result,
     ZonedDateTime actionDate) {

    clockToFixedDateTime(actionDate);

    overrideRenewalFixture.overrideRenewalByBarcode(result.getLoan(),
      servicePointsFixture.cd1().getId());
  }
}
