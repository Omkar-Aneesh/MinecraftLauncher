package client;

import org.json.JSONObject;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeInstaller {

    ArrayList<String> versionList = new ArrayList<>();
    Map<String, List<String>> forgeMap = new HashMap<>();

    public static void main(String[] args) {
        ForgeInstaller fi = new ForgeInstaller();
        fi.install("1.21.11");
    }

    public void install(String version){
        try {
            Files.createDirectories(Paths.get("minecraft/versions", version));
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        String forgeVersion = getBestForgeVersion(version);
        download(version, forgeVersion);

        System.out.println(forgeVersion);
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
                System.out.println("downloading: " + progress + "%");
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
}
