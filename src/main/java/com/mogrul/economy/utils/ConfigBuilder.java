package com.mogrul.economy.utils;

import com.mogrul.economy.MogrulEconomy;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = MogrulEconomy.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final CommonConfig COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(builder);
        COMMON_CONFIG = builder.build();
    }

    public static class CommonConfig {
        // General configurations.
        public final ForgeConfigSpec.ConfigValue<String> currencyName;
        public final ForgeConfigSpec.ConfigValue<String> currencyCommandName;
        public final ForgeConfigSpec.ConfigValue<String> currencySymbol;
        public final ForgeConfigSpec.ConfigValue<Integer> startingCurrency;

        // Mob rewards configurations.
        public final ForgeConfigSpec.ConfigValue<Boolean> mobRewardsEnabled;
        public final ForgeConfigSpec.ConfigValue<String> mobRewardsCommandName;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            // General configurations.
            builder.push("General");

            currencyName = builder.comment("Name of the currency.").define("currencyName", "Pounds");
            currencyCommandName = builder.comment("Command used to interact with the mod. (lowercase, no spaces)").define("currencyCommandName", "mogrulcurrency");
            currencySymbol = builder.comment("Symbol of the currency.").define("currencySymbol", "£");
            startingCurrency = builder.comment("Starting currency for players.").defineInRange("startingCurrency", 500, 0, Integer.MAX_VALUE);

            builder.pop();

            // Mob rewards configurations.
            builder.push("MobRewards");

            mobRewardsEnabled = builder.comment("Enable/Disable mob rewards.").define("mobRewardsEnabled", true);
            mobRewardsCommandName = builder.comment("Command used to interact with the mob rewards component. (lowercase, no spaces)")
                    .define("mobRewardsCommandName", "mobrewards");

            builder.pop();
        }
    }

    public static void registerConfig(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, ConfigBuilder.COMMON_CONFIG);
        LOGGER.info("[{}] Registered configs", MogrulEconomy.MODID);
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == COMMON_CONFIG) {
            LOGGER.info("Reloading common config.");

            setConfigs();
        }
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == COMMON_CONFIG) {
            LOGGER.info("[{}] Loading common config.", MogrulEconomy.MODID);

            setConfigs();
        }
    }

    private static void setConfigs() {
        // General configurations.
        Config.currencyName = COMMON.currencyName.get();
        Config.currencyCommandName = COMMON.currencyCommandName.get();
        Config.currencySymbol = COMMON.currencySymbol.get();
        Config.startingCurrency = COMMON.startingCurrency.get();

        // Mob rewards configurations.
        Config.mobRewardsEnabled = COMMON.mobRewardsEnabled.get();
        Config.mobRewardsCommandName = COMMON.mobRewardsCommandName.get();
    }
}
