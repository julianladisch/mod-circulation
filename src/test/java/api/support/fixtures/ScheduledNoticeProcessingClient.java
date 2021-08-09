package api.support.fixtures;

import static api.support.APITestContext.circulationModuleUrl;
import static api.support.APITestContext.getOkapiHeadersFromContext;
import static org.folio.circulation.support.ClockManager.getClockManager;

import java.net.URL;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import api.support.http.TimedTaskClient;

public class ScheduledNoticeProcessingClient {
  private final TimedTaskClient timedTaskClient;

  public ScheduledNoticeProcessingClient() {
    timedTaskClient = new TimedTaskClient(getOkapiHeadersFromContext());
  }

  public void runLoanNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFrozenTime(this::runLoanNoticesProcessing, mockSystemTime);
    Clock.systemDefaultZone();
  }

  public void runLoanNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/loan-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "loan-scheduled-notices-processing-request");
  }

  public void runDueDateNotRealTimeNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFrozenTime(this::runDueDateNotRealTimeNoticesProcessing, mockSystemTime);
    Clock.systemDefaultZone();
  }

  public void runDueDateNotRealTimeNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/due-date-not-real-time-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "due-date-not-real-time-scheduled-notices-processing-request");
  }

  public void runRequestNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFrozenTime(this::runRequestNoticesProcessing, mockSystemTime);
    Clock.systemDefaultZone();
  }

  public void runRequestNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/request-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "request-scheduled-notices-processing-request");
  }

  public void runFeeFineNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFrozenClock(this::runFeeFineNoticesProcessing, mockSystemTime);
  }

  public void runFeeFineNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/fee-fine-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "fee-fine-scheduled-notices-processing-request");
  }

  private void runWithFrozenTime(Runnable runnable, ZonedDateTime mockSystemTime) {
    Clock.fixed(mockSystemTime.toInstant(), mockSystemTime.getZone());
    runnable.run();
  }

  private void runWithFrozenClock(Runnable runnable, ZonedDateTime mockSystemTime) {
    try {
      getClockManager().setClock(
        Clock.fixed(
          mockSystemTime.toInstant(),
          ZoneOffset.UTC));
      runnable.run();
    } finally {
      getClockManager().setDefaultClock();
    }
  }

}
