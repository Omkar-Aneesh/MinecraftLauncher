package client;

public interface ProgressListener {
    void onProgress(long downloaded, long total);
}
