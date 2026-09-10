package com.tio.inrp.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tio.inrp.InRP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Registry of eliminated players, stored in {@code <world>/inrp_dead_players.json}.
 *
 * <p>This store deliberately duplicates the {@code IS_DEAD} attachment. Attachments can only be read while a player
 * is online, so admins could not revive an offline player; the JSON file can be edited at any time and the login
 * handler reconciles the two (file says alive, attachment says dead &rarr; the player was revived while offline).
 *
 * <p>All access is guarded by a single lock, and writes go through a temporary file so an unclean shutdown mid-write
 * cannot truncate the live file. I/O failures are logged and swallowed &mdash; this mod must never take a server down.
 */
public final class InRPLivesManager {

    private static final String FILE_NAME = "inrp_dead_players.json";
    private static final Gson GSON = new Gson();
    private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();

    private static final Object LOCK = new Object();
    private static final Set<UUID> DEAD_PLAYERS = new HashSet<>();

    private static volatile Path storageFile;

    private InRPLivesManager() {
    }

    /** Binds the store to the loaded world and reads its contents. Called on {@code ServerStartingEvent}. */
    public static void init(MinecraftServer server) {
        synchronized (LOCK) {
            DEAD_PLAYERS.clear();
            storageFile = null;
            try {
                storageFile = server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
                load();
            } catch (Exception e) {
                InRP.LOGGER.error("Failed to initialize In-RP eliminated-player storage", e);
            }
        }
    }

    /**
     * Releases the store so a subsequent world load in the same JVM (single player, or a server reload) starts from
     * a clean slate instead of inheriting the previous world's data.
     */
    public static void shutdown() {
        synchronized (LOCK) {
            DEAD_PLAYERS.clear();
            storageFile = null;
        }
    }

    /** Records a player as eliminated. Writes to disk only when the state actually changed. */
    public static void markDead(UUID uuid) {
        if (uuid == null) {
            return;
        }
        synchronized (LOCK) {
            if (DEAD_PLAYERS.add(uuid)) {
                save();
            }
        }
    }

    /** Clears a player's elimination. Writes to disk only when the state actually changed. */
    public static void unmarkDead(UUID uuid) {
        if (uuid == null) {
            return;
        }
        synchronized (LOCK) {
            if (DEAD_PLAYERS.remove(uuid)) {
                save();
            }
        }
    }

    /**
     * Bulk variant of {@link #unmarkDead(UUID)} that performs a single write for the whole batch.
     *
     * @return how many of the given players were actually marked as eliminated.
     */
    public static int unmarkDeadAll(Collection<UUID> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return 0;
        }
        synchronized (LOCK) {
            int removed = 0;
            for (UUID uuid : uuids) {
                if (uuid != null && DEAD_PLAYERS.remove(uuid)) {
                    removed++;
                }
            }
            if (removed > 0) {
                save();
            }
            return removed;
        }
    }

    /** @return whether the player is currently recorded as eliminated. */
    public static boolean isMarkedDead(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        synchronized (LOCK) {
            return DEAD_PLAYERS.contains(uuid);
        }
    }

    /** Reads the store into memory. Callers must hold {@link #LOCK}. */
    private static void load() {
        Path file = storageFile;
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Set<String> stored = GSON.fromJson(reader, SET_TYPE);
            if (stored == null) {
                return;
            }
            for (String raw : stored) {
                if (raw == null) {
                    continue;
                }
                try {
                    DEAD_PLAYERS.add(UUID.fromString(raw));
                } catch (IllegalArgumentException e) {
                    InRP.LOGGER.warn("Ignoring malformed UUID '{}' in {}", raw, FILE_NAME);
                }
            }
            InRP.LOGGER.info("Loaded {} eliminated player(s) from {}", DEAD_PLAYERS.size(), FILE_NAME);
        } catch (Exception e) {
            InRP.LOGGER.error("Failed to load {}", FILE_NAME, e);
        }
    }

    /** Atomically replaces the store on disk. Callers must hold {@link #LOCK}. */
    private static void save() {
        Path file = storageFile;
        if (file == null) {
            return;
        }
        Path temporary = file.resolveSibling(FILE_NAME + ".tmp");
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Set<String> serialized = new LinkedHashSet<>();
            for (UUID uuid : DEAD_PLAYERS) {
                serialized.add(uuid.toString());
            }

            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(serialized, writer);
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            InRP.LOGGER.error("Failed to save {}", FILE_NAME, e);
        }
    }
}
