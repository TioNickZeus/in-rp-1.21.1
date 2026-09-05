package com.tio.inrp.data;

import com.tio.inrp.InRP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class InRPAttachments {
    private static final String NBT_KEY_INRP = InRP.MODID;
    private static final String TAG_IN_RP = "in_rp";
    private static final String TAG_DEATH_COUNT = "death_count";
    private static final String TAG_MAX_LIVES = "max_lives";
    private static final String TAG_IS_DEAD = "is_dead";

    private static CompoundTag getModData(Player player) {
        if (player == null) return null;
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        return persisted.getCompound(NBT_KEY_INRP);
    }

    private static CompoundTag getModDataForWrite(Player player) {
        if (player == null) return null;
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        CompoundTag modData = persisted.getCompound(NBT_KEY_INRP);
        persisted.put(NBT_KEY_INRP, modData);
        persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        return modData;
    }

    // RP Mode
    public static boolean isInRP(Player player) {
        if (player == null) return false;
        CompoundTag tag = getModData(player);
        return tag != null && tag.getBoolean(TAG_IN_RP);
    }

    public static void setInRP(Player player, boolean inRP) {
        if (player == null) return;
        CompoundTag tag = getModDataForWrite(player);
        if (tag != null) {
            tag.putBoolean(TAG_IN_RP, inRP);
        }
    }

    // Death Count
    public static int getDeathCount(Player player) {
        if (player == null) return 0;
        CompoundTag tag = getModData(player);
        return tag != null ? tag.getInt(TAG_DEATH_COUNT) : 0;
    }

    public static void setDeathCount(Player player, int count) {
        if (player == null) return;
        CompoundTag tag = getModDataForWrite(player);
        if (tag != null) {
            tag.putInt(TAG_DEATH_COUNT, Math.max(0, count));
        }
    }

    public static void incrementDeathCount(Player player) {
        if (player == null) return;
        setDeathCount(player, getDeathCount(player) + 1);
    }

    // Max Lives (-1 = unlimited)
    public static int getMaxLives(Player player) {
        if (player == null) return -1;
        CompoundTag tag = getModData(player);
        if (tag != null && tag.contains(TAG_MAX_LIVES)) {
            return tag.getInt(TAG_MAX_LIVES);
        }
        return -1;
    }

    public static void setMaxLives(Player player, int maxLives) {
        if (player == null) return;
        CompoundTag tag = getModDataForWrite(player);
        if (tag != null) {
            tag.putInt(TAG_MAX_LIVES, maxLives);
        }
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
        CompoundTag tag = getModData(player);
        return tag != null && tag.getBoolean(TAG_IS_DEAD);
    }

    public static void setDead(Player player, boolean dead) {
        if (player == null) return;
        CompoundTag tag = getModDataForWrite(player);
        if (tag != null) {
            tag.putBoolean(TAG_IS_DEAD, dead);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player original = event.getOriginal();
            Player newPlayer = event.getEntity();
            CompoundTag oldData = getModData(original);
            if (oldData != null) {
                CompoundTag target = getModDataForWrite(newPlayer);
                if (target != null) {
                    target.merge(oldData.copy());
                }
            }
        }
    }
}
