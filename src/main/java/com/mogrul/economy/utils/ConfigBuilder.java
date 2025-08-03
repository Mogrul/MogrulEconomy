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
        public final ForgeConfigSpec.ConfigValue<String> currencyName;
        public final ForgeConfigSpec.ConfigValue<String> currencySymbol;
        public final ForgeConfigSpec.ConfigValue<Integer> startingCurrency;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("general");

            currencyName = builder.comment("Name of the currency.").define("currencyName", "Pounds");
            currencySymbol = builder.comment("Symbol of the currency.").define("currencySymbol", "£");
            startingCurrency = builder.comment("Starting currency for players.").defineInRange("startingCurrency", 500, 0, Integer.MAX_VALUE);

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
        Config.currencyName = COMMON.currencyName.get();
        Config.currencySymbol = COMMON.currencySymbol.get();
        Config.startingCurrency = COMMON.startingCurrency.get();
    }
}
