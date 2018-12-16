package at.favre.lib.crypto.bkdf;

import at.favre.lib.bytes.Bytes;
import at.favre.lib.bytes.BytesValidators;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * +
 * Model containing all the parts required for the "BKDF Password Hash Message Format"
 */
@SuppressWarnings("WeakerAccess")
public final class HashData {
    public final byte cost;
    public final Version version;
    public final byte[] rawSalt;
    public final byte[] rawHash;

    /**
     * Parse BKDF Password Hash Message Formt 1
     *
     * @param bkdfPasswordHashFormat1 "BKDF Password Hash Message Format 1" which is in blob/byte array form
     * @return parsed data
     */
    public static HashData parse(byte[] bkdfPasswordHashFormat1) {
        ByteBuffer buffer = ByteBuffer.wrap(bkdfPasswordHashFormat1);
        byte versionByte = buffer.get();
        Version version = Version.Util.getByCode(versionByte);

        byte costFactor = buffer.get();
        byte[] salt = new byte[16];
        byte[] hash = new byte[(version.isUseOnly23ByteBcryptOut() ? 23 : 24)];
        buffer.get(salt);
        buffer.get(hash);
        return new HashData(costFactor, version, salt, hash);
    }

    /**
     * Parse BKDF Password Hash Message Formt 2
     *
     * @param bkdfPasswordHashFormat2 "BKDF Password Hash Message Format 2" ie. Base64 encoded password hash for storage,
     *                                see {@link PasswordHasher#hash(char[], int)};
     * @return parsed data
     */
    public static HashData parse(String bkdfPasswordHashFormat2) {
        return parse(Bytes.parseBase64(bkdfPasswordHashFormat2).array());
    }

    public HashData(byte cost, Version version, byte[] rawSalt, byte[] rawHash) {
        Objects.requireNonNull(rawHash);
        Objects.requireNonNull(rawSalt);
        Objects.requireNonNull(version);
        if (Bytes.wrap(rawSalt).validate(BytesValidators.exactLength(16))
                && Bytes.wrap(rawHash).validate(BytesValidators
                .or(BytesValidators.exactLength(23), BytesValidators.exactLength(24)))) {
            this.cost = cost;
            this.version = version;
            this.rawSalt = rawSalt;
            this.rawHash = rawHash;
        } else {
            throw new IllegalArgumentException("salt must be exactly 16 bytes and hash 23/24 bytes long");
        }
    }

    /**
     * Get the "BKDF Password Hash Message Format 1" which is in blob/byte array form
     * <p>
     * Currently this is the following format:
     *
     * <code>V C S S S S S S S S S S S S S S S S H H H H H H H H H H ...</code>
     * <ul>
     * <li>V: 1 byte version code</li>
     * <li>C: 1 byte cost factor</li>
     * <li>S: 16 byte salt</li>
     * <li>H: 23/24 byte hash</li>
     * </ul>
     *
     * @return message as byte array aka "Format 1"
     */
    public byte[] getAsBlobMessageFormat() {
        if (rawHash == null) {
            throw new IllegalStateException("cannot reuse wiped instance");
        }

        ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + rawSalt.length + rawHash.length);
        buffer.put(version.getVersionCode());
        buffer.put(cost);
        buffer.put(rawSalt);
        buffer.put(rawHash);
        return buffer.array();
    }

    /**
     * Get the "BKDF Password Hash Message Format 2" which is a base64-url encoded
     * (rfc4648 "Base 64 Encoding with URL and Filename Safe Alphabet") message containing
     * all information needed to verify a password, including cost factor, version and salt.
     * <p>
     * See  {@link #getAsBlobMessageFormat()} for the exact message format.
     *
     * @return base64-url-safe encoded password hash message aka "Format 2"
     */
    public String getAsEncodedMessageFormat() {
        return Bytes.wrap(getAsBlobMessageFormat()).encodeBase64Url();
    }

    /**
     * Wipe the internal byte arrays for security purposes.
     * This instance must not be used after calling this.
     */
    public void wipe() {
        Bytes.wrapNullSafe(this.rawSalt).mutable().secureWipe();
        Bytes.wrapNullSafe(this.rawHash).mutable().secureWipe();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HashData hashData = (HashData) o;
        return cost == hashData.cost &&
                Objects.equals(version, hashData.version) &&
                Bytes.wrapNullSafe(rawSalt).equalsConstantTime(hashData.rawSalt) &&
                Bytes.wrapNullSafe(rawHash).equalsConstantTime(hashData.rawHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(cost, version);
        result = 31 * result + Arrays.hashCode(rawSalt);
        result = 31 * result + Arrays.hashCode(rawHash);
        return result;
    }
}
