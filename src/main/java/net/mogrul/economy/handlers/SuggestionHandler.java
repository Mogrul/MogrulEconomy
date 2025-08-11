package net.mogrul.economy.handlers;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.stream.Collectors;

public class SuggestionHandler {
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_MOBS = (context, builder) -> SharedSuggestionProvider.suggest(
            BuiltInRegistries.ENTITY_TYPE.stream()
                    .filter(entityType -> entityType.getCategory() != MobCategory.MISC)
                    .map(EntityType::getKey)
                    .map(ResourceLocation::toString)
                    .collect(Collectors.toList()),
            builder
    );
}
