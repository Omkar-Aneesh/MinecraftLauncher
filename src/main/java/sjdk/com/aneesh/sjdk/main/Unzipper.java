package sjdk.com.aneesh.sjdk.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Unzipper {
    public static void unzip(String zipFilePath, String destDir){
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null){
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()){
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)){
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = zis.read(buffer)) > 0){
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
