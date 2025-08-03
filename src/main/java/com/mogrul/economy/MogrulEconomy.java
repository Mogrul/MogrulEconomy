package com.mogrul.economy;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mogrul.economy.utils.ConfigBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mogrul.economy.utils.Config;

@Mod(MogrulEconomy.MODID)
public class MogrulEconomy
{
    public static final String MODID = "mogruleconomy";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MogrulEconomy(FMLJavaModLoadingContext context)
    {
        MinecraftForge.EVENT_BUS.register(this);
        ConfigBuilder.registerConfig(context);
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartedEvent event) {
        LOGGER.info("{}{}{}{}", MODID, Config.currencyName, Config.startingCurrency, Config.currencySymbol);
    }
}
