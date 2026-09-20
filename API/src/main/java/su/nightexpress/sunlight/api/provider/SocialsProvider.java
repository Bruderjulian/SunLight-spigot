package su.nightexpress.sunlight.api.provider;

import java.util.Map;

public interface SocialsProvider {

    void broadcastToDiscord(String text);

    Map<String, String> getLinks();
}
