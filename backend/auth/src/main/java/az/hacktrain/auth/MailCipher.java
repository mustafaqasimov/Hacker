package az.hacktrain.auth;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
@Component
class MailCipher {
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();
    MailCipher(AuthProperties props) { key = new SecretKeySpec(Base64.getDecoder().decode(props.mailEncryptionKey()), "AES"); }
    String encrypt(String body) {
        byte[] iv = new byte[12]; random.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,iv));
            byte[] encrypted = cipher.doFinal(body.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length+encrypted.length).put(iv).put(encrypted).array());
        } catch (Exception e) { throw new IllegalStateException("Mail encryption failed",e); }
    }
    String decrypt(String encoded) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(encoded));
            byte[] iv = new byte[12]; buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()]; buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
            return new String(cipher.doFinal(ciphertext),StandardCharsets.UTF_8);
        } catch (Exception e) { throw new IllegalStateException("Mail decryption failed",e); }
    }
}
