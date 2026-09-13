package com.meteoritecraft.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Void Anchor: sneak + right click saves a point, right click teleports you back
 * to it. Handy for looting a far away meteor and jumping home again.
 */
public class VoidAnchorItem extends Item {
	private static final int COOLDOWN_TICKS = 60 * 20;

	private record Anchor(String world, BlockPos pos) {
	}

	private static final Map<UUID, Anchor> ANCHORS = new HashMap<>();

	public VoidAnchorItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		String worldId = serverWorld.getRegistryKey().getValue().toString();

		if (player.isSneaking()) {
			ANCHORS.put(player.getUuid(), new Anchor(worldId, player.getBlockPos()));
			world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN,
					SoundCategory.PLAYERS, 1.0F, 1.2F);
			player.sendMessage(Text.literal("Точка якоря сохранена").formatted(Formatting.GREEN), true);
			return ActionResult.SUCCESS;
		}

		Anchor anchor = ANCHORS.get(player.getUuid());
		if (anchor == null) {
			player.sendMessage(Text.literal("Сначала Shift + ПКМ, чтобы сохранить точку")
					.formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}
		if (!anchor.world().equals(worldId)) {
			player.sendMessage(Text.literal("Якорь стоит в другом мире").formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		serverWorld.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6,
				1.0, 0.6, 0.4);
		BlockPos pos = anchor.pos();
		player.requestTeleport(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
		world.playSound(null, pos, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0F, 0.9F);
		player.sendMessage(Text.literal("Телепорт к якорю").formatted(Formatting.DARK_PURPLE), true);

		player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
		stack.damage(1, player);
		return ActionResult.SUCCESS;
	}
}
