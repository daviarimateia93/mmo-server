package com.mmo.server.infrastructure.server;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mmo.server.core.packet.Packet;
import com.mmo.server.infrastructure.server.TestPacket.TestPacketConverter;
import com.mmo.server.infrastructure.server.packet.PacketGateway;

public class PacketFactoryTest {

    @Test
    public void bindAndInAndOutBuilderByUUID() {
        UUID source = UUID.randomUUID();
        String property1 = "hahha";
        Integer property2 = 7;

        TestPacket expected = TestPacket.builder()
                .source(source)
                .property1(property1)
                .property2(property2)
                .build();

        TestPacketConverter converter = TestPacket.converter();

        PacketGateway.getInstance()
                .bindReader(expected.getAliasAsUUID(), converter)
                .bindWriter(expected.getAliasAsUUID(), converter);

        byte[] expectedBytes = PacketGateway.getInstance().out(expected);

        Packet result1 = PacketGateway.getInstance().in(
                expected.getAliasAsUUID(),
                expected.getSource(),
                expectedBytes);

        Packet result2 = PacketGateway.getInstance().in(
                expected.getAlias(),
                expected.getSource(),
                expectedBytes);

        assertThat(result1, equalTo(expected));
        assertThat(result2, equalTo(expected));
    }

    @Test
    public void bindAndInAndOutBuilderByString() {
        UUID source = UUID.randomUUID();
        String property1 = "hehhe";
        Integer property2 = 8;

        TestPacket expected = TestPacket.builder()
                .source(source)
                .property1(property1)
                .property2(property2)
                .build();

        TestPacketConverter converter = TestPacket.converter();

        PacketGateway.getInstance()
                .bindReader(expected.getAlias(), converter)
                .bindWriter(expected.getAlias(), converter);

        byte[] expectedBytes = PacketGateway.getInstance().out(expected);

        Packet result1 = PacketGateway.getInstance().in(
                expected.getAliasAsUUID(),
                expected.getSource(),
                expectedBytes);

        Packet result2 = PacketGateway.getInstance().in(
                expected.getAlias(),
                expected.getSource(),
                expectedBytes);

        assertThat(result1, equalTo(expected));
        assertThat(result2, equalTo(expected));
    }

    @Test
    public void bindAndInAndOutBuilderByPacket() {
        UUID source = UUID.randomUUID();
        String property1 = "hihihi";
        Integer property2 = 9;

        TestPacket expected = TestPacket.builder()
                .source(source)
                .property1(property1)
                .property2(property2)
                .build();

        TestPacketConverter converter = TestPacket.converter();

        PacketGateway.getInstance()
                .bindReader(expected, converter)
                .bindWriter(expected, converter);

        byte[] expectedBytes = PacketGateway.getInstance().out(expected);

        Packet result1 = PacketGateway.getInstance().in(
                expected.getAliasAsUUID(),
                expected.getSource(),
                expectedBytes);

        Packet result2 = PacketGateway.getInstance().in(
                expected.getAlias(),
                expected.getSource(),
                expectedBytes);

        assertThat(result1, equalTo(expected));
        assertThat(result2, equalTo(expected));
    }
}
