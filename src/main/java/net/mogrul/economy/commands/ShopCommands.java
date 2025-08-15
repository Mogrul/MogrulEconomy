package net.mogrul.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.mogrul.economy.Config;
import net.mogrul.economy.data.MemoryData;
import net.mogrul.economy.data.ShopData;
import net.mogrul.economy.handlers.ShopDataHandler;
import net.mogrul.economy.handlers.SuggestionHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;

import static net.mogrul.economy.MogrulEconomy.*;

@EventBusSubscriber(modid = MODID)
public class ShopCommands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        if (Config.shopsEnabled) {
            registerCommands(dispatcher);
        }
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LOGGER.info("[{}] Registering shop commands!", LOGNAME);

        dispatcher.register(
                Commands.literal(Config.shopsCommandName)
                        .requires(source -> source.hasPermission(4))

                        // OP command: /shops create
                        .then(Commands.literal("create")
                                .then(Commands.argument("entityName", StringArgumentType.string())
                                        .then(Commands.argument("sellingItem", ResourceLocationArgument.id())
                                                .suggests(SuggestionHandler.SUGGEST_ITEMS)
                                                .then(Commands.argument("amountPerTrade", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("pricePerTrade", IntegerArgumentType.integer(0))
                                                                .executes(source -> createShop(
                                                                        source.getSource(),
                                                                        StringArgumentType.getString(source, "entityName"),
                                                                        ResourceLocationArgument.getId(source, "sellingItem"),
                                                                        IntegerArgumentType.getInteger(source, "amountPerTrade"),
                                                                        IntegerArgumentType.getInteger(source, "pricePerTrade")
                                                                ))
                                                        )
                                                )
                                        )

                                )
                        )

                        // OP command: /shops remove
                        .then(Commands.literal("remove")
                                .executes(source -> removeShop(
                                        source.getSource()
                                ))
                        )
        );
    }

    public static int createShop(CommandSourceStack source, String entityName, ResourceLocation sellingItem, int amountPerTrade, int pricePerTrade) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        }

        double sourcePlayerX = sourcePlayer.getBlockX() + 0.5;
        double sourcePlayerZ = sourcePlayer.getBlockZ() + 0.5;
        int sourcePlayerY = sourcePlayer.getBlockY();

        float sourcePlayerYaw = sourcePlayer.getYRot();
        float sourcePlayerPitch = sourcePlayer.getXRot();

        ServerLevel serverLevel = (ServerLevel) sourcePlayer.level();
        String levelName = serverLevel.dimension().location().toString();

        Item item = BuiltInRegistries.ITEM.get(sellingItem);

        // Create the ShopData object.
        UUID shopUUID = UUID.randomUUID();
        ShopData shopData = new ShopData(
                shopUUID.toString(),
                entityName,
                levelName,
                item.toString(),
                amountPerTrade,
                pricePerTrade,
                sourcePlayerX,
                sourcePlayerZ,
                sourcePlayerY,
                sourcePlayerYaw,
                sourcePlayerPitch,
                0
        );

        // Save and add the entity to the world.
        ShopDataHandler.save(shopData);
        MemoryData.shops.put(shopUUID.toString(), shopData);

        ShopDataHandler.createShopEntity(shopData);

        return 1;
    }

    public static int removeShop(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer sourcePlayer)) {
            source.sendFailure(Component.literal("Only players can issue this command!"));
            return 0;
        }

        // Get the entity the user is looking at.
        Vec3 eyePos = sourcePlayer.getEyePosition();
        Vec3 lookVec = sourcePlayer.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(10));

        AABB aabb = sourcePlayer.getBoundingBox().expandTowards(lookVec.scale(10)).inflate(1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                sourcePlayer,
                eyePos,
                reachVec,
                aabb,
                e -> !e.isSpectator() && e.isPickable(),
                10 * 10
        );

        if (entityHitResult == null) {
            source.sendFailure(Component.literal("No entity found!"));
            return 0;
        }
        Entity entity = entityHitResult.getEntity();

        ShopData shopMemoryData = MemoryData.shops.get(entity.getStringUUID());
        if (shopMemoryData == null) {
            source.sendFailure(Component.literal("Shop with UUID: " + entity.getStringUUID() + " does not exist!"));
            return 0;
        }

        // Despawn entity.
        ResourceLocation levelID = ResourceLocation.tryParse(shopMemoryData.entityLevel);
        if (levelID != null) {
            ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, levelID);
            ServerLevel serverLevel = server.getLevel(level);
            assert serverLevel != null;
            Entity serverEntity = serverLevel.getEntity(UUID.fromString(shopMemoryData.shopUUID));
            if (serverEntity == null) {
                source.sendFailure(Component.literal("Failed to get entity in server level!"));
                return 0;
            } else {
                serverEntity.discard();
            }
        } else {
            source.sendFailure(Component.literal("Failed to get ResouceLocation of entity level!"));
            return 0;
        }

        // Remove from data.
        MemoryData.shops.remove(entity.getStringUUID());
        ShopDataHandler.remove(shopMemoryData);

        source.sendSuccess(() -> Component.literal("Shop with UUID: " + entity.getStringUUID() + " has been removed!"), true);
        return 1;
    }
}
