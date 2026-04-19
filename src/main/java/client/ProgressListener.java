package client;

public interface ProgressListener {
    void onProgress(int done, int total, double speedMBps, long etaSeconds);
}
