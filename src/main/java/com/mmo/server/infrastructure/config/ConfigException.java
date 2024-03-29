package com.mmo.server.infrastructure.config;

import com.mmo.server.core.exception.RuntimeException;

public abstract class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 5640903564570240835L;

    public ConfigException(String messageFormat, Object... arguments) {
        super(messageFormat, arguments);
    }

    public ConfigException(Throwable throwable, String messageFormat, Object... arguments) {
        super(throwable, messageFormat, arguments);
    }
}
