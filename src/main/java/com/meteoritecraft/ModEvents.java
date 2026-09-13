package com.meteoritecraft;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class ModEvents {

	/** Blocks that are normally unbreakable but the Meteor Pickaxe can mine. */
	private static boolean isProtected(BlockState state) {
		return state.isOf(Blocks.BEDROCK) || state.isOf(Blocks.BARRIER) || state.isOf(Blocks.END_PORTAL_FRAME)
				|| state.isOf(Blocks.COMMAND_BLOCK) || state.isOf(Blocks.CHAIN_COMMAND_BLOCK)
				|| state.isOf(Blocks.REPEATING_COMMAND_BLOCK) || state.isOf(Blocks.STRUCTURE_BLOCK)
				|| state.isOf(Blocks.LIGHT) || state.isOf(Blocks.JIGSAW);
	}

	/** Ore -> smelted result for the auto-smelt feature. */
	private static final Map<Block, net.minecraft.item.Item> SMELT = new HashMap<>();

	static {
		SMELT.put(Blocks.IRON_ORE, Items.IRON_INGOT);
		SMELT.put(Blocks.DEEPSLATE_IRON_ORE, Items.IRON_INGOT);
		SMELT.put(Blocks.RAW_IRON_BLOCK, Items.IRON_BLOCK);
		SMELT.put(Blocks.GOLD_ORE, Items.GOLD_INGOT);
		SMELT.put(Blocks.DEEPSLATE_GOLD_ORE, Items.GOLD_INGOT);
		SMELT.put(Blocks.NETHER_GOLD_ORE, Items.GOLD_INGOT);
		SMELT.put(Blocks.RAW_GOLD_BLOCK, Items.GOLD_BLOCK);
		SMELT.put(Blocks.COPPER_ORE, Items.COPPER_INGOT);
		SMELT.put(Blocks.DEEPSLATE_COPPER_ORE, Items.COPPER_INGOT);
		SMELT.put(Blocks.RAW_COPPER_BLOCK, Items.COPPER_BLOCK);
		SMELT.put(Blocks.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP);
		SMELT.put(Blocks.SAND, Items.GLASS);
		SMELT.put(Blocks.RED_SAND, Items.GLASS);
		SMELT.put(Blocks.COBBLESTONE, Items.STONE);
		SMELT.put(Blocks.STONE, Items.SMOOTH_STONE);
		SMELT.put(Blocks.CLAY, Items.BRICK);
		SMELT.put(Blocks.NETHERRACK, Items.NETHER_BRICK);
		SMELT.put(ModBlocks.METEORITE_ORE, ModItems.METEOR_INGOT);
	}

	public static void initialize() {
		// Meteor pickaxe: breaks anything (incl. bedrock) and auto-smelts the drops.
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
			ItemStack tool = player.getMainHandStack();
			if (!tool.isOf(ModItems.METEOR_PICKAXE) || !(world instanceof ServerWorld serverWorld)) {
				return true;
			}

			net.minecraft.item.Item smelted = SMELT.get(state.getBlock());
			if (smelted == null && !isProtected(state)) {
				return true; // normal drops, vanilla handles it (mining is already instant)
			}

			world.breakBlock(pos, false, player);
			if (smelted != null && !player.isCreative()) {
				Block.dropStack(serverWorld, pos, new ItemStack(smelted));
				serverWorld.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.4F,
						1.8F);
			} else if (isProtected(state) && !player.isCreative()) {
				Block.dropStack(serverWorld, pos, new ItemStack(state.getBlock().asItem()));
			}
			return false;
		});

		// Full meteor set -> permanent fire resistance.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) {
				return;
			}
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				if (hasFullSet(player)) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 220, 0, true,
							false, false));
				}
			}
		});
	}

	private static boolean hasFullSet(ServerPlayerEntity player) {
		return player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.METEOR_HELMET)
				&& player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.METEOR_CHESTPLATE)
				&& player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.METEOR_LEGGINGS)
				&& player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.METEOR_BOOTS);
	}

	private static BlockPos unused() {
		return BlockPos.ORIGIN;
	}

	private ModEvents() {
	}
}
