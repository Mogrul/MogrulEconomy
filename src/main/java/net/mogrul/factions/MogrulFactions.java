package net.mogrul.factions;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

public class MogrulFactions {
    public static final String MODID = "mogrulfactions";
    public static final String LOGNAME = "MogrulFactions";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MogrulFactions(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
    }
}
