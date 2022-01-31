package com.mmo.server.infrastructure.packet;

import static org.mockito.Mockito.*;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mmo.server.core.animate.Animate;
import com.mmo.server.core.game.GameRunnerMapMocker;
import com.mmo.server.core.map.Map;
import com.mmo.server.core.map.Position;
import com.mmo.server.core.packet.AnimateMovePacket;

public class AnimateMovePacketHandlerTest {

    private static Map map;
    private static AnimateMovePacket packet;
    private static AnimateMovePacketHandler packetHandler;
    private static Animate source;

    @BeforeAll
    public static void setup() {
        map = GameRunnerMapMocker.run();
        packet = AnimateMovePacket.builder()
                .source(UUID.randomUUID())
                .target(Position.builder()
                        .x(10)
                        .z(15)
                        .build())
                .build();

        packetHandler = new AnimateMovePacketHandler();

        source = mock(Animate.class);

        when(map.getEntity(packet.getSource(), Animate.class)).thenReturn(source);
    }

    @AfterAll
    private static void clear() {
        GameRunnerMapMocker.stop();
    }

    @Test
    public void handle() {
        packetHandler.handle(packet);

        verify(source).move(packet.getTarget());
    }
}
