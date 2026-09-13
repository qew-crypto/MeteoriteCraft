package com.meteoritecraft;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ModItemGroups {
	public static final RegistryKey<ItemGroup> METEOR_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP,
			Identifier.of(MeteoriteCraft.MOD_ID, "meteor"));

	public static final ItemGroup METEOR_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(ModItems.METEOR_CORE))
			.displayName(Text.translatable("itemGroup.meteoritecraft.meteor"))
			.build();

	public static void initialize() {
		Registry.register(Registries.ITEM_GROUP, METEOR_GROUP_KEY, METEOR_GROUP);

		// Own tab with everything
		ItemGroupEvents.modifyEntriesEvent(METEOR_GROUP_KEY).register(entries -> {
			entries.add(ModItems.METEOR_SHARD);
			entries.add(ModItems.METEOR_INGOT);
			entries.add(ModItems.METEOR_CORE);
			entries.add(ModItems.COSMIC_DUST);
			entries.add(ModBlocks.METEORITE_BLOCK);
			entries.add(ModBlocks.METEORITE_ORE);
			entries.add(ModBlocks.CHARGED_METEORITE_BLOCK);
			entries.add(ModBlocks.METEOR_BEACON);
			entries.add(ModItems.METEOR_SWORD);
			entries.add(ModItems.METEOR_PICKAXE);
			entries.add(ModItems.METEOR_AXE);
			entries.add(ModItems.METEOR_SHOVEL);
			entries.add(ModItems.METEOR_HOE);
			entries.add(ModItems.METEOR_HELMET);
			entries.add(ModItems.METEOR_CHESTPLATE);
			entries.add(ModItems.METEOR_LEGGINGS);
			entries.add(ModItems.METEOR_BOOTS);
			entries.add(ModItems.METEOR_STAFF);
			entries.add(ModItems.OBLIVION_STICK);
			entries.add(ModItems.METEOR_LOCATOR);
		});

		// Also inside the matching vanilla tabs, so items are where players expect them
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
			entries.addAfter(Items.NETHERITE_SWORD, ModItems.METEOR_SWORD);
			entries.addAfter(Items.NETHERITE_AXE, ModItems.METEOR_AXE);
			entries.addAfter(Items.NETHERITE_HELMET, ModItems.METEOR_HELMET);
			entries.addAfter(Items.NETHERITE_CHESTPLATE, ModItems.METEOR_CHESTPLATE);
			entries.addAfter(Items.NETHERITE_LEGGINGS, ModItems.METEOR_LEGGINGS);
			entries.addAfter(Items.NETHERITE_BOOTS, ModItems.METEOR_BOOTS);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.addAfter(Items.NETHERITE_PICKAXE, ModItems.METEOR_PICKAXE);
			entries.addAfter(Items.NETHERITE_SHOVEL, ModItems.METEOR_SHOVEL);
			entries.addAfter(Items.NETHERITE_HOE, ModItems.METEOR_HOE);
			entries.add(ModItems.METEOR_STAFF);
			entries.add(ModItems.OBLIVION_STICK);
			entries.add(ModItems.METEOR_LOCATOR);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
			entries.add(ModItems.METEOR_SHARD);
			entries.add(ModItems.METEOR_INGOT);
			entries.add(ModItems.METEOR_CORE);
			entries.add(ModItems.COSMIC_DUST);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
			entries.add(ModBlocks.METEORITE_BLOCK);
			entries.add(ModBlocks.METEORITE_ORE);
			entries.add(ModBlocks.CHARGED_METEORITE_BLOCK);
			entries.add(ModBlocks.METEOR_BEACON);
		});
	}

	private ModItemGroups() {
	}
}
