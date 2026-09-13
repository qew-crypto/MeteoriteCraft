package com.meteoritecraft.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Gravity Cube: yanks every mob around you into one pile and slows them down. */
public class GravityCubeItem extends Item {
	private static final int COOLDOWN_TICKS = 30 * 20;
	private static final double RADIUS = 24.0;

	public GravityCubeItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		ServerWorld serverWorld = (ServerWorld) world;
		Box box = player.getBoundingBox().expand(RADIUS);
		int pulled = 0;

		for (Entity entity : serverWorld.getOtherEntities(player, box)) {
			if (!(entity instanceof LivingEntity living) || living instanceof PlayerEntity) {
				continue;
			}
			Vec3d pull = player.getPos().subtract(living.getPos()).normalize().multiply(1.4);
			living.addVelocity(pull.x, 0.35, pull.z);
			living.velocityModified = true;
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2));
			living.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
			if (living instanceof MobEntity mob) {
				mob.setTarget(player);
			}
			pulled++;
		}

		serverWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
				160, 2.5, 1.5, 2.5, 0.5);
		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS,
				1.2F, 0.6F);
		player.sendMessage(Text.literal("Гравитационный рывок: стянуто существ — " + pulled)
				.formatted(Formatting.LIGHT_PURPLE), true);

		player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
		stack.damage(1, player);
		return ActionResult.SUCCESS;
	}
}
