package api.loans;

import static api.support.fixtures.CalendarExamples.CASE_CURRENT_IS_OPEN;
import static api.support.fixtures.CalendarExamples.CASE_CURRENT_IS_OPEN_CURR_DAY;
import static api.support.fixtures.CalendarExamples.CASE_CURRENT_IS_OPEN_PREV_DAY;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_DAY_ALL_PREV_DATE;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_ID;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_PREV_DAY;
import static api.support.fixtures.CalendarExamples.END_TIME_FIRST_PERIOD;
import static api.support.fixtures.CalendarExamples.ROLLOVER_SCENARIO_NEXT_DAY_CLOSED_SERVICE_POINT_ID;
import static api.support.fixtures.CalendarExamples.ROLLOVER_SCENARIO_SERVICE_POINT_ID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.folio.circulation.domain.policy.DueDateManagement;
import org.folio.circulation.domain.policy.Period;
import org.folio.circulation.support.http.client.Response;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import api.support.APITests;
import api.support.builders.CheckOutByBarcodeRequestBuilder;
import api.support.builders.LoanPolicyBuilder;
import api.support.fixtures.ConfigurationExample;
import api.support.http.IndividualResource;
import io.vertx.core.json.JsonObject;

/**
 * Test case for Short-term loans
 * Loanable = Y
 * Loan profile = Rolling
 * Closed Library Due Date Management = Move to the end of the current service point hours
 * <p>
 * Expected result:
 * If SPID-1 is determined to be CLOSED for system-calculated due date and timestamp
 * Then the due date timestamp should be changed to the endTime of the current service point for SPID-1 (i.e., truncating the loan length)
 */
public class CheckOutCalculateDueDateShortTermTests extends APITests {

  private static final String LOAN_POLICY_NAME = "Move to the end of the current service point hours";
  private static final String INTERVAL_HOURS = "Hours";
  private static final String INTERVAL_MINUTES = "Minutes";
  private static final String UTC = "UTC";
  private static final LocalTime TEST_TIME_MORNING = LocalTime.of(11, 0);

  private final String dueDateManagement =
    DueDateManagement.MOVE_TO_END_OF_CURRENT_SERVICE_POINT_HOURS.getValue();

  @Test
  public void testRespectSelectedTimezoneForDueDateCalculations() throws Exception {
    String expectedTimeZone = "America/New_York";
    int duration = 24;

    Response response = configClient.create(ConfigurationExample.newYorkTimezoneConfiguration())
      .getResponse();
    assertThat(response.getBody(), containsString(expectedTimeZone));

    ZonedDateTime loanDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_DAY_ALL_PREV_DATE, TEST_TIME_MORNING,
      ZoneOffset.of(expectedTimeZone));

    ZonedDateTime expectedDueDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_DAY_ALL_PREV_DATE.plusDays(1),
      LocalTime.MIDNIGHT, ZoneOffset.of(expectedTimeZone));

    checkOffsetTime(loanDate, expectedDueDate, CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID, INTERVAL_HOURS, duration);
  }

  @Test
  public void testRespectUtcTimezoneForDueDateCalculations() throws Exception {
    int duration = 24;

    Response response = configClient.create(ConfigurationExample.utcTimezoneConfiguration())
      .getResponse();
    assertThat(response.getBody(), containsString(UTC));

    ZonedDateTime loanDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_DAY_ALL_PREV_DATE, TEST_TIME_MORNING, ZoneOffset.UTC);

    ZonedDateTime expectedDueDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_DAY_ALL_PREV_DATE.plusDays(1), LocalTime.MIDNIGHT, ZoneOffset.UTC);

    checkOffsetTime(loanDate, expectedDueDate, CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID, INTERVAL_HOURS, duration);
  }

  @Test
  public void testMoveToTheEndOfCurrentServicePointHoursRolloverScenario() throws Exception {
    int duration = 18;

    Response response = configClient.create(ConfigurationExample.utcTimezoneConfiguration())
      .getResponse();
    assertThat(response.getBody(), containsString(UTC));

    ZonedDateTime loanDate = ZonedDateTime.of(
      CASE_CURRENT_IS_OPEN_CURR_DAY, TEST_TIME_MORNING, ZoneOffset.UTC);

    ZonedDateTime expectedDueDate = ZonedDateTime.of(
      CASE_CURRENT_IS_OPEN_CURR_DAY, LocalTime.MIDNIGHT.plusHours(3),
      ZoneOffset.UTC);

    checkOffsetTime(loanDate, expectedDueDate, ROLLOVER_SCENARIO_SERVICE_POINT_ID, INTERVAL_HOURS, duration);
  }

  @Test
  public void testMoveToTheEndOfCurrentServicePointHoursNextDayIsClosed() throws Exception {
    int duration = 1;

    Response response = configClient.create(ConfigurationExample.utcTimezoneConfiguration())
      .getResponse();
    assertThat(response.getBody(), containsString(UTC));

    ZonedDateTime loanDate = ZonedDateTime.of(
      CASE_CURRENT_IS_OPEN_CURR_DAY, TEST_TIME_MORNING, ZoneOffset.UTC);

    ZonedDateTime expectedDueDate = ZonedDateTime.of(
      CASE_CURRENT_IS_OPEN_CURR_DAY,
      LocalTime.MIDNIGHT.minusMinutes(1), ZoneOffset.UTC);

    checkOffsetTime(loanDate, expectedDueDate, ROLLOVER_SCENARIO_NEXT_DAY_CLOSED_SERVICE_POINT_ID, INTERVAL_HOURS, duration);
  }

  /**
   * Loan period: Hours
   * Current day: closed
   * Next and prev day: open allDay
   * Test period: FRI=open, SAT=close, MON=open
   */
  @Test
  public void testHoursLoanPeriodIfCurrentDayIsClosedAndNextAllDayOpen() throws Exception {
    int duration = 24;

    ZonedDateTime loanDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_DAY_ALL_PREV_DATE, TEST_TIME_MORNING, ZoneOffset.UTC);

    ZonedDateTime expectedDueDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_DAY_ALL_PREV_DATE.plusDays(1), LocalTime.MIDNIGHT,
      ZoneOffset.UTC);

    checkOffsetTime(loanDate, expectedDueDate, CASE_FRI_SAT_MON_DAY_ALL_SERVICE_POINT_ID, INTERVAL_HOURS, duration);
  }

  /**
   * Loan period: Hours
   * Current day: closed
   * Next and prev day: period
   * Test period: FRI=open, SAT=close, MON=open
   */
  @Test
  public void testHoursLoanPeriodIfCurrentDayIsClosedAndNextDayHasPeriod() throws Exception {
    ZonedDateTime loanDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_SERVICE_POINT_PREV_DAY, TEST_TIME_MORNING, ZoneOffset.UTC);

    ZonedDateTime expectedDueDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_SERVICE_POINT_PREV_DAY, END_TIME_FIRST_PERIOD,
      ZoneOffset.UTC);

    checkOffsetTime(loanDate, expectedDueDate, CASE_FRI_SAT_MON_SERVICE_POINT_ID, INTERVAL_HOURS, 25);
  }

  /**
   * Loan period: Minutes
   * Current day: open
   * Next and prev day: period
   */
  @Test
  public void testMinutesLoanPeriodIfCurrentDayIsClosedAndNextDayHasPeriod() throws Exception {
    ZonedDateTime loanDate = ZonedDateTime.of(
      CASE_CURRENT_IS_OPEN_PREV_DAY, END_TIME_FIRST_PERIOD, ZoneOffset.UTC)
      .minusHours(1);

    ZonedDateTime expectedDueDate = ZonedDateTime.of(
      CASE_CURRENT_IS_OPEN_PREV_DAY, END_TIME_FIRST_PERIOD,
      ZoneOffset.UTC);

    checkOffsetTime(loanDate, expectedDueDate, CASE_CURRENT_IS_OPEN, INTERVAL_MINUTES, 90);
  }

  /**
   * Check result
   */
  private void checkOffsetTime(ZonedDateTime loanDate, ZonedDateTime expectedDueDate,
                               String servicePointId, String interval, int duration)
    throws InterruptedException,
    TimeoutException,
    ExecutionException {

    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    final IndividualResource steve = usersFixture.steve();

    final UUID checkoutServicePointId = UUID.fromString(servicePointId);

    JsonObject loanPolicyEntry = createLoanPolicyEntry(duration, interval);
    final IndividualResource loanPolicy = createLoanPolicy(loanPolicyEntry);
    UUID requestPolicyId = requestPoliciesFixture.allowAllRequestPolicy().getId();
    UUID noticePolicyId = noticePoliciesFixture.activeNotice().getId();
    IndividualResource overdueFinePolicy = overdueFinePoliciesFixture.facultyStandard();
    IndividualResource lostItemFeePolicy = lostItemFeePoliciesFixture.facultyStandard();
    useFallbackPolicies(loanPolicy.getId(), requestPolicyId, noticePolicyId,
      overdueFinePolicy.getId(), lostItemFeePolicy.getId());

    IndividualResource response = null;

    try (MockedStatic<System> system = Mockito.mockStatic(System.class)) {
      system.when(System::currentTimeMillis).thenReturn(loanDate
        .toInstant().toEpochMilli());

      response = checkOutFixture.checkOutByBarcode(
        new CheckOutByBarcodeRequestBuilder()
          .forItem(smallAngryPlanet)
          .to(steve)
          .on(loanDate)
          .at(checkoutServicePointId));
    }

    final JsonObject loan = response.getJson();

    loanHasLoanPolicyProperties(loan, loanPolicy);
    loanHasOverdueFinePolicyProperties(loan,  overdueFinePolicy);
    loanHasLostItemPolicyProperties(loan,  lostItemFeePolicy);

    ZonedDateTime actualDueDate = getThresholdDateTime(ZonedDateTime.parse(loan.getString("dueDate")));
    ZonedDateTime thresholdDateTime = getThresholdDateTime(expectedDueDate);

    assertThat("due date should be " + thresholdDateTime + ", actual due date is " + actualDueDate,
      actualDueDate.isEqual(thresholdDateTime));
  }

  /**
   * Minor threshold when comparing minutes or milliseconds of dateTime
   */
  private ZonedDateTime getThresholdDateTime(ZonedDateTime dateTime) {
    return dateTime.withSecond(0).withNano(0);
  }

  private IndividualResource createLoanPolicy(JsonObject loanPolicyEntry) {

    return loanPoliciesFixture.create(loanPolicyEntry);
  }

  /**
   * Create a fake json LoanPolicy
   */
  private JsonObject createLoanPolicyEntry(int duration, String intervalId) {
    return new LoanPolicyBuilder()
      .withName(LOAN_POLICY_NAME)
      .withDescription("LoanPolicy")
      .rolling(Period.from(duration, intervalId))
      .withClosedLibraryDueDateManagement(dueDateManagement)
      .renewFromCurrentDueDate()
      .create();
  }

}
