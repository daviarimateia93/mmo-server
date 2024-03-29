package com.mmo.server.core.packet;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class PacketHandlerDelegator {

    private static final Logger logger = LoggerFactory.getLogger(PacketHandlerDelegator.class);

    private static PacketHandlerDelegator instance;

    private final ConcurrentHashMap<Class<? extends Packet>, PacketHandler<?>> handlers = new ConcurrentHashMap<>();

    public static PacketHandlerDelegator getInstance() {
        if (Objects.isNull(instance)) {
            instance = new PacketHandlerDelegator();
        }

        return instance;
    }

    private PacketHandlerDelegator() {

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void delegate(Packet packet) throws PacketHandlerNotBindedException {
        PacketHandler handler = get(packet).orElseThrow(
                () -> new PacketHandlerNotBindedException("There is no packet handler for packet %s", packet));

        logger.info("Delegating packet {} to handler {}", packet, handler);

        handler.handle(packet);
    }

    private Optional<PacketHandler<?>> get(Packet packet) {
        return Optional.ofNullable(handlers.get(packet.getClass()));
    }

    public <T extends Packet> PacketHandlerDelegator bind(Class<T> type, PacketHandler<T> handler) {
        handlers.put(type, handler);
        return this;
    }
}
