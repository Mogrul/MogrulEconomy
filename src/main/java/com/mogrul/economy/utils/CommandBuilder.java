package com.mogrul.economy.utils;

import com.mogrul.economy.MogrulEconomy;
import com.mogrul.economy.commands.CurrencyCommands;
import com.mogrul.economy.commands.MobRewardsCommands;
import com.mogrul.economy.commands.TradeCommands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = MogrulEconomy.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        CurrencyCommands.registerCommand(dispatcher);

        if (Boolean.TRUE.equals(Config.tradeEnabled)) {
            TradeCommands.registerCommand(dispatcher);
        } else {
            LOGGER.info("[{}] Trade component disabled, skipping trade command registration.", MogrulEconomy.MODID);
        }

        if (Boolean.TRUE.equals(Config.mobRewardsEnabled)) {
            MobRewardsCommands.registerCommand(dispatcher);
        } else {
            LOGGER.info("[{}] Mob Rewards component disabled, skipping mob rewards command registration.", MogrulEconomy.MODID);
        }
    }
}
