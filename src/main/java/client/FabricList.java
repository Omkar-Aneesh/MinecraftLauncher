package client;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class FabricList {
    ArrayList<String> loaderVersions = new ArrayList<>();
    String latestVersion = "";
    String bestVersion = "";

    public void fetchLoaderVersions(String version){
        try {
            JSONArray arr = readJsonArray("https://meta.fabricmc.net/v2/versions/loader/" + version);

            JSONObject latest = arr.getJSONObject(0);
            latestVersion = latest.getJSONObject("loader").getString("version");

            for (int i = 0; i < arr.length(); i ++){
                JSONObject obj = arr.getJSONObject(i);
                JSONObject loader = obj.getJSONObject("loader");

                String loaderVersion = loader.getString("version");
                boolean stable = loader.getBoolean("stable");

                loaderVersions.add(loaderVersion);
            }

            for (int i = 0; i < arr.length(); i ++){
                JSONObject obj = arr.getJSONObject(i);
                JSONObject loader = obj.getJSONObject("loader");

                String loaderVersion = loader.getString("version");
                boolean stable = loader.getBoolean("stable");

                if (stable){
                    bestVersion = loaderVersion;
                    break;
                }
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public JSONArray readJsonArray(String url) throws Exception {
        try (InputStream in = new URL(url).openStream()){
            return new JSONArray(new String(in.readAllBytes()));
        }
    }
}
