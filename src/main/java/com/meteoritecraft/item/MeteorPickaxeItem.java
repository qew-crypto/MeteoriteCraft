package com.meteoritecraft.item;

import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ToolMaterial;

/** PickaxeItem has a protected constructor, so we expose a public one. */
public class MeteorPickaxeItem extends PickaxeItem {
	public MeteorPickaxeItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
		super(material, attackDamage, attackSpeed, settings);
	}
}
