package com.meteoritecraft.item;

import com.meteoritecraft.MeteorManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Prints the coordinates of every active meteor. 1 minute cooldown. */
public class MeteorLocatorItem extends Item {
	private static final int COOLDOWN_TICKS = 60 * 20;

	public MeteorLocatorItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
			return ActionResult.SUCCESS;
		}

		var meteors = MeteorManager.active();
		if (meteors.isEmpty()) {
			player.sendMessage(Text.literal("Локатор молчит: активных метеоритов нет").formatted(Formatting.GRAY),
					false);
		} else {
			player.sendMessage(Text.literal("☄ Метеориты (" + meteors.size() + "):").formatted(Formatting.GOLD),
					false);
			for (MeteorManager.Meteor meteor : meteors) {
				double distance = Math.sqrt(meteor.pos.getSquaredDistance(player.getPos()));
				player.sendMessage(Text.literal(" • ").formatted(Formatting.DARK_GRAY)
						.append(Text.literal(meteor.rarity.displayName + " ").formatted(meteor.rarity.color))
						.append(MeteorManager.coordsText(meteor.pos))
						.append(Text.literal(" — " + (int) distance + " бл.").formatted(Formatting.DARK_GRAY)),
						false);
			}
		}

		world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS,
				0.8F, 1.6F);
		player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);
		return ActionResult.SUCCESS;
	}
}
