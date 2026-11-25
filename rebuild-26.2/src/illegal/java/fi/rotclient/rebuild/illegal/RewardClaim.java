package fi.rotclient.rebuild.illegal;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

final class RewardClaim {
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private final IllegalSettings settings;
    private String lastLink;
    private String announcedLink;
    private long announcedAt;
    private Pending pending;
    private boolean showingOriginalBook;

    RewardClaim(IllegalSettings settings) {
        this.settings = settings;
    }

    void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) onGameMessage(message);
            return true;
        });
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> {
            if (!overlay) onGameMessage(message);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((connection, client) -> {
            pending = null;
            lastLink = null;
            announcedLink = null;
        });
    }

    private void onGameMessage(Component message) {
        if (!settings.rewardClaim) return;
        findId(message).ifPresent(id -> {
            announcedLink = id;
            announcedAt = System.nanoTime();
            Screen screen = Minecraft.getInstance().gui.screen();
            if (screen instanceof RewardClaimLoadingScreen waiting && waiting.awaitingLink()) {
                open(id, waiting);
            } else if (pending == null && !id.equals(lastLink)) {
                open(id, null);
            }
        });
    }

    boolean openBook(BookViewScreen book, BookViewScreen.BookAccess access) {
        if (!settings.rewardClaim || showingOriginalBook) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) return false;
        var pages = access.pages();
        String id = findBookId(pages).orElse(null);
        if (id == null && !isRewardBook(pages)) return false;
        if (id == null && announcedLink != null && System.nanoTime() - announcedAt <= Duration.ofSeconds(4).toNanos()) {
            id = announcedLink;
        }
        Screen screen = client.gui.screen();
        if (id != null && id.equals(lastLink) && screen instanceof RewardClaimScreen) return true;
        var waiting = new RewardClaimLoadingScreen(returnScreen(screen), book, this);
        client.gui.setScreen(waiting);
        if (id != null) open(id, waiting);
        return true;
    }

    static Optional<String> findBookId(List<Component> pages) {
        if (!isRewardBook(pages)) return Optional.empty();
        for (Component page : pages) {
            Optional<String> id = findId(page);
            if (id.isPresent()) return id;
        }
        return Optional.empty();
    }

    static boolean isRewardBook(List<Component> pages) {
        return pages.stream().map(page -> page.getString().toLowerCase(Locale.ROOT))
                .anyMatch(text -> text.contains("daily reward")
                        && (text.contains("click to claim") || text.contains("reward card")));
    }

    private static Optional<String> findId(Component message) {
        return message.visit((style, text) -> {
            if (style.getClickEvent() instanceof ClickEvent.OpenUrl link) {
                return RewardClaimPolicy.findClaimId(link.uri());
            }
            return Optional.<String>empty();
        }, Style.EMPTY).or(() -> RewardClaimPolicy.findClaimId(message.getString()));
    }

    private void open(String id, RewardClaimLoadingScreen waiting) {
        Minecraft client = Minecraft.getInstance();
        ClientPacketListener connection = client.getConnection();
        if (connection == null || !settings.rewardClaim) return;
        if (waiting != null) waiting.startRequest();
        if (pending != null && pending.id.equals(id) && pending.connection == connection) {
            if (waiting != null) pending.loading = waiting;
            return;
        }
        var current = new Pending(id, connection);
        current.loading = waiting;
        pending = current;
        lastLink = id;
        var session = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .connectTimeout(TIMEOUT).followRedirects(HttpClient.Redirect.NEVER).build();
        URI link = RewardClaimPolicy.fetchUrl(id);
        session.sendAsync(request(link, "text/html").GET().build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) throw new IllegalStateException("Reward page unavailable.");
                    return RewardClaimPolicy.parsePage(response.body(), id);
                })
                .whenComplete((page, error) -> client.execute(() -> {
                    if (pending != current || client.getConnection() != connection) return;
                    pending = null;
                    if (current.loading != null && client.gui.screen() != current.loading) return;
                    if (error != null || !settings.rewardClaim) {
                        lastLink = null;
                        if (current.loading != null) current.loading.showFailure();
                        else if (settings.rewardClaim) notice(client, "Could not load Daily Rewards. Open the official link.");
                        return;
                    }
                    Screen parent = returnScreen(client.gui.screen());
                    client.gui.setScreen(new RewardClaimScreen(parent, page, link, this, session, connection));
                }));
    }

    void showBook(BookViewScreen book) {
        showingOriginalBook = true;
        try {
            Minecraft.getInstance().gui.setScreen(book);
        } finally {
            showingOriginalBook = false;
        }
    }

    private static Screen returnScreen(Screen screen) {
        if (screen instanceof RewardClaimLoadingScreen loading) return loading.parent();
        if (screen instanceof RewardClaimScreen reward) return reward.parent();
        return screen;
    }

    private static final class Pending {
        final String id;
        final ClientPacketListener connection;
        RewardClaimLoadingScreen loading;

        Pending(String id, ClientPacketListener connection) {
            this.id = id;
            this.connection = connection;
        }
    }

    void claim(RewardClaimScreen origin, RewardClaimPolicy.ParsedPage page, RewardClaimPolicy.RewardOption reward,
               HttpClient session, ClientPacketListener connection) {
        Minecraft client = Minecraft.getInstance();
        if (!settings.rewardClaim || client.getConnection() != connection) {
            origin.finishClaim("The reward session ended. Open a fresh link from chat.");
            return;
        }
        URI claimLink = RewardClaimPolicy.claimUrl(page, reward.index());
        session.sendAsync(request(claimLink, "application/json,text/plain")
                .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, error) -> client.execute(() -> {
                    if (client.getConnection() != connection) return;
                    boolean sent = error == null && response.statusCode() >= 200 && response.statusCode() < 300;
                    if (sent && RewardClaimPolicy.successfulClaim(response.body())) {
                        notice(client, "Claimed " + reward.title() + ".");
                        if (client.gui.screen() == origin) origin.onClose();
                        return;
                    }
                    origin.finishClaim(sent ? "Claim sent. Check your reward on the official page."
                            : "Claim could not be confirmed. Check the official page.");
                }));
    }

    private static HttpRequest.Builder request(URI link, String accept) {
        return HttpRequest.newBuilder(link).timeout(TIMEOUT).header("Accept", accept);
    }

    private static void notice(Minecraft client, String text) {
        if (client.player != null) client.player.sendSystemMessage(Component.literal("[Rot Client] " + text));
    }
}
