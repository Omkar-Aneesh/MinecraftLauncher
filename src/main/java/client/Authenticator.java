package client;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Scanner;

public class Authenticator {
    String secret = "";

    public String fetchJson(String urlStr){
        try {
            URL url = new URL(urlStr);
            InputStream is = url.openStream();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void getSecret() throws FileNotFoundException {
        File file = new File("githubSecret");

        Scanner scanner = new Scanner(file);

        secret = scanner.nextLine();
    }

    public String getFileSha(){
        try {
            String apiUrl = "https://api.github.com/repos/Omkar-Aneesh/authenticationSystem/contents/manifest.json";

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
            String apiUrl = "https://api.github.com/repos/Omkar-Aneesh/authenticationSystem/contents/manifest.json";

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
}
