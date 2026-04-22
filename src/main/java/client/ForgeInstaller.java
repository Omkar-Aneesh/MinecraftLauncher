package client;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ForgeInstaller {

    ProgressListener listener;

    String currentSituationString = "";

    String MC_DIR = "minecraft";

    ArrayList<String> versionList = new ArrayList<>();
    Map<String, List<String>> forgeMap = new HashMap<>();

    public static void main(String[] args) {
        ForgeInstaller fi = new ForgeInstaller();
        fi.install("1.21.11");
    }

    public void setProcessListener(ProgressListener listener){
        this.listener = listener;
    }

    public String install(String version){
        currentSituationString = "Creating Directories";
        try {
            Files.createDirectories(Paths.get("minecraft/versions", version));
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        currentSituationString = "Installing Minecraft";

        installVanilla(version);

        currentSituationString = "Installing Forge";

        String forgeVersion = getBestForgeVersion(version);
        download(version, forgeVersion);

//        System.out.println(forgeVersion);

        return forgeVersion;
    }

    public void install(String version, String forgeVersion){
        currentSituationString = "Creating Directories";
        try {
            Files.createDirectories(Paths.get("minecraft/versions", version));
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        currentSituationString = "Installing Minecraft";

        installVanilla(version);

        currentSituationString = "Installing Forge";

        try {
            download(version, forgeVersion);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
//        System.out.println(forgeVersion);
    }

    public JSONObject readJson(String url) {
        try (InputStream in = new URL(url).openStream()){
            return new JSONObject(new String(in.readAllBytes()));
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void installVanilla(String version){
        try {
            JSONObject manifest = readJson("https://launchermeta.mojang.com/mc/game/version_manifest.json");

            String versionUrl = null;
            for (Object o: manifest.getJSONArray("versions")){
                JSONObject v = (JSONObject) o;
                if (v.getString("id").equals(version)){
                    versionUrl = v.getString("url");
                    break;
                }
            }

            currentSituationString = "Creating Directories";

            JSONObject versionJson = readJson(versionUrl);

            Path versionDir = Paths.get(MC_DIR, "versions", version);
            Files.createDirectories(versionDir);
//        MC_DIR = "minecraft/versions/" + version + "/res";
            Files.createDirectories(Paths.get(MC_DIR, "libraries"));
            Files.createDirectories(Paths.get(MC_DIR, "assets", "indexes"));
            Files.createDirectories(Paths.get(MC_DIR, "assets", "objects"));

            Files.write(versionDir.resolve(version + ".json"), versionJson.toString(2).getBytes());

//            String jarUrl = versionJson.getJSONObject("downloads").getJSONObject("client").getString("url");
//            download(jarUrl, versionDir.resolve(version + ".jar"));

            currentSituationString = "Downloading Dependencies";

            JSONArray libs = versionJson.getJSONArray("libraries");
            for (int i = 0; i < libs.length(); i ++){
                JSONObject lib = libs.getJSONObject(i);

                if (!lib.has("downloads")) continue;
                JSONObject downloads = lib.getJSONObject("downloads");

                if (!downloads.has("artifact")) continue;
                JSONObject artifact = downloads.getJSONObject("artifact");

                String url = artifact.getString("url");
                String path = artifact.getString("path");

                Path out = Paths.get(MC_DIR, "libraries", path);
                Files.createDirectories(out.getParent());

                download(url, out);
            }

            currentSituationString = "Setting Up Everything";

            JSONObject assetIndex = versionJson.getJSONObject("assetIndex");
            String assetUrl = assetIndex.getString("url");
            String assetId = assetIndex.getString("id");

            Path assetIndexPath = Paths.get(MC_DIR, "assets", "indexes", assetId + ".json");
            download(assetUrl, assetIndexPath);

            JSONObject assetJson = new JSONObject(Files.readString(assetIndexPath));

            JSONObject objects = assetJson.getJSONObject("objects");

            ExecutorService pool = Executors.newFixedThreadPool(8);

            AtomicInteger done = new AtomicInteger(0);
            int total = objects.length();

            AtomicLong bytesDownloaded = new AtomicLong(0);
            long startTime = System.currentTimeMillis();

            for (String key: objects.keySet()){
                JSONObject obj = objects.getJSONObject(key);

                String hash = obj.getString("hash");
                String sub = hash.substring(0, 2);

                String url = "https://resources.download.minecraft.net/" + sub + "/" + hash;
                Path out = Paths.get(MC_DIR, "assets", "objects", sub, hash);

                pool.submit(() -> {
                    try{
                        if (Files.exists(out)){
                            int current = done.incrementAndGet();
                            return;
                        }

                        Files.createDirectories(out.getParent());
                        download(url, out, bytesDownloaded);

                        int current = done.incrementAndGet();

                        long now = System.currentTimeMillis();
                        double seconds = (now - startTime) / 1000.0;

                        double speedMBps = (bytesDownloaded.get() / 1024.0 / 1024.0) / seconds;

                        double filePerSecond = current / seconds;
                        long eta = (long) ((total - current) / filePerSecond);

                        if (listener != null){
                            listener.onProgress(current, total, speedMBps, eta);
                        }
                    } catch (Exception e){
                        System.out.println("Failed: " + url);
                    }
                });
            }

//        System.out.println(done.get());

            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
//            System.out.println("Minecraft " + version + " Installed!");

            MC_DIR = "minecraft";
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        currentSituationString = "";
    }

    public void download(String version, String forgeVersion){
        try {
            URL url = new URL("https://maven.minecraftforge.net/net/minecraftforge/forge/" + version + "-" + forgeVersion + "/forge-"+ version + "-" + forgeVersion +"-installer.jar");
            URLConnection conn = url.openConnection();
            int fileSize = conn.getContentLength();

            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream("minecraft/forge-installer.jar");

            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalRead = 0;

            while ((bytesRead = in.read(buffer)) != -1){
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                int progress = (int)((totalRead * 100L) / fileSize);
//                System.out.println("downloading: " + progress + "%");
            }

            out.close();
            in.close();
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public String getBestForgeVersion(String version){
        String out = "";

        String versionKeyRecommended = version + "-recommended";
        String versionKeyLatest = version + "-latest";

        try {
            URL url = new URL("https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            StringBuilder json = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();

            JSONObject obj = new JSONObject(json.toString());
            JSONObject promos = obj.getJSONObject("promos");

            String forgeVersion;

            if (promos.has(versionKeyRecommended)){
                forgeVersion = promos.getString(versionKeyRecommended);
            } else {
                forgeVersion = promos.getString(versionKeyLatest);
            }

            out = forgeVersion;
        } catch (Exception e){
            throw new RuntimeException(e);
        }
        return out;
    }

    public void getAllVersionList(){

        try {
            URL url = new URL("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml");

             Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url.openStream());

            NodeList versionNodes = doc.getElementsByTagName("version");

            for (int i = 0; i < versionNodes.getLength(); i ++){
                String fullVersion = versionNodes.item(i).getTextContent();
                versionList.add(fullVersion);
            }

            for (int i = 0; i < versionNodes.getLength(); i ++){
                String full = versionNodes.item(i).getTextContent();

                int dash = full.indexOf('-');
                String mc = full.substring(0, dash);
                String forge = full.substring(dash + 1);

                forgeMap.computeIfAbsent(mc, k -> new ArrayList<>()).add(forge);
            }

        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void download(String url, Path path, AtomicLong globalBytes) throws Exception {
        if (Files.exists(path)) return;

        URL Url = new URL(url);
//        URLConnection connection = Url.openConnection();

        try (InputStream in = new BufferedInputStream(new URL(url).openStream());
             OutputStream out = Files.newOutputStream(path)){
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer, 0, buffer.length)) != -1){
                out.write(buffer, 0, bytesRead);
                globalBytes.addAndGet(bytesRead);

//                if (listener != null){
//                    listener.onProgress(downloaded, fileSize);
//                }

//                if (fileSize > 0){
//                    int percent = (int) ((downloaded * 100) / fileSize);
//
//                    long now = System.currentTimeMillis();
//
//                    if (now - lastPrintTime > 200){
//                        System.out.println("\rDownloading: " + percent + "%");
//                        lastPrintTime = now;
//                    }
//                } else {
//                    System.out.println("\rDownloaded: " + downloaded + "bytes");
//                }
            }
        }
//        System.out.println("\nDownload complete!");
    }

    public static void download(String url, Path path) throws Exception {
        if (Files.exists(path)) return;

        try (InputStream in = new BufferedInputStream(new URL(url).openStream())){
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
