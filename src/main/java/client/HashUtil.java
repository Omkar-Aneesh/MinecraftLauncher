package client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class HashUtil {
    public static String sha1(Path file) throws Exception{
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        try (InputStream is = Files.newInputStream(file)){
            byte[] buffer = new byte[8192];
            int read;

            while ((read = is.read(buffer)) != -1){
                digest.update(buffer, 0, read);
            }
        }

        byte[] hash = digest.digest();

        StringBuilder hex = new StringBuilder();

        for (byte b: hash){
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }

    public static boolean verifyFile(Path path, String expectedSha1){
        try {
            if (!Files.exists(path)) return false;

            String actual = HashUtil.sha1(path);

            return actual.equalsIgnoreCase(expectedSha1);
        } catch (Exception e){
            return false;
        }
    }
}
