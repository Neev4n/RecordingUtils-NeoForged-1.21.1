package net.neevan.recordingutilsmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec3;

public class SummonFacingCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed"));
    private static final SimpleCommandExceptionType ERROR_DUPLICATE_UUID = new SimpleCommandExceptionType(Component.translatable("commands.summon.failed.uuid"));
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(Component.translatable("commands.summon.invalidPosition"));
    private static final SimpleCommandExceptionType ERROR_NO_ENTITY = new SimpleCommandExceptionType(Component.literal("You must provide an entity or hold a spawn egg."));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                Commands.literal("summonfacing")
                        .requires(source -> source.hasPermission(2)) // Requires permission level 2
                        .executes(SummonFacingCommand::spawnAtPlayerFromHeldItem) // <-- Added no-argument version
                        .then(
                                Commands.argument("entity", ResourceArgument.resource(context, Registries.ENTITY_TYPE))
                                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                        .executes(SummonFacingCommand::spawnAtPlayer)
                                        .then(
                                                Commands.argument("nbt", CompoundTagArgument.compoundTag())
                                                        .executes(SummonFacingCommand::spawnAtPlayerWithNBT)
                                        )
                        )
        );
    }

    private static int spawnAtPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return spawnEntity(context, new CompoundTag());
    }

    private static int spawnAtPlayerWithNBT(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CompoundTag tag = CompoundTagArgument.getCompoundTag(context, "nbt");
        return spawnEntity(context, tag);
    }

    private static int spawnAtPlayerFromHeldItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel world = source.getLevel();

        ItemStack itemStack = player.getMainHandItem();
        if (!(itemStack.getItem() instanceof SpawnEggItem spawnEgg)) {
            throw ERROR_NO_ENTITY.create(); // Throw error if not holding a spawn egg
        }

        EntityType<?> entityType = spawnEgg.getType(itemStack);

        if (entityType == null) {
            throw ERROR_NO_ENTITY.create();
        }

        Vec3 pos = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        Entity entity = entityType.create(world);
        if (entity == null) {
            throw ERROR_FAILED.create();
        }

        entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);

        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
            mob.setNoAi(true);
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
            mob.setXRot(pitch);
        }

        if (!world.tryAddFreshEntityWithPassengers(entity)) {
            throw ERROR_DUPLICATE_UUID.create();
        }

        source.sendSuccess(() -> Component.translatable("commands.summon.success", entity.getDisplayName()), true);
        return 1;
    }

    private static int spawnEntity(CommandContext<CommandSourceStack> context, CompoundTag tag) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        Vec3 pos = player.position();
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        Holder.Reference<EntityType<?>> entityTypeRef = ResourceArgument.getSummonableEntityType(context, "entity");
        EntityType<?> entityType = entityTypeRef.value();
        ServerLevel world = source.getLevel();

        Entity entity = entityType.create(world);
        if (entity == null) {
            throw ERROR_FAILED.create();
        }

        entity.moveTo(pos.x, pos.y, pos.z, yaw, pitch);

        if (!tag.isEmpty()) {
            entity.load(tag);
        }

        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null);
            mob.setNoAi(true);
            mob.setYRot(yaw);
            mob.setYHeadRot(yaw);
            mob.setXRot(pitch);
        }

        if (!world.tryAddFreshEntityWithPassengers(entity)) {
            throw ERROR_DUPLICATE_UUID.create();
        }

        source.sendSuccess(() -> Component.translatable("commands.summon.success", entity.getDisplayName()), true);
        return 1;
    }
}
