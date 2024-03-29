package com.mmo.server.core.player;

import java.util.UUID;

import com.mmo.server.core.animate.Animate;
import com.mmo.server.core.attribute.Attributes;
import com.mmo.server.core.map.Position;
import com.mmo.server.core.packet.PlayerPersistPacket;
import com.mmo.server.core.packet.PlayerUpdatePacket;
import com.mmo.server.core.stat.Stats;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class Player extends Animate {

    private final UUID userId;
    private final UUID instanceId;
    private final String name;
    private final Position position;
    private final Stats stats;
    private final Attributes attributes;

    @Builder
    private Player(
            @NonNull UUID userId,
            @NonNull UUID instanceId,
            @NonNull String name,
            @NonNull Position position,
            @NonNull Stats stats,
            @NonNull Attributes attributes) {

        this.userId = userId;
        this.instanceId = instanceId;
        this.name = name;
        this.position = position;
        this.stats = stats;
        this.attributes = attributes;
    }

    @Override
    public UUID getId() {
        return instanceId;
    }

    @Override
    public void move(Position target) {
        super.move(target);
        dispatchUpdate();
    }

    @Override
    public void attack(Animate target) {
        super.attack(target);
        dispatchUpdate();
    }

    @Override
    protected void onDie(Animate source) {
        super.onDie(source);
        dispatchPersist();
    }

    private void dispatchUpdate() {
        dispatch(PlayerUpdatePacket.builder()
                .source(instanceId)
                .player(this)
                .build());
    }

    private void dispatchPersist() {
        dispatch(PlayerPersistPacket.builder()
                .source(instanceId)
                .player(this)
                .build());
    }
}
