package com.meteoritecraft;

import com.meteoritecraft.item.BossHeartItem;
import com.meteoritecraft.item.CosmicElixirItem;
import com.meteoritecraft.item.GravityCubeItem;
import com.meteoritecraft.item.StarfallOrbItem;
import com.meteoritecraft.item.VoidAnchorItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Second wave of content: utility items, boss summon and crafting materials. */
public final class ModExtraItems {

	public static final Item STARFALL_ORB = register("starfall_orb", StarfallOrbItem::new,
			new Item.Settings().maxDamage(16).rarity(Rarity.EPIC).fireproof(),
			lore("ПКМ: звездопад — сразу 5 метеоритов вокруг", "Перезарядка: 3 минуты"));

	public static final Item GRAVITY_CUBE = register("gravity_cube", GravityCubeItem::new,
			new Item.Settings().maxDamage(64).rarity(Rarity.RARE),
			lore("ПКМ: стягивает всех мобов в радиусе 24 блоков",
					"Даёт им замедление и слабость", "Перезарядка: 30 секунд"));

	public static final Item VOID_ANCHOR = register("void_anchor", VoidAnchorItem::new,
			new Item.Settings().maxDamage(128).rarity(Rarity.RARE),
			lore("Shift + ПКМ: сохранить точку", "ПКМ: телепорт к сохранённой точке",
					"Перезарядка: 1 минута"));

	public static final Item COSMIC_ELIXIR = register("cosmic_elixir", CosmicElixirItem::new,
			new Item.Settings().maxCount(8).rarity(Rarity.RARE),
			lore("ПКМ: сила II, сопротивление II, реген II",
					"Скорость II, огнестойкость, поглощение IV", "Длительность: 90 секунд"));

	public static final Item BOSS_HEART = register("boss_heart", BossHeartItem::new,
			new Item.Settings().maxCount(4).rarity(Rarity.EPIC).fireproof(),
			lore("ПКМ: призывает случайного босса", "Осторожно: у боссов до 520 HP", "Одноразовое"));

	public static final Item VOID_CRYSTAL = register("void_crystal", Item::new,
			new Item.Settings().rarity(Rarity.RARE),
			lore("Кристалл пустоты", "Материал для звёздных рецептов"));

	public static final Item STAR_FRAGMENT = register("star_fragment", Item::new,
			new Item.Settings().rarity(Rarity.UNCOMMON),
			lore("Обломок звезды", "Падает из боссов и легендарных метеоритов"));

	public static void initialize() {
		ItemGroupEvents.modifyEntriesEvent(ModItemGroups.METEOR_GROUP_KEY).register(entries -> {
			entries.add(STARFALL_ORB);
			entries.add(GRAVITY_CUBE);
			entries.add(VOID_ANCHOR);
			entries.add(COSMIC_ELIXIR);
			entries.add(BOSS_HEART);
			entries.add(VOID_CRYSTAL);
			entries.add(STAR_FRAGMENT);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
			entries.add(STARFALL_ORB);
			entries.add(GRAVITY_CUBE);
			entries.add(VOID_ANCHOR);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
			entries.add(VOID_CRYSTAL);
			entries.add(STAR_FRAGMENT);
			entries.add(BOSS_HEART);
		});
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> entries.add(COSMIC_ELIXIR));
	}

	private static LoreComponent lore(String... lines) {
		List<Text> texts = new ArrayList<>();
		for (String line : lines) {
			texts.add(Text.literal(line).formatted(Formatting.GRAY));
		}
		return new LoreComponent(texts);
	}

	private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings,
			LoreComponent lore) {
		RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MeteoriteCraft.MOD_ID, name));
		return Registry.register(Registries.ITEM, key,
				factory.apply(settings.registryKey(key).component(DataComponentTypes.LORE, lore)));
	}

	private ModExtraItems() {
	}
}
