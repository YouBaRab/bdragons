package com.yb.dragons.network;

import com.yb.dragons.YBDragons;
import com.yb.dragons.player.DragonPlayerData;
import com.yb.dragons.player.DragonPlayerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

/** Network channel: server -> client data sync, client -> server key actions. */
public final class ModNetwork {
    private static final int VERSION = 7;
    public static SimpleChannel CHANNEL;

    private ModNetwork() {}

    public static void init() {
        CHANNEL = ChannelBuilder.named(YBDragons.id("main"))
                .networkProtocolVersion(VERSION)
                .simpleChannel()
                .play()
                    .clientbound()
                        .addMain(PublicSync.class, PublicSync.STREAM_CODEC, PublicSync::handle)
                        .addMain(PrivateSync.class, PrivateSync.STREAM_CODEC, PrivateSync::handle)
                        .addMain(LairSync.class, LairSync.STREAM_CODEC, LairSync::handle)
                    .serverbound()
                        .addMain(Action.class, Action.STREAM_CODEC, Action::handle)
                .build();
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(msg, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToTrackingAndSelf(Entity entity, Object msg) {
        CHANNEL.send(msg, PacketDistributor.TRACKING_ENTITY_AND_SELF.with(entity));
    }

    public static void sendToServer(Object msg) {
        CHANNEL.send(msg, PacketDistributor.SERVER.noArg());
    }

    // ---------------------------------------------------------------------------------------------------------

    /**
     * What other players need to render someone as a dragon or a hybrid: race, stage, form, breath, the
     * alpha, the awakening ritual of the alpha (start time in game ticks, -1 = none, and the body yaw), the
     * Beam of Life of the alpha of the forest and the Storm of the alpha of the storm and the Territory of the alpha of the Nether (start times,
     * -1 = none), and the last throw of the sun of the alpha of the sunlight (its sun grows back during the recharge).
     */
    public record PublicSync(int entityId, int type, int stage, int form, boolean breathing, boolean alpha, long ritualStart, float ritualYaw,
                             long beamStart, long stormStart, long domainStart, long sunStart) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PublicSync> STREAM_CODEC = StreamCodec.ofMember(PublicSync::encode, PublicSync::decode);

        public static PublicSync of(ServerPlayer player, DragonPlayerData data) {
            return new PublicSync(player.getId(), data.type() == null ? -1 : data.type().ordinal(),
                    data.stage().ordinal(), data.form().ordinal(), data.breathing, data.isAlpha(),
                    data.inRitual() ? data.ritualStart : -1, data.ritualYaw, data.beaming() ? data.beamStart : -1,
                    data.storming() ? data.stormStart : -1, data.domaining() ? data.domainStart : -1, data.sunStart);
        }

        public void encode(RegistryFriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeByte(type);
            buf.writeByte(stage);
            buf.writeByte(form);
            buf.writeBoolean(breathing);
            buf.writeBoolean(alpha);
            buf.writeLong(ritualStart);
            buf.writeFloat(ritualYaw);
            buf.writeLong(beamStart);
            buf.writeLong(stormStart);
            buf.writeLong(domainStart);
            buf.writeLong(sunStart);
        }

        public static PublicSync decode(RegistryFriendlyByteBuf buf) {
            return new PublicSync(buf.readVarInt(), buf.readByte(), buf.readByte(), buf.readByte(), buf.readBoolean(),
                    buf.readBoolean(), buf.readLong(), buf.readFloat(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong());
        }

        public void handle(CustomPayloadEvent.Context ctx) {
            com.yb.dragons.client.ClientDragonData.handlePublic(this);
            ctx.setPacketHandled(true);
        }
    }

    /** Full data for the owner (quests, power, energy...). */
    public record PrivateSync(DragonPlayerData data) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PrivateSync> STREAM_CODEC = StreamCodec.ofMember(PrivateSync::encode, PrivateSync::decode);

        public void encode(FriendlyByteBuf buf) {
            data.write(buf);
        }

        public static PrivateSync decode(FriendlyByteBuf buf) {
            return new PrivateSync(DragonPlayerData.read(buf));
        }

        public void handle(CustomPayloadEvent.Context ctx) {
            com.yb.dragons.client.ClientDragonData.handlePrivate(this);
            ctx.setPacketHandled(true);
        }
    }

    /** The dragon lairs the player knows or senses in his current dimension (map markers, codex). */
    public record LairSync(String dimension, List<Entry> lairs) {
        public static final StreamCodec<RegistryFriendlyByteBuf, LairSync> STREAM_CODEC = StreamCodec.ofMember(LairSync::encode, LairSync::decode);

        /** One lair: position, race, rank, defeated (empty for now), known (discovered) or only sensed. */
        public record Entry(long region, int x, int z, int type, int rank, boolean defeated, boolean known) {}

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(dimension);
            buf.writeVarInt(lairs.size());
            for (Entry e : lairs) {
                buf.writeLong(e.region());
                buf.writeInt(e.x());
                buf.writeInt(e.z());
                buf.writeByte(e.type());
                buf.writeByte(e.rank());
                buf.writeBoolean(e.defeated());
                buf.writeBoolean(e.known());
            }
        }

        public static LairSync decode(FriendlyByteBuf buf) {
            String dim = buf.readUtf();
            int n = Math.min(buf.readVarInt(), 4096);
            List<Entry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(new Entry(buf.readLong(), buf.readInt(), buf.readInt(), buf.readByte(), buf.readByte(), buf.readBoolean(), buf.readBoolean()));
            }
            return new LairSync(dim, list);
        }

        public void handle(CustomPayloadEvent.Context ctx) {
            com.yb.dragons.client.ClientLairData.handle(this);
            ctx.setPacketHandled(true);
        }
    }

    /** Key actions sent by the client. */
    public record Action(int action, boolean pressed) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.ofMember(Action::encode, Action::decode);
        public static final int BREATH = 0;
        public static final int ROAR = 1;
        public static final int CYCLE_FORM = 2;
        /** Start (pressed = true) or stop gliding. */
        public static final int GLIDE = 3;
        /** The power of the alpha of the race (dragon form, recharge of 45 seconds). */
        public static final int ALPHA_POWER = 4;

        public void encode(FriendlyByteBuf buf) {
            buf.writeByte(action);
            buf.writeBoolean(pressed);
        }

        public static Action decode(FriendlyByteBuf buf) {
            return new Action(buf.readByte(), buf.readBoolean());
        }

        public void handle(CustomPayloadEvent.Context ctx) {
            ServerPlayer player = ctx.getSender();
            if (player != null) DragonPlayerManager.handleAction(player, action, pressed);
            ctx.setPacketHandled(true);
        }
    }
}
