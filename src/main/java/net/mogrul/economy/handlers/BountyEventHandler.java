package net.mogrul.economy.handlers;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityEvent;
import net.mogrul.economy.Colours;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.BountyData;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.PlayerData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class BountyEventHandler {
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer deadPlayer)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killerPlayer)) return;

        // Get deadPlayer's bounty.
        BountyData deadPlayerBountyData = MemoryData.bounties.get(deadPlayer.getStringUUID());
        if (deadPlayerBountyData == null || deadPlayerBountyData.bounty == 0) return;

        // Send the bounty to the killer player and remove the bounty from the player.
        PlayerData killerPlayerData = MemoryData.players.get(killerPlayer.getStringUUID());
        if (killerPlayerData == null) return;

        killerPlayerData.currency += deadPlayerBountyData.bounty;
        PlayerDataHandler.save(killerPlayerData);

        // Send message to server of claimed bounty.
        Component serverBountyClaimMessage = Component.literal("")
                        .append(Component.literal(deadPlayer.getScoreboardName())
                                .withStyle(Colours.playerName
                                        .withBold(true)
                                )
                        )
                        .append(Component.literal("'s bounty of "))
                        .append(Component.literal(Config.currencySymbol + deadPlayerBountyData.bounty)
                                .withStyle(Colours.currencyName
                                        .withBold(true)
                                )
                        )
                        .append(Component.literal(" has been claimed by "))
                        .append(Component.literal(killerPlayer.getScoreboardName())
                                .withStyle(Colours.playerName
                                        .withBold(true)
                                )
                        )
                        .append(Component.literal("!"));

        // Send message to every player in the server.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(serverBountyClaimMessage);
        }

        BountyDataHandler.remove(deadPlayerBountyData);
        MemoryData.bounties.remove(deadPlayer.getStringUUID());
    }
}
