package nexign_project_maven.hrs.cache;

import nexign_project_maven.hrs.model.TariffData;
import nexign_project_maven.hrs.model.SubscriberData;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CacheManagerDB {

    @Cacheable(value = "tariffs", key = "#id")
    public TariffData getTariffById(int id) {
        return null;
    }

    @CachePut(value = "tariffs", key = "#tariffData.id")
    public TariffData updateTariffCache(TariffData tariffData) {
        return tariffData;
    }

    @CacheEvict(value = "tariffs", allEntries = true)
    public void clearTariffCache() {
    }


    @Cacheable(value = "subscribers", key = "#phoneNumber")
    public SubscriberData getSubscriberByPhoneNumber(String phoneNumber) {
        return null;
    }

    @CachePut(value = "subscribers", key = "#subscriberData.phoneNumber")
    public SubscriberData updateSubscriberCache(SubscriberData subscriberData) {
        return subscriberData;
    }

    @CacheEvict(value = "subscribers", allEntries = true)
    public void clearSubscriberCache() {
    }
}