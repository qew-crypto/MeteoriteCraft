package com.meteoritecraft.item;

import com.meteoritecraft.MeteorManager;
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

/** Single-use: calls a meteor down onto the block the player is looking at. */
public class OblivionStickItem extends Item {
	public OblivionStickItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		BlockPos target = TargetHelper.lookingAt(player, 180.0);
		if (target == null) {
			player.sendMessage(Text.literal("Нет цели в точке взгляда").formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		BlockPos impact = MeteorManager.spawnAt((ServerWorld) world, target, null, true);
		if (impact == null) {
			player.sendMessage(Text.literal("Не удалось вызвать метеорит здесь").formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F,
				0.5F);
		player.sendMessage(Text.literal("Палка Забвения рассыпалась в прах...").formatted(Formatting.DARK_PURPLE),
				false);

		stack.decrement(1); // one-time use
		player.getItemCooldownManager().set(stack, 60);
		return ActionResult.SUCCESS;
	}
}
