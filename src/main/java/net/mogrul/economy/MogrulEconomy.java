package net.mogrul.economy;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.mogrul.economy.commands.TradeCommands;
import net.mogrul.economy.data.*;
import net.mogrul.economy.handlers.PlayerDataHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Iterator;
import java.util.Map;

import static net.mogrul.economy.builders.ConfigBuilder.COMMON_CONFIG;

@Mod(MogrulEconomy.MODID)
public class MogrulEconomy {
    public static final String MODID = "mogruleconomy";
    public static final String LOGNAME = "MogrulEconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static MinecraftServer server;

    public MogrulEconomy(ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
    }

    public static Style hexToTextColorStyle(String hex) {
        // Remove the '#' character if it exists
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        // Parse the hex string into an RGB integer
        int rgb = Integer.parseInt(hex, 16);
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    @SubscribeEvent
    public void onEntityDeath(LivingDeathEvent event) {
        if (!Config.mobRewardsEnabled) return;

        LivingEntity victimEntity = event.getEntity();
        DamageSource damageSource = event.getSource();
        Entity killerEntity = damageSource.getEntity();

        if (!(killerEntity instanceof ServerPlayer killerPlayer)) return;
        if (victimEntity instanceof ServerPlayer) return;

        if (Config.mobRewardsEnabled) {
            ResourceLocation mobResource = EntityType.getKey(victimEntity.getType());
            String mobString = mobResource.toString();
            String mobIDString = mobString.replace(":", "_");

            MobRewardData mobRewardData = MemoryData.mobRewards.get(mobIDString);
            if (mobRewardData == null) return;
            PlayerData killerData = MemoryData.players.get(killerPlayer.getStringUUID());
            if (killerData == null) return;
            killerData.currency += mobRewardData.rewardAmount;
            PlayerDataHandler.save(killerData);

            LOGGER.info(
                    "[{}] {} has been rewarded {}{} for killing {}!",
                    LOGNAME,
                    killerData.name,
                    Config.currencySymbol,
                    mobRewardData.rewardAmount,
                    mobString
            );

            Component mobRewardSuccessMessage = Component.literal("[Mob Rewards]\n")
                    .append(Component.literal(mobString)
                            .withStyle(hexToTextColorStyle(Colours.mobName)
                                    .withBold(true)
                            )
                    )
                    .append(Component.literal(" -> "))
                    .append(Component.literal(Config.currencySymbol + mobRewardData.rewardAmount)
                            .withStyle(hexToTextColorStyle(Colours.currencyName)
                                    .withBold(true)
                            )
                    );

            killerPlayer.sendSystemMessage(mobRewardSuccessMessage);
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!Config.tradeEnabled) return;

        long now = System.currentTimeMillis();

        // Using an iterator to safely remove entries from the map
        Iterator<Map.Entry<String, PendingTradeData>> iterator = TradeCommands.pendingTrades.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, PendingTradeData> entry = iterator.next();
            PendingTradeData pendingTradeData = entry.getValue();

            // Check for expiration
            if (pendingTradeData.expiresAt <= now) {
                // Remove the expired entry safely using the iterator
                iterator.remove();

                // Send the items back to the player
                TradeCommands.sendItemsBack(pendingTradeData.fromPlayer, pendingTradeData.items);

                // Notify the players involved in the trade
                pendingTradeData.fromPlayer.sendSystemMessage(Component.literal("Your trade expired!"));
                pendingTradeData.toPlayer.sendSystemMessage(Component.literal(
                                "Trade from " +
                                        pendingTradeData.fromPlayer.getName().getString() +
                                        " expired!"
                        )
                );
            }
        }
    }

    public static boolean canFitInPlayerInventory(Inventory inventory, ItemStack stack) {
        for (int i = 0; i <= 35; i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(slotStack, stack) && slotStack.getCount() < slotStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
}
