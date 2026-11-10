package net.blazemc.blazentenance.common.security;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacSigner {

    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] key;

    public HmacSigner(byte[] key) {
        this.key = Objects.requireNonNull(key, "key").clone();
        if (this.key.length < 16) {
            throw new IllegalArgumentException("Shared secret is too short");
        }
    }

    public byte[] sign(byte[] payload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            return mac.doFinal(payload);
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to compute HMAC-SHA256", exception);
        }
    }

    public boolean verify(byte[] payload, byte[] expected) {
        if (expected == null || expected.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(sign(payload), expected);
    }
}
