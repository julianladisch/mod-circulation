package api.loans;

import static api.support.builders.FixedDueDateSchedule.wholeMonth;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_CURR_DAY;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_ID;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_NEXT_DAY;
import static api.support.fixtures.CalendarExamples.CASE_FRI_SAT_MON_SERVICE_POINT_PREV_DAY;
import static api.support.matchers.TextDateTimeMatcher.isEquivalentTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.folio.circulation.domain.policy.DueDateManagement;
import org.folio.circulation.domain.policy.Period;
import org.junit.Test;

import api.support.APITests;
import api.support.builders.CheckOutByBarcodeRequestBuilder;
import api.support.builders.FixedDueDateSchedulesBuilder;
import api.support.builders.LoanPolicyBuilder;
import api.support.http.IndividualResource;

/**
 * Test cases for scenarios when due date calculated by CLDDM
 * extends beyond the DueDate for the configured Fixed due date schedule
 */
public class CheckOutByBarcodeFixedDueDateScenariosTest extends APITests {

  @Test
  public void shouldUseMoveToThePreviousOpenDayStrategyForLongTermLoanPolicyWhenDueDateExtendsBeyondFixedDueDate() {

    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    IndividualResource jessica = usersFixture.jessica();
    UUID checkoutServicePointId = UUID.fromString(CASE_FRI_SAT_MON_SERVICE_POINT_ID);

    ZonedDateTime loanDate =
      ZonedDateTime.of(2019, 1, 25, 10, 0, 0, 0, ZoneOffset.UTC);

    ZonedDateTime limitDueDate =
      CASE_FRI_SAT_MON_SERVICE_POINT_CURR_DAY.atStartOfDay(ZoneOffset.UTC);
    FixedDueDateSchedulesBuilder fixedDueDateSchedules = new FixedDueDateSchedulesBuilder()
      .withName("Fixed Due Date Schedule")
      .addSchedule(wholeMonth(2019, 1, limitDueDate));
    UUID fixedDueDateSchedulesId = loanPoliciesFixture.createSchedule(
      fixedDueDateSchedules).getId();

    LoanPolicyBuilder loanPolicy = new LoanPolicyBuilder()
      .withName("Loan policy")
      .rolling(Period.days(8))
      .withClosedLibraryDueDateManagement(DueDateManagement.MOVE_TO_THE_END_OF_THE_NEXT_OPEN_DAY.getValue())
      .limitedBySchedule(fixedDueDateSchedulesId);

    use(loanPolicy);

    IndividualResource loan = checkOutFixture.checkOutByBarcode(
      new CheckOutByBarcodeRequestBuilder()
        .forItem(smallAngryPlanet)
        .to(jessica)
        .at(checkoutServicePointId)
        .on(loanDate));

    ZonedDateTime expectedDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_SERVICE_POINT_PREV_DAY, LocalTime.MAX, ZoneOffset.UTC);
    assertThat("due date should be " + expectedDate,
      loan.getJson().getString("dueDate"), isEquivalentTo(expectedDate));
  }

  @Test
  public void shouldUseMoveToThePreviousOpenDayStrategyForFixedLoanPolicyWhenDueDateExtendsBeyondFixedDueDate() {

    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    IndividualResource jessica = usersFixture.jessica();
    UUID checkoutServicePointId = UUID.fromString(CASE_FRI_SAT_MON_SERVICE_POINT_ID);

    ZonedDateTime loanDate =
      ZonedDateTime.of(2019, 1, 25, 10, 0, 0, 0, ZoneOffset.UTC);

    ZonedDateTime limitDueDate =
      CASE_FRI_SAT_MON_SERVICE_POINT_CURR_DAY.atStartOfDay(ZoneOffset.UTC);

    FixedDueDateSchedulesBuilder fixedDueDateSchedules = new FixedDueDateSchedulesBuilder()
      .withName("Fixed Due Date Schedule")
      .addSchedule(wholeMonth(2019, 1, limitDueDate));
    UUID fixedDueDateSchedulesId = loanPoliciesFixture.createSchedule(
      fixedDueDateSchedules).getId();

    LoanPolicyBuilder loanPolicy = new LoanPolicyBuilder()
      .withName("Loan policy")
      .withClosedLibraryDueDateManagement(DueDateManagement.MOVE_TO_THE_END_OF_THE_NEXT_OPEN_DAY.getValue())
      .fixed(fixedDueDateSchedulesId);

    use(loanPolicy);

    IndividualResource loan = checkOutFixture.checkOutByBarcode(
      new CheckOutByBarcodeRequestBuilder()
        .forItem(smallAngryPlanet)
        .to(jessica)
        .at(checkoutServicePointId)
        .on(loanDate));

    ZonedDateTime expectedDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_SERVICE_POINT_PREV_DAY, LocalTime.MAX, ZoneOffset.UTC);
    assertThat("due date should be " + expectedDate,
      loan.getJson().getString("dueDate"), isEquivalentTo(expectedDate));
  }


  @Test
  public void shouldUseSelectedClosedLibraryStrategyWhenDueDateDoesNotExtendBeyondFixedDueDate() {

    IndividualResource smallAngryPlanet = itemsFixture.basedUponSmallAngryPlanet();
    IndividualResource jessica = usersFixture.jessica();
    UUID checkoutServicePointId = UUID.fromString(CASE_FRI_SAT_MON_SERVICE_POINT_ID);

    ZonedDateTime loanDate =
      ZonedDateTime.of(2019, 1, 25, 10, 0, 0, 0, ZoneOffset.UTC);

    ZonedDateTime limitDueDate =
      CASE_FRI_SAT_MON_SERVICE_POINT_NEXT_DAY.atStartOfDay(ZoneOffset.UTC);
    FixedDueDateSchedulesBuilder fixedDueDateSchedules = new FixedDueDateSchedulesBuilder()
      .withName("Fixed Due Date Schedule")
      .addSchedule(wholeMonth(2019, 1, limitDueDate));
    UUID fixedDueDateSchedulesId = loanPoliciesFixture.createSchedule(
      fixedDueDateSchedules).getId();

    LoanPolicyBuilder loanPolicy = new LoanPolicyBuilder()
      .withName("Loan policy")
      .rolling(Period.days(8))
      .withClosedLibraryDueDateManagement(DueDateManagement.MOVE_TO_THE_END_OF_THE_NEXT_OPEN_DAY.getValue())
      .limitedBySchedule(fixedDueDateSchedulesId);

    use(loanPolicy);

    IndividualResource loan = checkOutFixture.checkOutByBarcode(
      new CheckOutByBarcodeRequestBuilder()
        .forItem(smallAngryPlanet)
        .to(jessica)
        .at(checkoutServicePointId)
        .on(loanDate));

    ZonedDateTime expectedDate = ZonedDateTime.of(
      CASE_FRI_SAT_MON_SERVICE_POINT_NEXT_DAY, LocalTime.MAX, ZoneOffset.UTC); 
    assertThat("due date should be " + expectedDate,
      loan.getJson().getString("dueDate"), isEquivalentTo(expectedDate));
  }
}
