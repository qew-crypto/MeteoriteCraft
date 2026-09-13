package com.meteoritecraft;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class TargetHelper {
	/** Block the player is looking at, or the point in front of them if nothing is hit. */
	public static BlockPos lookingAt(PlayerEntity player, double distance) {
		HitResult hit = player.raycast(distance, 1.0F, false);
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			return blockHit.getBlockPos();
		}
		net.minecraft.util.math.Vec3d direction = player.getRotationVec(1.0F);
		net.minecraft.util.math.Vec3d end = player.getEyePos().add(direction.multiply(distance));
		return BlockPos.ofFloored(end);
	}

	private TargetHelper() {
	}
}
