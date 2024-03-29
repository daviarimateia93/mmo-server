package com.mmo.server.core.packet;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class HelloPacketTest {

    @Test
    public void getAlias() {
        HelloPacket packet = HelloPacket.builder()
                .source(UUID.randomUUID())
                .userName("userName")
                .userPassword("userPassword")
                .build();

        assertThat(packet.getAlias(), is("HELLO"));
    }
}
