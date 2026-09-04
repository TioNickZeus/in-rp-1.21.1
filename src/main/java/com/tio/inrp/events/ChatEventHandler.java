package com.tio.inrp.events;

import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class ChatEventHandler {
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (InRPAttachments.isInRP(player)) {
            // Keep for custom processing if needed
        }
    }

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player && InRPAttachments.isInRP(player)) {
            event.setDisplayname(Component.empty()
                    .append(event.getDisplayname())
                    .append(Component.literal(" "))
                    .append(com.tio.inrp.util.LocalizationHelper.getMessage("inrp.chat.suffix").withStyle(ChatFormatting.GOLD)));
        }
    }
}
