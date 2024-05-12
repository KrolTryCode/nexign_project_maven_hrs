package nexign_project_maven.hrs.utils;

import nexign_project_maven.hrs.model.TariffData;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Utils {
    public static final String GROUP_ID = "hrs-service";
    public static final String TARIFF_DATA_TOPIC = "tariffDataTopic";
    public static final String SUBSCRIBER_DATA_TOPIC = "subscriberDataTopic";
    public static final String AUTH_RECORDS_TOPIC = "authRecordsTopic";
    public static final String PAYMENT_TOPIC = "payment";
    public static final String MONTH_EVENT_TOPIC = "monthEventTopic";

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy").withZone(ZoneId.systemDefault());

    // Метод для преобразования Unix-времени в строку "Месяц-Год"
    public static String unixTimeToMonthYear(long unixTime) {
        return formatter.format(Instant.ofEpochSecond(unixTime));
    }

    public static double calculatePayment(double minutes, boolean isSameOperator, TariffData tariffData) {
        return isSameOperator ? minutes * tariffData.callRateSameOperator() : minutes * tariffData.callRateOtherOperator();
    }
}
