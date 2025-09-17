package com.apitest.utils;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;

/**
 * @Author baigq-a
 * @Description
 * @createTime 2024年04月10日 11:12:00
 */
public class Base64Utils {
    public static String generateKeySecretBase64(String toBeEncoded) {
        try {
            String keySecretBase64 = new String(Base64.encodeBase64String(toBeEncoded.getBytes("UTF-8")));
            return keySecretBase64.replace("\r\n", "");
        } catch (UnsupportedEncodingException var3) {
            throw new IllegalStateException("Could not convert String");
        }
    }
}
