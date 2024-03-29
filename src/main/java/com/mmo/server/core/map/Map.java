package com.mmo.server.core.map;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.mmo.server.core.looper.LooperContext;
import com.mmo.server.core.looper.LooperUpdater;
import com.mmo.server.core.packet.Packet;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Map implements LooperUpdater {

    @EqualsAndHashCode.Include
    private final UUID id;

    @EqualsAndHashCode.Include
    private final String name;

    @EqualsAndHashCode.Include
    private final String description;

    @EqualsAndHashCode.Include
    private final Integer nearbyRatio;

    @EqualsAndHashCode.Include
    private final Terrain terrain;

    private final ConcurrentHashMap<UUID, MapEntity> entities = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Set<MapPacketDispatchSubscriber> packetSubscribers = new LinkedHashSet<>();

    @Builder
    private Map(
            @NonNull UUID id,
            @NonNull String name,
            @NonNull String description,
            @NonNull Integer nearbyRatio,
            @NonNull Terrain terrain,
            Collection<MapPacketDispatchSubscriber> packetSubscribers) {

        this.id = id;
        this.name = name;
        this.description = description;
        this.nearbyRatio = nearbyRatio;
        this.terrain = terrain;

        if (Objects.nonNull(packetSubscribers)) {
            this.packetSubscribers.addAll(packetSubscribers);
        }
    }

    public <T extends MapEntity> T getEntity(UUID instanceId, Class<T> type) throws MapEntityNotFoundException {
        return findEntity(instanceId, type)
                .orElseThrow(() -> new MapEntityNotFoundException("Entity not found with instanceId %s", instanceId));
    }

    public MapEntity getEntity(UUID instanceId) throws MapEntityNotFoundException {
        return findEntity(instanceId)
                .orElseThrow(() -> new MapEntityNotFoundException("Entity not found with instanceId %s", instanceId));
    }

    @SuppressWarnings("unchecked")
    public <T extends MapEntity> Optional<T> findEntity(UUID instanceId, Class<T> type) {
        return findEntity(instanceId)
                .filter(entity -> type.isAssignableFrom(entity.getClass()))
                .map(entity -> (T) entity);
    }

    public Optional<MapEntity> findEntity(UUID instanceId) {
        return Optional.ofNullable(entities.get(instanceId));
    }

    public Collection<MapEntity> getEntities() {
        return Collections.unmodifiableCollection(entities.values());
    }

    @Override
    public void update(LooperContext context) {
        entities.values().forEach(animate -> animate.update(context));
    }

    public void addEntity(MapEntity entity) {
        entities.put(entity.getInstanceId(), entity);
    }

    public void removeEntity(MapEntity entity) {
        removeEntity(entity.getInstanceId());
    }

    public void removeEntity(UUID instanceId) {
        entities.remove(instanceId);
    }

    @SuppressWarnings("unchecked")
    public <T extends MapEntity> Set<T> getNearbyEntities(MapEntity baseEntity, Class<T> type) {
        return getNearbyEntities(baseEntity)
                .stream()
                .filter(entity -> type.isAssignableFrom(entity.getClass()))
                .map(entity -> (T) entity)
                .collect(Collectors.toSet());
    }

    public Set<MapEntity> getNearbyEntities(MapEntity baseEntity) {
        return entities.values().stream()
                .filter(entity -> isNearby(baseEntity, entity))
                .collect(Collectors.toSet());
    }

    public void dispatch(Packet packet) {
        packetSubscribers.forEach(subscriber -> subscriber.onDispatch(packet, Optional.empty()));
    }

    public void dispatch(Packet packet, UUID target) {
        packetSubscribers.forEach(subscriber -> subscriber.onDispatch(packet, Optional.ofNullable(target)));
    }

    private boolean isNearby(MapEntity baseEntity, MapEntity testingEntity) {
        return baseEntity.getPosition().isNearby(testingEntity.getPosition(), nearbyRatio);
    }
}
