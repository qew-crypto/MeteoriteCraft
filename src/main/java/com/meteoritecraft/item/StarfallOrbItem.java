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
import net.minecraft.world.World;

/** Starfall: rains down a whole volley of meteors around the player. */
public class StarfallOrbItem extends Item {
	private static final int COOLDOWN_TICKS = 3 * 60 * 20;
	private static final int METEOR_COUNT = 5;

	public StarfallOrbItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		int spawned = 0;
		for (int i = 0; i < METEOR_COUNT; i++) {
			if (MeteorManager.spawnNearPlayer(player, null, 90, false) != null) {
				spawned++;
			}
		}

		ServerWorld serverWorld = (ServerWorld) world;
		serverWorld.spawnParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.5, player.getZ(), 140,
				1.5, 1.5, 1.5, 0.4);
		world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS,
				2.0F, 0.7F);

		player.sendMessage(MeteorManager.prefix()
				.append(Text.literal("Звездопад! Метеоритов вызвано: " + spawned + ". /meteor locate — координаты")
						.formatted(Formatting.GOLD)), false);

		player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
		stack.damage(1, player);
		return ActionResult.SUCCESS;
	}
}
