package com.mogrul.economy;

import com.mogrul.economy.commands.TradeCommands;
import com.mogrul.economy.utils.Config;
import com.mogrul.economy.utils.ConfigBuilder;
import com.mogrul.economy.utils.database.CurrencyManager;
import com.mogrul.economy.utils.database.DatabaseManager;
import com.mogrul.economy.utils.database.MobRewardsManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod(MogrulEconomy.MODID)
public class MogrulEconomy
{
    public static final String MODID = "mogruleconomy";

    public MogrulEconomy(FMLJavaModLoadingContext context) {
        ConfigBuilder.registerConfig(context);

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
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

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity directEntity = source.getEntity();

        if (!(directEntity instanceof ServerPlayer player)) return;

        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(victim.getType());
        if (mobId == null) return;

        int reward = MobRewardsManager.getReward(mobId.toString());
        if (reward > 0) {
            CurrencyManager.addCurrency(player, reward);

            player.sendSystemMessage(Component.literal("You earned " + Config.currencySymbol + reward + " for killing a " + victim.getDisplayName().getString()));
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long now = System.currentTimeMillis();
        List<String> expired = new ArrayList<>();

        for (Map.Entry<String, TradeCommands.PendingTrade> entry : TradeCommands.pendingTrades.entrySet()) {
            if (entry.getValue().expiresAt() <= now) {
                expired.add(entry.getKey());
            }
        }

        for (String id : expired) {
            TradeCommands.PendingTrade trade = TradeCommands.pendingTrades.remove(id);
            if (trade != null) {
                TradeCommands.onTradeExpire(trade);
            }
        }
    }
}
