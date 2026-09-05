package com.tio.inrp;

import com.mojang.logging.LogUtils;
import com.tio.inrp.commands.LivesCommand;
import com.tio.inrp.commands.RPAdminCommand;
import com.tio.inrp.commands.RPCommand;
import com.tio.inrp.commands.RollCommand;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.events.ChatEventHandler;
import com.tio.inrp.events.LivesEventHandler;
import com.tio.inrp.events.RPGameplayRulesHandler;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.LocalizationHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(InRP.MODID)
public class InRP {
    public static final String MODID = "inrp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InRP(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing In-RP mod...");

        // Register data attachments
        InRPAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // Register Server Config
        modContainer.registerConfig(ModConfig.Type.SERVER, InRPConfig.SPEC);

        // Listen for config reloading/loading to refresh translations
        modEventBus.addListener(this::onConfigLoad);

        // Register game events on NeoForge bus
        NeoForge.EVENT_BUS.register(ScoreboardHandler.class);
        NeoForge.EVENT_BUS.register(ChatEventHandler.class);
        NeoForge.EVENT_BUS.register(RPGameplayRulesHandler.class);
        NeoForge.EVENT_BUS.register(LivesEventHandler.class);
        NeoForge.EVENT_BUS.register(this);
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == InRPConfig.SPEC) {
            LocalizationHelper.reloadTranslations();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RPCommand.register(event.getDispatcher());
        RollCommand.register(event.getDispatcher());
        RPAdminCommand.register(event.getDispatcher());
        LivesCommand.register(event.getDispatcher());
        LOGGER.info("Registered In-RP commands: /rp, /roll, /rpadmin, /lives");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("In-RP mod active on server.");
        LocalizationHelper.reloadTranslations();
        com.tio.inrp.data.InRPLivesManager.init(event.getServer());
    }
}
