package client;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Scanner;
import java.util.UUID;

public class SkinUploadHandler {
    String secret = "";

    public String convertToBase64(String path){
        try {
            byte[] fileBytes = Files.readAllBytes(Path.of(path));
            String encoded = Base64.getEncoder().encodeToString(fileBytes);
            return encoded;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void getSecret() throws FileNotFoundException {
        File file = new File("githubSecret");

        Scanner scanner = new Scanner(file);

        secret = scanner.nextLine();
    }

    public String fetchJson(String urlStr){
        try {
            URL url = new URL(urlStr);
            InputStream is = url.openStream();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public String getFileSha(){
        try {
            String apiUrl = "https://api.github.com/repos/Omkar-Aneesh/SkinSystem/contents/manifest.json";

            var conn = (HttpURLConnection) new URL(apiUrl).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + secret);

            String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            JSONObject obj = new JSONObject(response);
            return obj.getString("sha");
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void pushManifest(JSONObject object, String sha){
        try {
            String apiUrl = "https://api.github.com/repos/Omkar-Aneesh/SkinSystem/contents/manifest.json";

            String encoded = Base64.getEncoder().encodeToString(object.toString(4).getBytes(StandardCharsets.UTF_8));

            String json = "{"
                    + "\"message\":\"upload skin\","
                    + "\"content\":\"" + encoded + "\","
                    + "\"sha\":\"" + sha + "\""
                    + "}";

            var conn = (HttpURLConnection) new URL(apiUrl).openConnection();

            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Authorization", "Bearer " + secret);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes());
            os.close();

            int responseCode = conn.getResponseCode();
            System.out.println("Manifest update response: " + responseCode);

            InputStream is = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            System.out.println(new String(is.readAllBytes()));
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void uploadSkin(String uuid){
        try {
            getSecret();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        String jsonStr = fetchJson("https://raw.githubusercontent.com/Omkar-Aneesh/SkinSystem/main/manifest.json");

        JSONObject obj = new JSONObject(jsonStr);

        if (!obj.has(uuid)) {
            try {
                String apiUrl = "https://api.github.com/repos/Omkar-Aneesh/SkinSystem/contents/" + uuid + ".png";

                String encoded = convertToBase64("minecraft/toUpload/skin.png");

                String json = "{"
                        + "\"message\":\"upload skin\","
                        + "\"content\":\"" + encoded + "\""
                        + "}";

                obj.put(uuid, "https://raw.githubusercontent.com/Omkar-Aneesh/SkinSystem/main/" + uuid + ".png");

                String sha = getFileSha();
                pushManifest(obj, sha);

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Authorization", "Bearer " + secret);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.getBytes());

                int responseCode = conn.getResponseCode();
                System.out.println("Response:" + responseCode);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static void main(String[] args) {
        SkinUploadHandler skinUploadHandler = new SkinUploadHandler();
        skinUploadHandler.uploadSkin(UUID.nameUUIDFromBytes("Aneesh015".getBytes()).toString().replace("-", ""));
    }
}
