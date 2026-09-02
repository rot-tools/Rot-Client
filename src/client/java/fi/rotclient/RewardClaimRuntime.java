package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Opens the in-client daily-reward UI when Hypixel posts a
 * rewards.hypixel.net claim link.
 */
public final class RewardClaimRuntime {
    private static volatile long blockBrowserUntilMs;
    private static volatile boolean fetching;

    private RewardClaimRuntime() {
    }

    public static void onChat(String message) {
        if (!enabled() || fetching) {
            return;
        }
        RewardClaimPolicy.findClaimId(message).ifPresent(RewardClaimRuntime::open);
    }

    public static boolean shouldHideMessage(String message) {
        return enabled()
                && extras().rewardClaimMuteChatLink
                && RewardClaimPolicy.findClaimId(message).isPresent();
    }

    public static boolean shouldBlockScreen(Screen screen) {
        if (!enabled()
                || screen == null
                || !extras().rewardClaimBlockBrowser
                || System.currentTimeMillis() > blockBrowserUntilMs) {
            return false;
        }
        String name = screen.getClass().getName();
        return name.contains("ConfirmLink") || name.contains("ConfirmScreen");
    }

    static void open(String claimId) {
        if (!enabled() || claimId == null || claimId.isBlank() || fetching) {
            return;
        }
        fetching = true;
        blockBrowserUntilMs = System.currentTimeMillis() + 8_000L;
        CookieManager cookies = new CookieManager();
        cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        HttpClient http = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(12))
                .build();
        CompletableFuture.supplyAsync(() -> fetchPage(http, claimId))
                .whenComplete((page, error) -> Minecraft.getInstance().execute(() -> {
                    fetching = false;
                    if (error != null || page == null) {
                        notify("Could not load daily rewards. Use the Hypixel link if this keeps happening.");
                        return;
                    }
                    Minecraft client = Minecraft.getInstance();
                    if (client != null) {
                        client.gui.setScreen(new RewardClaimScreen(page, http));
                    }
                }));
    }

    static RewardClaimPolicy.ParsedPage fetchPage(HttpClient http, String claimId) {
        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(RewardClaimPolicy.fetchUrl(claimId)))
                            .GET()
                            .header("User-Agent", RewardClaimPolicy.USER_AGENT)
                            .header("Accept", "text/html,application/json")
                            .timeout(Duration.ofSeconds(15))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            return RewardClaimPolicy.parsePage(response.body());
        } catch (Exception exception) {
            throw new IllegalStateException(exception.getMessage(), exception);
        }
    }

    static void claim(
            RewardClaimPolicy.ParsedPage page,
            int option,
            HttpClient http,
            Runnable onDone) {
        CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> response = http.send(
                        HttpRequest.newBuilder(URI.create(RewardClaimPolicy.claimUrl(page, option)))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .header("User-Agent", RewardClaimPolicy.USER_AGENT)
                                .header("Accept", "application/json,text/plain,*/*")
                                .timeout(Duration.ofSeconds(15))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("HTTP " + response.statusCode());
                }
            } catch (Exception exception) {
                throw new IllegalStateException(exception.getMessage(), exception);
            }
        }).whenComplete((ignored, error) -> Minecraft.getInstance().execute(() -> {
            if (error != null) {
                notify("Claim failed: " + rootMessage(error));
            } else {
                RewardClaimPolicy.RewardOption reward = page.rewards().get(option);
                notify("Claimed " + reward.title() + ".");
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.gui.screen() instanceof RewardClaimScreen) {
                    client.gui.setScreen(null);
                }
            }
            if (onDone != null) {
                onDone.run();
            }
        }));
    }

    private static boolean enabled() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol != null && qol.isModuleEnabled("qol.reward_claim");
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }

    private static void notify(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(RotClientChat.message(text));
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? "unknown error" : message;
    }
}
