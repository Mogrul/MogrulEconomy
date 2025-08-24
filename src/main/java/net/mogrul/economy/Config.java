package net.mogrul.economy;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class Config {
    // ---
    // PUBLIC CONFIG VALUES
    // ---
    // Currency configs.
    public static String currencyCommandName = "currency";
    public static String currencyNameSingular = "pound";
    public static String currencyNamePlural = "pounds";
    public static String currencySymbol = "£";
    public static Integer startingCurrency = 100;

    // Mob reward configs.
    public static Boolean mobRewardsEnabled = true;
    public static String mobRewardsCommandName = "mobrewards";

    // Trade configs.
    public static Boolean tradeEnabled = true;
    public static String tradeCommandName = "trade";

    // Shop configs.
    public static Boolean shopsEnabled = true;
    public static String shopsCommandName = "shops";

    // Bounty configs.
    public static Boolean bountiesEnabled = true;
    public static String bountiesCommandName = "bounties";
    public static Integer bountiesMinimumCost = 100;

    // ---
    // MODCONFIGSPECS
    // ---
    // Currency values.
    private static final ModConfigSpec.ConfigValue<String> configSpecCurrencyCommandName;
    private static final ModConfigSpec.ConfigValue<String> configSpecCurrencyNameSingular;
    private static final ModConfigSpec.ConfigValue<String> configSpecCurrencyNamePlural;
    private static final ModConfigSpec.ConfigValue<String> configSpecCurrencySymbol;
    private static final ModConfigSpec.IntValue configSpecStartingCurrency;

    // Mob reward values.
    private static final ModConfigSpec.BooleanValue configSpecMobRewardsEnabled;
    private static final ModConfigSpec.ConfigValue<String> configSpecMobRewardsCommandName;

    // Trade values.
    private static final ModConfigSpec.BooleanValue configSpecTradeEnabled;
    private static final ModConfigSpec.ConfigValue<String> configSpecTradeCommandName;

    // Shop values.
    private static final ModConfigSpec.BooleanValue configSpecShopsEnabled;
    private static final ModConfigSpec.ConfigValue<String> configSpecShopsCommandName;

    // Bounty values.
    private static final ModConfigSpec.BooleanValue configSpecBountiesEnabled;
    private static final ModConfigSpec.ConfigValue<String> configSpecBountiesCommandName;
    private static final ModConfigSpec.IntValue configSpecBountiesMinimumCost;

    // The full config spec
    public static final ModConfigSpec COMMON_CONFIG;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Currency");

        configSpecCurrencyCommandName = builder
                .comment("Command to use when using the currency component in-game.")
                .define("currencyCommandName", currencyCommandName);
        configSpecCurrencyNameSingular = builder
                .comment("Name to use for singular currency. Example: (Pound)")
                .define("currencyNameSingular", currencyNameSingular);
        configSpecCurrencyNamePlural = builder
                .comment("Name to use for plural currency. Example: (Pounds)")
                .define("currencyNamePlural", currencyNamePlural);
        configSpecCurrencySymbol = builder
                .comment("Symbol to use for the currency.")
                .define("currencySymbol", currencySymbol);
        configSpecStartingCurrency = builder
                .comment("Default starting currency for new players")
                .defineInRange("startingCurrency", startingCurrency, 0, Integer.MAX_VALUE);

        builder.pop();

        builder.push("MobRewards");

        configSpecMobRewardsEnabled = builder
                .comment("Whether the mob rewards component should be anabled.")
                .define("mobRewardsEnabled", true);
        configSpecMobRewardsCommandName = builder
                .comment("Command to use when using the mob reward component in-game.")
                .define("mobRewardsCommandName", mobRewardsCommandName);

        builder.pop();

        builder.push("Trade");

        configSpecTradeEnabled = builder
                .comment("Whether the trade component should be enabled.")
                .define("tradeEnabled", true);
        configSpecTradeCommandName = builder
                .comment("Command to use when using the trade component in-game.")
                .define("tradeCommandName", tradeCommandName);

        builder.pop();

        builder.push("Shops");

        configSpecShopsEnabled = builder
                .comment("Whether the shop component should be enabled.")
                .define("shopsEnabled", true);
        configSpecShopsCommandName = builder
                .comment("Command to use when using the shops component in-game.")
                .define("shopsCommandName", shopsCommandName);

        builder.pop();

        builder.push("Bounties");

        configSpecBountiesEnabled = builder
                .comment("Whether the bounties component should be enabled.")
                .define("bountiesEnabled", true);

        configSpecBountiesCommandName = builder
                .comment("Command to use when using the bounties component in-game.")
                .define("bountiesCommandName", bountiesCommandName);

        configSpecBountiesMinimumCost = builder
                .comment("Minimum cost to place a bounty on a player.")
                .defineInRange("bountiesMinimumCost", bountiesMinimumCost, 1, Integer.MAX_VALUE);

        builder.pop();

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
        currencyCommandName = configSpecCurrencyCommandName.get();
        currencyNameSingular = configSpecCurrencyNameSingular.get();
        currencyNamePlural = configSpecCurrencyNamePlural.get();
        currencySymbol = configSpecCurrencySymbol.get();
        startingCurrency = configSpecStartingCurrency.get();

        // Mob reward configs.
        mobRewardsEnabled = configSpecMobRewardsEnabled.get();
        mobRewardsCommandName = configSpecMobRewardsCommandName.get();

        // Trade configs.
        tradeEnabled = configSpecTradeEnabled.get();
        tradeCommandName = configSpecTradeCommandName.get();

        // Shop configs.
        shopsEnabled = configSpecShopsEnabled.get();
        shopsCommandName = configSpecShopsCommandName.get();

        // Bounty configs
        bountiesEnabled = configSpecBountiesEnabled.get();
        bountiesCommandName = configSpecBountiesCommandName.get();
        bountiesMinimumCost = configSpecBountiesMinimumCost.get();
    }
}
