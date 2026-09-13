package com.meteoritecraft.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import net.minecraft.world.World;

/** Cosmic Elixir: full combat buff package for boss fights. */
public class CosmicElixirItem extends Item {
	private static final int DURATION = 90 * 20;

	public CosmicElixirItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, DURATION, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, DURATION, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, DURATION, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION, 1));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, DURATION, 0));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, DURATION, 0));
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, DURATION, 3));

		((ServerWorld) world).spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0,
				player.getZ(), 80, 0.6, 1.0, 0.6, 0.1);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F,
				1.4F);
		player.sendMessage(Text.literal("Космический эликсир: бусты на 90 секунд").formatted(Formatting.AQUA), true);

		if (!player.isCreative()) {
			stack.decrement(1);
		}
		player.getItemCooldownManager().set(stack, 20 * 60 * 2);
		return ActionResult.SUCCESS;
	}
}
