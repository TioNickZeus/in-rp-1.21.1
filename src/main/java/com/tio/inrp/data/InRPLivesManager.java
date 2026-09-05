package com.tio.inrp.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tio.inrp.InRP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class InRPLivesManager {
    private static final Gson GSON = new Gson();
    private static final Type SET_TYPE = new TypeToken<Set<String>>() {}.getType();
    private static final Object LOCK = new Object();
    private static final Set<UUID> DEAD_PLAYERS = new HashSet<>();
    private static File storageFile = null;

    public static void init(MinecraftServer server) {
        try {
            Path worldDir = server.getWorldPath(LevelResource.ROOT);
            File dir = worldDir.toFile();
            storageFile = new File(dir, "inrp_dead_players.json");
            load();
        } catch (Exception e) {
            InRP.LOGGER.error("Failed to initialize InRPLivesManager storage file", e);
        }
    }

    private static void load() {
        synchronized (LOCK) {
            DEAD_PLAYERS.clear();
            if (storageFile != null && storageFile.exists()) {
                try (FileReader reader = new FileReader(storageFile)) {
                    Set<String> set = GSON.fromJson(reader, SET_TYPE);
                    if (set != null) {
                        for (String str : set) {
                            try {
                                DEAD_PLAYERS.add(UUID.fromString(str));
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    InRP.LOGGER.error("Failed to load inrp_dead_players.json", e);
                }
            }
        }
    }

    // Must be called while holding LOCK
    private static void save() {
        if (storageFile == null) return;
        try {
            File tempFile = new File(storageFile.getParentFile(), "inrp_dead_players.json.tmp");
            try (FileWriter writer = new FileWriter(tempFile)) {
                Set<String> set = new HashSet<>();
                for (UUID id : DEAD_PLAYERS) {
                    set.add(id.toString());
                }
                GSON.toJson(set, writer);
            }
            Files.move(tempFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            InRP.LOGGER.error("Failed to save inrp_dead_players.json", e);
        }
    }

    public static void markDead(UUID uuid) {
        if (uuid == null) return;
        synchronized (LOCK) {
            DEAD_PLAYERS.add(uuid);
            save();
        }
    }

    public static void unmarkDead(UUID uuid) {
        if (uuid == null) return;
        synchronized (LOCK) {
            DEAD_PLAYERS.remove(uuid);
            save();
        }
    }

    public static boolean isMarkedDead(UUID uuid) {
        if (uuid == null) return false;
        synchronized (LOCK) {
            return DEAD_PLAYERS.contains(uuid);
        }
    }
}
