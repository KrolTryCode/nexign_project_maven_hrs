package nexign_project_maven.hrs.hrs_service;

import nexign_project_maven.hrs.cache.CacheManagerDB;
import nexign_project_maven.hrs.model.TariffData;
import nexign_project_maven.hrs.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static nexign_project_maven.hrs.utils.Utils.*;
/**
 * Service class for managing billing and call record processing.
 * It listens to Kafka topics for call records and processes billing based on tariff and subscriber information stored in cache.
 */
@Service
public class HrsService {

    public static KafkaTemplate<String, String> kafkaTemplate;
    public static CacheManagerDB cacheManagerDB;
    /**
     * Constructs the HRS (Hourly Rate Service) with necessary Kafka template and cache manager.
     *
     * @param kafkaTemplate the Kafka template for message sending
     * @param cacheManagerDB the database cache manager for retrieving subscriber and tariff information
     */
    @Autowired
    public HrsService(KafkaTemplate<String, String> kafkaTemplate, CacheManagerDB cacheManagerDB) {
        HrsService.kafkaTemplate = kafkaTemplate;
        HrsService.cacheManagerDB = cacheManagerDB;
    }

    private long prev_time = 0;

    /**
     * Processes monthly operations by resetting usage data and notifying change of month.
     */
    private void processNewMonth() {
        HrsService.monthlyMinutes.forEach((k, v) -> {
            debit(k);
            v = 0.0;
        });
        kafkaTemplate.send(MONTH_EVENT_TOPIC, "changeMonth");
    }

    /**
     * Calculates and sends the billing information to Kafka based on the subscriber's phone number.
     *
     * @param phoneNumber the phone number of the subscriber to be debited
     */
    public synchronized static void debit(String phoneNumber) {
        double payment = cacheManagerDB.getTariffById(cacheManagerDB.getSubscriberByPhoneNumber(phoneNumber).tariffId()).monthlyFee();
        kafkaTemplate.send(Utils.PAYMENT_TOPIC, phoneNumber + "," + payment);
        System.out.println(phoneNumber + "," + payment);
    }

    // Кэш для хранения использованных минут за месяц
    public static final Map<String, Double> monthlyMinutes = new HashMap<>();

    /**
     * Listens to call records from Kafka and processes each record for billing.
     * It calculates the charges based on call duration, tariff details, and subscriber information.
     *
     * @param record the call record data received from Kafka
     */
    @KafkaListener(topics = Utils.AUTH_RECORDS_TOPIC, groupId = Utils.GROUP_ID)
    public synchronized void processCallRecord(String record) {
        String[] data = record.split(",");

        int callType = Integer.parseInt(data[0]);
        String servedPhoneNumber = data[1];
        String callingPhoneNumber = data[2];
        long startTime = Long.parseLong(data[3]);
        long endTime = Long.parseLong(data[4]);
        int tariffId = Integer.parseInt(data[5]);

        TariffData tariffData = cacheManagerDB.getTariffById(tariffId);

        int durationInMinutes = (int) Math.ceil((endTime - startTime) / 60.0);
        double payment = 0;
        boolean isSameOperator = (cacheManagerDB.getSubscriberByPhoneNumber(callingPhoneNumber) != null);
        double usedMinutesPerMonth = monthlyMinutes.getOrDefault(servedPhoneNumber, 0.0);

        long roundUsedMinutesPerMonth = (long) Math.ceil(usedMinutesPerMonth);

        //для самой первой записи
        if (prev_time == 0 || prev_time > startTime) prev_time = startTime;

        boolean isNewMonth = !unixTimeToMonthYear(startTime).equals(unixTimeToMonthYear(prev_time));
        boolean isMonthTariff = tariffData.monthlyFee() != null && tariffData.freeIncomingMinutes() != null && tariffData.freeIncomingMinutes() - roundUsedMinutesPerMonth > 0;
        prev_time = startTime;

        if (isNewMonth) {
            processNewMonth();
        }

        if (isMonthTariff) {
            boolean isOverLimits =  usedMinutesPerMonth + durationInMinutes > tariffData.freeIncomingMinutes();
            double minutesOverLimit = usedMinutesPerMonth + durationInMinutes - tariffData.freeIncomingMinutes();
            if  (isOverLimits) {
                payment = calculatePayment(minutesOverLimit, isSameOperator, tariffData);
            }
            monthlyMinutes.put(servedPhoneNumber, usedMinutesPerMonth + durationInMinutes);
        } else if (callType == 1) { // Исходящий звонок
            payment = calculatePayment(durationInMinutes, isSameOperator, tariffData);
        }
        if (payment != 0) kafkaTemplate.send(Utils.PAYMENT_TOPIC, servedPhoneNumber + "," + payment);
    }
}
