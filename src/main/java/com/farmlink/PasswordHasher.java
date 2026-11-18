package com.farmlink;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/** Passwords are salted and PBKDF2-hashed; never stored in plaintext. */
public final class PasswordHasher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS=150_000;
    private PasswordHasher() {}
    public static String hash(String password){
        byte[] salt=new byte[16];RANDOM.nextBytes(salt);
        return ITERATIONS+":"+Base64.getEncoder().encodeToString(salt)+":"+Base64.getEncoder().encodeToString(derive(password,salt,ITERATIONS));
    }
    public static boolean verify(String input,String hash){
        try {String[] p=hash.split(":");int iterations=Integer.parseInt(p[0]);
            byte[] salt=Base64.getDecoder().decode(p[1]);byte[] expected=Base64.getDecoder().decode(p[2]);
            return iterations>=50_000 && iterations<=1_000_000 && MessageDigest.isEqual(expected,derive(input,salt,iterations));
        }catch(Exception e){return false;}
    }
    private static byte[] derive(String pass,byte[] salt,int rounds){
        PBEKeySpec spec=new PBEKeySpec(pass.toCharArray(),salt,rounds,256);
        try {return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(Exception e){throw new IllegalStateException("Password hashing unavailable",e);}finally{spec.clearPassword();}
    }
    public static String newToken(){byte[] bytes=new byte[32];RANDOM.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public static String tokenDigest(String token){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);}catch(Exception e){throw new IllegalStateException(e);}}
}
