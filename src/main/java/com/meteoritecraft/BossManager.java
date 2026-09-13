package com.meteoritecraft;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bosses are built on top of vanilla mobs (no custom renderer required, so they
 * always show up correctly), but they get their own health pool, attributes,
 * gear, boss bar, phases and special attacks driven from the server tick.
 */
public final class BossManager {

	private static final Random RANDOM = new Random();
	private static final List<Boss> BOSSES = new ArrayList<>();

	public enum BossType {
		STAR_WARDEN("Звёздный Страж", Formatting.AQUA, BossBar.Color.BLUE, 320.0, 18.0, 12.0, 1.8F, 6),
		METEOR_TITAN("Метеоритный Титан", Formatting.GOLD, BossBar.Color.YELLOW, 520.0, 24.0, 16.0, 2.2F, 8),
		VOID_HERALD("Вестник Пустоты", Formatting.DARK_PURPLE, BossBar.Color.PURPLE, 380.0, 20.0, 8.0, 2.0F, 7),
		FALLEN_STAR("Павшая Звезда", Formatting.RED, BossBar.Color.RED, 450.0, 22.0, 14.0, 2.4F, 9);

		public final String displayName;
		public final Formatting color;
		public final BossBar.Color barColor;
		public final double health;
		public final double damage;
		public final double armor;
		public final float scale;
		public final int lootRolls;

		BossType(String displayName, Formatting color, BossBar.Color barColor, double health, double damage,
				double armor, float scale, int lootRolls) {
			this.displayName = displayName;
			this.color = color;
			this.barColor = barColor;
			this.health = health;
			this.damage = damage;
			this.armor = armor;
			this.scale = scale;
			this.lootRolls = lootRolls;
		}

		public EntityType<? extends MobEntity> vanillaType() {
			return switch (this) {
				case STAR_WARDEN -> EntityType.WITHER_SKELETON;
				case METEOR_TITAN -> EntityType.RAVAGER;
				case VOID_HERALD -> EntityType.ENDERMAN;
				case FALLEN_STAR -> EntityType.BLAZE;
			};
		}
	}

	public static final class Boss {
		public final BossType type;
		public final MobEntity entity;
		public final ServerBossBar bar;
		public int age;
		public int phase = 1;
		public boolean rewarded;

		Boss(BossType type, MobEntity entity, ServerBossBar bar) {
			this.type = type;
			this.entity = entity;
			this.bar = bar;
		}
	}

	// ------------------------------------------------------------------ spawn
	public static Boss spawn(ServerWorld world, BlockPos requested, BossType type, boolean announce) {
		BlockPos ground = MeteorManager.findGround(world, requested.getX(), requested.getZ());
		BlockPos pos = ground != null ? ground.up() : requested.up();

		MobEntity mob = (MobEntity) type.vanillaType().create(world, SpawnReason.EVENT);
		if (mob == null) {
			return null;
		}

		mob.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, RANDOM.nextFloat() * 360.0F,
				0.0F);
		mob.setCustomName(Text.literal("☄ " + type.displayName).formatted(type.color));
		mob.setCustomNameVisible(true);
		mob.setPersistent();

		setAttribute(mob, EntityAttributes.MAX_HEALTH, type.health);
		setAttribute(mob, EntityAttributes.ATTACK_DAMAGE, type.damage);
		setAttribute(mob, EntityAttributes.ARMOR, type.armor);
		setAttribute(mob, EntityAttributes.ARMOR_TOUGHNESS, 12.0);
		setAttribute(mob, EntityAttributes.KNOCKBACK_RESISTANCE, 1.0);
		setAttribute(mob, EntityAttributes.FOLLOW_RANGE, 64.0);
		setAttribute(mob, EntityAttributes.MOVEMENT_SPEED, 0.32);
		setAttribute(mob, EntityAttributes.SCALE, type.scale);
		mob.setHealth((float) type.health);

		// Boss gear (also makes them visually distinct).
		if (type == BossType.STAR_WARDEN) {
			mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.METEOR_SWORD));
			mob.equipStack(EquipmentSlot.HEAD, new ItemStack(ModItems.METEOR_HELMET));
			mob.equipStack(EquipmentSlot.CHEST, new ItemStack(ModItems.METEOR_CHESTPLATE));
		} else if (type == BossType.METEOR_TITAN) {
			mob.equipStack(EquipmentSlot.HEAD, new ItemStack(ModItems.METEOR_HELMET));
		} else if (type == BossType.VOID_HERALD) {
			mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.METEOR_AXE));
		}
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			mob.setEquipmentDropChance(slot, 0.0F);
		}

		mob.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false,
				false, false));

		world.spawnEntity(mob);

		ServerBossBar bar = new ServerBossBar(
				Text.literal("☄ " + type.displayName).formatted(type.color), type.barColor,
				BossBar.Style.NOTCHED_10);
		bar.setDarkenSky(type == BossType.VOID_HERALD || type == BossType.FALLEN_STAR);
		Boss boss = new Boss(type, mob, bar);
		BOSSES.add(boss);

		world.playSound(null, pos, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 4.0F, 0.6F);
		world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
				160, 2.0, 2.0, 2.0, 0.2);

		if (announce) {
			for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
				player.sendMessage(MeteorManager.prefix()
						.append(Text.literal(type.displayName + " пробудился! ").formatted(type.color))
						.append(MeteorManager.coordsText(pos)), false);
			}
		}
		return boss;
	}

	private static void setAttribute(LivingEntity entity,
			net.minecraft.registry.entry.RegistryEntry<net.minecraft.entity.attribute.EntityAttribute> attribute,
			double value) {
		EntityAttributeInstance instance = entity.getAttributeInstance(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}

	// ------------------------------------------------------------------- tick
	public static void tick(MinecraftServer server) {
		if (BOSSES.isEmpty()) {
			return;
		}

		List<Boss> finished = new ArrayList<>();
		for (Boss boss : BOSSES) {
			boss.age++;
			MobEntity mob = boss.entity;

			if (mob.isRemoved() || !mob.isAlive()) {
				if (!boss.rewarded) {
					boss.rewarded = true;
					dropLoot(boss);
				}
				boss.bar.clearPlayers();
				boss.bar.setVisible(false);
				finished.add(boss);
				continue;
			}

			// Boss bar: show it to everyone within 80 blocks.
			boss.bar.setPercent(Math.max(0.0F, mob.getHealth() / mob.getMaxHealth()));
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				boolean near = player.getWorld() == mob.getWorld()
						&& player.squaredDistanceTo(mob) < 80 * 80;
				if (near) {
					boss.bar.addPlayer(player);
				} else {
					boss.bar.removePlayer(player);
				}
			}

			// Phases: the lower the health, the more aggressive it gets.
			float ratio = mob.getHealth() / mob.getMaxHealth();
			int phase = ratio > 0.66F ? 1 : ratio > 0.33F ? 2 : 3;
			if (phase != boss.phase) {
				boss.phase = phase;
				announce(boss, "переходит в фазу " + phase + "!");
				mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 60 * 10, phase - 1,
						false, true, true));
				mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60 * 10, phase - 1, false,
						true, true));
			}

			// Regeneration out of combat so the fight can't be cheesed forever.
			if (boss.age % 40 == 0 && mob.getAttacker() == null && mob.getHealth() < mob.getMaxHealth()) {
				mob.heal(2.0F);
			}

			int interval = 200 - boss.phase * 40; // faster attacks in later phases
			if (boss.age % interval == 0) {
				ability(boss);
			}
		}
		BOSSES.removeAll(finished);
	}

	// --------------------------------------------------------------- abilities
	private static void ability(Boss boss) {
		MobEntity mob = boss.entity;
		if (!(mob.getWorld() instanceof ServerWorld world)) {
			return;
		}
		ServerPlayerEntity target = nearestPlayer(world, mob, 48);
		if (target == null) {
			return;
		}

		int roll = RANDOM.nextInt(boss.phase >= 2 ? 5 : 4);
		switch (roll) {
			case 0 -> { // meteor strike on the target
				announce(boss, "обрушивает метеорит!");
				MeteorManager.spawnAt(world, target.getBlockPos(), MeteorManager.Rarity.RARE, false);
			}
			case 1 -> { // summon minions
				announce(boss, "зовёт слуг!");
				for (int i = 0; i < 2 + boss.phase; i++) {
					MobEntity minion = (MobEntity) EntityType.WITHER_SKELETON.create(world, SpawnReason.EVENT);
					if (minion != null) {
						minion.refreshPositionAndAngles(mob.getX() + RANDOM.nextInt(7) - 3, mob.getY(),
								mob.getZ() + RANDOM.nextInt(7) - 3, 0.0F, 0.0F);
						minion.setCustomName(Text.literal("Слуга " + boss.type.displayName)
								.formatted(boss.type.color));
						setAttribute(minion, EntityAttributes.MAX_HEALTH, 40.0);
						minion.setHealth(40.0F);
						minion.setTarget(target);
						world.spawnEntity(minion);
					}
				}
			}
			case 2 -> { // blink behind the target
				Vec3d back = target.getPos().subtract(target.getRotationVec(1.0F).multiply(2.5));
				mob.teleport(world, back.x, back.y, back.z, java.util.Set.of(), mob.getYaw(), mob.getPitch(),
						true);
				world.playSound(null, mob.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
						SoundCategory.HOSTILE, 2.0F, 0.7F);
			}
			case 3 -> { // gravity slam: pull and levitate everyone near
				announce(boss, "искажает гравитацию!");
				for (ServerPlayerEntity player : world.getPlayers(
						p -> p.squaredDistanceTo(mob) < 20 * 20)) {
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 60, 1));
					Vec3d pull = mob.getPos().subtract(player.getPos()).normalize().multiply(0.8);
					player.addVelocity(pull.x, 0.4, pull.z);
					player.velocityModified = true;
				}
				world.spawnParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 120,
						3.0, 2.0, 3.0, 0.4);
			}
			default -> { // phase 2+: burning nova
				announce(boss, "выпускает звёздную новую!");
				world.createExplosion(mob, mob.getX(), mob.getY() + 1, mob.getZ(), 3.5F,
						World.ExplosionSourceType.NONE);
				for (ServerPlayerEntity player : world.getPlayers(
						p -> p.squaredDistanceTo(mob) < 12 * 12)) {
					player.setOnFireFor(6);
				}
			}
		}
	}

	private static ServerPlayerEntity nearestPlayer(ServerWorld world, MobEntity mob, double radius) {
		ServerPlayerEntity best = null;
		double bestDistance = radius * radius;
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (player.isSpectator() || player.isCreative()) {
				continue;
			}
			double distance = player.squaredDistanceTo(mob);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = player;
			}
		}
		return best;
	}

	// ------------------------------------------------------------------- loot
	private static void dropLoot(Boss boss) {
		if (!(boss.entity.getWorld() instanceof ServerWorld world)) {
			return;
		}
		BlockPos pos = boss.entity.getBlockPos();

		List<Item> pool = List.of(ModItems.METEOR_INGOT, ModItems.METEOR_CORE, ModItems.COSMIC_DUST,
				ModItems.METEOR_SHARD, ModExtraItems.STARFALL_ORB, ModExtraItems.GRAVITY_CUBE,
				ModExtraItems.COSMIC_ELIXIR, ModExtraItems.VOID_CRYSTAL, ModItems.METEOR_STAFF,
				ModItems.METEOR_LOCATOR);

		drop(world, pos, new ItemStack(ModItems.METEOR_CORE, 2 + RANDOM.nextInt(3)));
		drop(world, pos, new ItemStack(ModExtraItems.BOSS_HEART));
		drop(world, pos, new ItemStack(Items.NETHER_STAR, 1 + RANDOM.nextInt(2)));
		for (int i = 0; i < boss.type.lootRolls; i++) {
			drop(world, pos, new ItemStack(pool.get(RANDOM.nextInt(pool.size())), 1 + RANDOM.nextInt(3)));
		}
		// Legendary gear from the toughest bosses.
		if (boss.type == BossType.METEOR_TITAN || boss.type == BossType.FALLEN_STAR) {
			drop(world, pos, new ItemStack(ModItems.METEOR_CHESTPLATE));
			drop(world, pos, new ItemStack(ModItems.METEOR_SWORD));
		}

		world.createExplosion(null, pos.getX(), pos.getY() + 1, pos.getZ(), 2.0F,
				World.ExplosionSourceType.NONE);
		world.playSound(null, pos, SoundEvents.ENTITY_ENDER_DRAGON_DEATH, SoundCategory.HOSTILE, 3.0F, 1.2F);

		for (ServerPlayerEntity player : world.getServer().getPlayerManager().getPlayerList()) {
			player.sendMessage(MeteorManager.prefix()
					.append(Text.literal(boss.type.displayName + " побеждён! ").formatted(boss.type.color))
					.append(MeteorManager.coordsText(pos)), false);
		}
	}

	private static void drop(ServerWorld world, BlockPos pos, ItemStack stack) {
		net.minecraft.block.Block.dropStack(world, pos.up(), stack);
	}

	private static void announce(Boss boss, String message) {
		if (!(boss.entity.getWorld() instanceof ServerWorld world)) {
			return;
		}
		Box box = boss.entity.getBoundingBox().expand(48.0);
		for (ServerPlayerEntity player : world.getPlayers(p -> box.contains(p.getPos()))) {
			player.sendMessage(Text.literal(boss.type.displayName + " " + message)
					.formatted(boss.type.color), true);
		}
	}

	public static List<Boss> active() {
		return BOSSES;
	}

	public static BossType randomType() {
		BossType[] values = BossType.values();
		return values[RANDOM.nextInt(values.length)];
	}

	private static ExplosionBehavior unused() {
		return null;
	}

	private BossManager() {
	}
}
