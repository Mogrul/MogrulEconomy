package net.mogrul.economy.builders;

import net.mogrul.economy.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class ConfigBuilder {
    // Config values.
    // Currency values.
    public static ModConfigSpec.ConfigValue<String> currencyCommandName;
    public static ModConfigSpec.ConfigValue<String> currencyNameSingular;
    public static ModConfigSpec.ConfigValue<String> currencyNamePlural;
    public static ModConfigSpec.ConfigValue<String> currencySymbol;
    public static ModConfigSpec.IntValue startingCurrency;

    // Mob reward values.
    public static ModConfigSpec.BooleanValue mobRewardsEnabled;
    public static ModConfigSpec.ConfigValue<String> mobRewardsCommandName;

    // Trade values.
    public static ModConfigSpec.BooleanValue tradeEnabled;
    public static ModConfigSpec.ConfigValue<String> tradeCommandName;

    // The full config spec
    public static final ModConfigSpec COMMON_CONFIG;
    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Currency");

        currencyCommandName = builder
                .comment("Command to use when using the currency component in-game.")
                .define("currencyCommandName", "currency");
        currencyNameSingular = builder
                .comment("Name to use for singular currency. Example: (Pound)")
                .define("currencyNameSingular", "Pound");
        currencyNamePlural = builder
                .comment("Name to use for plural currency. Example: (Pounds)")
                .define("currencyNamePlural", "Pounds");
        currencySymbol = builder
                .comment("Symbol to use for the currency.")
                .define("currencySymbol", "£");
        startingCurrency = builder
                .comment("Default starting currency for new players")
                .defineInRange("startingCurrency", 100, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("Mob Rewards");

        mobRewardsEnabled = builder
                .comment("Whether the mob rewards component should be anabled.")
                .define("mobRewardsEnabled", true);
        mobRewardsCommandName = builder
                .comment("Command to use when using the mob reward component in-game.")
                .define("mobRewardsCommandName", "mobreward");

        builder.pop();

        builder.push("Trade");

        tradeEnabled = builder
                .comment("Whether the trade component should be enabled.")
                .define("tradeEnabled", true);
        tradeCommandName = builder
                .comment("Command to use when using the trade component in-game.")
                .define("tradeCommandName", "trade");

        COMMON_CONFIG = builder.build();
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (!event.getConfig().getModId().equals(MODID)) return;

        if (event.getConfig().getSpec() == COMMON_CONFIG) {
            LOGGER.info("[{}] Config loading!", LOGNAME);
            setConfig();
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getModId().equals(MODID)) return;

        if (event.getConfig().getSpec() == COMMON_CONFIG) {
            LOGGER.info("[{}] Config reloaded!", LOGNAME);
            setConfig();
        }
    }

    public static void setConfig() {
        // Currency configs.
        Config.currencyCommandName = currencyCommandName.get();
        Config.currencyNameSingular = currencyNameSingular.get();
        Config.currencyNamePlural = currencyNamePlural.get();
        Config.currencySymbol = currencySymbol.get();
        Config.startingCurrency = startingCurrency.get();

        // Mob reward configs.
        Config.mobRewardsEnabled = mobRewardsEnabled.get();
        Config.mobRewardsCommandName = mobRewardsCommandName.get();

        // Trade configs.
        Config.tradeEnabled = tradeEnabled.get();
        Config.tradeCommandName = tradeCommandName.get();
    }
}
