package com.meteoritecraft.item;

import com.meteoritecraft.BossManager;
import com.meteoritecraft.TargetHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

/** Heart of the Fallen Star: summons a random boss where the player is looking. */
public class BossHeartItem extends Item {
	public BossHeartItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		BlockPos target = TargetHelper.lookingAt(player, 48.0);
		BossManager.BossType type = BossManager.randomType();
		BossManager.Boss boss = BossManager.spawn((ServerWorld) world, target, type, true);
		if (boss == null) {
			player.sendMessage(Text.literal("Босс не смог пробудиться здесь").formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 2.0F,
				0.8F);
		stack.decrement(1);
		player.getItemCooldownManager().set(stack, 200);
		return ActionResult.SUCCESS;
	}
}
