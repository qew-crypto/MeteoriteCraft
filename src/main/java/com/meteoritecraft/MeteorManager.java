package com.meteoritecraft;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Spawning, falling, impact, loot and tracking of all meteors. */
public final class MeteorManager {

	public enum Rarity {
		COMMON("Обычный", Formatting.WHITE, 3, 1, 58),
		RARE("Редкий", Formatting.AQUA, 4, 1, 27),
		EPIC("Эпический", Formatting.LIGHT_PURPLE, 5, 2, 12),
		LEGENDARY("Легендарный", Formatting.GOLD, 6, 2, 3);

		public final String displayName;
		public final Formatting color;
		public final int radius;
		public final int chests;
		public final int weight;

		Rarity(String displayName, Formatting color, int radius, int chests, int weight) {
			this.displayName = displayName;
			this.color = color;
			this.radius = radius;
			this.chests = chests;
			this.weight = weight;
		}

		public static Rarity roll(Random random) {
			int total = 0;
			for (Rarity r : values()) {
				total += r.weight;
			}
			int roll = random.nextInt(total);
			for (Rarity r : values()) {
				roll -= r.weight;
				if (roll < 0) {
					return r;
				}
			}
			return COMMON;
		}

		public static Rarity byName(String name) {
			for (Rarity r : values()) {
				if (r.name().equalsIgnoreCase(name)) {
					return r;
				}
			}
			return null;
		}
	}

	public static final class Meteor {
		public final BlockPos pos;
		public final Rarity rarity;
		public final String worldName;
		public long expiresAtTick;

		Meteor(BlockPos pos, Rarity rarity, String worldName, long expiresAtTick) {
			this.pos = pos;
			this.rarity = rarity;
			this.worldName = worldName;
			this.expiresAtTick = expiresAtTick;
		}
	}

	private static final class Falling {
		final ServerWorld world;
		final BlockPos target;
		final Rarity rarity;
		int ticksLeft;
		final int totalTicks;

		Falling(ServerWorld world, BlockPos target, Rarity rarity, int ticks) {
			this.world = world;
			this.target = target;
			this.rarity = rarity;
			this.ticksLeft = ticks;
			this.totalTicks = ticks;
		}
	}

	public static final int MAX_DISTANCE = 2000;
	private static final int FALL_TICKS = 100;
	private static final long LIFETIME_TICKS = 45L * 60L * 20L;
	private static final Random RANDOM = new Random();

	private static final List<Meteor> ACTIVE = new ArrayList<>();
	private static final List<Falling> FALLING = new ArrayList<>();
	private static final Map<String, Long> NEXT_AUTO = new HashMap<>();
	private static boolean autoEnabled = true;

	private MeteorManager() {
	}

	public static boolean isAutoEnabled() {
		return autoEnabled;
	}

	public static void setAutoEnabled(boolean value) {
		autoEnabled = value;
	}

	public static List<Meteor> active() {
		return ACTIVE;
	}

	// ------------------------------------------------------------------ tick
	public static void tick(MinecraftServer server) {
		long time = server.getOverworld().getTime();

		// falling animation + impact
		for (int i = FALLING.size() - 1; i >= 0; i--) {
			Falling falling = FALLING.get(i);
			falling.ticksLeft--;
			double progress = 1.0 - (double) falling.ticksLeft / falling.totalTicks;
			double y = falling.target.getY() + 140.0 * (1.0 - progress);
			falling.world.spawnParticles(ParticleTypes.FLAME, falling.target.getX() + 0.5, y,
					falling.target.getZ() + 0.5, 24, 0.6, 0.6, 0.6, 0.02);
			falling.world.spawnParticles(ParticleTypes.LARGE_SMOKE, falling.target.getX() + 0.5, y,
					falling.target.getZ() + 0.5, 12, 0.8, 0.8, 0.8, 0.01);
			if (falling.ticksLeft <= 0) {
				FALLING.remove(i);
				impact(falling.world, falling.target, falling.rarity, time);
			}
		}

		// expire old meteors
		ACTIVE.removeIf(meteor -> meteor.expiresAtTick <= time);

		// automatic spawns every 8-22 minutes
		if (autoEnabled) {
			for (ServerWorld world : server.getWorlds()) {
				if (world.getPlayers().isEmpty()) {
					continue;
				}
				String key = world.getRegistryKey().getValue().toString();
				long next = NEXT_AUTO.getOrDefault(key, time + randomDelay());
				if (time >= next) {
					NEXT_AUTO.put(key, time + randomDelay());
					ServerPlayerEntity player = world.getPlayers().get(RANDOM.nextInt(world.getPlayers().size()));
					spawnNearPlayer(player, null, MAX_DISTANCE, true);
				} else {
					NEXT_AUTO.put(key, next);
				}
			}
		}
	}

	private static long randomDelay() {
		return (8 * 60 + RANDOM.nextInt(14 * 60)) * 20L;
	}

	// ----------------------------------------------------------- spawn entry
	/** Spawns a meteor at a random position around the player. Returns the impact position or null. */
	public static BlockPos spawnNearPlayer(ServerPlayerEntity player, Rarity forced, int maxDistance,
			boolean announce) {
		ServerWorld world = (ServerWorld) player.getWorld();
		Rarity rarity = forced != null ? forced : Rarity.roll(RANDOM);

		// A beacon always wins: the meteor falls right onto it
		BlockPos beacon = findBeacon(world, player.getBlockPos(), 128);
		if (beacon != null) {
			return beginFall(world, beacon.up(), rarity, announce);
		}

		int[] distances = { maxDistance, Math.min(maxDistance, 800), Math.min(maxDistance, 300),
				Math.min(maxDistance, 110), 40 };
		for (int distance : distances) {
			for (int attempt = 0; attempt < 12; attempt++) {
				double angle = RANDOM.nextDouble() * Math.PI * 2.0;
				double r = 24 + RANDOM.nextDouble() * Math.max(1, distance - 24);
				int x = player.getBlockX() + (int) (Math.cos(angle) * r);
				int z = player.getBlockZ() + (int) (Math.sin(angle) * r);
				BlockPos ground = findGround(world, x, z);
				if (ground != null) {
					return beginFall(world, ground, rarity, announce);
				}
			}
		}
		return null;
	}

	/** Spawns a meteor exactly at the given position (Oblivion Stick / /meteor at). */
	public static BlockPos spawnAt(ServerWorld world, BlockPos pos, Rarity rarity, boolean announce) {
		BlockPos ground = findGround(world, pos.getX(), pos.getZ());
		BlockPos target = ground != null ? ground : pos;
		return beginFall(world, target, rarity != null ? rarity : Rarity.roll(RANDOM), announce);
	}

	private static BlockPos beginFall(ServerWorld world, BlockPos target, Rarity rarity, boolean announce) {
		world.getChunk(target.getX() >> 4, target.getZ() >> 4); // force-generate the chunk
		FALLING.add(new Falling(world, target, rarity, FALL_TICKS));
		world.playSound(null, target, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.AMBIENT, 1.2F,
				0.6F);
		if (announce) {
			broadcast(world, Text.literal("С неба падает ").formatted(Formatting.GRAY)
					.append(Text.literal(rarity.displayName.toLowerCase() + " метеорит").formatted(rarity.color))
					.append(Text.literal("! Удар через 5 секунд...").formatted(Formatting.GRAY)));
		}
		return target;
	}

	// --------------------------------------------------------------- impact
	private static void impact(ServerWorld world, BlockPos center, Rarity rarity, long time) {
		BlockPos ground = findGround(world, center.getX(), center.getZ());
		BlockPos pos = ground != null ? ground : center;
		int radius = rarity.radius;

		// crater
		for (int x = -radius - 1; x <= radius + 1; x++) {
			for (int y = -radius - 1; y <= radius + 1; y++) {
				for (int z = -radius - 1; z <= radius + 1; z++) {
					double dist = Math.sqrt(x * x + y * y + z * z);
					BlockPos p = pos.add(x, y, z);
					if (p.getY() < world.getBottomY() + 2 || p.getY() > world.getTopYInclusive() - 1) {
						continue;
					}
					if (dist <= radius - 1) {
						world.setBlockState(p, Blocks.AIR.getDefaultState());
					} else if (dist <= radius + 0.7) {
						BlockState state;
						double r = RANDOM.nextDouble();
						if (r < 0.14) {
							state = ModBlocks.METEORITE_ORE.getDefaultState();
						} else if (r < 0.2 && rarity.ordinal() >= Rarity.EPIC.ordinal()) {
							state = ModBlocks.CHARGED_METEORITE_BLOCK.getDefaultState();
						} else if (r < 0.3) {
							state = Blocks.MAGMA_BLOCK.getDefaultState();
						} else {
							state = ModBlocks.METEORITE_BLOCK.getDefaultState();
						}
						world.setBlockState(p, state);
					}
				}
			}
		}

		// loot chests in the center
		boolean placed = false;
		for (int i = 0; i < rarity.chests; i++) {
			BlockPos chestPos = pos.add(i == 0 ? 0 : 1, 0, 0);
			world.setBlockState(chestPos, Blocks.CHEST.getDefaultState());
			BlockEntity entity = world.getBlockEntity(chestPos);
			if (entity instanceof ChestBlockEntity chest) {
				fillChest(world, chest, rarity);
				placed = true;
			}
		}

		world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS, 6.0F, 0.5F);
		world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
				4, 2.0, 1.0, 2.0, 0.0);

		if (!placed) {
			return; // nothing verified -> do not lie about coordinates
		}

		ACTIVE.add(new Meteor(pos, rarity, world.getRegistryKey().getValue().toString(), time + LIFETIME_TICKS));

		MutableText message = Text.literal("⚡ ").formatted(rarity.color)
				.append(Text.literal(rarity.displayName + " метеорит").formatted(rarity.color, Formatting.BOLD))
				.append(Text.literal(" упал на ").formatted(Formatting.GRAY))
				.append(coordsText(pos));
		broadcast(world, message);
	}

	public static MutableText coordsText(BlockPos pos) {
		String coords = pos.getX() + " " + pos.getY() + " " + pos.getZ();
		return Text.literal("[" + coords + "]").styled(style -> style
				.withColor(Formatting.YELLOW)
				.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, coords))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
						Text.literal("Нажми, чтобы скопировать координаты"))));
	}

	// --------------------------------------------------------------- helpers
	/** Real surface: topmost solid block, never a fixed height. */
	public static BlockPos findGround(ServerWorld world, int x, int z) {
		world.getChunk(x >> 4, z >> 4);
		int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
		for (int y = Math.min(top + 1, world.getTopYInclusive() - 2); y > world.getBottomY() + 2; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = world.getBlockState(pos);
			if (!state.isAir() && !state.isLiquid()) {
				return pos.up();
			}
		}
		return null;
	}

	private static BlockPos findBeacon(ServerWorld world, BlockPos around, int radius) {
		for (int x = -radius; x <= radius; x += 4) {
			for (int z = -radius; z <= radius; z += 4) {
				for (int y = -40; y <= 40; y += 4) {
					BlockPos pos = around.add(x, y, z);
					if (world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)
							&& world.getBlockState(pos).isOf(ModBlocks.METEOR_BEACON)) {
						return pos;
					}
				}
			}
		}
		return null;
	}

	public static MutableText prefix() {
		return Text.literal("[☄ Метеориты] ").formatted(Formatting.GOLD);
	}

	public static Meteor nearest(ServerPlayerEntity player) {
		String world = player.getWorld().getRegistryKey().getValue().toString();
		Meteor best = null;
		double bestDist = Double.MAX_VALUE;
		for (Meteor meteor : ACTIVE) {
			if (!meteor.worldName.equals(world)) {
				continue;
			}
			double dist = meteor.pos.getSquaredDistance(player.getPos());
			if (dist < bestDist) {
				bestDist = dist;
				best = meteor;
			}
		}
		return best;
	}

	private static void broadcast(ServerWorld world, Text text) {
		MutableText prefix = prefix();
		world.getServer().getPlayerManager().broadcast(prefix.append(text), false);
	}

	// ------------------------------------------------------------------ loot
	private static void fillChest(ServerWorld world, ChestBlockEntity chest, Rarity rarity) {
		List<ItemStack> loot = new ArrayList<>();

		switch (rarity) {
			case COMMON -> {
				loot.add(new ItemStack(ModItems.METEOR_SHARD, 2 + RANDOM.nextInt(4)));
				loot.add(new ItemStack(ModItems.COSMIC_DUST, 1 + RANDOM.nextInt(3)));
				loot.add(new ItemStack(Items.IRON_INGOT, 3 + RANDOM.nextInt(6)));
				loot.add(new ItemStack(Items.COAL, 4 + RANDOM.nextInt(8)));
				loot.add(new ItemStack(Items.GOLD_INGOT, 1 + RANDOM.nextInt(4)));
				if (RANDOM.nextFloat() < 0.25F) {
					loot.add(enchant(world, new ItemStack(Items.IRON_PICKAXE), 1, 3));
				}
			}
			case RARE -> {
				loot.add(new ItemStack(ModItems.METEOR_SHARD, 4 + RANDOM.nextInt(6)));
				loot.add(new ItemStack(ModItems.METEOR_INGOT, 1 + RANDOM.nextInt(2)));
				loot.add(new ItemStack(Items.DIAMOND, 2 + RANDOM.nextInt(4)));
				loot.add(new ItemStack(Items.ENDER_PEARL, 2 + RANDOM.nextInt(4)));
				loot.add(new ItemStack(Items.GOLDEN_APPLE, 1 + RANDOM.nextInt(2)));
				loot.add(enchant(world, new ItemStack(Items.DIAMOND_SWORD), 2, 4));
				if (RANDOM.nextFloat() < 0.3F) {
					loot.add(new ItemStack(ModItems.METEOR_LOCATOR));
				}
			}
			case EPIC -> {
				loot.add(new ItemStack(ModItems.METEOR_INGOT, 3 + RANDOM.nextInt(4)));
				loot.add(new ItemStack(ModItems.METEOR_SHARD, 6 + RANDOM.nextInt(8)));
				loot.add(new ItemStack(Items.DIAMOND_BLOCK, 1 + RANDOM.nextInt(2)));
				loot.add(new ItemStack(Items.NETHERITE_SCRAP, 2 + RANDOM.nextInt(3)));
				loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
				loot.add(pickArmor(world));
				if (RANDOM.nextFloat() < 0.5F) {
					loot.add(new ItemStack(ModItems.METEOR_PICKAXE));
				}
				if (RANDOM.nextFloat() < 0.35F) {
					loot.add(new ItemStack(ModItems.METEOR_SWORD));
				}
			}
			case LEGENDARY -> {
				loot.add(new ItemStack(ModItems.METEOR_CORE, 1 + RANDOM.nextInt(2)));
				loot.add(new ItemStack(ModItems.METEOR_INGOT, 6 + RANDOM.nextInt(6)));
				loot.add(new ItemStack(ModItems.METEOR_SWORD));
				loot.add(new ItemStack(ModItems.METEOR_PICKAXE));
				loot.add(new ItemStack(ModItems.METEOR_HELMET));
				loot.add(new ItemStack(ModItems.METEOR_CHESTPLATE));
				loot.add(new ItemStack(ModItems.METEOR_LEGGINGS));
				loot.add(new ItemStack(ModItems.METEOR_BOOTS));
				loot.add(new ItemStack(Items.NETHERITE_BLOCK));
				loot.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 3));
				loot.add(new ItemStack(ModBlocks.METEOR_BEACON));
				loot.add(new ItemStack(Items.TOTEM_OF_UNDYING));
			}
		}

		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < 27; i++) {
			slots.add(i);
		}
		java.util.Collections.shuffle(slots, RANDOM);
		for (int i = 0; i < loot.size() && i < slots.size(); i++) {
			chest.setStack(slots.get(i), loot.get(i));
		}
		chest.markDirty();
	}

	private static ItemStack pickArmor(ServerWorld world) {
		ItemStack[] pool = {
				new ItemStack(ModItems.METEOR_HELMET),
				new ItemStack(ModItems.METEOR_CHESTPLATE),
				new ItemStack(ModItems.METEOR_LEGGINGS),
				new ItemStack(ModItems.METEOR_BOOTS)
		};
		return pool[RANDOM.nextInt(pool.length)];
	}

	/** Adds 1-2 random vanilla enchantments so loot may be enchanted or plain. */
	private static ItemStack enchant(ServerWorld world, ItemStack stack, int minLevel, int maxLevel) {
		RegistryKey<Enchantment>[] pool = new RegistryKey[] {
				Enchantments.SHARPNESS, Enchantments.EFFICIENCY, Enchantments.UNBREAKING,
				Enchantments.FORTUNE, Enchantments.PROTECTION, Enchantments.FIRE_ASPECT,
				Enchantments.MENDING, Enchantments.LOOTING
		};
		try {
			Registry<Enchantment> registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
			int count = 1 + RANDOM.nextInt(2);
			for (int i = 0; i < count; i++) {
				RegistryEntry<Enchantment> entry = registry.getOrThrow(pool[RANDOM.nextInt(pool.length)]);
				stack.addEnchantment(entry, minLevel + RANDOM.nextInt(Math.max(1, maxLevel - minLevel + 1)));
			}
		} catch (Exception exception) {
			MeteoriteCraft.LOGGER.warn("Could not enchant loot: {}", exception.toString());
		}
		return stack;
	}
}
