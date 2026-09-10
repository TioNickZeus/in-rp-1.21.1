package com.tio.inrp;

import com.mojang.logging.LogUtils;
import com.tio.inrp.commands.AFKCommand;
import com.tio.inrp.commands.ChatSpyCommand;
import com.tio.inrp.commands.GlobalChatCommand;
import com.tio.inrp.commands.LivesCommand;
import com.tio.inrp.commands.RPAdminCommand;
import com.tio.inrp.commands.RPCommand;
import com.tio.inrp.commands.RollCommand;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.data.InRPLivesManager;
import com.tio.inrp.events.AFKEventHandler;
import com.tio.inrp.events.ChatEventHandler;
import com.tio.inrp.events.LivesEventHandler;
import com.tio.inrp.events.RPGameplayRulesHandler;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.ConfirmationManager;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

/**
 * Entry point for In-RP, a fully server-side roleplay toolkit for Minecraft 1.21.1 on NeoForge.
 *
 * <p>The mod registers no client-side resources and communicates only through vanilla packets (system messages,
 * scoreboard teams, tab list and game mode updates), which is what lets unmodified clients join a server running it.
 */
@Mod(InRP.MODID)
public class InRP {

    public static final String MODID = "inrp";

    /** Vanilla operator level required by every staff-facing command and by the RP rule bypass. */
    public static final int STAFF_PERMISSION_LEVEL = 2;

    public static final Logger LOGGER = LogUtils.getLogger();

    public InRP(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing In-RP mod...");

        InRPAttachments.ATTACHMENT_TYPES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, InRPConfig.SPEC);

        // Translations depend on a config value, so they are refreshed whenever the config is (re)loaded.
        modEventBus.addListener(this::onConfigLoad);

        NeoForge.EVENT_BUS.register(ScoreboardHandler.class);
        NeoForge.EVENT_BUS.register(RPGameplayRulesHandler.class);
        NeoForge.EVENT_BUS.register(LivesEventHandler.class);
        NeoForge.EVENT_BUS.register(AFKEventHandler.class);
        NeoForge.EVENT_BUS.register(ChatEventHandler.class);
        NeoForge.EVENT_BUS.register(this);
    }

    private void onConfigLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == InRPConfig.SPEC && !(event instanceof ModConfigEvent.Unloading)) {
            LocalizationHelper.reloadTranslations();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        RPCommand.register(event.getDispatcher());
        RollCommand.register(event.getDispatcher());
        RPAdminCommand.register(event.getDispatcher());
        LivesCommand.register(event.getDispatcher());
        AFKCommand.register(event.getDispatcher());
        GlobalChatCommand.register(event.getDispatcher());
        ChatSpyCommand.register(event.getDispatcher());
        LOGGER.info("Registered In-RP commands: /rp, /roll, /rpadmin, /lives, /afk, /g, /chatspy");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LocalizationHelper.reloadTranslations();
        InRPConfig.logSuspiciousValues();
        InRPLivesManager.init(event.getServer());
        LOGGER.info("In-RP mod active on server.");
    }

    /**
     * Releases every piece of in-memory state. A single JVM can load several worlds in a row (single player, or a
     * server reload), and stale cooldowns or a stale eliminated-player list would otherwise carry over.
     */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        InRPLivesManager.shutdown();
        AFKEventHandler.reset();
        AFKCommand.reset();
        GlobalChatCommand.reset();
        ConfirmationManager.reset();
    }

    /** Drops the per-session state that is keyed by player UUID but not meant to survive a disconnect. */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GlobalChatCommand.clearCooldown(player.getUUID());
            ConfirmationManager.clear(player.getUUID());
        }
    }
}
