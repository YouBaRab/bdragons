package com.yb.dragons.player;

import com.yb.dragons.dragon.AlphaForest;
import com.yb.dragons.dragon.AlphaSun;
import com.yb.dragons.dragon.AlphaMoon;
import com.yb.dragons.dragon.AlphaNether;
import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.DragonAlpha;
import com.yb.dragons.dragon.DragonForm;
import com.yb.dragons.dragon.DragonQuests;
import com.yb.dragons.dragon.DragonStage;
import com.yb.dragons.dragon.DragonStats;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Everything the mod knows about one player. Stored server side in {@link DragonSavedData}
 * (keyed by UUID, so it survives death and dimension changes) and mirrored to the client.
 */
public class DragonPlayerData {
    // --- persistent ---------------------------------------------------------------------------------------
    /** Race of the player once transformed, null while still human. */
    @Nullable private DragonType type;
    /** Race chosen by the essence quest (locked forever once chosen). */
    @Nullable private DragonType chosenType;
    private int questIndex;
    private int[] questProgress = new int[4];
    private DragonStage stage = DragonStage.HATCHLING;
    private int power;
    private int maxDefeatedRank;
    private DragonForm form = DragonForm.DRAGON;
    private int dragonsDefeated;
    /** The codex is given once, on the first arrival in the world. */
    private boolean codexGiven;
    /** Lairs discovered by this player (approached or revealed by the Challenge Horn): "dimension|regionKey". */
    private final Set<String> knownLairs = new HashSet<>();
    /** The Alpha of his race (orb of power consumed, forever). */
    private boolean alpha;
    /** Offerings waiting in the altar forge: alpha scales of each race (by ordinal) and Nether Stars. */
    private int[] forgeScales = new int[DragonType.values().length];
    private int forgeStars;

    // --- runtime (not saved) ------------------------------------------------------------------------------
    public float energy = 100;
    public int flightStamina = 200;
    public boolean breathing;
    public boolean gliding;
    public int breathTicks;
    public int roarCooldown;
    public int formCooldown;
    /** Recharge of the alpha power (ticks left). Saved, so that leaving the world does not recharge it. */
    public int alphaCooldown;
    /** Awakening ritual of the alpha: ticks since it started, -1 when there is none. */
    public int ritualTick = -1;
    /** Game time when the ritual started (sent to the clients so that everybody plays the same animation). */
    public long ritualStart = -1;
    /** Body yaw of the dragon when the ritual started (the dragon faces this way during the cinematic). */
    public float ritualYaw;
    /** Entity id of the floating orb of the ritual (-1 = none). */
    public int ritualOrb = -1;
    /** Beam of Life of the alpha of the forest: ticks since it started (-1 = not firing). */
    public int beamTicks = -1;
    /** Game time when the beam started (sent to the clients, -1 = none). */
    public long beamStart = -1;
    /** The Storm of the alpha of the storm: ticks since it was called (-1 = no storm). */
    public int stormTicks = -1;
    /** Game time when the storm was called (sent to the clients, -1 = none). */
    public long stormStart = -1;
    /** The Territory of the alpha of the Nether: ticks since it was called (-1 = none). */
    public int domainTicks = -1;
    /** Game time when the Territory was called (sent to the clients, -1 = none). */
    public long domainStart = -1;
    /** The sun thrown by the alpha of the sunlight: ticks since the power was called (-1 = none). */
    public int sunTicks = -1;
    /**
     * Game time when the alpha of the sunlight last threw its sun (sent to the clients, -1 = never): its sun grows back
     * on its head during the recharge, so this time is kept after the end of the power.
     */
    public long sunStart = -1;
    public boolean dirty = true;
    public boolean publicDirty = true;

    public boolean isDragon() { return type != null; }
    /** True when the player is a dragon AND currently in the full dragon form. */
    public boolean isActiveDragon() { return type != null && form == DragonForm.DRAGON; }
    /** True in the hybrid and dragon forms: the dragon powers can be used. */
    public boolean hasPowers() { return type != null && form.hasPowers(); }
    public boolean isHybrid() { return type != null && form == DragonForm.HYBRID; }
    /** Hybrid claws: dig like an enchanted netherite pickaxe (0 = no claws). */
    public int clawLevel() { return type != null && form.hasClaws() ? stage.number() : 0; }
    /** Multiplier of the powers in the current form (0 for a human). */
    public float powerFactor() { return type == null ? 0 : form.powerFactor(); }
    @Nullable public DragonType type() { return type; }
    @Nullable public DragonType chosenType() { return chosenType; }
    public int questIndex() { return questIndex; }
    public int[] questProgress() { return questProgress; }
    public DragonStage stage() { return stage; }
    public int power() { return power; }
    public int maxDefeatedRank() { return maxDefeatedRank; }
    public DragonForm form() { return form; }
    public int dragonsDefeated() { return dragonsDefeated; }
    public boolean codexGiven() { return codexGiven; }
    public Set<String> knownLairs() { return knownLairs; }

    public boolean questsDone() { return questIndex >= DragonQuests.QUEST_COUNT; }

    /** The Alpha of his race (only a legendary dragon can become one). */
    public boolean isAlpha() { return type != null && alpha; }
    /**
     * Counts down the recharges, every tick, whatever the form: the alpha power keeps recharging in the
     * hybrid form even though its bar is hidden there.
     */
    public void tickTimers() {
        if (roarCooldown > 0) roarCooldown--;
        if (formCooldown > 0) formCooldown--;
        if (alphaCooldown > 0) alphaCooldown--;
    }

    /** The bar of the alpha power is shown in the dragon form only (the hybrid cannot use the power). */
    public boolean showsAlphaPowerBar() { return isAlpha() && form == DragonForm.DRAGON; }

    /** 0 = just used, 1 = ready (each race has its own recharge). */
    public float alphaPowerReady() { return 1.0F - Math.min(1.0F, alphaCooldown / (float) DragonAlpha.powerCooldown(type)); }

    /** The awakening ritual of the alpha is playing (the dragon cannot move, act nor be hurt). */
    public boolean inRitual() { return ritualTick >= 0; }
    /** The numbers of this dragon: his stage, or the alpha of his race. */
    public DragonStats stats() { return DragonStats.of(type, stage, isAlpha()); }
    /** The numbers of the hybrid form (the same for every alpha: the human body stays human sized). */
    public DragonStats hybridStats() { return DragonStats.of(stage, isAlpha()); }
    /** The alpha of the forest in his full dragon form: the Sylvan Tortoise. */
    public boolean isTortoise() { return AlphaForest.isTortoise(type, stage.number(), isAlpha(), form); }
    /** The Beam of Life is firing. */
    public boolean beaming() { return beamTicks >= 0; }
    /** The alpha of the storm in his full dragon form: the Tempest Dragon. */
    public boolean isSerpent() { return AlphaStorm.isSerpent(type, stage.number(), isAlpha(), form); }
    /** The Storm of the alpha of the storm is raging. */
    public boolean storming() { return stormTicks >= 0; }
    /** The alpha of the Nether in his full dragon form: the Demon of the Nether. */
    public boolean isDemon() { return AlphaNether.isDemon(type, stage.number(), isAlpha(), form); }
    /** The Territory of the alpha of the Nether is being called (incantation, ground, acid rain). */
    public boolean domaining() { return domainTicks >= 0; }
    /** The alpha of the moonlight in his full dragon form: the Moon Dragon (it has the body of a serpent too). */
    public boolean isMoon() { return AlphaMoon.isMoon(type, stage.number(), isAlpha(), form); }
    /** The alpha of the sunlight in his full dragon form: the Sun Steed. */
    public boolean isSteed() { return AlphaSun.isSteed(type, stage.number(), isAlpha(), form); }
    /** The Sun Steed is throwing its sun (rearing up, the sun flying or exploding). */
    public boolean striking() { return sunTicks >= 0; }
    public int forgeScales(DragonType race) { return forgeScales[race.ordinal()]; }
    public int forgeStars() { return forgeStars; }
    public boolean hasForgeOfferings() {
        if (forgeStars > 0) return true;
        for (int n : forgeScales) if (n > 0) return true;
        return false;
    }

    public void setType(@Nullable DragonType type) { this.type = type; markAll(); }
    public void setChosenType(@Nullable DragonType chosen) { this.chosenType = chosen; markDirty(); }
    public void setStage(DragonStage stage) { this.stage = stage; markAll(); }
    public void setPower(int power) { this.power = Math.max(0, power); markDirty(); }
    public void addPower(int amount) { setPower(power + amount); }
    public void setMaxDefeatedRank(int rank) { this.maxDefeatedRank = Math.max(0, Math.min(5, rank)); markDirty(); }
    public void setForm(DragonForm form) { this.form = form; markAll(); }
    public void incrementDefeated() { dragonsDefeated++; markDirty(); }
    public void setCodexGiven(boolean given) { this.codexGiven = given; markDirty(); }
    public void setAlpha(boolean alpha) { this.alpha = alpha; markAll(); }
    public void setForgeScales(DragonType race, int count) { forgeScales[race.ordinal()] = Math.max(0, Math.min(DragonAlpha.SCALES_PER_ORB, count)); markDirty(); }
    public void setForgeStars(int count) { forgeStars = Math.max(0, Math.min(1, count)); markDirty(); }
    /** Empties the forge of the altar (the items are given back by the caller). */
    public void clearForge() {
        Arrays.fill(forgeScales, 0);
        forgeStars = 0;
        markDirty();
    }

    public boolean addKnownLair(String key) {
        boolean added = knownLairs.add(key);
        if (added) markDirty();
        return added;
    }

    public void setQuestIndex(int index) {
        this.questIndex = Math.max(0, Math.min(DragonQuests.QUEST_COUNT, index));
        Arrays.fill(questProgress, 0);
        markDirty();
    }

    public void setQuestProgress(int slot, int value) {
        if (slot >= 0 && slot < questProgress.length) {
            questProgress[slot] = value;
            markDirty();
        }
    }

    /** Back to a plain human with no quest progress (admin command). The codex stays given. */
    public void reset() {
        type = null;
        chosenType = null;
        questIndex = 0;
        Arrays.fill(questProgress, 0);
        stage = DragonStage.HATCHLING;
        power = 0;
        maxDefeatedRank = 0;
        form = DragonForm.DRAGON;
        dragonsDefeated = 0;
        gliding = false;
        beamTicks = -1;
        beamStart = -1;
        stormTicks = -1;
        stormStart = -1;
        domainTicks = -1;
        domainStart = -1;
        sunTicks = -1;
        sunStart = -1;
        alpha = false;
        alphaCooldown = 0;
        Arrays.fill(forgeScales, 0);
        forgeStars = 0;
        markAll();
    }

    public void markDirty() { dirty = true; }
    public void markAll() { dirty = true; publicDirty = true; }

    /** Power needed for the next stage, or -1 at max stage. */
    public int nextStagePower() {
        return stage.isMax() ? -1 : stage.next().powerRequired();
    }

    // --- NBT ----------------------------------------------------------------------------------------------
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (type != null) tag.putString("type", type.id());
        if (chosenType != null) tag.putString("chosen", chosenType.id());
        tag.putInt("quest", questIndex);
        tag.putIntArray("progress", questProgress);
        tag.putInt("stage", stage.number());
        tag.putInt("power", power);
        tag.putInt("defeatedRank", maxDefeatedRank);
        tag.putString("form", form.id());
        tag.putInt("defeated", dragonsDefeated);
        tag.putBoolean("codex", codexGiven);
        ListTag lairs = new ListTag();
        for (String key : knownLairs) lairs.add(StringTag.valueOf(key));
        tag.put("knownLairs", lairs);
        if (alpha) tag.putBoolean("alpha", true);
        tag.putIntArray("forgeScales", forgeScales);
        tag.putInt("forgeStars", forgeStars);
        if (alphaCooldown > 0) tag.putInt("alphaCooldown", alphaCooldown);
        return tag;
    }

    public static DragonPlayerData load(CompoundTag tag) {
        DragonPlayerData d = new DragonPlayerData();
        d.type = tag.getString("type").map(DragonType::byId).orElse(null);
        d.chosenType = tag.getString("chosen").map(DragonType::byId).orElse(null);
        d.questIndex = tag.getIntOr("quest", 0);
        int[] progress = tag.getIntArray("progress").orElse(new int[4]);
        d.questProgress = Arrays.copyOf(progress, 4);
        d.stage = DragonStage.byNumber(tag.getIntOr("stage", 1));
        d.power = tag.getIntOr("power", 0);
        d.maxDefeatedRank = tag.getIntOr("defeatedRank", 0);
        // worlds created with the first version stored a "dragonForm" boolean
        d.form = tag.getString("form").map(DragonForm::byId)
                .orElse(tag.getBooleanOr("dragonForm", true) ? DragonForm.DRAGON : DragonForm.HYBRID);
        d.dragonsDefeated = tag.getIntOr("defeated", 0);
        d.codexGiven = tag.getBooleanOr("codex", false);
        tag.getList("knownLairs").ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) list.getString(i).ifPresent(d.knownLairs::add);
        });
        d.alpha = tag.getBooleanOr("alpha", false);
        int[] forge = tag.getIntArray("forgeScales").orElse(new int[0]);
        for (int i = 0; i < Math.min(forge.length, d.forgeScales.length); i++) d.forgeScales[i] = Math.max(0, forge[i]);
        d.forgeStars = Math.max(0, tag.getIntOr("forgeStars", 0));
        d.alphaCooldown = Math.max(0, Math.min(DragonAlpha.powerCooldown(d.type), tag.getIntOr("alphaCooldown", 0)));
        d.energy = d.stats().maxEnergy();
        return d;
    }

    // --- network (full private copy for the owner) -----------------------------------------------------------
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(type == null ? -1 : type.ordinal());
        buf.writeByte(chosenType == null ? -1 : chosenType.ordinal());
        buf.writeByte(questIndex);
        for (int i = 0; i < 4; i++) buf.writeVarInt(questProgress[i]);
        buf.writeByte(stage.ordinal());
        buf.writeVarInt(power);
        buf.writeByte(maxDefeatedRank);
        buf.writeByte(form.ordinal());
        buf.writeVarInt(dragonsDefeated);
        buf.writeFloat(energy);
        buf.writeVarInt(flightStamina);
        buf.writeVarInt(roarCooldown);
        buf.writeBoolean(gliding);
        buf.writeBoolean(alpha);
        buf.writeVarInt(alphaCooldown);
        buf.writeVarInt(ritualTick);
        buf.writeVarInt(beamTicks);
        buf.writeVarInt(stormTicks);
        buf.writeVarInt(domainTicks);
        buf.writeVarInt(sunTicks);
        for (int n : forgeScales) buf.writeByte(n);
        buf.writeByte(forgeStars);
    }

    public static DragonPlayerData read(FriendlyByteBuf buf) {
        DragonPlayerData d = new DragonPlayerData();
        int t = buf.readByte();
        d.type = t < 0 ? null : DragonType.byOrdinal(t);
        int c = buf.readByte();
        d.chosenType = c < 0 ? null : DragonType.byOrdinal(c);
        d.questIndex = buf.readByte();
        for (int i = 0; i < 4; i++) d.questProgress[i] = buf.readVarInt();
        d.stage = DragonStage.values()[Math.max(0, Math.min(4, buf.readByte()))];
        d.power = buf.readVarInt();
        d.maxDefeatedRank = buf.readByte();
        d.form = DragonForm.byOrdinal(buf.readByte());
        d.dragonsDefeated = buf.readVarInt();
        d.energy = buf.readFloat();
        d.flightStamina = buf.readVarInt();
        d.roarCooldown = buf.readVarInt();
        d.gliding = buf.readBoolean();
        d.alpha = buf.readBoolean();
        d.alphaCooldown = buf.readVarInt();
        d.ritualTick = buf.readVarInt();
        d.beamTicks = buf.readVarInt();
        d.stormTicks = buf.readVarInt();
        d.domainTicks = buf.readVarInt();
        d.sunTicks = buf.readVarInt();
        for (int i = 0; i < d.forgeScales.length; i++) d.forgeScales[i] = buf.readByte();
        d.forgeStars = buf.readByte();
        return d;
    }
}
