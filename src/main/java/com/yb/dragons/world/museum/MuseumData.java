package com.yb.dragons.world.museum;

import com.mojang.serialization.Codec;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.entity.WildDragon;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Brightness;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * State of the museum of a level: whether it was built, and the exhibits (dragons on pedestals,
 * floating text panels, training targets) still waiting to appear. Exhibits are only spawned when
 * their chunk is ticking near a player, like any normal entity.
 */
public class MuseumData extends SavedData {
    public static final Codec<MuseumData> CODEC = CompoundTag.CODEC.xmap(MuseumData::fromTag, MuseumData::toTag);
    public static final SavedDataType<MuseumData> TYPE = new SavedDataType<>(YBDragons.id("museum"), MuseumData::new, CODEC, null);

    private boolean built;
    /** Where the last museum of this level was built (feet level of the spawn plaza). */
    @org.jspecify.annotations.Nullable private BlockPos origin;
    private final List<CompoundTag> pending = new ArrayList<>();

    public MuseumData() {}

    public static MuseumData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean isBuilt() {
        return built;
    }

    public void setBuilt(BlockPos origin) {
        built = true;
        this.origin = origin;
        setDirty();
    }

    @org.jspecify.annotations.Nullable
    public BlockPos origin() {
        return origin;
    }

    // --- exhibits ---------------------------------------------------------------------------------------------

    public void addDragon(double x, double y, double z, float yaw, DragonType type, int rank) {
        CompoundTag t = pos("dragon", x, y, z);
        t.putFloat("yaw", yaw);
        t.putString("type", type.id());
        t.putInt("rank", rank);
        pending.add(t);
        setDirty();
    }

    public void addText(double x, double y, double z, Component text, int lineWidth) {
        CompoundTag t = pos("text", x, y, z);
        t.store("text", ComponentSerialization.CODEC, text);
        t.putInt("width", lineWidth);
        pending.add(t);
        setDirty();
    }

    public void addHusk(double x, double y, double z) {
        pending.add(pos("husk", x, y, z));
        setDirty();
    }

    private static CompoundTag pos(String kind, double x, double y, double z) {
        CompoundTag t = new CompoundTag();
        t.putString("kind", kind);
        t.putDouble("x", x);
        t.putDouble("y", y);
        t.putDouble("z", z);
        return t;
    }

    /** Called every second: spawns the exhibits whose chunk is now active. */
    public static void tick(ServerLevel level) {
        MuseumData data = get(level);
        if (data.pending.isEmpty()) return;
        boolean changed = false;
        for (Iterator<CompoundTag> it = data.pending.iterator(); it.hasNext(); ) {
            CompoundTag t = it.next();
            BlockPos pos = BlockPos.containing(t.getDoubleOr("x", 0), t.getDoubleOr("y", 0), t.getDoubleOr("z", 0));
            if (!level.isPositionEntityTicking(pos) || !level.areEntitiesLoaded(ChunkPos.pack(pos))) continue;
            spawn(level, t);
            it.remove();
            changed = true;
        }
        if (changed) data.setDirty();
    }

    private static void spawn(ServerLevel level, CompoundTag t) {
        double x = t.getDoubleOr("x", 0), y = t.getDoubleOr("y", 0), z = t.getDoubleOr("z", 0);
        switch (t.getStringOr("kind", "")) {
            case "dragon" -> {
                DragonType type = DragonType.byId(t.getStringOr("type", "fire"));
                WildDragon dragon = ModRegistry.WILD_DRAGON.get().create(level, EntitySpawnReason.STRUCTURE);
                if (dragon == null || type == null) return;
                float yaw = t.getFloatOr("yaw", 0);
                dragon.setup(type, t.getIntOr("rank", 3));
                dragon.setDisplay(true);
                dragon.snapTo(x, y, z, yaw, 0);
                dragon.setYBodyRot(yaw);
                dragon.setYHeadRot(yaw);
                level.addFreshEntity(dragon);
            }
            case "text" -> {
                Display.TextDisplay display = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.STRUCTURE);
                if (display == null) return;
                CompoundTag tag = new CompoundTag();
                t.read("text", ComponentSerialization.CODEC).ifPresent(c -> tag.store("text", ComponentSerialization.CODEC, c));
                tag.putInt("line_width", t.getIntOr("width", 200));
                tag.putInt("background", 0xC0140F1A);
                tag.putBoolean("shadow", true);
                tag.putString("billboard", "vertical");
                tag.store("brightness", Brightness.CODEC, new Brightness(15, 15));
                display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                display.snapTo(x, y, z, 0, 0);
                level.addFreshEntity(display);
            }
            case "husk" -> {
                Husk husk = EntityTypes.HUSK.create(level, EntitySpawnReason.STRUCTURE);
                if (husk == null) return;
                husk.setNoAi(true);
                husk.setSilent(true);
                husk.setPersistenceRequired();
                husk.snapTo(x, y, z, 90, 0);
                level.addFreshEntity(husk);
            }
            default -> {
            }
        }
    }

    // --- save -------------------------------------------------------------------------------------------------

    private static MuseumData fromTag(CompoundTag tag) {
        MuseumData d = new MuseumData();
        d.built = tag.getBooleanOr("built", false);
        tag.read("origin", BlockPos.CODEC).ifPresent(p -> d.origin = p);
        tag.getList("pending").ifPresent(list -> {
            for (int i = 0; i < list.size(); i++) list.getCompound(i).ifPresent(d.pending::add);
        });
        return d;
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("built", built);
        if (origin != null) tag.store("origin", BlockPos.CODEC, origin);
        ListTag list = new ListTag();
        list.addAll(pending);
        tag.put("pending", list);
        return tag;
    }
}
