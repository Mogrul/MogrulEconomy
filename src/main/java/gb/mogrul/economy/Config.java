package gb.mogrul.economy;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import static gb.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class Config {
    public static final ModConfigSpec COMMON_CONFIG;

    // Config values.
    public static String economyCommandName = "currency";
    public static int economyStartingCurrency = 100;
    public static String economyCurrencySymbol = "£";
    public static String economyCurrencyNamePlural = "Pounds";
    public static String economyCurrencyNameSingular = "Pound";

    // Config specs.
    private static final ModConfigSpec.ConfigValue<String> configEconomyCommandName;
    private static final ModConfigSpec.IntValue configEconomyStartingCurrency;
    private static final ModConfigSpec.ConfigValue<String> configEconomyCurrencySymbol;
    private static final ModConfigSpec.ConfigValue<String> configEconomyCurrencyPlural;
    private static final ModConfigSpec.ConfigValue<String> configEconomyCurrencySingular;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Economy");

        configEconomyCommandName = builder
            .comment("Command to use when using the economy mod in-game.")
            .define("economyCommandName", economyCommandName);
        
        configEconomyStartingCurrency = builder
            .comment("Starting currency given to a player on first join.")
            .defineInRange("economyStartingCurrency", 100, 0, Integer.MAX_VALUE);
        
        configEconomyCurrencySymbol = builder
            .comment("Symbol to use when displaying the currency.")
            .define("economyCurrencySymbol", economyCurrencySymbol);
        
        configEconomyCurrencyPlural = builder
            .comment("Plural name of the currency.")
            .define("economyCurrencyPlural", economyCurrencyNamePlural);
        
        configEconomyCurrencySingular = builder
            .comment("Singular name of the currency.")
            .define("economyCurrencySingular", economyCurrencyNameSingular);

        builder.pop();

        COMMON_CONFIG = builder.build();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (!event.getConfig().getModId().equals(MODID)) return;

        if (event.getConfig().getSpec() == COMMON_CONFIG) {
            LOGGER.info("[{}] Config Reloading!", LOGNAME);
            setConfig();
        }
    }

    @SubscribeEvent
    public static void onConfigLoading(ModConfigEvent.Loading event) {
        if (!event.getConfig().getModId().equals(MODID)) return;

        if (event.getConfig().getSpec() == COMMON_CONFIG) {
            LOGGER.info("[{}] Config loading!", LOGNAME);
            setConfig();
        }
    }

    private static void setConfig() {
        economyCommandName = configEconomyCommandName.get();
        economyStartingCurrency = configEconomyStartingCurrency.getAsInt();
        economyCurrencySymbol = configEconomyCurrencySymbol.get();
        economyCurrencyNameSingular = configEconomyCurrencySingular.get();
        economyCurrencyNamePlural = configEconomyCurrencyPlural.get();
    }
}
