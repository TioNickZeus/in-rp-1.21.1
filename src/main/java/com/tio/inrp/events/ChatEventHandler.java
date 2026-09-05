package com.tio.inrp.events;

import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChatEventHandler {

    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player && InRPAttachments.isInRP(player)) {
            String configSuffix = InRPConfig.CHAT_SUFFIX != null ? InRPConfig.CHAT_SUFFIX.get() : null;
            Component suffixText = (configSuffix != null && !configSuffix.isEmpty())
                    ? Component.literal(configSuffix)
                    : LocalizationHelper.getMessage("inrp.chat.suffix");

            event.setDisplayname(Component.empty()
                    .append(event.getDisplayname())
                    .append(Component.literal(" "))
                    .append(suffixText.copy().withStyle(ChatFormatting.GOLD)));
        }
    }
}
