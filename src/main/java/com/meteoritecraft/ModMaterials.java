package com.meteoritecraft;

import net.minecraft.item.ToolMaterial;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.Map;

public final class ModMaterials {
	/** Registry key of the equipment asset -> assets/meteoritecraft/equipment/meteor.json */
	public static final RegistryKey<EquipmentAsset> METEOR_ASSET = RegistryKey.of(
			RegistryKey.<EquipmentAsset>ofRegistry(Identifier.ofVanilla("equipment_asset")),
			Identifier.of(MeteoriteCraft.MOD_ID, "meteor"));

	/** Tool material: netherite-beating durability, huge mining speed, high enchantability. */
	public static final ToolMaterial METEOR_TOOL = new ToolMaterial(
			ModTags.NOTHING, // nothing is "incorrect" -> mines every block that can be mined
			4096, // durability
			120.0F, // mining speed (instant on virtually every block)
			5.0F, // attack damage bonus
			22, // enchantability (netherite = 15)
			ModTags.METEOR_REPAIR);

	/** Armor material: +1 armor point per piece compared to netherite. */
	public static final ArmorMaterial METEOR_ARMOR = new ArmorMaterial(
			45, // base durability multiplier (netherite = 37)
			Map.of(
					EquipmentType.HELMET, 4,
					EquipmentType.CHESTPLATE, 9,
					EquipmentType.LEGGINGS, 7,
					EquipmentType.BOOTS, 4),
			22, // enchantability
			SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
			4.0F, // toughness (netherite = 3)
			0.15F, // knockback resistance
			ModTags.METEOR_REPAIR,
			METEOR_ASSET);

	private ModMaterials() {
	}

	public static Registry<?> unused() {
		return null;
	}
}
