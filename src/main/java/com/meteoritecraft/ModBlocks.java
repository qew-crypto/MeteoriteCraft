package com.meteoritecraft;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public final class ModBlocks {
	public static final Block METEORITE_BLOCK = register("meteorite_block", Block::new,
			AbstractBlock.Settings.create().strength(28.0F, 900.0F).requiresTool()
					.sounds(BlockSoundGroup.NETHERITE));

	public static final Block METEORITE_ORE = register("meteorite_ore", Block::new,
			AbstractBlock.Settings.create().strength(32.0F, 1200.0F).requiresTool()
					.sounds(BlockSoundGroup.ANCIENT_DEBRIS));

	public static final Block CHARGED_METEORITE_BLOCK = register("charged_meteorite_block", Block::new,
			AbstractBlock.Settings.create().strength(36.0F, 1200.0F).requiresTool()
					.luminance(state -> 10).sounds(BlockSoundGroup.NETHERITE));

	/** Place it and the next meteor will fall onto it. */
	public static final Block METEOR_BEACON = register("meteor_beacon", Block::new,
			AbstractBlock.Settings.create().strength(12.0F, 600.0F).requiresTool()
					.luminance(state -> 14).sounds(BlockSoundGroup.METAL));

	private static Block register(String name, Function<AbstractBlock.Settings, Block> factory,
			AbstractBlock.Settings settings) {
		Identifier id = Identifier.of(MeteoriteCraft.MOD_ID, name);
		RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);
		Block block = factory.apply(settings.registryKey(blockKey));
		Registry.register(Registries.BLOCK, blockKey, block);

		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
		Registry.register(Registries.ITEM, itemKey,
				new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
		return block;
	}

	public static void initialize() {
	}

	private ModBlocks() {
	}
}
