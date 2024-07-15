package com.app.MailService.Utilities;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESHelper {
    private static final String AES = "AES";
    private static final String AES_CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16; // 16 bytes for AES/CBC IV
    private static final int KEY_LENGTH = 32; // 32 bytes for AES-256 key

    // Method to encrypt a plain text
    private static String encrypt(String plainText, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String encrypt(String plainText, String key, String iv) throws Exception {
        return encrypt(plainText, getKeyFromString(key), getIvFromString(iv));
    }

    // Method to decrypt a cipher text
    private static String decrypt(String cipherText, SecretKey key, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decodedCipherText = Base64.getDecoder().decode(cipherText);
        byte[] plainTextBytes = cipher.doFinal(decodedCipherText);
        return new String(plainTextBytes, StandardCharsets.UTF_8);
    }

    public static String decrypt(String cipherText, String key, String iv) throws Exception {
        return decrypt(cipherText, getKeyFromString(key), getIvFromString(iv));
    }

    // Method to convert a string to a SecretKey
    public static SecretKey getKeyFromString(String key) {
        if (key.length() != KEY_LENGTH) {
            throw new IllegalArgumentException("Key length must be 32 characters (256 bits).");
        }
        return new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), AES);
    }

    // Method to convert a string to an IV
    public static IvParameterSpec getIvFromString(String iv) {
        if (iv.length() != IV_LENGTH) {
            throw new IllegalArgumentException("IV length must be 16 characters (128 bits).");
        }
        return new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
    }
}
