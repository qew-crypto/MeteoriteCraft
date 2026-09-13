package com.meteoritecraft.item;

import net.minecraft.item.HoeItem;
import net.minecraft.item.ToolMaterial;

/** HoeItem has a protected constructor, so we expose a public one. */
public class MeteorHoeItem extends HoeItem {
	public MeteorHoeItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
		super(material, attackDamage, attackSpeed, settings);
	}
}
