package gb.mogrul.economy;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import static gb.mogrul.economy.Config.COMMON_CONFIG;

@Mod(MogrulEconomy.MODID)
public class MogrulEconomy {
    public static final String MODID = "mogruleconomy";
    public static final String LOGNAME = "MogrulEconomy";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MogrulEconomy(ModContainer modcontainer) {
        NeoForge.EVENT_BUS.register(this);
        modcontainer.registerConfig(ModConfig.Type.COMMON, COMMON_CONFIG);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {

    }
}
