package com.mmo.server.infrastructure.server.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mmo.server.core.packet.NetworkPacket;
import com.mmo.server.core.packet.Packet;
import com.mmo.server.infrastructure.security.Decryptor;
import com.mmo.server.infrastructure.security.Encryptor;
import com.mmo.server.infrastructure.security.TokenData;
import com.mmo.server.infrastructure.server.packet.DisconnectPacket;
import com.mmo.server.infrastructure.server.packet.PacketGateway;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Client {

    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    @EqualsAndHashCode.Include
    private final UUID id = UUID.randomUUID();
    private final Socket socket;
    private final Encryptor encryptor;
    private final Decryptor decryptor;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;
    private final ClientDisconnectSubscriber disconnectSubscriber;
    private final ClientPacketSendSubscriber sendSubscriber;
    private final ClientPacketReceiveSubscriber receiveSubscriber;
    private final BlockingQueue<NetworkPacket> sendingQueue = new LinkedBlockingQueue<>();
    private final ExecutorService sendingPool = Executors.newSingleThreadExecutor();
    private final ExecutorService receivingPool = Executors.newSingleThreadExecutor();
    private final PacketGateway packetGateway;
    private boolean connected;

    @Builder(builderMethodName = "serverBuilder", buildMethodName = "buildServer")
    private Client(
            @NonNull Socket socket,
            @NonNull Encryptor encryptor,
            @NonNull Decryptor decryptor,
            ClientDisconnectSubscriber disconnectSubscriber,
            ClientPacketSendSubscriber sendSubscriber,
            ClientPacketReceiveSubscriber receiveSubscriber) {

        this.socket = socket;
        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.inputStream = getDataInputStream();
        this.outputStream = getDataOutputStream();
        this.disconnectSubscriber = disconnectSubscriber;
        this.sendSubscriber = sendSubscriber;
        this.receiveSubscriber = receiveSubscriber;
        this.packetGateway = PacketGateway.getInstance();
        this.connected = true;

        startPools();
    }

    @Builder(builderMethodName = "clientBuilder", buildMethodName = "buildClient")
    private Client(
            @NonNull String host,
            @NonNull Integer port,
            @NonNull Encryptor encryptor,
            @NonNull Decryptor decryptor,
            ClientDisconnectSubscriber disconnectSubscriber,
            ClientPacketSendSubscriber sendSubscriber,
            ClientPacketReceiveSubscriber receiveSubscriber) {

        this.socket = connect(host, port);
        this.encryptor = encryptor;
        this.decryptor = decryptor;
        this.inputStream = getDataInputStream();
        this.outputStream = getDataOutputStream();
        this.disconnectSubscriber = disconnectSubscriber;
        this.sendSubscriber = sendSubscriber;
        this.receiveSubscriber = receiveSubscriber;
        this.packetGateway = PacketGateway.getInstance();
        this.connected = true;

        startPools();
    }

    public UUID getId() {
        return id;
    }

    public boolean isConnected() {
        return connected;
    }

    private Optional<ClientDisconnectSubscriber> getDisconnectSubscriber() {
        return Optional.ofNullable(disconnectSubscriber);
    }

    private Optional<ClientPacketSendSubscriber> getSendSubscriber() {
        return Optional.ofNullable(sendSubscriber);
    }

    private Optional<ClientPacketReceiveSubscriber> getReceiveSubscriber() {
        return Optional.ofNullable(receiveSubscriber);
    }

    private DataInputStream getDataInputStream() {
        try {
            return new DataInputStream(socket.getInputStream());
        } catch (Exception exception) {
            throw new ClientConnectException(exception, "Failed to get input stream");
        }
    }

    private DataOutputStream getDataOutputStream() {
        try {
            return new DataOutputStream(socket.getOutputStream());
        } catch (Exception exception) {
            throw new ClientConnectException(exception, "Failed to get output stream");
        }
    }

    private Socket connect(String host, Integer port) {
        try {
            return new Socket(host, port);
        } catch (Exception exception) {
            throw new ClientConnectException(exception, "Failed to create socket");
        }
    }

    public void disconnect() throws ClientDisconnectException {
        try {
            socket.close();
        } catch (Exception exception) {
            throw new ClientDisconnectException(exception, "Failed to close socket");
        } finally {
            if (isConnected()) {
                connected = false;

                sendingQueue.add(DisconnectPacket.builder()
                        .source(getId())
                        .build());

                sendingPool.shutdownNow();
                receivingPool.shutdownNow();

                getDisconnectSubscriber().ifPresent(subscriber -> subscriber.onDisconnect(this));
            }
        }
    }

    public void send(NetworkPacket packet) {
        sendingQueue.add(packet);
    }

    private void startPools() {
        sendingPool.execute(this::send);
        receivingPool.execute(this::receive);

    }

    private void send() {
        NetworkPacket packet;

        try {
            while (isConnected() && (packet = sendingQueue.take()) != null) {
                if (packet instanceof DisconnectPacket) {
                    break;
                }

                sendPacket(packet);
            }
        } catch (Exception exception) {
            logger.error("Stop sending packet", new ClientSendException(exception));
        } finally {
            disconnect();
        }
    }

    private void receive() {
        try {
            while (isConnected()) {
                receivePacket();
            }
        } catch (Exception exception) {
            logger.error("Stop receiving packet", new ClientReadException(exception));
        } finally {
            disconnect();
        }
    }

    private void sendPacket(NetworkPacket packet) throws IOException {
        byte[] bytes = packetGateway.write(packet);
        UUID source = packet.getSource();
        UUID alias = packet.getAliasAsUUID();
        String token = encryptor.encrypt(TokenData.create(source).getToken());

        outputStream.writeUTF(token);
        outputStream.writeLong(alias.getMostSignificantBits());
        outputStream.writeLong(alias.getLeastSignificantBits());
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);

        getSendSubscriber().ifPresent(subscriber -> subscriber.onSend(this, packet));
    }

    private void receivePacket() throws IOException {
        String token = decryptor.decrypt(inputStream.readUTF());
        long aliasHigh = inputStream.readLong();
        long aliasLow = inputStream.readLong();
        int size = inputStream.readInt();

        TokenData tokenData = TokenData.parse(token);

        if (tokenData.isExpired()) {
            throw new ClientReadException("Token is expired %s", tokenData);
        }

        UUID source = tokenData.getSource();
        OffsetDateTime creation = tokenData.getCreation();
        UUID alias = new UUID(aliasHigh, aliasLow);

        byte[] bytes = new byte[size];
        inputStream.readFully(bytes);

        Packet packet = packetGateway.read(alias, source, creation, bytes);

        getReceiveSubscriber().ifPresent(subscriber -> subscriber.onReceive(this, packet));
    }
}
