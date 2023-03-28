package com.del.pst.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.del.pst.dao.DBItem;
import com.google.android.gms.common.util.Base64Utils;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utils {

    public final static String KS_ALIAS = "com.del.pst.key";

    public static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;
        String phrase = "";
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase += Character.toUpperCase(c);
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase += c;
        }
        return phrase;
    }

    public static String translateSign(String encodeMd5, byte[] digestOfPasswordMd5, byte[] digestOfPassword) throws Exception {
        String origin = decodeValue(encodeMd5, digestOfPasswordMd5);
        return encodeValue(origin, digestOfPassword);
    }

    public static String encodeValue(String p, String _key) throws Exception {
        return encodeValue(p, _key, false);
    }

    public static String encodeValue(String p, String _key, boolean md5) throws Exception {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, _key, md5);
        byte[] plainTextBytes = p.getBytes(StandardCharsets.UTF_8);
        return Base64Utils.encode(cipher.doFinal(plainTextBytes));
    }

    public static byte[] encodeData(String p, String _key, boolean md5) throws Exception {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, _key, md5);
        byte[] plainTextBytes = p.getBytes(StandardCharsets.UTF_8);
        return cipher.doFinal(plainTextBytes);
    }

    public static String encodeValue(String p, byte[] digestOfPassword) throws Exception {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, digestOfPassword);
        byte[] plainTextBytes = p.getBytes(StandardCharsets.UTF_8);
        return Base64Utils.encode(cipher.doFinal(plainTextBytes));
    }

    public static String decodeValue(String p, String _key) throws Exception {
        return decodeValue(p, _key, false);
    }

    public static byte[] decodeData(byte[] p, String _key, boolean md5) throws Exception {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, _key, md5);
        return cipher.doFinal(p);
    }

    public static String decodeValue(String p, String _key, boolean md5) throws Exception {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, _key, md5);
        return new String(cipher.doFinal(Base64Utils.decode(p)), StandardCharsets.UTF_8);
    }

    public static String decodeValueORNull(String p, byte[] digestOfPassword) {
        try {
            return decodeValue(p, digestOfPassword);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decodeValue(String p, byte[] digestOfPassword) throws Exception {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, digestOfPassword);
        return new String(cipher.doFinal(Base64Utils.decode(p)), StandardCharsets.UTF_8);
    }

    private static Cipher getCipher(int opmode, String _key, boolean md5) throws Exception {
        byte[] digestOfPassword = getDigestOfPassword(_key, md5);
        return getCipher(opmode, digestOfPassword);
    }

    private static Cipher getCipher(int opmode, byte[] digestOfPassword) throws Exception {
        SecretKey key = new SecretKeySpec(digestOfPassword, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(new byte[16]);
        cipher.init(opmode, key, iv);
        return cipher;
    }

    public static boolean init() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(KS_ALIAS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore");
                keyGenerator.init(new KeyGenParameterSpec.Builder(KS_ALIAS, KeyProperties.PURPOSE_SIGN).build());
                SecretKey key = keyGenerator.generateKey();
                Mac mac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
                mac.init(key);
            } else {
                return false;
            }
        }
        return true;
    }

    public static byte[] getDigestOfPassword(String text, boolean md5) throws Exception {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] data = null;
        if (!md5) {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(KS_ALIAS)) {
                SecretKey key = (SecretKey) keyStore.getKey(KS_ALIAS, null);
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(key);
                mac.update(textBytes);
                data = mac.doFinal();
            }
        }
        if (data == null) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            data = md.digest(textBytes);
        }
        return Arrays.copyOf(data, 16);
    }

    public static void printMac(String text) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KS_ALIAS)) {
            SecretKey key = (SecretKey) keyStore.getKey(KS_ALIAS, null);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            mac.update(text.getBytes(StandardCharsets.UTF_8));
            Log.d("TEST", String.format("%s [%s]", text, Base64Utils.encode(mac.doFinal())));
        } else {
            Log.d("TEST", "Secret key not found");
        }
    }

    public static boolean valid(DBItem dbItem, byte[] digestOfPassword) {
        try {
            return Objects.equals(
                    Utils.decodeValue(dbItem.getCheck(), digestOfPassword),
                    DBItem.CHECK_ORIG
            );
        } catch (Exception e) {
            //
        }
        return false;
    }

    public static String writeToFile(byte[] data) throws IOException {
        File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(directory, String.format("export_%s_.db", System.currentTimeMillis()));
        if (file.exists() || file.createNewFile()) {
            FileOutputStream fout = new FileOutputStream(file);
            fout.write(data);
            fout.close();
        } else {
            throw new IOException("Не удалось сохранить файл");
        }
        return file.getAbsolutePath();
    }

    public static byte[] readFile(Context ctx, Uri uri) throws IOException {
        InputStream is = ctx.getContentResolver().openInputStream(uri);
        ReadableByteChannel src = Channels.newChannel(is);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WritableByteChannel dest = Channels.newChannel(out);
        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
        transfer(src, dest, buffer);
        return out.toByteArray();
    }

    private static void transfer(ReadableByteChannel src,
                                 WritableByteChannel dest,
                                 ByteBuffer buffer) throws IOException {
        try {
            while (src.read(buffer) != -1) {
                buffer.flip();
                dest.write(buffer);
                buffer.compact();
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                dest.write(buffer);
            }
        } finally {
            safeClose(dest);
            safeClose(src);
        }
    }

    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                //
            }
        }
    }

    public static void showError(View view, String text, Exception e) {
        String message = "";
        if (text != null) {
            message = text;
        }
        if (e != null) {
            if (message.length() > 0) message += ": ";
            message += e.getMessage();
        }
        Log.e("ERROR", message, e);
        Snackbar.make(view, message, 5000).show();
    }

}
