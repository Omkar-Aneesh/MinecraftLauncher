package client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ForgeRunner {

    public String currentSituationString = "Initializing Forge";

    public static void main(String[] args) {
        ForgeRunner forgeRunner = new ForgeRunner();
        forgeRunner.run("1.21.11", "61.1.0");
    }

    public void deleteInstaller(){
        File installer = new File("minecraft/forge-installer.jar");

        if (installer.exists()){
            boolean deleted = installer.delete();

            if (deleted){
                System.out.println("Deleted");
            }
        }
    }

    public void run(String mcVersion, String forgeVersion){
//        String versionId = mcVersion + "-forge-" + forgeVersion;
//
        String mcDir = "minecraft"; // root folder
//        String installerPath = mcDir + "/forge-installer.jar";
//
//        // temp file
//        String tempJson = mcDir + "/temp_version.json";
//
//        // 1. Extract version.json
//        extractVersionJson(installerPath, tempJson);
//
//        // 2. Create version folder
//        File versionDir = createVersion(mcDir, versionId);
//
//        // 3. Fix JSON
//        String finalJson = versionDir.getAbsolutePath() + "/" + versionId + ".json";
//        fix(tempJson, finalJson, versionId, mcVersion);
//
//        // 4. Download libraries
//        downloadLibraries(finalJson, mcDir);
//
//        // 5. Run installer (IMPORTANT)
        createLauncherProfile("minecraft");
        runJar(mcDir);

        System.out.println("✅ Forge setup complete");
    }

    public void extractVersionJson(String installerPath, String outputPath){
        try {
            JarFile jar = new JarFile(installerPath);

            JarEntry entry = jar.getJarEntry("version.json");

            if (entry == null){
                throw new RuntimeException("version.json not found");
            }

            InputStream is = jar.getInputStream(entry);
            FileOutputStream fos = new FileOutputStream(outputPath);

            is.transferTo(fos);

            fos.close();
            is.close();
            jar.close();

            System.out.println("Version Extraction Complete");

        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public File createVersion(String mcDir, String versionId){
        File dir = new File(mcDir + "/versions/" + versionId);

        if (!dir.exists()) dir.mkdirs();

        return dir;
    }

    public void fix(String input, String output, String newId, String mcVersion){
        try {
            String content = Files.readString(Path.of(input));

            JSONObject obj = new JSONObject(content);

            obj.put("id", newId);
            obj.put("inheritsFrom", mcVersion);

            Files.writeString(Path.of(output), obj.toString(4));

            System.out.println("version.json Modification Complete");
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void downloadLibraries(String versionJsonPath, String mcDir){
        try {
            String content = Files.readString(Path.of(versionJsonPath));
            JSONObject obj = new JSONObject(content);

            JSONArray libs = obj.getJSONArray("libraries");

            for (int i = 0; i < libs.length(); i ++){
                JSONObject lib = libs.getJSONObject(i);

                if (!lib.has("downloads")) continue;

                JSONObject artifact = lib.getJSONObject("downloads").optJSONObject("artifact");
                if (artifact == null) continue;

                if (!artifact.has("url")) continue;

                String url = artifact.getString("url");

                System.out.println(url);
                String path = artifact.getString("path");

                File file = new File(mcDir + "/libraries/" + path);
                file.getParentFile().mkdirs();

                download(url, file);
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void download(String urlStr, File file){
        try {
            if (file.exists()) return;

            URL url = new URL(urlStr);
            InputStream in = url.openStream();
            FileOutputStream out = new FileOutputStream(file);

            in.transferTo(out);

            out.close();
            in.close();

            System.out.println("Downloaded: " + file.getName());
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void createLauncherProfile(String mcDir){
        try {
            File file = new File(mcDir, "launcher_profiles.json");

            if (file.exists()) return;

            file.getParentFile().mkdirs();

            String json = """
                    {
                      "profiles": {
                        "forge": {
                          "name": "forge",
                          "type": "custom"
                        }
                      }
                    }
                    """;

            Files.writeString(file.toPath(), json);

            System.out.println("Created launcher_profiles.json");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void runJar(String mcDir){
        File file = new File(mcDir);

        currentSituationString = "Running Forge Installer";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-jar",
                    "forge-installer.jar",
                    "--installClient"
//                    "--mcDir",
//                    "minecraft"
            );

            pb.directory(new File(mcDir));

            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Forge] " + line);
                if (line.contains("Copying")){
                    currentSituationString = "Copying Data";
                }
                if (line.contains("Patching")){
                    currentSituationString = "Patching Minecraft";
                }
            }

            int exit = process.waitFor();

            currentSituationString = "Deleting Installer";

            deleteInstaller();

            currentSituationString = "";

//            if (exit != 0) {
//                throw new RuntimeException("Forge install failed!");
//            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
