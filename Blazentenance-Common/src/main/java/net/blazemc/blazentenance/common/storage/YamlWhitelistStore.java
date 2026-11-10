package net.blazemc.blazentenance.common.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.blazemc.blazentenance.common.config.YamlSupport;
import net.blazemc.blazentenance.common.util.PlayerName;
import net.blazemc.blazentenance.common.util.Uuids;
import org.yaml.snakeyaml.Yaml;

/**
 * Persistent UUID-first whitelist stored in {@code players.yml}.
 */
public final class YamlWhitelistStore {

    private final Path file;
    private final ConcurrentHashMap<UUID, WhitelistedPlayer> byUuid = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UUID> byName = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();

    public YamlWhitelistStore(Path file) {
        this.file = file;
    }

    public void load() throws IOException {
        byUuid.clear();
        byName.clear();
        if (!Files.exists(file)) {
            save();
            return;
        }
        try (InputStream input = Files.newInputStream(file)) {
            Object loaded = new Yaml().load(input);
            if (!(loaded instanceof Map<?, ?> root)) {
                return;
            }
            Object playersNode = root.get("players");
            if (!(playersNode instanceof Map<?, ?> players)) {
                return;
            }
            for (Map.Entry<?, ?> entry : players.entrySet()) {
                Optional<UUID> uuid = Uuids.parse(String.valueOf(entry.getKey()));
                if (uuid.isEmpty() || !(entry.getValue() instanceof Map<?, ?> rawValues)) {
                    continue;
                }
                Map<String, Object> values = YamlSupport.asMap(rawValues);
                String name = YamlSupport.string(values, "name", "Unknown");
                Instant addedAt = parseInstant(YamlSupport.string(values, "added-at", Instant.now().toString()));
                remember(new WhitelistedPlayer(uuid.get(), name, addedAt));
            }
        }
    }

    public void save() throws IOException {
        synchronized (saveLock) {
            Files.createDirectories(file.getParent());
            Map<String, Object> root = new LinkedHashMap<>();
            Map<String, Object> players = new LinkedHashMap<>();
            List<WhitelistedPlayer> snapshot = new ArrayList<>(byUuid.values());
            snapshot.sort(Comparator.comparing(player -> player.name().toLowerCase(Locale.ROOT)));
            for (WhitelistedPlayer player : snapshot) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("name", player.name());
                node.put("added-at", player.addedAt().toString());
                players.put(player.uuid().toString(), node);
            }
            root.put("players", players);

            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            try (Writer writer = new OutputStreamWriter(
                    Files.newOutputStream(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING),
                    StandardCharsets.UTF_8
            )) {
                writer.write("# ==========================================\n");
                writer.write("#   BLAZENTENANCE MAINTENANCE WHITELIST\n");
                writer.write("#   UUID is the primary player identifier.\n");
                writer.write("# ==========================================\n\n");
                YamlSupport.pretty().dump(root, writer);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ignored) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public AddResult add(WhitelistedPlayer player) throws IOException {
        WhitelistedPlayer existing = byUuid.get(player.uuid());
        if (existing != null) {
            if (!existing.name().equals(player.name())) {
                remember(existing.withName(player.name()));
                save();
            }
            return AddResult.ALREADY_PRESENT;
        }
        remember(player);
        save();
        return AddResult.ADDED;
    }

    public Optional<WhitelistedPlayer> remove(String identifier) throws IOException {
        Optional<UUID> uuid = Uuids.parse(identifier);
        WhitelistedPlayer removed;
        if (uuid.isPresent()) {
            removed = byUuid.remove(uuid.get());
        } else {
            UUID mapped = byName.remove(PlayerName.key(identifier));
            removed = mapped == null ? null : byUuid.remove(mapped);
        }
        if (removed == null) {
            return Optional.empty();
        }
        byName.remove(PlayerName.key(removed.name()), removed.uuid());
        save();
        return Optional.of(removed);
    }

    public boolean contains(UUID uuid) {
        return uuid != null && byUuid.containsKey(uuid);
    }

    public boolean containsName(String name) {
        return name != null && byName.containsKey(PlayerName.key(name));
    }

    public Optional<WhitelistedPlayer> findByName(String name) {
        UUID uuid = byName.get(PlayerName.key(name));
        return uuid == null ? Optional.empty() : Optional.ofNullable(byUuid.get(uuid));
    }

    public Optional<WhitelistedPlayer> find(UUID uuid) {
        return Optional.ofNullable(byUuid.get(uuid));
    }

    public List<WhitelistedPlayer> list() {
        List<WhitelistedPlayer> snapshot = new ArrayList<>(byUuid.values());
        snapshot.sort(Comparator.comparing(player -> player.name().toLowerCase(Locale.ROOT)));
        return List.copyOf(snapshot);
    }

    public Collection<WhitelistedPlayer> all() {
        return list();
    }

    public boolean isEmpty() {
        return byUuid.isEmpty();
    }

    public int size() {
        return byUuid.size();
    }

    private void remember(WhitelistedPlayer player) {
        byUuid.put(player.uuid(), player);
        byName.put(PlayerName.key(player.name()), player.uuid());
    }

    private static Instant parseInstant(String raw) {
        try {
            return Instant.parse(raw);
        } catch (RuntimeException ignored) {
            return Instant.now();
        }
    }

    public enum AddResult {
        ADDED,
        ALREADY_PRESENT
    }
}
