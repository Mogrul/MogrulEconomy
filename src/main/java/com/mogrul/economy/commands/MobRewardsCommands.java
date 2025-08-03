package com.mogrul.economy.commands;

import com.mogrul.economy.utils.Config;
import com.mogrul.economy.utils.database.MobRewardsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.Objects;

public class MobRewardsCommands {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(Config.mobRewardsCommandName)
                        .requires(source -> source.hasPermission(4))

                        // OP command: /mobrewards set <mobid> <amount> - Sets the reward amount for a mob.
                        .then(Commands.literal("set")
                                .then(Commands.argument("mob", ResourceLocationArgument.id())
                                        .suggests(SUGGEST_MOBS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(MobRewardsCommands::setMobReward)
                                        )
                                )
                        )

                        // OP command: /mobrewards remove <mobid> - Removes a mob from rewards.
                        .then(Commands.literal("remove")
                                .then(Commands.argument("mob", ResourceLocationArgument.id())
                                        .suggests(SUGGEST_MOBS)
                                        .executes(MobRewardsCommands::removeMobReward)
                                )
                        )
        );
    }

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MOBS = (context, builder) -> SharedSuggestionProvider.suggest(
            ForgeRegistries.ENTITY_TYPES.getValues().stream()
                    .filter(entityType -> entityType.getCategory() != MobCategory.MISC)
                    .map(ForgeRegistries.ENTITY_TYPES::getKey)
                    .filter(Objects::nonNull) // Filter out nulls just in case
                    .map(ResourceLocation::toString),
            builder
    );

    private static int setMobReward(CommandContext<CommandSourceStack> context) {
        ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
        CommandSourceStack source = context.getSource();
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (!ForgeRegistries.ENTITY_TYPES.containsKey(mobId)) {
            source.sendFailure(Component.literal("Unknown mob type: " + mobId));
            return 0;
        }

        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(mobId);
        if (entityType == null || entityType.getCategory() == MobCategory.MISC || !entityType.canSummon()) {
            source.sendFailure(Component.literal("Entity '" + mobId + "' is not a valid mob."));
            return 0;
        }

        LOGGER.info("Setting mob reward for '{}' to {}", mobId, amount);
        MobRewardsManager.setReward(mobId.toString(), amount);

        source.sendSuccess(() -> Component.literal("Set reward for " + mobId + " to " + amount), true);

        return 1;
    }

    private static int removeMobReward(CommandContext<CommandSourceStack> context) {
        ResourceLocation mobId = ResourceLocationArgument.getId(context, "mob");
        CommandSourceStack source = context.getSource();

        Integer returnAmount = MobRewardsManager.removeReward(mobId.toString());

        if (returnAmount == 0) {
            source.sendFailure(Component.literal(mobId + " isn't in rewards."));
            return 0;
        } else {
            source.sendSuccess(() -> Component.literal("Removed mob " + mobId + " from rewards."), true);
            return 1;
        }
    }
}
