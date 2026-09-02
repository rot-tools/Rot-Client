package fi.rotclient;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Best-effort, opt-in Discord webhook sender. URL is never logged. */
final class SlayerCarryWebhookRuntime {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private SlayerCarryWebhookRuntime() {}

    static boolean send(String rawUrl, String content) {
        String url = SlayerCarryPolicy.sanitizeWebhookUrl(rawUrl);
        if (url.isBlank() || content == null || content.isBlank()) return false;
        JsonObject body = new JsonObject();
        body.addProperty("content", content.substring(0, Math.min(1800, content.length())));
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ignored -> null);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
