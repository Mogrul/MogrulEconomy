package net.mogrul.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.mogrul.economy.Colours;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.PendingTradeData;
import net.mogrul.economy.data.PlayerData;
import net.mogrul.economy.handlers.PlayerDataHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class TradeCommands {
    public static final Map<String, PendingTradeData> pendingTrades = new HashMap<>();

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        if (Config.tradeEnabled) {
            registerCommands(dispatcher);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering trade commands!", MODID);

        dispatcher.register(
                Commands.literal(Config.tradeCommandName)
                        .requires(source -> source.hasPermission(0))

                        // Member command: /trade <amount> <player> - Sends a trade request from the item in their hand to a player.
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("price", IntegerArgumentType.integer(0))
                                                .executes(context -> sendTradeRequest(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "amount"),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        IntegerArgumentType.getInteger(context, "price")
                                                ))
                                        )
                                )
                        )
        );

        dispatcher.register(
                Commands.literal("accepttrade")
                        .then(Commands.argument("tradeId", StringArgumentType.word())
                                .executes(context -> acceptTrade(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "tradeId")
                                ))
                        )
        );

        dispatcher.register(
                Commands.literal("rejecttrade")
                        .then(Commands.argument("tradeId", StringArgumentType.word())
                                .executes(context -> rejectTrade(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "tradeId")
                                ))
                        )
        );

        dispatcher.register(
                Commands.literal("canceltrade")
                        .then(Commands.argument("tradeId", StringArgumentType.word())
                                .executes(context -> cancelTrade(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "tradeId")
                                ))
                        )
        );
    }

    public static int sendTradeRequest(CommandSourceStack source, Integer count, ServerPlayer toPlayer, Integer price) {
        if (!(source.getEntity() instanceof ServerPlayer fromPlayer)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        if (!fromPlayer.getName().getString().equals("Dev")) {
            if (fromPlayer.getUUID().equals(toPlayer.getUUID())) {
                source.sendFailure(Component.literal("You can't trade with yourself!"));
                return 0;
            }
        }

        ItemStack heldStack = fromPlayer.getMainHandItem();
        if (heldStack.isEmpty()) {
            source.sendFailure(Component.literal("You don't have anything in your hand!"));
            return 0;
        }

        if (heldStack.getCount() < count) {
            source.sendFailure(Component.literal("You don't have enough items in your hand for that!"));
            return 0;
        }

        ItemStack stackCopy = heldStack.copy();
        stackCopy.setCount(count);
        fromPlayer.getMainHandItem().shrink(count);

        String tradeUUID = UUID.randomUUID().toString();
        long expiresAt = System.currentTimeMillis() + 30_000L;

        PendingTradeData pendingTradeData = new PendingTradeData(
                tradeUUID,
                fromPlayer,
                toPlayer,
                stackCopy,
                count,
                price,
                expiresAt
        );
        pendingTrades.put(tradeUUID, pendingTradeData);

        // [TRADE REQUEST]
        // From: {name}
        // Items: {count} {itemName}
        // Price: {symbol} {price}
        // [ACCEPT] [REJECT]
        Component receiverMessage = Component.literal("")
                .append(Component.literal("[TRADE REQUEST]\n")
                    .withStyle(hexToTextColorStyle(Colours.currencyName)
                            .withBold(true)
                    )
                )
                .append(Component.literal("From: "))
                .append(Component.literal(fromPlayer.getName().getString())
                        .withStyle(hexToTextColorStyle(Colours.playerName)
                                .withBold(true)
                        )
                )
                .append(Component.literal("\nItems: "))
                .append(Component.literal(String.format("%,d", count) + " ")
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                )
                .append(stackCopy.getHoverName().copy()
                        .withStyle(hexToTextColorStyle(Colours.mobName)
                                .withBold(true)
                        )
                )
                .append(Component.literal("\nPrice: ")
                .append(Component.literal(Config.currencySymbol + String.format("%,d", price))
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(false)
                        )
                )
                .append(Component.literal("\n[ACCEPT] ")
                        .withStyle(hexToTextColorStyle("#00FE05")
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/accepttrade " + tradeUUID))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to accept this trade."))
                                )
                        )
                )
                .append(Component.literal(" [REJECT]")
                        .withStyle(hexToTextColorStyle("#FE0301")
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rejecttrade " + tradeUUID))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to reject this trade."))
                                )
                        )
                ));

        Component senderMessage = Component.literal("[TRADE REQUEST SENT]\n")
                .withStyle(hexToTextColorStyle("#C6F601")
                        .withBold(true)
                )
                .append(Component.literal("To: "))
                .append(Component.literal(toPlayer.getDisplayName().getString())
                        .withStyle(hexToTextColorStyle("#FB7100")
                                .withBold(true)
                        )
                )
                .append(Component.literal("\nItems: "))
                .append(Component.literal(String.format("%,d", count) + " ")
                        .withStyle(hexToTextColorStyle("#A8FF01")
                                .withBold(false)
                        )
                )
                .append(stackCopy.getHoverName().copy()
                        .withStyle(hexToTextColorStyle("#A8FF01")
                                .withBold(false)
                        )
                )
                .append(Component.literal("\nPrice: "))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", price))
                        .withStyle(hexToTextColorStyle("#FFDA00")
                                .withBold(false)
                        )
                )
                .append(Component.literal("\n[CANCEL]")
                        .withStyle(hexToTextColorStyle("#FE0301")
                                .withBold(true)
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/canceltrade " + tradeUUID))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Click to cancel this trade."))
                                )
                        )
                );

        LOGGER.info(
                "[{}] Trade of {} {} for {}{} from {} sent to {}",
                MODID,
                String.format("%,d", count),
                stackCopy.getHoverName().getString(),
                Config.currencySymbol,
                String.format("%,d", price),
                fromPlayer.getGameProfile().getName(),
                toPlayer.getGameProfile().getName()
        );

        toPlayer.sendSystemMessage(receiverMessage);
        fromPlayer.sendSystemMessage(senderMessage);

        return 1;
    }

    public static int acceptTrade(CommandSourceStack source, String tradeUUID) {
        ServerPlayer toPlayer = source.getPlayer();

        PendingTradeData pendingTradeData = pendingTrades.get(tradeUUID);
        if (pendingTradeData == null) {
            source.sendFailure(Component.literal("Trade no longer exists!"));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) return 0;
        if (!pendingTradeData.toPlayer.getUUID().equals(sourcePlayer.getUUID())) {
            source.sendFailure(Component.literal("This isn't your trade to accept!"));
            return 0;
        }
        if (System.currentTimeMillis() > pendingTradeData.expiresAt) {
            source.sendFailure(Component.literal("Trade expired!"));
            return 0;
        }

        assert toPlayer != null;
        PlayerData toPlayerData = MemoryData.players.get(toPlayer.getStringUUID());
        if (toPlayerData == null) return 0;
        if (toPlayerData.currency < pendingTradeData.price) {
            source.sendFailure(Component.literal("You can't afford this trade!"));
            pendingTradeData.fromPlayer.sendSystemMessage(Component.literal("")
                    .append(Component.literal(toPlayer.getName().getString())
                            .withStyle(hexToTextColorStyle("#FE0301")
                                    .withBold(true)
                            )
                    )
                    .append(Component.literal(" can't afford this trade!"))
            );

            return 0;
        }

        ItemStack itemStackCopy = pendingTradeData.items.copy();
        if (!canFitInPlayerInventory(toPlayer.getInventory(), itemStackCopy)) {
            toPlayer.sendSystemMessage(Component.literal("Make some room in your inventory!"));
            return 0;
        }

        toPlayer.getInventory().add(itemStackCopy);

        toPlayerData.currency -= pendingTradeData.price;
        PlayerDataHandler.save(toPlayerData);
        PlayerData fromPlayerData = MemoryData.players.get(pendingTradeData.fromPlayer.getStringUUID());
        if (fromPlayerData == null) return 0;
        fromPlayerData.currency += pendingTradeData.price;
        PlayerDataHandler.save(fromPlayerData);

        LOGGER.info(
                "[{}] Trade from {} to {} accepted.",
                MODID,
                toPlayer.getDisplayName().getString(),
                fromPlayerData.name
        );

        Component accepterMessage = Component.literal("Accepted trade of ")
                .append(Component.literal(String.format("%,d", pendingTradeData.count) + " " + pendingTradeData.items.getHoverName().getString())
                        .withStyle(hexToTextColorStyle("#A8FF01")
                                .withBold(true)
                        )
                )
                .append(Component.literal(" from "))
                .append(Component.literal(fromPlayerData.name)
                        .withStyle(hexToTextColorStyle("#FB7100")
                                .withBold(true)
                        )
                )
                .append(Component.literal(" for "))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", pendingTradeData.price))
                        .withStyle(hexToTextColorStyle("#FFDA00")
                                .withBold(false)
                        )
                );

        Component sellerMessage = Component.literal("")
                .append(Component.literal(toPlayerData.name)
                    .withStyle(hexToTextColorStyle("#FB7100")
                            .withBold(true)
                    )
                )
                .append(Component.literal(" accepted your trade of "))
                .append(Component.literal(String.format("%,d", pendingTradeData.count) + " " + pendingTradeData.items.getHoverName().getString())
                        .withStyle(hexToTextColorStyle("#A8FF01")
                                .withBold(true)
                        )
                )
                .append(Component.literal(" for "))
                .append(Component.literal(Config.currencySymbol + String.format("%,d", pendingTradeData.price))
                        .withStyle(hexToTextColorStyle("#FFDA00")
                                .withBold(false)
                        )
                );

        source.sendSuccess(() -> accepterMessage, false);
        pendingTradeData.fromPlayer.sendSystemMessage(sellerMessage);
        pendingTrades.remove(tradeUUID);

        return 1;
    }

    public static int rejectTrade(CommandSourceStack source, String tradeUUID) {
        ServerPlayer toPlayer = source.getPlayer();

        PendingTradeData pendingTradeData = pendingTrades.remove(tradeUUID);
        if (pendingTradeData == null) {
            source.sendFailure(Component.literal("Trade no longer exists!"));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) return 0;
        if (!pendingTradeData.toPlayer.getUUID().equals(sourcePlayer.getUUID())) {
            source.sendFailure(Component.literal("This isn't your trade to reject!"));
            return 0;
        }
        if (System.currentTimeMillis() > pendingTradeData.expiresAt) {
            source.sendFailure(Component.literal("Trade expired!"));
            return 0;
        }

        assert toPlayer != null;
        LOGGER.info("[{}] Trade from {} to {} rejected.",
                MODID,
                pendingTradeData.fromPlayer.getName().getString(),
                pendingTradeData.toPlayer.getName().getString()
        );

        source.sendSuccess(() -> Component.literal(
                        "You rejected the trade from " +
                                pendingTradeData.fromPlayer.getName().getString()),
                true
        );
        pendingTradeData.fromPlayer.sendSystemMessage(Component.literal(toPlayer.getName().getString() + " rejected your trade."));
        sendItemsBack(pendingTradeData.fromPlayer, pendingTradeData.items);

        return 1;
    }

    private static int cancelTrade(CommandSourceStack source, String tradeUUID) {
        ServerPlayer toPlayer = source.getPlayer();
        PendingTradeData pendingTradeData = pendingTrades.remove(tradeUUID);

        if (pendingTradeData == null) {
            source.sendFailure(Component.literal("Trade no longer exists!"));
            return 0;
        }

        assert toPlayer != null;
        if (!pendingTradeData.toPlayer.getUUID().equals(toPlayer.getUUID())) {
            source.sendFailure(Component.literal("This isn't your trade to cancel!"));
            return 0;
        }

        if (System.currentTimeMillis() > pendingTradeData.expiresAt) {
            source.sendFailure(Component.literal("Trade expired!"));
            return 0;
        }

        Component sourceMessage = Component.literal("Trade request to ")
                .append(Component.literal(pendingTradeData.toPlayer.getGameProfile().getName())
                        .withStyle(hexToTextColorStyle("#FB7100")
                                .withBold(false)
                        )
                )
                .append(Component.literal(" cancelled")
                        .withStyle(hexToTextColorStyle("#FE0301")
                                .withBold(true)
                        )
                );

        Component receiverMessage = Component.literal(pendingTradeData.fromPlayer.getDisplayName().getString())
                .append(Component.literal(" cancelled the trade."));

        source.sendSuccess(() -> sourceMessage, true);
        pendingTradeData.toPlayer.sendSystemMessage(receiverMessage);

        sendItemsBack(pendingTradeData.fromPlayer, pendingTradeData.items);

        return 1;
    }

    public static void sendItemsBack(ServerPlayer fromPlayer, ItemStack item) {
        if (!fromPlayer.getInventory().add(item.copy())) {
            fromPlayer.drop(item, false);
        }
    }
}
