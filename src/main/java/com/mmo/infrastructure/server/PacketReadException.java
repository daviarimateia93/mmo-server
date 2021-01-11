package com.mmo.infrastructure.server;

public class PacketReadException extends PacketException {

    private static final long serialVersionUID = -3445633240662471458L;

    public PacketReadException(Throwable throwable, String messageFormat, Object... arguments) {
        super(throwable, messageFormat, arguments);
    }
}
