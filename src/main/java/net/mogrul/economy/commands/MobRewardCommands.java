package net.mogrul.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.mogrul.economy.Colours;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.MobRewardData;
import net.mogrul.economy.handlers.MobDataHandler;
import net.mogrul.economy.handlers.SuggestionHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Objects;

import static net.mogrul.economy.MogrulEconomy.*;
@EventBusSubscriber(modid = MODID)
public class MobRewardCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        if (Config.mobRewardsEnabled) {
            registerCommands(dispatcher);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering mob rewards commands!", LOGNAME);

        dispatcher.register(
                Commands.literal(Config.mobRewardsCommandName)
                        .requires(source -> source.hasPermission(4))

                        // OP command: /mobrewards set <mobid> <amount> - Sets the reward amount for a mob.
                        .then(Commands.literal("set")
                                .then(Commands.argument("mob", ResourceLocationArgument.id())
                                        .suggests(SuggestionHandler.SUGGEST_MOBS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(source -> setMobReward(
                                                        source.getSource(),
                                                        ResourceLocationArgument.getId(source, "mob"),
                                                        IntegerArgumentType.getInteger(source, "amount")
                                                ))
                                        )
                                )
                        )

                        // OP command: /mobrewards remove <mobid> - Removes the reward from a mob.
                        .then(Commands.literal("remove")
                                .then(Commands.argument("mob", ResourceLocationArgument.id())
                                        .suggests(SuggestionHandler.SUGGEST_MOBS)
                                        .executes(source -> removeMobReward(
                                                source.getSource(),
                                                ResourceLocationArgument.getId(source, "mob")
                                        ))
                                )
                        )
        );
    }

    private static int setMobReward(CommandSourceStack source, ResourceLocation mob, int amount) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mob)) {
            Component unknownMobFailMessage = Component.literal("Unknown mob type: ")
                    .append(Component.literal(mob.toString())
                            .withStyle(hexToTextColorStyle(Colours.mobName)
                                    .withBold(true)
                            )
                    );

            source.sendFailure(unknownMobFailMessage);
            return 0;
        }

        String mobString = mob.toString();
        String mobIDString = mobString.replace(":", "_");

        // Update existing data.
        if (MemoryData.mobRewards.get(mobIDString) != null) {
            MobRewardData mobRewardData = MemoryData.mobRewards.get(mobIDString);
            mobRewardData.rewardAmount = amount;
            MobDataHandler.save(mobRewardData);

            Component mobUpdatedSuccessMessage = Component.literal("")
                    .append(Component.literal(mob.toString())
                            .withStyle(hexToTextColorStyle(Colours.mobName)
                                    .withBold(true)
                            )
                    )
                    .append(Component.literal(" has been updated to "))
                    .append(Component.literal(Config.currencySymbol + String.format("%,d", amount))
                            .withStyle(hexToTextColorStyle(Colours.currencyName)
                                    .withBold(true)
                            )
                    );

            source.sendSuccess(() -> mobUpdatedSuccessMessage, true);
            return 1;
        }

        // Create a new data piece.
        MobRewardData mobRewardData = new MobRewardData(mobString, amount);
        MemoryData.mobRewards.put(mobIDString, mobRewardData);
        MobDataHandler.save(mobRewardData);

        Component rewardSetSuccessMessage = Component.literal("")
                .append(Component.literal(mobString)
                        .withStyle(hexToTextColorStyle(Colours.mobName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" reward has been set to "))
                .append(Component.literal(Config.currencySymbol + amount)
                        .withStyle(hexToTextColorStyle(Colours.currencyName)
                                .withBold(true)
                        )
                );

        source.sendSuccess(() -> rewardSetSuccessMessage, false);

        LOGGER.info("[{}] {} set {}'s mob reward to {}",
                LOGNAME,
                Objects.requireNonNull(source.getPlayer()).getName().getString(),
                mobString,
                amount
        );

        return 1;
    }

    private static int removeMobReward(CommandSourceStack source, ResourceLocation mob) {
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(mob)) {
            Component unknownMobFailMessage = Component.literal("Unknown mob type: ")
                    .append(Component.literal(mob.toString())
                            .withStyle(hexToTextColorStyle(Colours.mobName)
                                    .withBold(true)
                            )
                    );

            source.sendFailure(unknownMobFailMessage);
            return 0;
        }

        String mobString = mob.toString();
        String mobIDString = mobString.replace(":", "_");
        MobRewardData mobRewardData = MemoryData.mobRewards.get(mobIDString);
        if (mobRewardData == null) {
            Component mobNotExistFailMessage = Component.literal("")
                    .append(Component.literal(mob.toString())
                            .withStyle(hexToTextColorStyle(Colours.mobName)
                                    .withBold(true)
                            )
                    )
                    .append(Component.literal(" doesn't have a set reward!"));

            source.sendFailure(mobNotExistFailMessage);
            return 0;
        }

        MemoryData.mobRewards.remove(mobIDString);
        MobDataHandler.remove(mobRewardData);

        Component successMessage = Component.literal("")
                .append(Component.literal(mob.toString())
                        .withStyle(hexToTextColorStyle(Colours.mobName)
                                .withBold(true)
                        )
                )
                .append(Component.literal(" has had its rewards removed!"));

        source.sendSuccess(() -> successMessage, false);
        LOGGER.info(
                "[{}] {} Removed {}'s mob reward!",
                LOGNAME,
                Objects.requireNonNull(source.getPlayer()).getName().getString(),
                mobString
        );
        return 1;
    }
}
