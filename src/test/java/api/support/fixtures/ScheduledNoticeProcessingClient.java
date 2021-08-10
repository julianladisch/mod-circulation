package api.support.fixtures;

import static api.support.APITestContext.circulationModuleUrl;
import static api.support.APITestContext.getOkapiHeadersFromContext;

import java.net.URL;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.folio.circulation.support.ClockManager;

import api.support.http.TimedTaskClient;

public class ScheduledNoticeProcessingClient {
  private final TimedTaskClient timedTaskClient;

  public ScheduledNoticeProcessingClient() {
    timedTaskClient = new TimedTaskClient(getOkapiHeadersFromContext());
  }

  public void runLoanNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFixedClock(this::runLoanNoticesProcessing, mockSystemTime);
  }

  public void runLoanNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/loan-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "loan-scheduled-notices-processing-request");
  }

  public void runDueDateNotRealTimeNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFixedClock(this::runDueDateNotRealTimeNoticesProcessing, mockSystemTime);
  }

  public void runDueDateNotRealTimeNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/due-date-not-real-time-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "due-date-not-real-time-scheduled-notices-processing-request");
  }

  public void runRequestNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFixedClock(this::runRequestNoticesProcessing, mockSystemTime);
  }

  public void runRequestNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/request-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "request-scheduled-notices-processing-request");
  }

  public void runFeeFineNoticesProcessing(ZonedDateTime mockSystemTime) {
    runWithFixedClock(this::runFeeFineNoticesProcessing, mockSystemTime);
  }

  public void runFeeFineNoticesProcessing() {
    URL url = circulationModuleUrl(
      "/circulation/fee-fine-scheduled-notices-processing");

    timedTaskClient.start(url, 204,
      "fee-fine-scheduled-notices-processing-request");
  }

  private void runWithFixedClock(Runnable runnable, ZonedDateTime mockSystemTime) {
    try {
      ClockManager.setClock(Clock.fixed(mockSystemTime.toInstant(),
        ZoneOffset.UTC));

      runnable.run();
    } finally {
      ClockManager.setDefaultClock();
    }
  }

}
