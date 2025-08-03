package com.mogrul.economy;

import com.mogrul.economy.utils.ConfigBuilder;
import com.mogrul.economy.utils.database.DatabaseManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MogrulEconomy.MODID)
public class MogrulEconomy
{
    public static final String MODID = "mogruleconomy";

    public MogrulEconomy(FMLJavaModLoadingContext context)
    {
        ConfigBuilder.registerConfig(context);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        DatabaseManager.init();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        DatabaseManager.close();
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DatabaseManager.addPlayer(player);
    }
}
