package api.requests.scenarios;

import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_ID;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_NEXT_DAY;
import static api.support.fixtures.ConfigurationExample.timezoneConfigurationFor;
import static api.support.http.CqlQuery.queryFromTemplate;
import static api.support.matchers.JsonObjectMatcher.hasJsonPath;
import static api.support.matchers.TextDateTimeMatcher.isEquivalentTo;
import static api.support.matchers.TextDateTimeMatcher.withinSecondsBefore;
import static java.lang.Boolean.TRUE;
import static java.time.Clock.offset;
import static java.time.Duration.ofDays;
import static java.time.ZoneOffset.UTC;
import static org.folio.circulation.domain.policy.DueDateManagement.KEEP_THE_CURRENT_DUE_DATE;
import static org.folio.circulation.domain.policy.library.ClosedLibraryStrategyUtils.END_OF_A_DAY;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.folio.circulation.domain.policy.DueDateManagement;
import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.ClockManager;
import org.folio.circulation.support.http.client.Response;
import org.folio.circulation.support.utils.DateTimeUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import api.support.APITests;
import api.support.MultipleJsonRecords;
import api.support.builders.LoanBuilder;
import api.support.builders.LoanPolicyBuilder;
import api.support.builders.RequestBuilder;
import api.support.builders.ServicePointBuilder;
import api.support.http.IndividualResource;
import io.vertx.core.json.JsonObject;

/**
 * Notes:<br>
 *  MGD = Minimum guaranteed due date<br>
 *  RD = Recall due date<br>
 *
 * @see <a href="https://issues.folio.org/browse/CIRC-203">CIRC-203</a>
 */
public class LoanDueDatesAfterRecallTests extends APITests {
  private static Clock clock;

  public LoanDueDatesAfterRecallTests() {
    super(true, true);
  }

  @BeforeAll
  public static void setupBeforeClass() {
    clock = Clock.fixed(ClockManager.getInstant(), ZoneOffset.UTC);
    ClockManager.setClock(clock);
  }

  @BeforeEach
  public void setup() {
    ClockManager.setClock(clock);
  }

  @AfterEach
  public void after() {
    ClockManager.setDefaultClock();
  }

  @Test
  public void recallRequestWithNoPolicyValuesChangesDueDateToSystemDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, ClockManager.getZonedDateTime());

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat("due date should not be the original due date",
        storedLoan.getString("dueDate"), not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime());
    assertThat("due date should be the current system date",
        storedLoan.getString("dueDate"), is(expectedDueDate));
  }

  @Test
  public void recallRequestWithMGDAndRDValuesChangesDueDateToRD() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.months(2));

    setFallbackPolicies(canCirculateRollingPolicy);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, ClockManager.getZonedDateTime());

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat("due date should not be the original date",
        storedLoan.getString("dueDate"), not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusMonths(2));
    assertThat("due date should be in 2 months",
        storedLoan.getString("dueDate"), is(expectedDueDate));
  }

  @Test
  public void recallRequestWithMGDAndRDValuesChangesDueDateToMGD() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.weeks(1));

    setFallbackPolicies(canCirculateRollingPolicy);

    // We use the loan date to calculate the MGD
    final ZonedDateTime loanDate = ClockManager.getZonedDateTime();

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, loanDate);

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat("due date should not be the original due date",
        storedLoan.getString("dueDate"), not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      loanDate.plusWeeks(2));
    assertThat("due date should be in 2 weeks (minumum guaranteed loan period)",
        storedLoan.getString("dueDate"), is(expectedDueDate));
  }

  @Test
  public void recallRequestWithRDAndNoMGDValuesChangesDueDateToRD() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsRecallReturnInterval(Period.weeks(1));

    setFallbackPolicies(canCirculateRollingPolicy);

    // We use the loan date to calculate the minimum guaranteed due date (MGD)
    final ZonedDateTime loanDate = ClockManager.getZonedDateTime();

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, loanDate);

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat("due date should not be the original due date",
        storedLoan.getString("dueDate"), not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusWeeks(1));
    assertThat("due date should be in 1 week (recall return interval)",
        storedLoan.getString("dueDate"), is(expectedDueDate));
  }

  @Test
  public void recallRequestWithMGDAndNoRDValuesChangesDueDateToMGD() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2));

    setFallbackPolicies(canCirculateRollingPolicy);

    // We use the loan date to calculate the minimum guaranteed due date (MGD)
    final ZonedDateTime loanDate = ClockManager.getZonedDateTime();

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, loanDate);

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat("due date sholud not be the original due date",
        storedLoan.getString("dueDate"), not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      loanDate.plusWeeks(2));
    assertThat("due date should be in 2 weeks (minimum guaranteed loan period)",
        storedLoan.getString("dueDate"), is(expectedDueDate));
  }

  @Test
  public void recallRequestWithMGDAndRDValuesChangesDueDateToMGDWithCLDDM() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final UUID checkOutServicePointId = UUID.fromString(CASE_FRI_SAT_MON_SERVICE_POINT_ID);
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.days(8))
        .withRecallsRecallReturnInterval(Period.weeks(1))
        .withClosedLibraryDueDateManagement(DueDateManagement.MOVE_TO_THE_END_OF_THE_NEXT_OPEN_DAY.getValue());

    setFallbackPolicies(canCirculateRollingPolicy);

    servicePointsFixture.create(new ServicePointBuilder(checkOutServicePointId, "CLDDM Desk", "clddm", "CLDDM Desk Test", null, null, TRUE, null));

    // We use the loan date to calculate the minimum guaranteed due date (MGD)
    final ZonedDateTime loanDate =
        ZonedDateTime.of(2019, 1, 25, 10, 0, 0, 0, UTC);

    clockToFixedDateTime(loanDate);

    final IndividualResource loan = loansFixture.createLoan(new LoanBuilder()
        .open()
        .withItemId(smallAngryPlanet.getId())
        .withUserId(steve.getId())
        .withLoanDate(loanDate)
        .withDueDate(loanDate.plusWeeks(3))
        .withCheckoutServicePointId(checkOutServicePointId));

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), "Recall");

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat("due date should not be the original due date",
        storedLoan.getString("dueDate"), not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(ZonedDateTime.of(
      CASE_FRI_SAT_MON_SERVICE_POINT_NEXT_DAY, END_OF_A_DAY, UTC));

    assertThat("due date should be moved to Monday",
        storedLoan.getString("dueDate"), is(expectedDueDate));
  }

  @ParameterizedTest(name = "{index}: {0} {1} {2} {3} {4}")
  @CsvSource(value = {
    // MGD duration|MGD interval|RD duration|RD interval|expected string
    "null,null,1,Months,the \"minimumGuaranteedLoanPeriod\" in the loan policy is not recognized",
    "1,Months,null,null,the \"recallReturnInterval\" in the loan policy is not recognized",
    "1,Years,1,Months,the interval \"Years\" in \"minimumGuaranteedLoanPeriod\" is not recognized",
    "1,Months,1,Years,the interval \"Years\" in \"recallReturnInterval\" is not recognized",
    "-100,Months,1,Months,the duration \"-100\" in \"minimumGuaranteedLoanPeriod\" is invalid",
    "1,Months,-100,Months,the duration \"-100\" in \"recallReturnInterval\" is invalid"
  }, nullValues={"null"})
  public void loanPolicyWithInvalidMGDOrRDPeriodValuesReturnsErrorOnRecallCreation(
      Integer mgdDuration,
      String mgdInterval,
      Integer rdDuration,
      String rdInterval,
      String expectedMessage) {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.from(mgdDuration, mgdInterval))
        .withRecallsRecallReturnInterval(Period.from(rdDuration, rdInterval));

    setFallbackPolicies(canCirculateRollingPolicy);

    // We use the loan date to calculate the minimum guaranteed due date (MGD)
    final ZonedDateTime loanDate = ClockManager.getZonedDateTime();

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, steve, loanDate);

    final Response response = requestsFixture.attemptPlaceHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    assertThat("Status code should be 422", response.getStatusCode(), is(422));
    assertThat("errors should be present", response.getJson().getJsonArray("errors"), notNullValue());
    assertThat("errors should be size 1", response.getJson().getJsonArray("errors").size(), is(1));
    assertThat("first error should have the expected message field",
        response.getJson().getJsonArray("errors").getJsonObject(0).getString("message"),
        is(expectedMessage));
  }

  @Test
  public void initialLoanDueDateOnCreateWithPrexistingRequests() {

    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withClosedLibraryDueDateManagement(KEEP_THE_CURRENT_DUE_DATE.getValue())
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.months(2));

    setFallbackPolicies(canCirculateRollingPolicy);

    checkOutFixture.checkOutByBarcode(smallAngryPlanet, charlotte,
      ClockManager.getZonedDateTime());

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, steve,
      ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
      ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    checkInFixture.checkInByBarcode(smallAngryPlanet);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, ClockManager.getZonedDateTime());

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ZonedDateTime.of(ClockManager.getZonedDateTime().plusMonths(2)
      .toLocalDate(), LocalTime.MIDNIGHT.minusSeconds(1), UTC));

    assertThat("due date should be in 2 months (recall return interval)",
        storedLoan.getString("dueDate"), is(expectedDueDate));
  }

  @Test
  public void changedDueDateAfterRecallingAnItemShouldRespectTenantTimezone() {
    final String stockholmTimeZone = "Europe/Stockholm";

    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();

    configClient.create(timezoneConfigurationFor(stockholmTimeZone));

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
      .withName("Can Circulate Rolling With Recalls")
      .withDescription("Can circulate item With Recalls")
      .rolling(Period.days(14))
      .unlimitedRenewals()
      .renewFromSystemDate()
      .withRecallsMinimumGuaranteedLoanPeriod(Period.days(5))
      .withClosedLibraryDueDateManagement(KEEP_THE_CURRENT_DUE_DATE.getValue());

    setFallbackPolicies(canCirculateRollingPolicy);

    ClockManager.setClock(Clock.fixed(Instant.parse("2020-01-24T08:34:21Z"),
      ZoneId.of("UTC")));

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(smallAngryPlanet, steve);

    //3 days later
    ClockManager.setClock(offset(ClockManager.getClock(), ofDays(3)));

    requestsFixture.place(
      new RequestBuilder()
        .recall()
        .forItem(smallAngryPlanet)
        .by(jessica)
        .withRequestDate(ClockManager.getZonedDateTime())
        .withPickupServicePoint(requestServicePoint));

    final var storedLoan = loansFixture.getLoanById(loan.getId()).getJson();

    assertThat("due date should be end of the day, 5 days from loan date",
      storedLoan.getString("dueDate"), isEquivalentTo(
        ZonedDateTime.parse("2020-01-29T23:59:59+01:00")));
  }

  @Test
  public void pagedItemRecalledThenLoanedAndNextRecallDoesNotChangeDueDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();
    final IndividualResource james = usersFixture.james();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.weeks(1));

    setFallbackPolicies(canCirculateRollingPolicy);

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Page");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, james,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, jessica, ClockManager
      .getZonedDateTime());

    // Recalled is applied when loaned, so the due date should be 2 weeks, not 3 weeks
    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusWeeks(2));

    JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String recalledDueDate = storedLoan.getString("dueDate");
    assertThat("due date after recall should be 2 weeks",
        recalledDueDate, is(expectedDueDate));

    ClockManager.setClock(Clock.offset(clock, Duration.ofDays(1)));

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, charlotte,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    storedLoan = loansStorageClient.getById(loan.getId()).getJson();
    assertThat("second recall should not change the due date",
        storedLoan.getString("dueDate"), is(recalledDueDate));
  }

  @Test
  public void pagedItemRecalledThenLoanedBecomesOverdueAndNextRecallDoesNotChangeDueDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();
    final IndividualResource james = usersFixture.james();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.weeks(1));

     setFallbackPolicies(canCirculateRollingPolicy);

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Page");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, james,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, jessica, ClockManager
      .getZonedDateTime());

    // Recalled is applied when loaned, so the due date should be 2 weeks, not 3 weeks
    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusWeeks(2));

    JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String recalledDueDate = storedLoan.getString("dueDate");
    assertThat("due date after recall should be 2 weeks",
        recalledDueDate, is(expectedDueDate));

    // Move the fixed clock so that the loan is now overdue
    ClockManager.setClock(Clock.offset(clock, Duration.ofDays(15)));

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, charlotte,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    storedLoan = loansStorageClient.getById(loan.getId()).getJson();
    assertThat("second recall should not change the due date",
        storedLoan.getString("dueDate"), is(recalledDueDate));
  }

  @Test
  public void secondRecallRequestWithMGDTruncationInPlaceDoesNotChangeDueDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.weeks(1));

    setFallbackPolicies(canCirculateRollingPolicy);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, ClockManager
      .getZonedDateTime());

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String recalledDueDate = storedLoan.getString("dueDate");
    assertThat("due date after recall should not be the original date",
        recalledDueDate, not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusWeeks(2));
    assertThat("due date after recall should be in 2 weeks",
        storedLoan.getString("dueDate"), is(expectedDueDate));

    ClockManager.setClock(Clock.offset(clock, Duration.ofDays(7)));

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, charlotte,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    storedLoan = loansStorageClient.getById(loan.getId()).getJson();
    assertThat("second recall should not change the due date (2 weeks)",
        storedLoan.getString("dueDate"), is(recalledDueDate));
  }

  @Test
  public void secondRecallRequestWithMGDTruncationInPlaceAndLoanOverdueDoesNotChangeDueDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.weeks(1));

    setFallbackPolicies(canCirculateRollingPolicy);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, ClockManager
      .getZonedDateTime());

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String recalledDueDate = storedLoan.getString("dueDate");
    assertThat("due date after recall should not be the original date",
        recalledDueDate, not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusWeeks(2));
    assertThat("due date after recall should be in 2 weeks",
        storedLoan.getString("dueDate"), is(expectedDueDate));

    // Move the fixed clock so that the loan is now overdue
    ClockManager.setClock(Clock.offset(clock, Duration.ofDays(15)));

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, charlotte,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    storedLoan = loansStorageClient.getById(loan.getId()).getJson();
    assertThat("second recall should not change the due date (2 weeks)",
        storedLoan.getString("dueDate"), is(recalledDueDate));
  }

  @Test
  public void secondRecallRequestWithRDTruncationInPlaceDoesNotChangeDueDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsRecallReturnInterval(Period.months(2));

    setFallbackPolicies(canCirculateRollingPolicy);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, ClockManager.getZonedDateTime());

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String recalledDueDate = storedLoan.getString("dueDate");
    assertThat("due date after recall should not be  the original date",
        recalledDueDate, not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusMonths(2));
    assertThat("due date after recall should be in 2 months",
        storedLoan.getString("dueDate"), is(expectedDueDate));

    ClockManager.setClock(Clock.offset(clock, Duration.ofDays(7)));

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, charlotte,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    storedLoan = loansStorageClient.getById(loan.getId()).getJson();
    assertThat("second recall should not change the due date (2 months)",
        storedLoan.getString("dueDate"), is(recalledDueDate));
  }

  @Test
  public void secondRecallRequestWithRDTruncationInPlaceAndLoanOverdueDoesNotChangeDueDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource steve = usersFixture.steve();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
        .withRecallsRecallReturnInterval(Period.months(2));

    setFallbackPolicies(canCirculateRollingPolicy);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, steve, ClockManager.getZonedDateTime());

    final String originalDueDate = loan.getJson().getString("dueDate");

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, jessica,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String recalledDueDate = storedLoan.getString("dueDate");
    assertThat("due date after recall should not be the original date",
        recalledDueDate, not(originalDueDate));

    final String expectedDueDate = DateTimeUtil.formatDateTimeOptional(
      ClockManager.getZonedDateTime().plusMonths(2));
    assertThat("due date after recall should be in 2 months",
        storedLoan.getString("dueDate"), is(expectedDueDate));

    // Move the fixed clock so that the loan is now overdue
    ClockManager.setClock(Clock.offset(clock, Duration.ofDays(70)));

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, charlotte,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    storedLoan = loansStorageClient.getById(loan.getId()).getJson();
    assertThat("second recall should not change the due date (2 months)",
        storedLoan.getString("dueDate"), is(recalledDueDate));
  }

  @Test
  public void itemRecalledThenCancelledAndNextRecallDoesNotChangeDueDate() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource charlotte = usersFixture.charlotte();
    final IndividualResource james = usersFixture.james();

    final LoanPolicyBuilder canCirculateRollingPolicy = new LoanPolicyBuilder()
        .withName("Can Circulate Rolling With Recalls")
        .withDescription("Can circulate item With Recalls")
        .rolling(Period.weeks(3))
        .unlimitedRenewals()
        .renewFromSystemDate()
        .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(1))
        .withRecallsRecallReturnInterval(Period.weeks(2));

    setFallbackPolicies(canCirculateRollingPolicy);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, jessica, ClockManager
      .getZonedDateTime());

    final String originalDueDate = loan.getJson().getString("dueDate");

    JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final IndividualResource request = requestsFixture.placeHoldShelfRequest(
        smallAngryPlanet, james, ClockManager.getZonedDateTime(),
        requestServicePoint.getId(), "Recall");

    final String recalledDueDate = request.getJson().getString("dueDate");
    assertThat("due date after recall should not be the original date",
        recalledDueDate, not(originalDueDate));

    requestsFixture.cancelRequest(request);

    ClockManager.setClock(Clock.offset(clock, Duration.ofDays(7)));

    final IndividualResource renewal = loansFixture.renewLoan(smallAngryPlanet, jessica);

    final String renewalDueDate = renewal.getJson().getString("dueDate");

    storedLoan = loansStorageClient.getById(renewal.getId()).getJson();

    requestsFixture.placeHoldShelfRequest(smallAngryPlanet, charlotte,
        ClockManager.getZonedDateTime(), requestServicePoint.getId(), "Recall");

    storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    final String recalledRenewalDueDate = storedLoan.getString("dueDate");
    assertThat("due date after recall should not change the renewal due date",
        recalledRenewalDueDate, is(renewalDueDate));
  }

  @Test
  public void shouldNotExtendLoanDueDateIfOverdueLoanIsRecalled() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();

    final Period loanPeriod = Period.weeks(3);
    setFallbackPolicies(new LoanPolicyBuilder()
      .withName("Can Circulate Rolling With Recalls")
      .withDescription("Can circulate item With Recalls")
      .rolling(loanPeriod)
      .unlimitedRenewals()
      .renewFromSystemDate()
      .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
      .withRecallsRecallReturnInterval(Period.months(2)));

    final ZonedDateTime loanCreateDate = loanPeriod.minusDate(ClockManager.getZonedDateTime())
      .minusMinutes(1);
    final ZonedDateTime expectedLoanDueDate = loanPeriod
      .plusDate(loanCreateDate).truncatedTo(ChronoUnit.MILLIS);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, usersFixture.steve(), loanCreateDate);

    requestsFixture.place(new RequestBuilder()
      .recall()
      .forItem(smallAngryPlanet)
      .fulfilToHoldShelf()
      .by(usersFixture.jessica())
      .fulfilToHoldShelf()
      .withPickupServicePointId(servicePointsFixture.cd1().getId()));

    assertThat(loansStorageClient.getById(loan.getId()).getJson(),
      hasJsonPath("dueDate", expectedLoanDueDate.toString()));

    // verify that loan action is recorder even though due date is not changed
    final MultipleJsonRecords loanHistory = loanHistoryClient
      .getMany(queryFromTemplate("loan.id==%s and operation==U", loan.getId()));

    assertThat(loanHistory, hasItem(allOf(
      hasJsonPath("loan.action", "recallrequested"),
      hasJsonPath("loan.itemStatus", "Checked out"))
    ));
  }

  @Test
  public void shouldExtendLoanDueDateByAlternatePeriodWhenOverdueLoanIsRecalledAndPolicyAllowsExtension() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();

    final Period alternateLoanPeriod = Period.weeks(3);

    final Period loanPeriod = Period.weeks(1);
    setFallbackPolicies(new LoanPolicyBuilder()
      .withName("Can Circulate Rolling With Recalls")
      .withDescription("Can circulate item With Recalls")
      .rolling(loanPeriod)
      .unlimitedRenewals()
      .renewFromSystemDate()
      .withAllowRecallsToExtendOverdueLoans(true)
      .withAlternateRecallReturnInterval(alternateLoanPeriod));

    final ZonedDateTime loanCreateDate = loanPeriod
      .minusDate(ClockManager.getZonedDateTime())
      .minusMinutes(1);

    ZonedDateTime expectedLoanDueDate = alternateLoanPeriod
      .plusDate(loanPeriod.plusDate(loanCreateDate))
      .plusMinutes(1);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, usersFixture.steve(), loanCreateDate);

    requestsFixture.place(new RequestBuilder()
      .recall()
      .forItem(smallAngryPlanet)
      .fulfilToHoldShelf()
      .by(usersFixture.jessica())
      .fulfilToHoldShelf()
      .withPickupServicePointId(servicePointsFixture.cd1().getId()));

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat(storedLoan.getString("dueDate"), withinSecondsBefore(30, expectedLoanDueDate));
  }

  public void shouldExtendLoanDueDateByRecallReturnIntervalForOverdueLoansIsRecalledAndAlternateRecallReturnIntervalForOverdueLoansIsEmpty() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();

    final Period recalReturnInterval = Period.weeks(3);

    final Period loanPeriod = Period.weeks(1);
    setFallbackPolicies(new LoanPolicyBuilder()
      .withName("Can Circulate Rolling With Recalls")
      .withDescription("Can circulate item With Recalls")
      .rolling(loanPeriod)
      .unlimitedRenewals()
      .renewFromSystemDate()
      .withAllowRecallsToExtendOverdueLoans(true)
      .withRecallsRecallReturnInterval(recalReturnInterval));

    final ZonedDateTime loanCreateDate = loanPeriod
      .minusDate(ClockManager.getZonedDateTime())
      .minusMinutes(1);

    ZonedDateTime expectedLoanDueDate = recalReturnInterval
      .plusDate(loanPeriod.plusDate(loanCreateDate))
      .plusMinutes(1);

    final IndividualResource loan = checkOutFixture.checkOutByBarcode(
      smallAngryPlanet, usersFixture.steve(), loanCreateDate);

    requestsFixture.place(new RequestBuilder()
      .recall()
      .forItem(smallAngryPlanet)
      .fulfilToHoldShelf()
      .by(usersFixture.jessica())
      .fulfilToHoldShelf()
      .withPickupServicePointId(servicePointsFixture.cd1().getId()));

    final JsonObject storedLoan = loansStorageClient.getById(loan.getId()).getJson();

    assertThat(storedLoan.getString("dueDate"), withinSecondsBefore(30, expectedLoanDueDate));
  }

  @Test
  public void loanDueDateTruncatedOnCheckoutWhenRecallAnywhereInQueue() {
    final IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource requestServicePoint = servicePointsFixture.cd1();
    final IndividualResource jessica = usersFixture.jessica();
    final IndividualResource james = usersFixture.james();
    final IndividualResource rebecca = usersFixture.rebecca();
    final IndividualResource steve = usersFixture.steve();

    final Period loanPeriod = Period.weeks(4);
    setFallbackPolicies(new LoanPolicyBuilder()
      .withName("Can Circulate Rolling With Recalls")
      .withDescription("Can circulate item With Recalls")
      .rolling(loanPeriod)
      .unlimitedRenewals()
      .renewFromSystemDate()
      .withRecallsMinimumGuaranteedLoanPeriod(Period.weeks(2))
      .withRecallsRecallReturnInterval(Period.weeks(2)));

      checkOutFixture.checkOutByBarcode(smallAngryPlanet, steve, ClockManager.getZonedDateTime());

      requestsFixture.placeHoldShelfRequest(
        smallAngryPlanet, james, ClockManager.getZonedDateTime(),
        requestServicePoint.getId(), "Hold");

      requestsFixture.placeHoldShelfRequest(
        smallAngryPlanet, jessica, ClockManager.getZonedDateTime(),
        requestServicePoint.getId(), "Hold");

      requestsFixture.place(new RequestBuilder()
        .recall()
        .forItem(smallAngryPlanet)
        .fulfilToHoldShelf()
        .by(rebecca)
        .fulfilToHoldShelf()
        .withPickupServicePointId(requestServicePoint.getId()));

      checkInFixture.checkInByBarcode(smallAngryPlanet);

      final ZonedDateTime checkOutDate = ClockManager.getZonedDateTime();
      final ZonedDateTime truncatedLoanDate = checkOutDate.plusWeeks(2);

      final IndividualResource loan = checkOutFixture.checkOutByBarcode(
        smallAngryPlanet, james, checkOutDate);

      String loanDueDate = loan.getJson().getString("dueDate");
      assertThat(loanDueDate, is(truncatedLoanDate.toString()));
  }
}
