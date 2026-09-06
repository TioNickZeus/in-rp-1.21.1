package com.tio.inrp.data;

import com.mojang.serialization.Codec;
import com.tio.inrp.InRP;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class InRPAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, InRP.MODID);

    public static final Supplier<AttachmentType<Boolean>> IN_RP = ATTACHMENT_TYPES.register(
            "in_rp",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Integer>> DEATH_COUNT = ATTACHMENT_TYPES.register(
            "death_count",
            () -> AttachmentType.builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Integer>> MAX_LIVES = ATTACHMENT_TYPES.register(
            "max_lives",
            () -> AttachmentType.builder(() -> -1) // -1 means unlimited
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Boolean>> IS_DEAD = ATTACHMENT_TYPES.register(
            "is_dead",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<Boolean>> IS_AFK = ATTACHMENT_TYPES.register(
            "is_afk",
            () -> AttachmentType.builder(() -> false)
                    .serialize(Codec.BOOL)
                    .copyOnDeath()
                    .build()
    );

    // RP Mode
    public static boolean isInRP(Player player) {
        if (player == null) return false;
        return Boolean.TRUE.equals(player.getData(IN_RP));
    }

    public static void setInRP(Player player, boolean inRP) {
        if (player == null) return;
        player.setData(IN_RP, inRP);
    }

    // Death Count
    public static int getDeathCount(Player player) {
        if (player == null) return 0;
        return player.getData(DEATH_COUNT);
    }

    public static void setDeathCount(Player player, int count) {
        if (player == null) return;
        player.setData(DEATH_COUNT, Math.max(0, count));
    }

    public static void incrementDeathCount(Player player) {
        if (player == null) return;
        setDeathCount(player, getDeathCount(player) + 1);
    }

    // Max Lives (-1 = unlimited)
    public static int getMaxLives(Player player) {
        if (player == null) return -1;
        return player.getData(MAX_LIVES);
    }

    public static void setMaxLives(Player player, int maxLives) {
        if (player == null) return;
        player.setData(MAX_LIVES, maxLives);
    }

    public static boolean hasLivesLimit(Player player) {
        return getMaxLives(player) > 0;
    }

    public static int getRemainingLives(Player player) {
        int max = getMaxLives(player);
        if (max <= 0) return -1;
        return Math.max(0, max - getDeathCount(player));
    }

    // Is Permanently Dead
    public static boolean isDead(Player player) {
        if (player == null) return false;
        return Boolean.TRUE.equals(player.getData(IS_DEAD));
    }

    public static void setDead(Player player, boolean dead) {
        if (player == null) return;
        player.setData(IS_DEAD, dead);
    }

    // AFK Mode
    public static boolean isAFK(Player player) {
        if (player == null) return false;
        return Boolean.TRUE.equals(player.getData(IS_AFK));
    }

    public static void setAFK(Player player, boolean afk) {
        if (player == null) return;
        player.setData(IS_AFK, afk);
    }
}
