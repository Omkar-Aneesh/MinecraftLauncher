package client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MinecraftInstaller {

    static String MC_DIR = "minecraft";

    public void install(String version) throws Exception {
        JSONObject manifest = readJson("https://launchermeta.mojang.com/mc/game/version_manifest.json");

        String versionUrl = null;
        for (Object o: manifest.getJSONArray("versions")){
            JSONObject v = (JSONObject) o;
            if (v.getString("id").equals(version)){
                versionUrl = v.getString("url");
                break;
            }
        }

        JSONObject versionJson = readJson(versionUrl);

        Path versionDir = Paths.get(MC_DIR, "versions", version);
        Files.createDirectories(versionDir);
        Files.createDirectories(Paths.get(MC_DIR, "libraries"));
        Files.createDirectories(Paths.get(MC_DIR, "assets", "indexes"));
        Files.createDirectories(Paths.get(MC_DIR, "assets", "objects"));

        Files.write(versionDir.resolve(version + ".json"), versionJson.toString(2).getBytes());

        String jarUrl = versionJson.getJSONObject("downloads").getJSONObject("client").getString("url");
        download(jarUrl, versionDir.resolve(version + ".jar"));

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

        JSONObject assetIndex = versionJson.getJSONObject("assetIndex");
        String assetUrl = assetIndex.getString("url");
        String assetId = assetIndex.getString("id");

        Path assetIndexPath = Paths.get(MC_DIR, "assets", "indexes", assetId + ".json");
        download(assetUrl, assetIndexPath);

        JSONObject assetJson = new JSONObject(Files.readString(assetIndexPath));

        JSONObject objects = assetJson.getJSONObject("objects");

        ExecutorService pool = Executors.newFixedThreadPool(8);

        for (String key: objects.keySet()){
            JSONObject obj = objects.getJSONObject(key);

            String hash = obj.getString("hash");
            String sub = hash.substring(0, 2);

            String url = "https://resources.download.minecraft.net/" + sub + "/" + hash;
            Path out = Paths.get(MC_DIR, "assets", "objects", sub, hash);

            AtomicInteger counter = new AtomicInteger(0);
            int total = objects.length();

            pool.submit(() -> {
                try{
                    Files.createDirectories(out.getParent());
                    download(url, out);
                    int done = counter.incrementAndGet();
                    System.out.println("Progress: " + done + "/" + total);
                } catch (Exception e){
                    System.out.println("Failed: " + url);
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        System.out.println("Minecraft " + version + " Installed!");
    }

    public JSONObject readJson(String url) throws Exception {
        try (InputStream in = new URL(url).openStream()){
            return new JSONObject(new String(in.readAllBytes()));
        }
    }

    public static void download(String url, Path path) throws Exception {
        if (Files.exists(path)) return;

        try (InputStream in = new BufferedInputStream(new URL(url).openStream())){
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void main(String[] args) throws Exception {
        MinecraftInstaller mi = new MinecraftInstaller();
        mi.install("1.20.2");
    }
}
