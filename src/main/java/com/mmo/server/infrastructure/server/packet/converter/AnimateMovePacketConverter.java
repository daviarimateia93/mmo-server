package com.mmo.server.infrastructure.server.packet.converter;

import java.util.UUID;

import com.mmo.server.core.map.Position;
import com.mmo.server.core.packet.AnimateMovePacket;
import com.mmo.server.infrastructure.server.packet.PacketConverter;
import com.mmo.server.infrastructure.server.packet.PacketReader;
import com.mmo.server.infrastructure.server.packet.PacketWriter;

public class AnimateMovePacketConverter implements PacketConverter<AnimateMovePacket> {

    @Override
    public AnimateMovePacket fromBytes(UUID source, byte[] bytes) {
        try (PacketReader reader = new PacketReader(bytes)) {
            return AnimateMovePacket.builder()
                    .source(source)
                    .target(Position.builder()
                            .x(reader.readInt())
                            .z(reader.readInt())
                            .build())
                    .build();
        }
    }

    @Override
    public byte[] toBytes(AnimateMovePacket packet) {
        try (PacketWriter writer = new PacketWriter()) {
            writer.writeInt(packet.getTarget().getX());
            writer.writeInt(packet.getTarget().getZ());
            return writer.toBytes();
        }
    }
}