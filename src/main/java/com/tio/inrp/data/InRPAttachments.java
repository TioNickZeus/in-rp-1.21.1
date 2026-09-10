package com.tio.inrp.data;

import com.mojang.serialization.Codec;
import com.tio.inrp.InRP;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Per-player state, persisted by NeoForge into {@code <world>/playerdata/<uuid>.dat}.
 *
 * <p>Every attachment declares {@code copyOnDeath()} so roleplay state survives the death/respawn cycle, and every
 * accessor tolerates a {@code null} player so event handlers never need their own guards.
 */
public final class InRPAttachments {

    /** Value reported for {@code MAX_LIVES} when a player has no lives limit. */
    public static final int UNLIMITED_LIVES = -1;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, InRP.MODID);

    public static final Supplier<AttachmentType<Boolean>> IN_RP = registerFlag("in_rp");
    public static final Supplier<AttachmentType<Boolean>> IS_DEAD = registerFlag("is_dead");
    public static final Supplier<AttachmentType<Boolean>> IS_AFK = registerFlag("is_afk");
    public static final Supplier<AttachmentType<Boolean>> IS_CHAT_SPY = registerFlag("is_chat_spy");

    public static final Supplier<AttachmentType<Integer>> DEATH_COUNT = registerCounter("death_count", 0);
    public static final Supplier<AttachmentType<Integer>> MAX_LIVES = registerCounter("max_lives", UNLIMITED_LIVES);

    private InRPAttachments() {
    }

    // ---------------------------------------------------------------- RP mode

    public static boolean isInRP(Player player) {
        return getFlag(player, IN_RP);
    }

    public static void setInRP(Player player, boolean inRP) {
        setFlag(player, IN_RP, inRP);
    }

    // ------------------------------------------------------------ Death count

    public static int getDeathCount(Player player) {
        return getCounter(player, DEATH_COUNT, 0);
    }

    public static void setDeathCount(Player player, int count) {
        if (player != null) {
            player.setData(DEATH_COUNT.get(), Math.max(0, count));
        }
    }

    public static void incrementDeathCount(Player player) {
        setDeathCount(player, getDeathCount(player) + 1);
    }

    // -------------------------------------------------------------- Max lives

    public static int getMaxLives(Player player) {
        return getCounter(player, MAX_LIVES, UNLIMITED_LIVES);
    }

    public static void setMaxLives(Player player, int maxLives) {
        if (player != null) {
            player.setData(MAX_LIVES.get(), Math.max(UNLIMITED_LIVES, maxLives));
        }
    }

    /** @return whether a finite lives limit applies to this player. */
    public static boolean hasLivesLimit(Player player) {
        return getMaxLives(player) > 0;
    }

    /** @return the lives left, or {@link #UNLIMITED_LIVES} when the player has no limit. */
    public static int getRemainingLives(Player player) {
        int max = getMaxLives(player);
        if (max <= 0) {
            return UNLIMITED_LIVES;
        }
        return Math.max(0, max - getDeathCount(player));
    }

    /** @return whether the player exhausted their lives and has not been revived. */
    public static boolean hasRunOutOfLives(Player player) {
        return hasLivesLimit(player) && getDeathCount(player) >= getMaxLives(player);
    }

    // --------------------------------------------------------- Permanent death

    public static boolean isDead(Player player) {
        return getFlag(player, IS_DEAD);
    }

    public static void setDead(Player player, boolean dead) {
        setFlag(player, IS_DEAD, dead);
    }

    // --------------------------------------------------------------- AFK state

    public static boolean isAFK(Player player) {
        return getFlag(player, IS_AFK);
    }

    public static void setAFK(Player player, boolean afk) {
        setFlag(player, IS_AFK, afk);
    }

    // ---------------------------------------------------------------- Chat spy

    public static boolean isChatSpy(Player player) {
        return getFlag(player, IS_CHAT_SPY);
    }

    public static void setChatSpy(Player player, boolean spy) {
        setFlag(player, IS_CHAT_SPY, spy);
    }

    // ---------------------------------------------------------------- Internals

    private static Supplier<AttachmentType<Boolean>> registerFlag(String name) {
        return ATTACHMENT_TYPES.register(name, () -> AttachmentType.builder(() -> false)
                .serialize(Codec.BOOL)
                .copyOnDeath()
                .build());
    }

    private static Supplier<AttachmentType<Integer>> registerCounter(String name, int defaultValue) {
        return ATTACHMENT_TYPES.register(name, () -> AttachmentType.builder(() -> defaultValue)
                .serialize(Codec.INT)
                .copyOnDeath()
                .build());
    }

    /**
     * Reads a flag without touching the holder. {@code getData} materialises the default value into the player's
     * attachment map, so the {@code hasData} check keeps read-only lookups (hot paths such as the per-tick AFK
     * guard) free of side effects.
     */
    private static boolean getFlag(Player player, Supplier<AttachmentType<Boolean>> type) {
        if (player == null) {
            return false;
        }
        AttachmentType<Boolean> attachment = type.get();
        return player.hasData(attachment) && Boolean.TRUE.equals(player.getData(attachment));
    }

    private static void setFlag(Player player, Supplier<AttachmentType<Boolean>> type, boolean value) {
        if (player != null) {
            player.setData(type.get(), value);
        }
    }

    private static int getCounter(Player player, Supplier<AttachmentType<Integer>> type, int absentValue) {
        if (player == null) {
            return absentValue;
        }
        AttachmentType<Integer> attachment = type.get();
        if (!player.hasData(attachment)) {
            return absentValue;
        }
        Integer value = player.getData(attachment);
        return value != null ? value : absentValue;
    }
}
