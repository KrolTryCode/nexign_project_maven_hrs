package nexign_project_maven.hrs.cache.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nexign_project_maven.hrs.hrs_service.HrsService;
import nexign_project_maven.hrs.model.SubscriberData;
import nexign_project_maven.hrs.utils.Utils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class SubscriberHandler {

    @KafkaListener(topics = Utils.SUBSCRIBER_DATA_TOPIC, groupId = Utils.GROUP_ID)
    private void handleSubscriberData(String subscribers) throws JsonProcessingException {
        HrsService.cacheManagerDB.clearSubscriberCache();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonArray = objectMapper.readTree(subscribers);
        jsonArray.forEach(jsonNode -> {
            SubscriberData subscriberData = processSubscriberJsonNode(jsonNode);
            HrsService.cacheManagerDB.updateSubscriberCache(subscriberData);
        });
    }

    private SubscriberData processSubscriberJsonNode(JsonNode jsonNode) {
        String phoneNumber = jsonNode.get("phoneNumber").asText();
        int tariffId = jsonNode.get("tariffId").intValue();
        return new SubscriberData(phoneNumber, tariffId, 0);
    }
}
