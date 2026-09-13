package com.meteoritecraft;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class ModTags {
	public static final TagKey<Item> METEOR_REPAIR = TagKey.of(RegistryKeys.ITEM,
			Identifier.of(MeteoriteCraft.MOD_ID, "meteor_repair"));

	// Blocks that never drop when mined with meteor tools (empty tag = everything drops).
	public static final TagKey<Block> NOTHING = TagKey.of(RegistryKeys.BLOCK,
			Identifier.of(MeteoriteCraft.MOD_ID, "incorrect_for_meteor_tool"));

	private ModTags() {
	}
}
