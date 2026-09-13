package com.meteoritecraft.item;

import com.meteoritecraft.MeteorManager;
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

/** Teleports the player to the nearest active meteor. 5 minute cooldown. */
public class MeteorStaffItem extends Item {
	private static final int COOLDOWN_TICKS = 5 * 60 * 20;

	public MeteorStaffItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		MeteorManager.Meteor meteor = MeteorManager.nearest(player);
		if (meteor == null) {
			player.sendMessage(Text.literal("Активных метеоритов нет. Используй /meteor spawn")
					.formatted(Formatting.RED), true);
			return ActionResult.FAIL;
		}

		BlockPos ground = MeteorManager.findGround((ServerWorld) world, meteor.pos.getX(), meteor.pos.getZ());
		BlockPos target = ground != null ? ground.up() : meteor.pos.up(2);

		((ServerWorld) world).spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0,
				player.getZ(), 60, 0.6, 1.0, 0.6, 0.4);
		player.requestTeleport(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
		world.playSound(null, target, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
		((ServerWorld) world).spawnParticles(ParticleTypes.END_ROD, target.getX() + 0.5, target.getY() + 1.0,
				target.getZ() + 0.5, 80, 0.8, 1.2, 0.8, 0.2);

		player.sendMessage(Text.literal("Телепорт к " + meteor.rarity.displayName.toLowerCase() + " метеориту ")
				.formatted(Formatting.GRAY).append(MeteorManager.coordsText(meteor.pos)), false);

		player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
		stack.damage(1, player);
		return ActionResult.SUCCESS;
	}
}
