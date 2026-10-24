package dev.luminous.api.utils.math;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.combat.CombatUtil;
import dev.luminous.core.impl.PlayerManager;
import dev.luminous.mod.modules.impl.client.ClientSetting;
import dev.luminous.mod.modules.impl.combat.Criticals;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import static dev.luminous.api.utils.Wrapper.mc;

public class DamageUtils {
    // Explosion damage

    public static float calculateDamage(BlockPos pos, LivingEntity entity) {
        return DamageUtils.explosionDamage(entity, null, new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5), 12);
    }

    public static float calculateDamage(Vec3d pos, LivingEntity entity) {
        return DamageUtils.explosionDamage(entity, null, pos, 12);
    }

    public static final RaycastFactory HIT_FACTORY = (context, blockPos) -> {
        BlockState blockState = mc.world.getBlockState(blockPos);
        if (blockState.getBlock().getBlastResistance() < 600) return null;

        return blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
    };

    public static float explosionDamage(LivingEntity target, Vec3d targetPos, Box targetBox, Vec3d explosionPos, float power, RaycastFactory raycastFactory) {
        double modDistance = distance(targetPos.x, targetPos.y, targetPos.z, explosionPos.x, explosionPos.y, explosionPos.z);
        if (modDistance > power) return 0f;

        double exposure = getExposure(explosionPos, targetBox, raycastFactory);
        double impact = (1 - (modDistance / power)) * exposure;
        float damage = (int) ((impact * impact + impact) / 2 * 7 * 12 + 1);

        return calculateReductionsExplosion(damage, target, mc.world.getDamageSources().explosion(null));
    }

    public static float anchorDamage(LivingEntity target, LivingEntity predict, Vec3d anchor) {
        return overridingExplosionDamage(target, predict, anchor, 10f, BlockPos.ofFloored(anchor), Blocks.AIR.getDefaultState());
    }

    public static float overridingExplosionDamage(LivingEntity target, LivingEntity predict, Vec3d explosionPos, float power, BlockPos overridePos, BlockState overrideState) {
        return explosionDamage(target, predict, explosionPos, power, getOverridingHitFactory(overridePos, overrideState));
    }

    private static float explosionDamage(LivingEntity target, LivingEntity predict, Vec3d explosionPos, float power, RaycastFactory raycastFactory) {
        if (target == null) return 0f;
        if (target instanceof PlayerEntity player && getGameMode(player) == GameMode.CREATIVE) return 0f;

        return explosionDamage(target, predict != null ? predict.getEntityPos() : target.getEntityPos(), predict != null ? predict.getBoundingBox() : target.getBoundingBox(), explosionPos, power, raycastFactory);
    }

    public static float explosionDamage(LivingEntity target, LivingEntity predict, Vec3d explosionPos, float power) {
        if (target == null) return 0f;
        if (target instanceof PlayerEntity player && getGameMode(player) == GameMode.CREATIVE) return 0f;

        return explosionDamage(target, predict != null ? predict.getEntityPos() : target.getEntityPos(), predict != null ? predict.getBoundingBox() : target.getBoundingBox(), explosionPos, power, HIT_FACTORY);
    }

    public static RaycastFactory getOverridingHitFactory(BlockPos overridePos, BlockState overrideState) {
        return (context, blockPos) -> {
            BlockState blockState;
            if (blockPos.equals(overridePos)) blockState = overrideState;
            else {
                blockState = mc.world.getBlockState(blockPos);
                if (blockState.getBlock().getBlastResistance() < 600) return null;
            }

            return blockState.getCollisionShape(mc.world, blockPos).raycast(context.start(), context.end(), blockPos);
        };
    }

    // Sword damage

    public static float getAttackDamage(LivingEntity attacker, LivingEntity target) {
        float itemDamage = (float) attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        DamageSource damageSource = attacker instanceof PlayerEntity player ? mc.world.getDamageSources().playerAttack(player) : mc.world.getDamageSources().mobAttack(attacker);

        StatusEffectInstance effect = attacker.getStatusEffect(StatusEffects.STRENGTH);
        if (effect != null) {
            itemDamage += 3f * (effect.getAmplifier() + 1);
        }
        float damage = modifyAttackDamage(attacker, target, attacker.getWeaponStack(), damageSource, itemDamage);
        return calculateReductions(damage, target, damageSource);
    }

    public static float getAttackDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon) {
        EntityAttributeInstance original = attacker.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        EntityAttributeInstance copy = new EntityAttributeInstance(EntityAttributes.ATTACK_DAMAGE, o -> {
        });

        copy.setBaseValue(original.getBaseValue());
        for (EntityAttributeModifier modifier : original.getModifiers()) {
            copy.addTemporaryModifier(modifier);
        }
        copy.removeModifier(Item.BASE_ATTACK_DAMAGE_MODIFIER_ID);

        AttributeModifiersComponent attributeModifiers = weapon.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributeModifiers != null) {
            attributeModifiers.applyModifiers(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                if (entry == EntityAttributes.ATTACK_DAMAGE) copy.updateModifier(modifier);
            });
        }

        float itemDamage = (float) copy.getValue();
        DamageSource damageSource = attacker instanceof PlayerEntity player ? mc.world.getDamageSources().playerAttack(player) : mc.world.getDamageSources().mobAttack(attacker);

        float damage = modifyAttackDamage(attacker, target, weapon, damageSource, itemDamage);
        return calculateReductions(damage, target, damageSource);
    }

    private static float modifyAttackDamage(LivingEntity attacker, LivingEntity target, ItemStack weapon, DamageSource damageSource, float damage) {
        // Get enchant damage
        Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
        getEnchantments(weapon, enchantments);
        float enchantDamage = 0f;

        int sharpness = getEnchantmentLevel(enchantments, Enchantments.SHARPNESS);
        if (sharpness > 0) {
            enchantDamage += 1 + 0.5f * (sharpness - 1);
        }

        int baneOfArthropods = getEnchantmentLevel(enchantments, Enchantments.BANE_OF_ARTHROPODS);
        if (baneOfArthropods > 0 && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_BANE_OF_ARTHROPODS)) {
            enchantDamage += 2.5f * baneOfArthropods;
        }

        int impaling = getEnchantmentLevel(enchantments, Enchantments.IMPALING);
        if (impaling > 0 && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_IMPALING)) {
            enchantDamage += 2.5f * impaling;
        }

        int smite = getEnchantmentLevel(enchantments, Enchantments.SMITE);
        if (smite > 0 && target.getType().isIn(EntityTypeTags.SENSITIVE_TO_SMITE)) {
            enchantDamage += 2.5f * smite;
        }

        // Factor charge
        if (attacker instanceof PlayerEntity playerEntity) {
            float charge = playerEntity.getAttackCooldownProgress(0.5f);
            damage *= 0.2f + charge * charge * 0.8f;
            enchantDamage *= charge;

            if (weapon.getItem() instanceof MaceItem item) {
                float bonusDamage = item.getBonusAttackDamage(target, damage, damageSource);
                if (bonusDamage > 0f) {
                    int density = getEnchantmentLevel(weapon, Enchantments.DENSITY);
                    if (density > 0) bonusDamage += 0.5f * attacker.fallDistance;
                    damage += bonusDamage;
                }
            }

            // Factor critical hit
            if (charge > 0.9f
                    && (attacker.fallDistance > 0f || attacker == mc.player && Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground) && (mc.player.isOnGround() || !Criticals.INSTANCE.onlyGround.getValue()))
                    && (!attacker.isOnGround() || attacker == mc.player && Criticals.INSTANCE.isOn() && !Criticals.INSTANCE.mode.is(Criticals.Mode.Ground))
                    && !attacker.isClimbing()
                    && !attacker.isTouchingWater()
                    && !attacker.hasStatusEffect(StatusEffects.BLINDNESS)
                    && !attacker.hasVehicle()) {
                damage *= 1.5f;
            }
        }

        return damage + enchantDamage;
    }

    // Fall Damage

    public static float fallDamage(LivingEntity entity) {
        if (entity instanceof PlayerEntity player && player.getAbilities().flying) return 0f;
        if (entity.hasStatusEffect(StatusEffects.SLOW_FALLING) || entity.hasStatusEffect(StatusEffects.LEVITATION))
            return 0f;

        // Fast path - Above the surface
        int surface = mc.world.getWorldChunk(entity.getBlockPos()).getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(entity.getBlockX() & 15, entity.getBlockZ() & 15);
        if (entity.getBlockY() >= surface) return fallDamageReductions(entity, surface);

        // Under the surface
        BlockHitResult raycastResult = mc.world.raycast(new RaycastContext(entity.getEntityPos(), new Vec3d(entity.getX(), mc.world.getBottomY(), entity.getZ()), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.WATER, entity));
        if (raycastResult.getType() == HitResult.Type.MISS) return 0;

        return fallDamageReductions(entity, raycastResult.getBlockPos().getY());
    }

    private static float fallDamageReductions(LivingEntity entity, int surface) {
        int fallHeight = (int) (entity.getY() - surface + entity.fallDistance - 3d);
        @Nullable StatusEffectInstance jumpBoostInstance = entity.getStatusEffect(StatusEffects.JUMP_BOOST);
        if (jumpBoostInstance != null) fallHeight -= jumpBoostInstance.getAmplifier() + 1;

        return calculateReductions(fallHeight, entity, mc.world.getDamageSources().fall());
    }

    // Utils

    public static float calculateReductionsExplosion(float damage, LivingEntity entity, DamageSource damageSource) {
        if (damageSource.isScaledWithDifficulty()) {
            switch (mc.world.getDifficulty()) {
                case EASY -> damage = Math.min(damage / 2 + 1, damage);
                case HARD -> damage *= 1.5f;
            }
        }

        // Armor reduction
        damage = DamageUtil.getDamageLeft(entity, damage, damageSource, getArmor(entity), (float) getARMOR_TOUGHNESS(entity));

        // Resistance reduction
        damage = resistanceReduction(entity, damage);

        // Protection reduction
        damage = DamageUtil.getInflictedDamage((float)damage, (float)DamageUtils.getProtectionAmount(List.of(entity.getEquippedStack(EquipmentSlot.HEAD), entity.getEquippedStack(EquipmentSlot.CHEST), entity.getEquippedStack(EquipmentSlot.LEGS), entity.getEquippedStack(EquipmentSlot.FEET))));

        return Math.max(damage, 0);
    }

    public static float calculateReductions(float damage, LivingEntity entity, DamageSource damageSource) {
        if (damageSource.isScaledWithDifficulty()) {
            switch (mc.world.getDifficulty()) {
                case EASY -> damage = Math.min(damage / 2 + 1, damage);
                case HARD -> damage *= 1.5f;
            }
        }

        // Armor reduction
        damage = DamageUtil.getDamageLeft(entity, damage, damageSource, getArmor(entity), (float) getARMOR_TOUGHNESS(entity));

        // Resistance reduction
        damage = resistanceReduction(entity, damage);

        // Protection reduction
        damage = protectionReduction(entity, damage, damageSource);

        return Math.max(damage, 0);
    }

    public static double getARMOR_TOUGHNESS(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            PlayerManager.EntityAttribute entityAttribute = Alien.PLAYER.map.get(player);
            if (entityAttribute != null)
                return entityAttribute.toughness();
        }
        return entity.getAttributeValue(EntityAttributes.ARMOR_TOUGHNESS);
    }

    private static float getArmor(LivingEntity entity) {
        if (entity instanceof PlayerEntity player) {
            PlayerManager.EntityAttribute entityAttribute = Alien.PLAYER.map.get(player);
            if (entityAttribute != null)
                return entityAttribute.armor();
        }
        return (float) Math.floor(entity.getAttributeValue(EntityAttributes.ARMOR));
    }

    private static float protectionReduction(LivingEntity player, float damage, DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return damage;

        int damageProtection = 0;

        for (ItemStack stack : List.of(player.getEquippedStack(EquipmentSlot.HEAD), player.getEquippedStack(EquipmentSlot.CHEST), player.getEquippedStack(EquipmentSlot.LEGS), player.getEquippedStack(EquipmentSlot.FEET))) {
            Object2IntMap<RegistryEntry<Enchantment>> enchantments = new Object2IntOpenHashMap<>();
            getEnchantments(stack, enchantments);

            int protection = getEnchantmentLevel(enchantments, Enchantments.PROTECTION);
            if (protection > 0) {
                damageProtection += protection;
            }

            int fireProtection = getEnchantmentLevel(enchantments, Enchantments.FIRE_PROTECTION);
            if (fireProtection > 0 && source.isIn(DamageTypeTags.IS_FIRE)) {
                damageProtection += 2 * fireProtection;
            }

            int blastProtection = getEnchantmentLevel(enchantments, Enchantments.BLAST_PROTECTION);
            if (blastProtection > 0 && source.isIn(DamageTypeTags.IS_EXPLOSION)) {
                damageProtection += 2 * blastProtection;
            }

            int projectileProtection = getEnchantmentLevel(enchantments, Enchantments.PROJECTILE_PROTECTION);
            if (projectileProtection > 0 && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
                damageProtection += 2 * projectileProtection;
            }

            int featherFalling = getEnchantmentLevel(enchantments, Enchantments.FEATHER_FALLING);
            if (featherFalling > 0 && source.isIn(DamageTypeTags.IS_FALL)) {
                damageProtection += 3 * featherFalling;
            }
        }

        return DamageUtil.getInflictedDamage(damage, damageProtection);
    }

    public static int getProtectionAmount(Iterable<ItemStack> equipment) {
        MutableInt mutableInt = new MutableInt();
        equipment.forEach(i -> mutableInt.add(getProtectionAmount(i)));
        return mutableInt.intValue();
    }

    public static int getProtectionAmount(ItemStack stack) {
        int modifierBlast = EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry) Wrapper.mc.world.getRegistryManager().getOrThrow(Enchantments.BLAST_PROTECTION.getRegistryRef()).getOrThrow(Enchantments.BLAST_PROTECTION)), (ItemStack)stack);
        int modifier = EnchantmentHelper.getLevel((RegistryEntry)((RegistryEntry)Wrapper.mc.world.getRegistryManager().getOrThrow(Enchantments.PROTECTION.getRegistryRef()).getOrThrow(Enchantments.PROTECTION)), (ItemStack)stack);
        return modifierBlast * 2 + modifier;
    }

    private static float resistanceReduction(LivingEntity player, float damage) {
        StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
        if (resistance != null) {
            int lvl = resistance.getAmplifier() + 1;
            damage *= (1 - (lvl * 0.2f));
        }

        return Math.max(damage, 0);
    }

    private static float getExposure(Vec3d source, Box box, RaycastFactory raycastFactory) {
        if (ClientSetting.INSTANCE.optimizedCalc.getValue()) {
            int miss = 0;
            int hit = 0;

            for (int k = 0; k <= 1; k += 1) {
                for (int l = 0; l <= 1; l += 1) {
                    for (int m = 0; m <= 1; m += 1) {
                        double n = MathHelper.lerp(k, box.minX, box.maxX);
                        double o = MathHelper.lerp(l, box.minY, box.maxY);
                        double p = MathHelper.lerp(m, box.minZ, box.maxZ);
                        Vec3d vec3d = new Vec3d(n, o, p);
                        if (raycast(vec3d, source, CombatUtil.terrainIgnore) == HitResult.Type.MISS)
                            ++miss;
                        ++hit;
                    }
                }
            }
            return (float) miss / (float) hit;
        }
        double xDiff = box.maxX - box.minX;
        double yDiff = box.maxY - box.minY;
        double zDiff = box.maxZ - box.minZ;

        double xStep = 1 / (xDiff * 2 + 1);
        double yStep = 1 / (yDiff * 2 + 1);
        double zStep = 1 / (zDiff * 2 + 1);

        if (xStep > 0 && yStep > 0 && zStep > 0) {
            int misses = 0;
            int hits = 0;

            double xOffset = (1 - Math.floor(1 / xStep) * xStep) * 0.5;
            double zOffset = (1 - Math.floor(1 / zStep) * zStep) * 0.5;

            xStep = xStep * xDiff;
            yStep = yStep * yDiff;
            zStep = zStep * zDiff;

            double startX = box.minX + xOffset;
            double startY = box.minY;
            double startZ = box.minZ + zOffset;
            double endX = box.maxX + xOffset;
            double endY = box.maxY;
            double endZ = box.maxZ + zOffset;

            for (double x = startX; x <= endX; x += xStep) {
                for (double y = startY; y <= endY; y += yStep) {
                    for (double z = startZ; z <= endZ; z += zStep) {
                        Vec3d position = new Vec3d(x, y, z);

                        if (raycast(new ExposureRaycastContext(position, source), raycastFactory) == null) misses++;

                        hits++;
                    }
                }
            }

            return (float) misses / hits;
        }

        return 0f;
    }

    /* Raycasts */

    public static HitResult.Type raycast(Vec3d start, Vec3d end, boolean ignoreTerrain) {
        return BlockView.raycast(start, end, null, (innerContext, blockPos) -> {
            BlockState blockState = mc.world.getBlockState(blockPos);
            if (blockState.getBlock().getBlastResistance() < 600 && ignoreTerrain) return null;
            BlockHitResult hitResult = blockState.getCollisionShape(mc.world, blockPos).raycast(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, (innerContext) -> HitResult.Type.MISS);
    }

    public static BlockHitResult raycast(ExposureRaycastContext context, RaycastFactory raycastFactory) {
        return BlockView.raycast(context.start(), context.end(), context, raycastFactory, ctx -> null);
    }

    public record ExposureRaycastContext(Vec3d start, Vec3d end) {
    }

    @FunctionalInterface
    public interface RaycastFactory extends BiFunction<ExposureRaycastContext, BlockPos, BlockHitResult> {
    }

    public static int getEnchantmentLevel(ItemStack itemStack, RegistryKey<Enchantment> enchantment) {
        if (itemStack.isEmpty()) return 0;
        Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);
        return getEnchantmentLevel(itemEnchantments, enchantment);
    }

    public static int getEnchantmentLevel(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantment) {
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : Object2IntMaps.fastIterable(itemEnchantments)) {
            if (entry.getKey().matchesKey(enchantment)) return entry.getIntValue();
        }
        return 0;
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(squaredDistance(x1, y1, z1, x2, y2, z2));
    }

    public static GameMode getGameMode(PlayerEntity player) {
        if (player == null) return null;
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(player.getUuid());
        if (playerListEntry == null) return null;
        return playerListEntry.getGameMode();
    }

    public static double squaredDistanceTo(Entity entity) {
        return squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
    }

    public static double squaredDistanceTo(BlockPos blockPos) {
        return squaredDistanceTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public static double squaredDistanceTo(double x, double y, double z) {
        return squaredDistance(mc.player.getX(), mc.player.getY(), mc.player.getZ(), x, y, z);
    }

    public static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double f = x1 - x2;
        double g = y1 - y2;
        double h = z1 - z2;
        return org.joml.Math.fma(f, f, org.joml.Math.fma(g, g, h * h));
    }

    public static void getEnchantments(ItemStack itemStack, Object2IntMap<RegistryEntry<Enchantment>> enchantments) {
        enchantments.clear();

        if (!itemStack.isEmpty()) {
            Set<Object2IntMap.Entry<RegistryEntry<Enchantment>>> itemEnchantments = itemStack.getItem() == Items.ENCHANTED_BOOK
                    ? itemStack.get(DataComponentTypes.STORED_ENCHANTMENTS).getEnchantmentEntries()
                    : itemStack.getEnchantments().getEnchantmentEntries();

            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : itemEnchantments) {
                enchantments.put(entry.getKey(), entry.getIntValue());
            }
        }
    }
}
