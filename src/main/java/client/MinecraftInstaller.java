package client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MinecraftInstaller {

    static String MC_DIR = "minecraft";

    ProgressListener listener;

    String currentSituationString = "";

    public void setProcessListener(ProgressListener listener){
        this.listener = listener;
    }

    public String install(String version) throws Exception {
        String outString = "";

        currentSituationString = "Fetching Versions";

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

        currentSituationString = "Creating Directories";

        Path versionDir = Paths.get(MC_DIR, "versions", version);
        Files.createDirectories(versionDir);
        MC_DIR = "minecraft/versions/" + version + "/res";
        Files.createDirectories(Paths.get(MC_DIR, "libraries"));
        Files.createDirectories(Paths.get(MC_DIR, "assets", "indexes"));
        Files.createDirectories(Paths.get(MC_DIR, "assets", "objects"));

        Files.write(versionDir.resolve(version + ".json"), versionJson.toString(2).getBytes());

        currentSituationString = "Fetching Jar File";

        String jarUrl = versionJson.getJSONObject("downloads").getJSONObject("client").getString("url");
        download(jarUrl, versionDir.resolve(version + ".jar"));

        currentSituationString = "Downloading Dependencies";

        JSONArray libs = versionJson.getJSONArray("libraries");
        for (int i = 0; i < libs.length(); i ++){
            JSONObject lib = libs.getJSONObject(i);

            currentSituationString = "Downloading Dependencies.";

            if (!lib.has("downloads")) continue;
            JSONObject downloads = lib.getJSONObject("downloads");

            if (!downloads.has("artifact")) continue;
            JSONObject artifact = downloads.getJSONObject("artifact");

            String url = artifact.getString("url");
            String path = artifact.getString("path");

            currentSituationString = "Downloading Dependencies..";

            Path out = Paths.get(MC_DIR, "libraries", path);
            Files.createDirectories(out.getParent());

            download(url, out);

            currentSituationString = "Downloading Dependencies...";
        }

        JSONObject assetIndex = versionJson.getJSONObject("assetIndex");
        String assetUrl = assetIndex.getString("url");
        String assetId = assetIndex.getString("id");

        Path assetIndexPath = Paths.get(MC_DIR, "assets", "indexes", assetId + ".json");
        download(assetUrl, assetIndexPath);

        JSONObject assetJson = new JSONObject(Files.readString(assetIndexPath));

        JSONObject objects = assetJson.getJSONObject("objects");

        ExecutorService pool = Executors.newFixedThreadPool(8);

        currentSituationString = "Downloading Everything";

        AtomicInteger done = new AtomicInteger(0);
        int total = objects.length();

        AtomicLong bytesDownloaded = new AtomicLong(0);
        long startTime = System.currentTimeMillis();

        currentSituationString = "";
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
        System.out.println("Minecraft " + version + " Installed!");

        MC_DIR = "minecraft";

        return outString;
    }

    public JSONObject readJson(String url) throws Exception {
        try (InputStream in = new URL(url).openStream()){
            return new JSONObject(new String(in.readAllBytes()));
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

    public static void main(String[] args) throws Exception {
        MinecraftInstaller mi = new MinecraftInstaller();
        mi.install("1.20.2");
    }
}
