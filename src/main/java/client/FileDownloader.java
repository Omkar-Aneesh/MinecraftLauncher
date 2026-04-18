package client;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileDownloader {
    public static void downloadFile(String url, Path dest) {
        try {
            Files.createDirectories(dest.getParent());

            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
