package client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class SkinServer {
    static java.util.Map<String, String> skinMap = new java.util.HashMap<>();

    static Authenticator authenticator = new Authenticator();

    static void loadSkinMap() {
        try {
            Path path = Path.of("minecraft", "skins", "map.json");

            String json = Files.readString(path);
            JSONObject object = new JSONObject(json);

            for (String key: object.keySet()){
                skinMap.put(key, object.getString(key));
            }

            System.out.println("Loaded:" + skinMap.size());
        } catch (Exception e){
            System.out.println("failed");
            throw new RuntimeException(e);
        }
    }

    public void launch(){
        loadSkinMap();
        try {
            int port = 8080;

            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
//            server.createContext("/skins", new SkinHandler());
            server.createContext("/", new RootHandler());
//            server.createContext("/https/sessionserver.mojang.com", new ProfileHandler());
//            server.createContext("/https/api.minecraftservices.com", new ProfileHandler());

            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();

            System.out.println("started");
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) {
        SkinServer skinServer = new SkinServer();
        skinServer.launch();
    }

    static class RootHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try (httpExchange) {

                String path = httpExchange.getRequestURI().getPath();
                System.out.println(httpExchange.getRequestURI());

                if (path.contains("/session/minecraft/profile/")) {
                    new ProfileHandler().handle(httpExchange);
                    return;
                }

                if (path.startsWith("/skins/")) {
                    new SkinHandler().handle(httpExchange);
                    return;
                }

                String response = "{"
                        + "\"meta\": {"
                        + "  \"serverName\": \"CustomSkinServer\","
                        + "  \"implementationName\": \"CustomAuth\","
                        + "  \"implementationVersion\": \"1.0\","
                        + "  \"links\": {"
                        + "    \"homepage\": \"http://localhost:8080/\","
                        + "    \"register\": \"http://localhost:8080/\""
                        + "  }"
                        + "},"
                        + "\"skinDomains\": [\"localhost\"]"
                        + "}";

                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                httpExchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = httpExchange.getResponseBody();) {
                    os.write(response.getBytes());
                    os.close();
                }
            } catch (Exception e){
                e.printStackTrace();
                httpExchange.sendResponseHeaders(500, -1);
            }
        }
    }

    static class ProfileHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try (httpExchange) {
                System.out.println("RAW PATH: " + httpExchange.getRequestURI());

                String path = httpExchange.getRequestURI().getPath();

                if (!path.contains("/session/minecraft/profile/")) {
                    httpExchange.sendResponseHeaders(404, -1);
                    return;
                }

                String[] parts = path.split("/");
                String uuid = parts[parts.length - 1];

                System.out.println("UUID request: " + uuid);

                if (!skinMap.containsKey(uuid)) {
                    new Thread(() -> {
                        try {
                            String url = getSkinUrl(uuid);
                            downloadFile(url, "minecraft/skins/" + uuid + ".png");
                            updateSkinMap(uuid, uuid + ".png");
                        } catch (Exception e) {
                            System.err.println("Async download failed: " + e.getMessage());
                        }
                    }).start();

                    httpExchange.sendResponseHeaders(204, -1);
                    httpExchange.close();
                    return;
                }

                String fileName = skinMap.getOrDefault(uuid, "steve.png");
                String skinUrl = "http://localhost:8080/skins/" + fileName;

                String username = authenticator.authenticate(uuid);

                String textureJson = "{"
                        + "\"timestamp\":" + System.currentTimeMillis() + ","
                        + "\"profileId\":\"" + uuid + "\","
                        + "\"profileName\":\"" + username + "\","
                        + "\"signatureRequired\":false,"
                        + "\"textures\":{"
                        + "\"SKIN\":{"
                        + "\"url\":\"" + skinUrl + "\""
                        + "}"
                        + "}"
                        + "}";

                System.out.println("TEXTURE JSON: " + textureJson);

                String encoded = java.util.Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));

                String response = "{"
                        + "\"id\":\"" + uuid + "\","
                        + "\"name\":\"" + username + "\","
                        + "\"properties\":[{"
                        + "\"name\":\"textures\","
                        + "\"value\":\"" + encoded + "\""
                        + "}]"
                        + "}";

                System.out.println("Skin URL: " + skinUrl);
                System.out.println("Encoded length: " + encoded.length());

                httpExchange.getResponseHeaders().add("Content-Type", "application/json");
                httpExchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = httpExchange.getResponseBody();) {
                    os.write(response.getBytes());
                    os.close();
                }
            } catch (Exception e){
                e.printStackTrace();
                httpExchange.sendResponseHeaders(500, -1);
            }
        }

        public void updateSkinMap(String uuid, String fileName){
            try {
                Path path = Path.of("minecraft", "skins", "map.json");

                JSONObject object;

                if (Files.exists(path)){
                    String json = Files.readString(path);
                    object = new JSONObject(json);
                } else {
                    object = new JSONObject();
                }

                object.put(uuid, fileName);

                Files.writeString(path, object.toString(4));

                skinMap.put(uuid, fileName);

                System.out.println("updated");
            } catch (Exception e){
                throw new RuntimeException(e);
            }
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

        public String getSkinUrl(String uuid){
            String jsonStr = fetchJson("https://raw.githubusercontent.com/Omkar-Aneesh/SkinSystem/refs/heads/main/manifest.json");

            JSONObject obj = new JSONObject(jsonStr);
            String skinUrl = obj.getString(uuid);

            System.out.println("url:" + skinUrl);

            return skinUrl;
        }

        public void downloadFile(String fileUrl, String savePath){
            try (InputStream in = new URL(fileUrl).openStream()){
                Files.copy(in, Paths.get(savePath), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    static class SkinHandler implements HttpHandler{

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try (httpExchange) {
                String path = httpExchange.getRequestURI().getPath();

                String fileName = path.replace("/skins/", "");
                Path filePath = Path.of("minecraft/skins", fileName);

                if (!Files.exists(filePath)) {
                    String response = "Skin not found";
                    httpExchange.sendResponseHeaders(404, -1);
//                httpExchange.getResponseBody().write(response.getBytes());
                    httpExchange.close();
                    return;
                }

                byte[] fileBytes = Files.readAllBytes(filePath);

                httpExchange.getResponseHeaders().add("Content-Type", "image/png");
                httpExchange.sendResponseHeaders(200, fileBytes.length);

                try (OutputStream os = httpExchange.getResponseBody();) {
                    os.write(fileBytes);
                    os.close();
                }

                httpExchange.close();
            } catch (Exception e){
                httpExchange.sendResponseHeaders(500, -1);
            }
        }
    }
}
