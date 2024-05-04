package nexign_project_maven.hrs.cache.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nexign_project_maven.hrs.model.TariffData;
import nexign_project_maven.hrs.utils.Utils;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import static nexign_project_maven.hrs.hrs_service.HrsService.*;


@Component
public class TariffHandler {

    @KafkaListener(topics = Utils.TARIFF_DATA_TOPIC, groupId = Utils.GROUP_ID)
    private void handleTariffData(String tariffs) throws JsonProcessingException {
        cacheManagerDB.clearTariffCache();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonArray = objectMapper.readTree(tariffs);
        jsonArray.forEach(jsonNode -> {
            TariffData tariffData = processTariffJsonNode(jsonNode);
            cacheManagerDB.updateTariffCache(tariffData);
        });
    }

    private TariffData processTariffJsonNode(JsonNode jsonNode) {
        int id = jsonNode.get("id").intValue();
        double callRateSameOperator = jsonNode.get("callRateSameOperator").doubleValue();
        double callRateOtherOperator = jsonNode.get("callRateOtherOperator").doubleValue();
        Integer freeIncomingMinutes = jsonNode.get("freeIncomingMinutes").isNull() ? null : jsonNode.get("freeIncomingMinutes").intValue();
        Double monthlyFee = jsonNode.get("monthlyFee").isNull() ? null : jsonNode.get("monthlyFee").doubleValue();
        return new TariffData(id, callRateSameOperator, callRateOtherOperator, freeIncomingMinutes, monthlyFee);
    }
}
