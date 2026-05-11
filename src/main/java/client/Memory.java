package client;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Memory {
    public static boolean offlineMode;

    public static void loadAll(){
        try {
            File file = new File("mem");

            if (!file.exists()){
                Files.createFile(Path.of("mem"));
            }

            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String value = scanner.nextLine();

                if (value.contains("offlineMode")){
                    String[] values = value.split("=");

                    offlineMode = Boolean.parseBoolean(values[1]);
                }
            }
            scanner.close();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void saveAll(){
        try {
            FileWriter fileWriter = new FileWriter("mem");

            String object = "offlineMode";
            String value = String.valueOf(offlineMode);

            String string = createString(object, value);

            fileWriter.write(string);
            fileWriter.close();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static String createString(String object, String value){
        return object + "=" + value + "\n";
    }
}
