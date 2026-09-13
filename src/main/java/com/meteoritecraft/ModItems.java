package com.meteoritecraft;

import com.meteoritecraft.item.MeteorHoeItem;
import com.meteoritecraft.item.MeteorLocatorItem;
import com.meteoritecraft.item.MeteorPickaxeItem;
import com.meteoritecraft.item.MeteorStaffItem;
import com.meteoritecraft.item.OblivionStickItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.List;
import java.util.function.Function;

public final class ModItems {

	// ------------------------------------------------------------ resources
	public static final Item METEOR_SHARD = register("meteor_shard", Item::new,
			base().rarity(Rarity.UNCOMMON), lore("Осколок упавшего метеорита", "9 осколков = 1 слиток"));

	public static final Item METEOR_INGOT = register("meteor_ingot", Item::new,
			base().rarity(Rarity.RARE), lore("Метеоритный слиток", "Ремонтирует снаряжение метеорита"));

	public static final Item METEOR_CORE = register("meteor_core", Item::new,
			base().rarity(Rarity.EPIC).fireproof(), lore("Ядро метеорита", "Бьётся только в легендарных метеоритах"));

	public static final Item COSMIC_DUST = register("cosmic_dust", Item::new,
			base(), lore("Космическая пыль", "Используется в звёздных рецептах"));

	// ------------------------------------------------------------ tools
	public static final Item METEOR_SWORD = register("meteor_sword",
			settings -> new SwordItem(ModMaterials.METEOR_TOOL, 10.0F, -2.4F, settings),
			base().rarity(Rarity.EPIC).fireproof(),
			lore("Урон в бою: 15", "Всегда поджигает цель (Заговор огня)", "Принимает все чары незеритового меча"));

	public static final Item METEOR_PICKAXE = register("meteor_pickaxe",
			settings -> new MeteorPickaxeItem(ModMaterials.METEOR_TOOL, 6.0F, -2.8F, settings),
			base().rarity(Rarity.EPIC).fireproof(),
			lore("Ломает любой блок мгновенно", "Ломает даже бедрок и барьеры", "Автопереплавка руды при добыче"));

	public static final Item METEOR_AXE = register("meteor_axe",
			settings -> new AxeItem(ModMaterials.METEOR_TOOL, 12.0F, -3.0F, settings),
			base().rarity(Rarity.EPIC).fireproof(), lore("Урон в бою: 17", "Мгновенно валит дерево"));

	public static final Item METEOR_SHOVEL = register("meteor_shovel",
			settings -> new ShovelItem(ModMaterials.METEOR_TOOL, 7.0F, -3.0F, settings),
			base().rarity(Rarity.EPIC).fireproof(), lore("Копает мгновенно"));

	public static final Item METEOR_HOE = register("meteor_hoe",
			settings -> new MeteorHoeItem(ModMaterials.METEOR_TOOL, 5.0F, -3.0F, settings),
			base().rarity(Rarity.EPIC).fireproof(), lore("Обрабатывает землю мгновенно"));

	// ------------------------------------------------------------ armor
	public static final Item METEOR_HELMET = register("meteor_helmet",
			settings -> new Item(ModMaterials.METEOR_ARMOR.applySettings(settings, EquipmentType.HELMET)),
			base().rarity(Rarity.EPIC).fireproof(),
			lore("Броня: 4 (незерит 3)", "Полный сет: огнестойкость", "Принимает все чары брони"));

	public static final Item METEOR_CHESTPLATE = register("meteor_chestplate",
			settings -> new Item(ModMaterials.METEOR_ARMOR.applySettings(settings, EquipmentType.CHESTPLATE)),
			base().rarity(Rarity.EPIC).fireproof(),
			lore("Броня: 9 (незерит 8)", "Полный сет: огнестойкость", "Принимает все чары брони"));

	public static final Item METEOR_LEGGINGS = register("meteor_leggings",
			settings -> new Item(ModMaterials.METEOR_ARMOR.applySettings(settings, EquipmentType.LEGGINGS)),
			base().rarity(Rarity.EPIC).fireproof(),
			lore("Броня: 7 (незерит 6)", "Полный сет: огнестойкость", "Принимает все чары брони"));

	public static final Item METEOR_BOOTS = register("meteor_boots",
			settings -> new Item(ModMaterials.METEOR_ARMOR.applySettings(settings, EquipmentType.BOOTS)),
			base().rarity(Rarity.EPIC).fireproof(),
			lore("Броня: 4 (незерит 3)", "Полный сет: огнестойкость", "Принимает все чары брони"));

	// ------------------------------------------------------------ special
	public static final Item METEOR_STAFF = register("meteor_staff", MeteorStaffItem::new,
			base().maxDamage(256).rarity(Rarity.EPIC).fireproof(),
			lore("ПКМ: телепорт к ближайшему метеориту", "Перезарядка: 5 минут"));

	public static final Item OBLIVION_STICK = register("oblivion_stick", OblivionStickItem::new,
			base().maxCount(1).rarity(Rarity.EPIC).fireproof(),
			lore("ПКМ: вызывает метеорит туда, куда смотришь", "Одноразовая"));

	public static final Item METEOR_LOCATOR = register("meteor_locator", MeteorLocatorItem::new,
			base().maxCount(1).rarity(Rarity.RARE),
			lore("ПКМ: координаты всех активных метеоритов", "Перезарядка: 1 минута"));

	private static Item.Settings base() {
		return new Item.Settings();
	}

	private static LoreComponent lore(String... lines) {
		List<Text> texts = new java.util.ArrayList<>();
		for (String line : lines) {
			texts.add(Text.literal(line).formatted(Formatting.GRAY));
		}
		return new LoreComponent(texts);
	}

	private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings,
			LoreComponent lore) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MeteoriteCraft.MOD_ID, name));
		Item.Settings finalSettings = settings.registryKey(key).component(DataComponentTypes.LORE, lore);
		return Registry.register(Registries.ITEM, key, factory.apply(finalSettings));
	}

	public static void initialize() {
	}

	private ModItems() {
	}
}
