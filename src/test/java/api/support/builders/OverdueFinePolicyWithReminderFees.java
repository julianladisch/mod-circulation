package api.support.builders;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.UUID;

public class OverdueFinePolicyWithReminderFees implements Builder {
  JsonObject overdueFinePolicyJson = new JsonObject();
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String REMINDER_FEES_POLICY = "reminderFeesPolicy";
  public static final String REMINDER_SCHEDULE = "reminderSchedule";
  public static final String INTERVAL = "interval";
  public static final String TIME_UNIT_ID = "timeUnitId";
  public static final String REMINDER_FEE = "reminderFee";
  public static final String NOTICE_FORMAT = "noticeFormat";
  public static final String NOTICE_TEMPLATE_ID = "noticeTemplateId";

  public OverdueFinePolicyWithReminderFees(UUID id, String name) {
    overdueFinePolicyJson
      .put(ID, id.toString())
      .put(NAME, name)
      .put(REMINDER_FEES_POLICY, new JsonObject());

    overdueFinePolicyJson.getJsonObject(REMINDER_FEES_POLICY)
      .put(REMINDER_SCHEDULE, new JsonArray());
  }

  JsonArray getReminderSchedule () {
    return overdueFinePolicyJson.getJsonObject(REMINDER_FEES_POLICY).getJsonArray(REMINDER_SCHEDULE);
  }

  public OverdueFinePolicyWithReminderFees withAddedReminderEntry (
    Integer interval,
    String timeUnitId,
    Double reminderFee,
    String noticeFormat,
    String noticeTemplateId) {
    getReminderSchedule().add(
      new JsonObject()
      .put(INTERVAL, interval)
      .put(TIME_UNIT_ID, timeUnitId)
      .put(REMINDER_FEE, reminderFee)
      .put(NOTICE_FORMAT, noticeFormat)
      .put(NOTICE_TEMPLATE_ID, noticeTemplateId));
    return this;
  }

  @Override
  public JsonObject create() {
    return overdueFinePolicyJson;
  }
}
