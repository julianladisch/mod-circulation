package api.support;

import static api.support.fakes.FakePubSub.getPublishedEventsAsList;
import static api.support.fakes.PublishedEvents.byLogEventType;
import static api.support.matchers.EventMatchers.isValidAnonymizeLoansLogRecordEvent;
import static api.support.matchers.EventMatchers.isValidLoanLogRecordEvent;
import static org.folio.circulation.domain.representations.logs.LogEventType.LOAN;
import static org.folio.circulation.domain.representations.logs.LogEventType.NOTICE;
import static org.hamcrest.MatcherAssert.assertThat;

import api.support.matchers.EventMatchers;
import io.vertx.core.json.JsonObject;

public class PubsubPublisherTestUtils {
  private PubsubPublisherTestUtils() { }

  public static void assertThatPublishedLoanLogRecordEventsAreValid(JsonObject loan) {
    getPublishedEventsAsList(byLogEventType(LOAN.value())).forEach(event ->
        assertThat(event, isValidLoanLogRecordEvent(loan))
    );
  }

  public static void assertThatPublishedAnonymizeLoanLogRecordEventsAreValid(JsonObject loan) {
    getPublishedEventsAsList(byLogEventType(LOAN.value())).forEach(event ->
      assertThat(event, isValidAnonymizeLoansLogRecordEvent(loan))
    );
  }

  public static void assertThatPublishedLogRecordEventsAreValid() {
    getPublishedEventsAsList(byLogEventType(NOTICE.value())).forEach(EventMatchers::isValidNoticeLogRecordEvent);
  }
}
