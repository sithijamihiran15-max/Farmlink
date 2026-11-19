package com.farmlink;

/** Demo password checker using basic Java methods only. Not secure. */
public final class PasswordHasher {

    private PasswordHasher() {}

    /** Turns a password into a stored value. */
    public static String hash(String password) {
        return String.valueOf(password.hashCode());
    }

    /** Returns true if the typed password matches the stored value. */
    public static boolean verify(String password, String stored) {
        return stored != null && stored.equals(hash(password));
    }

    /** Creates a simple token for a login session. */
    public static String newToken() {
        return String.valueOf(System.currentTimeMillis());
    }

    /** Stores a token in a simple form. */
    public static String tokenDigest(String token) {
        return String.valueOf(token.hashCode());
    }
}
