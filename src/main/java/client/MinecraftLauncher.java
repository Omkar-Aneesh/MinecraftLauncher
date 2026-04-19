package client;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.print.attribute.HashAttributeSet;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MinecraftLauncher {
    static String MC_DIR = "minecraft";

    static String username;
    static String version;
    static String assetIndex;
    static UUID uuid;
    static Path nativesDir;
    static String classpath;

    public static String currentSituationString = "";

    public void run(String version, String username) {
        try {
            launch(version, username);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static void launch(String v, String name) throws Exception{
        username = name;
        version = v;

        currentSituationString = "Finding Version";

        Path versionDir = Paths.get(MC_DIR, "versions", version);
        JSONObject versionJson = new JSONObject( Files.readString(versionDir.resolve(version + ".json")) );

        JSONObject client = versionJson.getJSONObject("downloads").getJSONObject("client");

        String url = client.getString("url");
        String sha1 = client.getString("sha1");

        currentSituationString = "Finding Jar File";

        Path jarPath = Paths.get(MC_DIR, "versions", version, version + ".jar");

        if (!HashUtil.verifyFile(jarPath, sha1)){
            FileDownloader.downloadFile(url, jarPath);
        }

        uuid = UUID.nameUUIDFromBytes(username.getBytes());

        nativesDir = Paths.get(MC_DIR, "versions", version, "natives");
        Files.createDirectories(nativesDir);

        extractNatives(versionJson, nativesDir);

        currentSituationString = "Building Classpath";

        classpath = buildClasspath(versionJson, version);

        String mainClass = versionJson.getString("mainClass");

        assetIndex = versionJson.getJSONObject("assetIndex").getString("id");

        List<String> command = new ArrayList<>();

        currentSituationString = "Adding Commands";

        command.add("java");

        currentSituationString = "Adding JVM Arguments";

        if (versionJson.has("arguments")){
            JSONObject args = versionJson.getJSONObject("arguments");

            if (args.has("jvm")){
                addArguments(command, args.getJSONArray("jvm"));
            }
        }

        currentSituationString = "Overriding Minecraft Things";

        command.add("-Xms2G");

        command.add("-Djava.library.path=" + nativesDir.toAbsolutePath());

        command.add("-Dminecraft.api.session.host=http://localhost:8080");

        command.add("-cp");
        command.add(classpath);

        command.add(mainClass);

//        if (versionJson.has("arguments")){
//            JSONObject args = versionJson.getJSONObject("arguments");
//
//            if (args.has("game")){
//                addArguments(command, args.getJSONArray("game"));
//            }
//        }

        currentSituationString = "Setting Up Game Arguments";

        command.add("--username");
        command.add(username);

        command.add("--uuid");
        command.add(uuid.toString());

        command.add("--accessToken");
        command.add("0");

        command.add("--version");
        command.add(version);

        command.add("--gameDir");
        command.add(MC_DIR);

        command.add("--assetsDir");
        command.add(Paths.get(MC_DIR, "assets").toString());

        command.add("--assetIndex");
        command.add(assetIndex);

        command.add("--userType");
        command.add("mojang");

//        command.add("--server");
//        command.add("127.0.0.1");
//        command.add("--port");
//        command.add("25565");

//        command.add("--demo");
//        command.add("false");

        currentSituationString = "Launching Minecraft";

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        pb.start();

    }

    public static String buildClasspath(JSONObject versionJson, String version){
        StringBuilder cp = new StringBuilder();

        String sep = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";

        JSONArray libs = versionJson.getJSONArray("libraries");

        for (int i = 0; i < libs.length(); i ++){
            JSONObject lib = libs.getJSONObject(i);

            if (lib.has("rules")){
                if (!checkRules(lib.getJSONArray("rules"))){
                    continue;
                }
            }

            if (!lib.has("downloads")) continue;
            JSONObject downloads = lib.getJSONObject("downloads");

            if (!downloads.has("artifact")) continue;

            JSONObject artifact = downloads.getJSONObject("artifact");

            String path = artifact.getString("path");
            String sha1 = artifact.getString("sha1");
            String url = artifact.getString("url");

            Path libPath = Paths.get(MC_DIR, "libraries", path);

            if (!HashUtil.verifyFile(libPath, sha1)){
                System.out.println("Downloading / Fixing: " + path);
                FileDownloader.downloadFile(url, libPath);

                if (!HashUtil.verifyFile(libPath, sha1)){
                    throw new RuntimeException("Corrupted download: " + path);
                }
            }

            if (Files.exists(libPath)){
                System.out.println("Adding to classpath: " + libPath);
                cp.append(libPath.toAbsolutePath()).append(sep);
            }
        }

        Path jar = Paths.get(MC_DIR, "versions", version, version + ".jar");
        cp.append(jar.toAbsolutePath());

        return cp.toString();
    }
    public static void extractNatives(JSONObject versionJson, Path nativesDir) throws IOException {

        String os = getOS();
        JSONArray libs = versionJson.getJSONArray("libraries");

        for (int i = 0; i < libs.length(); i ++){
            JSONObject lib = libs.getJSONObject(i);

            if (!lib.has("downloads")) continue;
            JSONObject downloads = lib.getJSONObject("downloads");

            if (!downloads.has("classifiers")) continue;

            JSONObject classifiers = downloads.getJSONObject("classifiers");

            String key = "natives-" + os;
            if (!classifiers.has(key)) continue;

            JSONObject nativeObj = classifiers.getJSONObject(key);
            Path jarPath = Paths.get(MC_DIR, "libraries", nativeObj.getString("path"));

            if (!Files.exists(jarPath)) continue;

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jarPath))){
                ZipEntry entry;

                while ((entry = zis.getNextEntry()) != null){
                    if (entry.getName().startsWith("META-INF")) continue;

                    Path out = nativesDir.resolve(entry.getName());

                    if (entry.isDirectory()){
                        Files.createDirectories(out);
                    } else {
                        Files.createDirectories(out.getParent());
                        Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    static boolean checkRules(JSONArray rules){
        String os = getOS();

        boolean allow = false;

        for (int i = 0; i < rules.length(); i ++){
            JSONObject rule = rules.getJSONObject(i);

            String action = rule.getString("action");

            if (!rule.has("os")){
                allow = action.equals("allow");
                continue;
            }

            JSONObject osObj = rule.getJSONObject("os");

            if (!osObj.has("name")) continue;

            String ruleOS = osObj.getString("name");

            if (ruleOS.equals(os)){
                allow = action.equals("allow");
            }
        }

        return allow;
    }

    public static void addArguments(List<String> cmd, JSONArray argsJson){
        for (int i = 0; i < argsJson.length(); i ++){
            Object obj = argsJson.get(i);

            if (obj instanceof String){
                String argStr = replaceVars((String) obj);
                if (argStr.contains("quickPlay")) continue;
                cmd.add(argStr);
            } else {
                JSONObject arg = (JSONObject) obj;

                if (arg.has("rules")){
                    if (!checkRules(arg.getJSONArray("rules"))) continue;
                }

                if (arg.has("value")){
                    Object val = arg.get("value");

                    if (val instanceof String){
                        String argStr = replaceVars((String) val);
                        if (argStr.contains("quickPlay")) continue;
                        cmd.add(argStr);
                    } else if (val instanceof JSONArray){
                        JSONArray arr = (JSONArray) val;
                        for (int j = 0; j < arr.length(); j ++){
                            String argStr = replaceVars(arr.getString(j));
                            if (argStr.contains("quickPlay")) continue;
                            cmd.add(argStr);
                        }
                    }
                }
            }
        }
    }

    static String replaceVars(String arg) {

        return arg
                .replace("${auth_player_name}", username)
                .replace("${version_name}", version)
                .replace("${game_directory}", MC_DIR)
                .replace("${assets_root}", Paths.get(MC_DIR, "assets").toString())
                .replace("${assets_index_name}", assetIndex)
                .replace("${auth_uuid}", uuid.toString())
                .replace("${auth_access_token}", "0")
                .replace("${user_type}", "mojang")
                .replace("${natives_directory}", nativesDir.toAbsolutePath().toString())
                .replace("${launcher_name}", "CustomLauncher")
                .replace("${launcher_version}", "1.0")
                .replace("${classpath}", classpath);
    }
    static JSONObject resolveVersionJson(String version) throws Exception {
        Path versionDir = Paths.get(MC_DIR, "versions", version);
        JSONObject json = new JSONObject(
                Files.readString(versionDir.resolve(version + ".json"))
        );

        if (json.has("inheritsFrom")) {
            String parent = json.getString("inheritsFrom");
            JSONObject parentJson = resolveVersionJson(parent);

            // merge libraries
            JSONArray parentLibs = parentJson.getJSONArray("libraries");
            JSONArray childLibs = json.getJSONArray("libraries");

            for (int i = 0; i < childLibs.length(); i++) {
                parentLibs.put(childLibs.get(i));
            }

            parentJson.put("libraries", parentLibs);

            // override mainClass if exists
            if (json.has("mainClass")) {
                parentJson.put("mainClass", json.getString("mainClass"));
            }

            return parentJson;
        }

        return json;
    }

    static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) return "windows";
        if (os.contains("mac")) return "osx";
        return "linux";
    }


}
