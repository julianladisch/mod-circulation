package api.loans;

import static api.support.fakes.PublishedEvents.byLogEventType;
import static api.support.matchers.PatronNoticeMatcher.hasNoticeProperties;
import static api.support.matchers.ScheduledNoticeMatchers.hasScheduledFeeFineNotice;
import static java.util.UUID.randomUUID;
import static org.folio.circulation.domain.notice.NoticeTiming.AFTER;
import static org.folio.circulation.domain.notice.NoticeTiming.UPON_AT;
import static org.folio.circulation.domain.notice.schedule.TriggeringEvent.OVERDUE_FINE_RENEWED;
import static org.folio.circulation.domain.notice.schedule.TriggeringEvent.OVERDUE_FINE_RETURNED;
import static org.folio.circulation.domain.representations.logs.LogEventType.NOTICE;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.folio.circulation.domain.Account;
import org.folio.circulation.domain.FeeFineAction;
import org.folio.circulation.domain.notice.NoticeEventType;
import org.folio.circulation.domain.notice.NoticeTiming;
import org.folio.circulation.domain.notice.schedule.TriggeringEvent;
import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.ClockManager;
import org.folio.circulation.support.json.JsonPropertyWriter;
import org.hamcrest.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import api.support.APITests;
import api.support.builders.CheckInByBarcodeRequestBuilder;
import api.support.builders.FeeFineBuilder;
import api.support.builders.FeeFineOwnerBuilder;
import api.support.builders.NoticeConfigurationBuilder;
import api.support.builders.NoticePolicyBuilder;
import api.support.fakes.FakePubSub;
import api.support.fixtures.TemplateContextMatchers;
import api.support.http.IndividualResource;
import io.vertx.core.json.JsonObject;

public class FeeFineScheduledNoticesTests extends APITests {
  private static final Period AFTER_PERIOD = Period.days(1);
  private static final Period RECURRING_PERIOD = Period.hours(6);
  private static final String OVERDUE_FINE = "Overdue fine";
  private static final Map<NoticeTiming, UUID> TEMPLATE_IDS = new HashMap<>();

  private Account account;
  private FeeFineAction action;
  private ZonedDateTime actionDateTime;
  private UUID loanId;
  private UUID itemId;
  private UUID userId;
  private UUID actionId;
  private UUID accountId;

  static {
    TEMPLATE_IDS.put(UPON_AT, randomUUID());
    TEMPLATE_IDS.put(AFTER, randomUUID());
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void uponAtNoticeIsSentAndDeleted(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, UPON_AT, false));

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);

    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(actionDateTime));

    assertThatNoticesWereSent(TEMPLATE_IDS.get(UPON_AT));
    assertThatNumberOfScheduledNoticesIs(0);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void oneTimeAfterNoticeIsSentAndDeleted(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, AFTER, false));

    ZonedDateTime expectedNextRunTime = AFTER_PERIOD.plusDate(actionDateTime);

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, false, expectedNextRunTime);

    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(expectedNextRunTime));

    assertThatNoticesWereSent(TEMPLATE_IDS.get(AFTER));
    assertThatNumberOfScheduledNoticesIs(0);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void recurringAfterNoticeIsSentAndRescheduled(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, AFTER, true));

    ZonedDateTime expectedFirstRunTime = AFTER_PERIOD.plusDate(actionDateTime);

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, true, expectedFirstRunTime);

    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(expectedFirstRunTime));

    ZonedDateTime expectedSecondRunTime = RECURRING_PERIOD.plusDate(expectedFirstRunTime);

    assertThatNoticesWereSent(TEMPLATE_IDS.get(AFTER));
    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, true, expectedSecondRunTime);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void recurringNoticeIsRescheduledCorrectlyWhenNextCalculatedRunTimeIsBeforeNow(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, AFTER, true));

    ZonedDateTime expectedFirstRunTime = AFTER_PERIOD.plusDate(actionDateTime);

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, true, expectedFirstRunTime);

    ZonedDateTime fakeNow = rightAfter(
      RECURRING_PERIOD.plusDate(expectedFirstRunTime));

    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(fakeNow);

    ZonedDateTime expectedNextRunTime = RECURRING_PERIOD.plusDate(fakeNow);

    assertThatNoticesWereSent(TEMPLATE_IDS.get(AFTER));
    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, true, expectedNextRunTime);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void multipleScheduledNoticesAreProcessedDuringOneProcessingIteration(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent,
      createNoticeConfig(triggeringEvent, UPON_AT, false),
      createNoticeConfig(triggeringEvent, AFTER, false),
      createNoticeConfig(triggeringEvent, AFTER, true)
    );

    ZonedDateTime firstAfterRunTime = AFTER_PERIOD.plusDate(actionDateTime);

    assertThatNumberOfScheduledNoticesIs(3);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);  // send and delete
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, false, firstAfterRunTime); // send and delete
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, true, firstAfterRunTime);  // send and reschedule

    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(firstAfterRunTime));

    ZonedDateTime expectedRecurrenceRunTime = RECURRING_PERIOD.plusDate(firstAfterRunTime);

    assertThatNoticesWereSent(TEMPLATE_IDS.get(UPON_AT), TEMPLATE_IDS.get(AFTER), TEMPLATE_IDS.get(AFTER));
    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, AFTER, true, expectedRecurrenceRunTime);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void noticeIsDiscardedWhenReferencedActionDoesNotExist(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, UPON_AT, false));

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);

    feeFineActionsClient.delete(actionId);
    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(actionDateTime));

    assertThatNoNoticesWereSent();
    assertThatNumberOfScheduledNoticesIs(0);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void noticeIsDiscardedWhenReferencedAccountDoesNotExist(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, UPON_AT, false));

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);

    accountsClient.delete(accountId);
    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(actionDateTime));

    assertThatNoNoticesWereSent();
    assertThatNumberOfScheduledNoticesIs(0);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void noticeIsDiscardedWhenReferencedLoanDoesNotExist(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, UPON_AT, false));

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);

    loansClient.delete(loanId);
    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(actionDateTime));

    assertThatNoNoticesWereSent();
    assertThatNumberOfScheduledNoticesIs(0);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void noticeIsDiscardedWhenReferencedItemDoesNotExist(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, UPON_AT, false));

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);

    itemsClient.delete(itemId);
    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(actionDateTime));

    assertThatNoNoticesWereSent();
    assertThatNumberOfScheduledNoticesIs(0);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void noticeIsDiscardedWhenReferencedUserDoesNotExist(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, UPON_AT, false));

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);

    usersClient.delete(userId);
    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(actionDateTime));

    assertThatNoNoticesWereSent();
    assertThatNumberOfScheduledNoticesIs(0);
  }

  @ParameterizedTest
  @MethodSource("testParameters")
  public void noticeIsDiscardedWhenAccountIsClosed(TriggeringEvent triggeringEvent) {
    generateOverdueFine(triggeringEvent, createNoticeConfig(triggeringEvent, UPON_AT, false));

    assertThatNumberOfScheduledNoticesIs(1);
    assertThatScheduledNoticeExists(triggeringEvent, UPON_AT, false, actionDateTime);

    JsonObject closedAccountJson = account.toJson();
    JsonPropertyWriter.writeNamedObject(closedAccountJson, "status", "Closed");

    accountsClient.replace(accountId, closedAccountJson);
    scheduledNoticeProcessingClient.runFeeFineNoticesProcessing(rightAfter(actionDateTime));

    assertThatNoNoticesWereSent();
    assertThatNumberOfScheduledNoticesIs(0);
  }

  private void generateOverdueFine(TriggeringEvent triggeringEvent, JsonObject... patronNoticeConfigs) {
    NoticePolicyBuilder noticePolicyBuilder = new NoticePolicyBuilder()
      .withName("Patron notice policy with fee/fine notices")
      .withFeeFineNotices(Arrays.asList(patronNoticeConfigs));

    use(noticePolicyBuilder);

    templateFixture.createDummyNoticeTemplate(TEMPLATE_IDS.get(UPON_AT));
    templateFixture.createDummyNoticeTemplate(TEMPLATE_IDS.get(AFTER));

    UUID checkInServicePointId = servicePointsFixture.cd1().getId();
    IndividualResource location = locationsFixture.basedUponExampleLocation(
      builder -> builder.withPrimaryServicePoint(checkInServicePointId));
    IndividualResource user = usersFixture.james();
    userId = user.getId();
    IndividualResource item = itemsFixture.basedUponNod(builder ->
      builder.withPermanentLocation(location.getId()));
    itemId = item.getId();

    JsonObject servicePointOwner = new JsonObject()
      .put("value", checkInServicePointId.toString())
      .put("label", "Service Desk 1");

    feeFineOwnersClient.create(new FeeFineOwnerBuilder()
      .withId(randomUUID())
      .withOwner("test owner")
      .withServicePointOwner(Collections.singletonList(servicePointOwner)));

    feeFinesClient.create(new FeeFineBuilder()
      .withId(randomUUID())
      .withFeeFineType(OVERDUE_FINE)
      .withAutomatic(true));

    final ZonedDateTime checkOutDate = ClockManager.getZonedDateTime().minusYears(1);
    final ZonedDateTime checkInDate = checkOutDate.plusMonths(1);

    IndividualResource checkOutResponse = checkOutFixture.checkOutByBarcode(item, user, checkOutDate);
    loanId = UUID.fromString(checkOutResponse.getJson().getString("id"));

    switch (triggeringEvent) {
    case OVERDUE_FINE_RETURNED:
      checkInFixture.checkInByBarcode(new CheckInByBarcodeRequestBuilder()
        .forItem(item)
        .on(checkInDate)
        .at(checkInServicePointId));
      break;
    case OVERDUE_FINE_RENEWED:
      loansFixture.renewLoan(item, user);
      break;
    default:
      break;
    }

    List<JsonObject> accounts = Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(accountsClient::getAll, hasSize(1));

    assertThat("Fee/fine record should have been created", accounts, hasSize(1));
    account = Account.from(accounts.get(0));
    accountId = UUID.fromString(account.getId());

    List<JsonObject> actions = Awaitility.await()
      .atMost(1, TimeUnit.SECONDS)
      .until(feeFineActionsClient::getAll, hasSize(1));

    assertThat("Fee/fine action record should have been created", actions, hasSize(1));
    action = FeeFineAction.from(actions.get(0));
    actionId = UUID.fromString(action.getId());
    actionDateTime = action.getDateAction();
  }

  private JsonObject createNoticeConfig(TriggeringEvent triggeringEvent, NoticeTiming timing, boolean isRecurring) {
    JsonObject timingPeriod = timing == AFTER ? AFTER_PERIOD.asJson() : null;

    NoticeConfigurationBuilder builder = new NoticeConfigurationBuilder()
      .withEventType(NoticeEventType.from(triggeringEvent.getRepresentation()).getRepresentation())
      .withTemplateId(TEMPLATE_IDS.get(timing))
      .withTiming(timing.getRepresentation(), timingPeriod)
      .sendInRealTime(true);

    if (isRecurring) {
      builder = builder.recurring(RECURRING_PERIOD);
    }

    return builder.create();
  }

  private void assertThatScheduledNoticeExists(TriggeringEvent triggeringEvent, NoticeTiming timing, Boolean recurring, ZonedDateTime nextRunTime) {
    Period expectedRecurringPeriod = recurring ? RECURRING_PERIOD : null;

    assertThat(scheduledNoticesClient.getAll(), hasItems(
      hasScheduledFeeFineNotice(
        actionId, loanId, userId, TEMPLATE_IDS.get(timing),
        triggeringEvent, nextRunTime,
        timing, expectedRecurringPeriod, true)
    ));
  }

  private void assertThatNoticesWereSent(UUID... expectedTemplateIds) {
    List<JsonObject> sentNotices = patronNoticesClient.getAll();

    assertThat(sentNotices, hasSize(expectedTemplateIds.length));
    assertThat(FakePubSub.getPublishedEventsAsList(byLogEventType(NOTICE.value())),
      hasSize(expectedTemplateIds.length));

    Matcher<?> matcher = TemplateContextMatchers.getFeeChargeContextMatcher(account);

    Stream.of(expectedTemplateIds)
      .forEach(templateId -> assertThat(sentNotices, hasItem(
        hasNoticeProperties(userId, templateId, "email", "text/html", matcher))));
  }

  private void assertThatNumberOfScheduledNoticesIs(int numberOfNotices) {
    assertThat(scheduledNoticesClient.getAll(), hasSize(numberOfNotices));
  }

  private void assertThatNoNoticesWereSent() {
    assertThat(patronNoticesClient.getAll(), empty());
    assertThat(FakePubSub.getPublishedEventsAsList(byLogEventType(NOTICE.value())), empty());
  }

  private static ZonedDateTime rightAfter(ZonedDateTime dateTime) {
    return dateTime.plusMinutes(1);
  }

  private static Object[] testParameters() {
    return new Object[] { OVERDUE_FINE_RETURNED, OVERDUE_FINE_RENEWED };
  }

}
