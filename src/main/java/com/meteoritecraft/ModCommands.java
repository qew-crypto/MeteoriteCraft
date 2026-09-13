package com.meteoritecraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * /meteor                      - opens the menu
 * /meteor menu                 - clickable menu with everything
 * /meteor spawn [rarity]       - random meteor within 2000 blocks (rarity optional)
 * /meteor near [distance]      - meteor close to you
 * /meteor here                 - meteor right where you stand
 * /meteor at                   - meteor where you are looking
 * /meteor locate               - coordinates of every active meteor
 * /meteor clear                - forget tracked meteors
 * /meteor tp [number]          - teleport to a meteor
 * /meteor rain [count]         - starfall: several meteors at once
 * /meteor boss [type|list]     - summon or list bosses
 * /meteor ench                 - custom enchantment list
 * /meteor kit                  - full meteor set (creative helper)
 * /meteor auto on|off          - automatic random spawns
 * /meteor help                 - command list
 */
public final class ModCommands {

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		var root = CommandManager.literal("meteor")
				.executes(context -> menu(context.getSource()));

		root.then(CommandManager.literal("menu").executes(context -> menu(context.getSource())));
		root.then(CommandManager.literal("help").executes(context -> help(context.getSource())));

		// /meteor spawn  +  /meteor spawn common|rare|epic|legendary
		var spawn = CommandManager.literal("spawn")
				.requires(source -> source.hasPermissionLevel(0))
				.executes(context -> spawn(context, null, MeteorManager.MAX_DISTANCE));
		for (MeteorManager.Rarity rarity : MeteorManager.Rarity.values()) {
			spawn.then(CommandManager.literal(rarity.name().toLowerCase())
					.executes(context -> spawn(context, rarity, MeteorManager.MAX_DISTANCE)));
		}
		root.then(spawn);

		root.then(CommandManager.literal("near")
				.executes(context -> spawn(context, null, 110))
				.then(CommandManager.argument("distance", IntegerArgumentType.integer(16, 2000))
						.executes(context -> spawn(context, null,
								IntegerArgumentType.getInteger(context, "distance")))));

		root.then(CommandManager.literal("here").executes(ModCommands::here));
		root.then(CommandManager.literal("at").executes(ModCommands::at));
		root.then(CommandManager.literal("locate").executes(context -> locate(context.getSource())));
		root.then(CommandManager.literal("clear").executes(context -> {
			int count = MeteorManager.active().size();
			MeteorManager.active().clear();
			context.getSource().sendFeedback(() -> text("Список очищен: " + count, Formatting.GRAY), false);
			return 1;
		}));
		// /meteor tp  +  /meteor tp <number from /meteor locate>
		root.then(CommandManager.literal("tp")
				.executes(context -> teleport(context, 0))
				.then(CommandManager.argument("number", IntegerArgumentType.integer(1, 64))
						.executes(context -> teleport(context,
								IntegerArgumentType.getInteger(context, "number")))));

		// /meteor rain [count]
		root.then(CommandManager.literal("rain")
				.executes(context -> rain(context, 5))
				.then(CommandManager.argument("count", IntegerArgumentType.integer(1, 20))
						.executes(context -> rain(context, IntegerArgumentType.getInteger(context, "count")))));

		// /meteor boss  +  /meteor boss <type>
		var boss = CommandManager.literal("boss")
				.executes(context -> boss(context, null));
		for (BossManager.BossType type : BossManager.BossType.values()) {
			boss.then(CommandManager.literal(type.name().toLowerCase())
					.executes(context -> boss(context, type)));
		}
		boss.then(CommandManager.literal("list").executes(context -> bossList(context.getSource())));
		root.then(boss);

		root.then(CommandManager.literal("ench").executes(context -> enchantments(context.getSource())));
		root.then(CommandManager.literal("kit").executes(ModCommands::kit));
		root.then(CommandManager.literal("auto")
				.then(CommandManager.literal("on").executes(context -> auto(context.getSource(), true)))
				.then(CommandManager.literal("off").executes(context -> auto(context.getSource(), false))));

		dispatcher.register(root);
		dispatcher.register(CommandManager.literal("meteors")
				.executes(context -> menu(context.getSource())));
	}

	// ------------------------------------------------------------------ menu
	private static int menu(ServerCommandSource source) {
		source.sendFeedback(() -> text("═════ ☄ МЕТЕОРИТЫ ═════", Formatting.GOLD), false);
		source.sendFeedback(() -> row("Спавн случайного метеорита (до 2000 блоков)",
				"Спавн", "/meteor spawn", Formatting.AQUA), false);
		source.sendFeedback(() -> Text.literal(" Редкости: ").formatted(Formatting.GRAY)
				.append(button("Обычный", "/meteor spawn common", Formatting.WHITE))
				.append(Text.literal(" "))
				.append(button("Редкий", "/meteor spawn rare", Formatting.AQUA))
				.append(Text.literal(" "))
				.append(button("Эпический", "/meteor spawn epic", Formatting.LIGHT_PURPLE))
				.append(Text.literal(" "))
				.append(button("Легендарный", "/meteor spawn legendary", Formatting.GOLD)), false);
		source.sendFeedback(() -> row("Метеорит рядом с тобой", "Рядом", "/meteor near", Formatting.AQUA), false);
		source.sendFeedback(() -> row("Метеорит точно на тебя", "Сюда", "/meteor here", Formatting.AQUA), false);
		source.sendFeedback(() -> row("Метеорит туда, куда смотришь", "В точку взгляда", "/meteor at", Formatting.AQUA),
				false);
		source.sendFeedback(() -> row("Координаты всех активных метеоритов", "Найти", "/meteor locate",
				Formatting.GREEN), false);
		source.sendFeedback(() -> row("Телепорт к ближайшему метеориту", "Телепорт", "/meteor tp",
				Formatting.LIGHT_PURPLE), false);
		source.sendFeedback(() -> row("Звездопад: сразу 5 метеоритов рядом", "Звездопад", "/meteor rain",
				Formatting.GOLD), false);
		source.sendFeedback(() -> Text.literal(" Боссы: ").formatted(Formatting.GRAY)
				.append(button("Звёздный Страж", "/meteor boss star_warden", Formatting.AQUA))
				.append(Text.literal(" "))
				.append(button("Титан", "/meteor boss meteor_titan", Formatting.GOLD))
				.append(Text.literal(" "))
				.append(button("Вестник", "/meteor boss void_herald", Formatting.DARK_PURPLE))
				.append(Text.literal(" "))
				.append(button("Павшая Звезда", "/meteor boss fallen_star", Formatting.RED)), false);
		source.sendFeedback(() -> row("Список кастомных чар мода", "Чары", "/meteor ench", Formatting.BLUE), false);
		source.sendFeedback(() -> row("Полный набор снаряжения метеорита", "Набор", "/meteor kit", Formatting.GREEN),
				false);
		source.sendFeedback(() -> Text.literal(" Автоспавн: ").formatted(Formatting.GRAY)
				.append(Text.literal(MeteorManager.isAutoEnabled() ? "включён " : "выключен ")
						.formatted(MeteorManager.isAutoEnabled() ? Formatting.GREEN : Formatting.RED))
				.append(button("Вкл", "/meteor auto on", Formatting.GREEN))
				.append(Text.literal(" "))
				.append(button("Выкл", "/meteor auto off", Formatting.RED)), false);
		source.sendFeedback(() -> text("Активных метеоритов: " + MeteorManager.active().size(), Formatting.DARK_GRAY),
				false);
		return 1;
	}

	private static int help(ServerCommandSource source) {
		String[] lines = {
				"/meteor — открыть меню",
				"/meteor menu — меню со всеми кнопками",
				"/meteor spawn [common|rare|epic|legendary]",
				"/meteor near [дистанция]",
				"/meteor here — метеорит на тебя",
				"/meteor at — в точку взгляда",
				"/meteor locate — координаты метеоритов",
				"/meteor clear — очистить список",
				"/meteor tp [номер] — телепорт к метеориту",
				"/meteor rain [кол-во] — звездопад",
				"/meteor boss [star_warden|meteor_titan|void_herald|fallen_star]",
				"/meteor boss list — активные боссы",
				"/meteor ench — кастомные чары",
				"/meteor kit — полный сет",
				"/meteor auto on|off — автоспавн"
		};
		source.sendFeedback(() -> text("☄ Команды мода:", Formatting.GOLD), false);
		for (String line : lines) {
			source.sendFeedback(() -> text(" " + line, Formatting.GRAY), false);
		}
		return 1;
	}

	// ----------------------------------------------------------------- spawn
	private static int spawn(CommandContext<ServerCommandSource> context, MeteorManager.Rarity rarity,
			int distance) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendError(text("Команду нужно вызывать от игрока", Formatting.RED));
			return 0;
		}
		BlockPos pos = MeteorManager.spawnNearPlayer(player, rarity, distance, true);
		if (pos == null) {
			context.getSource().sendError(text("Не нашлось подходящего места, попробуй ещё раз", Formatting.RED));
			return 0;
		}
		return 1;
	}

	private static int here(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		MeteorManager.spawnAt((ServerWorld) player.getWorld(), player.getBlockPos(), null, true);
		return 1;
	}

	private static int at(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		BlockPos looking = TargetHelper.lookingAt(player, 160.0);
		if (looking == null) {
			context.getSource().sendError(text("Не вижу блок в точке взгляда", Formatting.RED));
			return 0;
		}
		MeteorManager.spawnAt((ServerWorld) player.getWorld(), looking, null, true);
		return 1;
	}

	private static int locate(ServerCommandSource source) {
		List<MeteorManager.Meteor> meteors = MeteorManager.active();
		if (meteors.isEmpty()) {
			source.sendFeedback(() -> text("Активных метеоритов нет", Formatting.GRAY), false);
			return 1;
		}
		source.sendFeedback(() -> text("☄ Активные метеориты (" + meteors.size() + "):", Formatting.GOLD), false);
		for (MeteorManager.Meteor meteor : meteors) {
			source.sendFeedback(() -> Text.literal(" • ").formatted(Formatting.DARK_GRAY)
					.append(Text.literal(meteor.rarity.displayName + " ").formatted(meteor.rarity.color))
					.append(MeteorManager.coordsText(meteor.pos)), false);
		}
		return 1;
	}

	// ------------------------------------------------------------ teleport
	private static int teleport(CommandContext<ServerCommandSource> context, int number) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		List<MeteorManager.Meteor> meteors = MeteorManager.active();
		if (meteors.isEmpty()) {
			context.getSource().sendError(text("Активных метеоритов нет. Сначала /meteor spawn", Formatting.RED));
			return 0;
		}

		MeteorManager.Meteor meteor;
		if (number <= 0) {
			meteor = MeteorManager.nearest(player);
			if (meteor == null) {
				meteor = meteors.get(0);
			}
		} else {
			if (number > meteors.size()) {
				context.getSource().sendError(text("Такого номера нет, всего: " + meteors.size(),
						Formatting.RED));
				return 0;
			}
			meteor = meteors.get(number - 1);
		}

		ServerWorld world = (ServerWorld) player.getWorld();
		BlockPos ground = MeteorManager.findGround(world, meteor.pos.getX(), meteor.pos.getZ());
		BlockPos target = ground != null ? ground.up() : meteor.pos.up(2);
		player.requestTeleport(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

		MeteorManager.Meteor finalMeteor = meteor;
		context.getSource().sendFeedback(() -> Text.literal("Телепорт к метеориту ")
				.formatted(Formatting.GRAY)
				.append(Text.literal(finalMeteor.rarity.displayName + " ").formatted(finalMeteor.rarity.color))
				.append(MeteorManager.coordsText(finalMeteor.pos)), false);
		return 1;
	}

	// -------------------------------------------------------------- starfall
	private static int rain(CommandContext<ServerCommandSource> context, int count) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			if (MeteorManager.spawnNearPlayer(player, null, 120, false) != null) {
				spawned++;
			}
		}
		int finalSpawned = spawned;
		context.getSource().sendFeedback(() -> text("☄ Звездопад! Метеоритов вызвано: " + finalSpawned
				+ ". /meteor locate — координаты", Formatting.GOLD), false);
		return 1;
	}

	// ----------------------------------------------------------------- bosses
	private static int boss(CommandContext<ServerCommandSource> context, BossManager.BossType type) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		BossManager.BossType finalType = type != null ? type : BossManager.randomType();
		BlockPos target = TargetHelper.lookingAt(player, 48.0);
		BossManager.Boss spawned = BossManager.spawn((ServerWorld) player.getWorld(), target, finalType, true);
		if (spawned == null) {
			context.getSource().sendError(text("Не удалось призвать босса здесь", Formatting.RED));
			return 0;
		}
		return 1;
	}

	private static int bossList(ServerCommandSource source) {
		var bosses = BossManager.active();
		if (bosses.isEmpty()) {
			source.sendFeedback(() -> text("Активных боссов нет", Formatting.GRAY), false);
			return 1;
		}
		source.sendFeedback(() -> text("☠ Активные боссы (" + bosses.size() + "):", Formatting.RED), false);
		for (BossManager.Boss boss : bosses) {
			source.sendFeedback(() -> Text.literal(" • ").formatted(Formatting.DARK_GRAY)
					.append(Text.literal(boss.type.displayName + " ").formatted(boss.type.color))
					.append(Text.literal("HP " + (int) boss.entity.getHealth() + "/"
							+ (int) boss.entity.getMaxHealth() + ", фаза " + boss.phase + " ")
							.formatted(Formatting.GRAY))
					.append(MeteorManager.coordsText(boss.entity.getBlockPos())), false);
		}
		return 1;
	}

	// ------------------------------------------------------------ enchantments
	private static int enchantments(ServerCommandSource source) {
		String[][] list = {
				{"meteor_strike", "Удар Метеора", "+урон и поджигает цель (V)"},
				{"cosmic_guard", "Космическая Защита", "сильнее Защиты, работает на всей броне (IV)"},
				{"gravity_well", "Гравитационный Колодец", "отбрасывает и замедляет врагов (III)"},
				{"star_vein", "Звёздная Жила", "больше опыта из блоков (III)"},
				{"void_step", "Шаг Пустоты", "скорость и прыжок с ботинок (III)"},
				{"starforged", "Звёздная Закалка", "почти не тратит прочность (III)"}
		};
		source.sendFeedback(() -> text("✦ Кастомные чары мода:", Formatting.BLUE), false);
		for (String[] entry : list) {
			source.sendFeedback(() -> Text.literal(" • ").formatted(Formatting.DARK_GRAY)
					.append(Text.literal(entry[1] + " ").formatted(Formatting.AQUA))
					.append(Text.literal("— " + entry[2] + " ").formatted(Formatting.GRAY))
					.append(button("Выдать", "/enchant @s meteoritecraft:" + entry[0], Formatting.GREEN)),
					false);
		}
		source.sendFeedback(() -> text(" Также выпадают в книгах из метеоритов и с боссов", Formatting.DARK_GRAY),
				false);
		return 1;
	}

	private static int kit(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			return 0;
		}
		give(player, ModItems.METEOR_SWORD);
		give(player, ModItems.METEOR_PICKAXE);
		give(player, ModItems.METEOR_AXE);
		give(player, ModItems.METEOR_SHOVEL);
		give(player, ModItems.METEOR_HOE);
		give(player, ModItems.METEOR_HELMET);
		give(player, ModItems.METEOR_CHESTPLATE);
		give(player, ModItems.METEOR_LEGGINGS);
		give(player, ModItems.METEOR_BOOTS);
		give(player, ModItems.METEOR_STAFF);
		give(player, ModItems.OBLIVION_STICK);
		give(player, ModItems.METEOR_LOCATOR);
		give(player, ModBlocks.METEOR_BEACON.asItem());
		give(player, ModExtraItems.STARFALL_ORB);
		give(player, ModExtraItems.GRAVITY_CUBE);
		give(player, ModExtraItems.VOID_ANCHOR);
		give(player, ModExtraItems.COSMIC_ELIXIR);
		give(player, ModExtraItems.BOSS_HEART);
		context.getSource().sendFeedback(() -> text("Набор выдан", Formatting.GREEN), false);
		return 1;
	}

	private static void give(ServerPlayerEntity player, net.minecraft.item.Item item) {
		player.getInventory().insertStack(new ItemStack(item));
	}

	private static int auto(ServerCommandSource source, boolean value) {
		MeteorManager.setAutoEnabled(value);
		source.sendFeedback(() -> text(value ? "Автоспавн включён" : "Автоспавн выключен",
				value ? Formatting.GREEN : Formatting.RED), false);
		return 1;
	}

	// ---------------------------------------------------------------- format
	private static MutableText text(String value, Formatting color) {
		return Text.literal(value).formatted(color);
	}

	private static MutableText button(String label, String command, Formatting color) {
		return Text.literal("[" + label + "]").styled(style -> style
				.withColor(color)
				.withBold(true)
				.withClickEvent(new net.minecraft.text.ClickEvent(
						net.minecraft.text.ClickEvent.Action.RUN_COMMAND, command))
				.withHoverEvent(new net.minecraft.text.HoverEvent(
						net.minecraft.text.HoverEvent.Action.SHOW_TEXT, Text.literal(command))));
	}

	private static MutableText row(String description, String label, String command, Formatting color) {
		return Text.literal(" ").append(button(label, command, color))
				.append(Text.literal(" " + description).formatted(Formatting.GRAY));
	}

	private ModCommands() {
	}
}
