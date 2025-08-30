package gb.mogrul.economy.player;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import static gb.mogrul.economy.MogrulEconomy.*;

import gb.mogrul.economy.Memory;

@EventBusSubscriber(modid = MODID)
public class PlayerEventHandler {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        PlayerData playerData = PlayerSQLHandler.getOne(serverPlayer);
        Memory.players.put(serverPlayer.getStringUUID(), playerData);

        LOGGER.info("[{}] Added {} to memory!", LOGNAME, serverPlayer.getScoreboardName());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        Memory.players.remove(serverPlayer.getStringUUID());

        LOGGER.info("[{}] Removed {} from memory!", LOGNAME, serverPlayer.getScoreboardName());
    }
}
