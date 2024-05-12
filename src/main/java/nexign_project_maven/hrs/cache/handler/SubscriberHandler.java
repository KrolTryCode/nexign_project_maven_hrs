package nexign_project_maven.hrs.cache.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nexign_project_maven.hrs.model.SubscriberData;
import nexign_project_maven.hrs.model.TariffData;
import nexign_project_maven.hrs.utils.Utils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static nexign_project_maven.hrs.hrs_service.HrsService.*;


@Component
public class SubscriberHandler {

    @KafkaListener(topics = Utils.SUBSCRIBER_DATA_TOPIC, groupId = Utils.GROUP_ID)
    private void handleSubscriberData(String subscribers) throws JsonProcessingException {
        cacheManagerDB.clearSubscriberCache();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonArray = objectMapper.readTree(subscribers);
        jsonArray.forEach(jsonNode -> {
            SubscriberData subscriberData = processSubscriberJsonNode(jsonNode);
            cacheManagerDB.updateSubscriberCache(subscriberData);

            int tariffId = subscriberData.tariffId();
            TariffData tariffData = cacheManagerDB.getTariffById(tariffId);

            boolean isMonthTariff = tariffData.monthlyFee() != null && tariffData.freeIncomingMinutes() != null;
            String phoneNumber = subscriberData.phoneNumber();

            if (isMonthTariff) {
                monthlyMinutes.put(phoneNumber, monthlyMinutes.getOrDefault(phoneNumber, 0.0));
            } else {
                monthlyMinutes.remove(phoneNumber);
            }
        });
    }

    private SubscriberData processSubscriberJsonNode(JsonNode jsonNode) {
        String phoneNumber = jsonNode.get("phoneNumber").asText();
        int newTariffId = jsonNode.get("tariffId").asInt();
        if (cacheManagerDB.getSubscriberByPhoneNumber(phoneNumber) != null) {
            int previousTariffId = cacheManagerDB.getSubscriberByPhoneNumber(phoneNumber).tariffId();
            if (monthlyMinutes.containsKey(phoneNumber) && newTariffId != previousTariffId) {
                System.out.println("over raschet");
                debit(phoneNumber);
            }
        }

        return new SubscriberData(phoneNumber, newTariffId);
    }
}
