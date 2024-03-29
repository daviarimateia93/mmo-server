package com.mmo.server.infrastructure.server.packet.converter;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mmo.server.core.packet.GoodByePacket;

public class GoodByePacketConverterTest {

    private static GoodByePacketConverter converter;

    @BeforeAll
    public static void setup() {
        converter = new GoodByePacketConverter();
    }

    @Test
    public void readAndWrite() {
        UUID source = UUID.randomUUID();

        GoodByePacket expected = GoodByePacket.builder()
                .source(source)
                .build();

        GoodByePacket result = converter.read(source, OffsetDateTime.now(), converter.write(expected));

        assertThat(result, equalTo(expected));
    }
}
