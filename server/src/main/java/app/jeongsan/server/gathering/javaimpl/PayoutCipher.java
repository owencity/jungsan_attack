package app.jeongsan.server.gathering.javaimpl;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Java는 학습 경로로만 사용하여 Spring Bean을 중복 등록하지 않는다. */
public final class PayoutCipher {
    private final SecretKeySpec key;
    public PayoutCipher(String encoded) {
        byte[] raw=Base64.getDecoder().decode(encoded);
        if(raw.length!=32) throw new IllegalArgumentException("32바이트 키가 필요하다");
        key=new SecretKeySpec(raw,"AES");
    }
    public byte[] encrypt(String text) throws java.security.GeneralSecurityException {
        byte[] nonce=new byte[12];new SecureRandom().nextBytes(nonce);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce));
        byte[] encrypted=cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        byte[] result=Arrays.copyOf(nonce,nonce.length+encrypted.length);
        System.arraycopy(encrypted,0,result,nonce.length,encrypted.length);return result;
    }
    public String decrypt(byte[] bytes) throws java.security.GeneralSecurityException {
        if(bytes.length<28) throw new IllegalArgumentException("암호문이 짧다");
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Arrays.copyOfRange(bytes,0,12)));
        return new String(cipher.doFinal(Arrays.copyOfRange(bytes,12,bytes.length)),StandardCharsets.UTF_8);
    }
}
