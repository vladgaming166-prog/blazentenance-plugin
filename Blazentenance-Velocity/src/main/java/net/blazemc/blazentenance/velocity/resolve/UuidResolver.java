package net.blazemc.blazentenance.velocity.resolve;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.blazemc.blazentenance.common.config.UuidMode;
import net.blazemc.blazentenance.common.util.PlayerName;
import net.blazemc.blazentenance.common.util.Uuids;
import net.blazemc.blazentenance.velocity.maintenance.MaintenanceManager;

public final class UuidResolver implements AutoCloseable {

    private static final URI MOJANG = URI.create("https://api.mojang.com/users/profiles/minecraft/");
    private static final URI SERVICES = URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/");

    private final ProxyServer proxy;
    private final MaintenanceManager maintenance;
    private final HttpClient httpClient;
    private final ConcurrentHashMap<String, ResolvedProfile> cache = new ConcurrentHashMap<>();

    public UuidResolver(ProxyServer proxy, MaintenanceManager maintenance) {
        this.proxy = proxy;
        this.maintenance = maintenance;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Optional<ResolvedProfile> fromOnlinePlayer(String name) {
        return proxy.getPlayer(name).map(player -> new ResolvedProfile(player.getUniqueId(), player.getUsername(), true));
    }

    public CompletableFuture<Optional<ResolvedProfile>> resolve(String raw) {
        Optional<UUID> parsedUuid = Uuids.parse(raw);
        if (parsedUuid.isPresent()) {
            String username = proxy.getPlayer(parsedUuid.get()).map(Player::getUsername).orElse(raw);
            return CompletableFuture.completedFuture(Optional.of(new ResolvedProfile(parsedUuid.get(), username, true)));
        }
        if (!PlayerName.isValid(raw)) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        Optional<ResolvedProfile> online = fromOnlinePlayer(raw);
        if (online.isPresent()) {
            return CompletableFuture.completedFuture(online);
        }
        ResolvedProfile cached = cache.get(PlayerName.key(raw));
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        if (usesOfflineUuids()) {
            ResolvedProfile offline = new ResolvedProfile(Uuids.offline(raw), raw, false);
            cache.put(PlayerName.key(raw), offline);
            return CompletableFuture.completedFuture(Optional.of(offline));
        }
        return CompletableFuture.supplyAsync(() -> lookupMojang(raw));
    }

    private Optional<ResolvedProfile> lookupMojang(String name) {
        Optional<ResolvedProfile> first = lookup(SERVICES.resolve(name));
        if (first.isPresent()) {
            cache.put(PlayerName.key(name), first.get());
            return first;
        }
        Optional<ResolvedProfile> second = lookup(MOJANG.resolve(name));
        second.ifPresent(profile -> cache.put(PlayerName.key(name), profile));
        return second;
    }

    private Optional<ResolvedProfile> lookup(URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Blazentenance/" + net.blazemc.blazentenance.common.Blazentenance.VERSION)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                return Optional.empty();
            }
            return parseProfile(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException exception) {
            return Optional.empty();
        }
    }

    private static Optional<ResolvedProfile> parseProfile(String body) {
        String id = extractJsonString(body, "id");
        String name = extractJsonString(body, "name");
        if (id == null) {
            return Optional.empty();
        }
        return Uuids.parse(id).map(uuid -> new ResolvedProfile(uuid, name == null ? "Unknown" : name, true));
    }

    private static String extractJsonString(String json, String key) {
        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex);
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) {
            return null;
        }
        return json.substring(firstQuote + 1, secondQuote);
    }

    private boolean usesOfflineUuids() {
        UuidMode mode = maintenance.config().uuidMode();
        return switch (mode) {
            case OFFLINE -> true;
            case ONLINE -> false;
            case AUTO -> !proxy.getConfiguration().isOnlineMode();
        };
    }

    @Override
    public void close() {
        httpClient.close();
    }

    public record ResolvedProfile(UUID uuid, String name, boolean official) {
        public String display() {
            return name + " (" + uuid + ")";
        }

        public String key() {
            return name.toLowerCase(Locale.ROOT);
        }
    }
}
