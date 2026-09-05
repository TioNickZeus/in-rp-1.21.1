package com.tio.inrp;

import com.mojang.logging.LogUtils;
import com.tio.inrp.commands.LivesCommand;
import com.tio.inrp.commands.RPAdminCommand;
import com.tio.inrp.commands.RPCommand;
import com.tio.inrp.commands.RollCommand;
import com.tio.inrp.config.InRPConfig;
import com.tio.inrp.data.InRPAttachments;
import com.tio.inrp.data.InRPLivesManager;
import com.tio.inrp.events.ChatEventHandler;
import com.tio.inrp.events.LivesEventHandler;
import com.tio.inrp.events.RPGameplayRulesHandler;
import com.tio.inrp.events.ScoreboardHandler;
import com.tio.inrp.util.LocalizationHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkConstants;
import org.slf4j.Logger;

@Mod(InRP.MODID)
public class InRP {
    public static final String MODID = "inrp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public InRP(FMLJavaModLoadingContext context) {
        LOGGER.info("Initializing In-RP mod...");

        IEventBus modEventBus = context.getModEventBus();

        // Register Server Config
        context.registerConfig(ModConfig.Type.SERVER, InRPConfig.SPEC);

        // Tell Forge that this is a server-side only mod so clients don't need it installed
        context.registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersion, isFromServer) -> true
                )
        );

        // Listen for config reloading/loading to refresh translations
        modEventBus.addListener(this::onConfigLoad);

        // Register game events on MinecraftForge bus
        MinecraftForge.EVENT_BUS.register(ScoreboardHandler.class);
        MinecraftForge.EVENT_BUS.register(ChatEventHandler.class);
        MinecraftForge.EVENT_BUS.register(RPGameplayRulesHandler.class);
        MinecraftForge.EVENT_BUS.register(LivesEventHandler.class);
        MinecraftForge.EVENT_BUS.register(InRPAttachments.class);
        MinecraftForge.EVENT_BUS.register(this);
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
        InRPLivesManager.init(event.getServer());
    }
}
