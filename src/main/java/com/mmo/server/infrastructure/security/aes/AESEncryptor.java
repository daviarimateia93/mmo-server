package com.mmo.server.infrastructure.security.aes;

import java.security.Key;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.mmo.server.infrastructure.security.EncryptionException;
import com.mmo.server.infrastructure.security.Encryptor;

import lombok.Builder;
import lombok.NonNull;

public class AESEncryptor implements Encryptor {

    private static final String CIPHER_TRANSFORMATION = "AES";

    private final Cipher cipher;

    @Builder
    private AESEncryptor(@NonNull String key) {
        try {
            Key aesKey = new SecretKeySpec(key.getBytes(), CIPHER_TRANSFORMATION);
            cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        } catch (Exception exception) {
            throw new CipherInitializationException(exception, "Failed to init cipher in decrypt mode");
        }
    }

    @Override
    public String encrypt(String string) throws EncryptionException {
        try {
            byte[] aesEncoded = cipher.doFinal(string.getBytes());
            byte[] b64Encoded = Base64.getEncoder().encode(aesEncoded);

            return new String(b64Encoded);
        } catch (Exception exception) {
            throw new EncryptionException(exception, "Failed to encrypt");
        }
    }
}
