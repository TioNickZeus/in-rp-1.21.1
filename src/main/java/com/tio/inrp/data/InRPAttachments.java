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

    public static boolean isInRP(Player player) {
        if (player == null) return false;
        return Boolean.TRUE.equals(player.getData(IN_RP));
    }

    public static void setInRP(Player player, boolean inRP) {
        if (player == null) return;
        player.setData(IN_RP, inRP);
    }
}
