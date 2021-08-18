package api.requests.scenarios;
import static api.support.matchers.JsonObjectMatcher.hasJsonPath;
import static api.support.matchers.JsonObjectMatcher.hasNoJsonPath;
import static api.support.matchers.LoanHistoryMatcher.hasLoanHistoryRecord;
import static org.folio.circulation.support.utils.DateTimeUtil.formatDateTimeOptional;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;

import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.ClockManager;
import org.junit.jupiter.api.Test;

import api.support.APITests;
import api.support.builders.LoanPolicyBuilder;
import lombok.val;

public class RecallItemsTests extends APITests {
  public RecallItemsTests() {
    super(true,true);
  }

  @Test
  public void loanActionCommentIsRemovedOnRecall() {
    // using non renewable loan policy just to be able to specify action comment
    // on override renew
    use(new LoanPolicyBuilder().withName("loanActionCommentIsRemovedOnRecall")
      .rolling(Period.weeks(3)).notRenewable().renewFromSystemDate());

    val overrideRenewComment = "Override renew";
    final String newDueDate = formatDateTimeOptional(ClockManager.getZonedDateTime().plusMonths(3));

    val item = itemsFixture.basedUponNod();
    val user = usersFixture.james();
    val loan = checkOutFixture.checkOutByBarcode(item, user);

    loansFixture.overrideRenewalByBarcode(item, user, overrideRenewComment, newDueDate);

    requestsFixture.recallItem(item, usersFixture.charlotte());

    assertThat(loan, hasLoanHistoryRecord(allOf(
      hasJsonPath("loan.action", "recallrequested"),
      hasNoJsonPath("loan.actionComment")
    )));
  }
}
