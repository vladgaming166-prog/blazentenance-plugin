package net.blazemc.blazentenance.common.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Binary codec for proxy-to-backend authorization tokens.
 *
 * <p>The payload is HMAC-SHA256 signed with a shared secret that is never the
 * Velocity forwarding secret. Clients cannot mint valid tokens.
 */
public final class AuthTokenCodec {

    public static final byte[] MAGIC = {'B', 'L', 'Z', 'N'};
    public static final Duration DEFAULT_TTL = Duration.ofSeconds(30);

    private static final int NONCE_LENGTH = 16;
    private static final int HMAC_LENGTH = 32;
    private static final int MAX_NAME_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final HmacSigner signer;
    private final Duration ttl;

    public AuthTokenCodec(HmacSigner signer) {
        this(signer, DEFAULT_TTL);
    }

    public AuthTokenCodec(HmacSigner signer, Duration ttl) {
        this.signer = signer;
        this.ttl = ttl;
    }

    public static byte[] randomNonce() {
        byte[] nonce = new byte[NONCE_LENGTH];
        RANDOM.nextBytes(nonce);
        return nonce;
    }

    public byte[] encode(AuthToken token) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(buffer);
            writeBody(output, token);
            output.flush();
            byte[] body = buffer.toByteArray();
            byte[] signature = signer.sign(body);

            ByteArrayOutputStream framed = new ByteArrayOutputStream(body.length + HMAC_LENGTH);
            framed.write(body);
            framed.write(signature);
            return framed.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode auth token", exception);
        }
    }

    public AuthToken decode(byte[] payload) throws InvalidAuthTokenException {
        if (payload == null || payload.length < MAGIC.length + HMAC_LENGTH + 20) {
            throw new InvalidAuthTokenException("Auth token is truncated");
        }
        byte[] body = Arrays.copyOfRange(payload, 0, payload.length - HMAC_LENGTH);
        byte[] signature = Arrays.copyOfRange(payload, payload.length - HMAC_LENGTH, payload.length);
        if (!signer.verify(body, signature)) {
            throw new InvalidAuthTokenException("Auth token signature is invalid");
        }
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(body))) {
            byte[] magic = input.readNBytes(MAGIC.length);
            if (!Arrays.equals(MAGIC, magic)) {
                throw new InvalidAuthTokenException("Auth token magic does not match");
            }
            int version = input.readUnsignedByte();
            if (version != AuthToken.CURRENT_VERSION) {
                throw new InvalidAuthTokenException("Unsupported auth token version: " + version);
            }
            long issuedEpoch = input.readLong();
            Instant issuedAt = Instant.ofEpochMilli(issuedEpoch);
            if (issuedAt.isAfter(Instant.now().plusSeconds(5))) {
                throw new InvalidAuthTokenException("Auth token timestamp is in the future");
            }
            if (issuedAt.isBefore(Instant.now().minus(ttl))) {
                throw new InvalidAuthTokenException("Auth token has expired");
            }
            long most = input.readLong();
            long least = input.readLong();
            UUID playerId = new UUID(most, least);
            int nameLength = input.readUnsignedByte();
            if (nameLength == 0 || nameLength > MAX_NAME_LENGTH) {
                throw new InvalidAuthTokenException("Auth token username is invalid");
            }
            byte[] nameBytes = input.readNBytes(nameLength);
            if (nameBytes.length != nameLength) {
                throw new InvalidAuthTokenException("Auth token username is truncated");
            }
            String username = new String(nameBytes, StandardCharsets.UTF_8);
            boolean authorized = input.readBoolean();
            boolean maintenanceEnabled = input.readBoolean();
            byte[] nonce = input.readNBytes(NONCE_LENGTH);
            if (nonce.length != NONCE_LENGTH) {
                throw new InvalidAuthTokenException("Auth token nonce is truncated");
            }
            if (input.available() != 0) {
                throw new InvalidAuthTokenException("Auth token has trailing data");
            }
            return new AuthToken(version, issuedAt, playerId, username, authorized, maintenanceEnabled, nonce);
        } catch (IOException exception) {
            throw new InvalidAuthTokenException("Auth token could not be parsed", exception);
        }
    }

    private static void writeBody(DataOutputStream output, AuthToken token) throws IOException {
        output.write(MAGIC);
        output.writeByte(token.version());
        output.writeLong(token.issuedAt().toEpochMilli());
        output.writeLong(token.playerId().getMostSignificantBits());
        output.writeLong(token.playerId().getLeastSignificantBits());
        byte[] name = token.username().getBytes(StandardCharsets.UTF_8);
        if (name.length == 0 || name.length > MAX_NAME_LENGTH) {
            throw new IOException("Invalid username length");
        }
        output.writeByte(name.length);
        output.write(name);
        output.writeBoolean(token.authorized());
        output.writeBoolean(token.maintenanceEnabled());
        if (token.nonce().length != NONCE_LENGTH) {
            throw new IOException("Invalid nonce length");
        }
        output.write(token.nonce());
    }
}
