package sjdk.com.aneesh.sjdk.main;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class Sound {
    Clip clip;
    ArrayList<URL> soundURL = new ArrayList<>();
    FloatControl fc;

    int volumeScale = 3;
    float volume;

    public void loadAudio(String path){
        soundURL.add(getClass().getResource(path));
    }
    
    public void setFile(int i){
        try{
            AudioInputStream ais =AudioSystem.getAudioInputStream(soundURL.get(i));
            clip = AudioSystem.getClip();
            clip.open(ais);
            fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            checkVolume();
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void play(){
        clip.start();
    }
    
    public void stop(){
        clip.start();
    }
    public void loop(){
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }
    public void checkVolume(){
        switch (volumeScale){
            case 0: volume = -80f;break;
            case 1: volume = -20;break;
            case 2: volume = -12;break;
            case 3: volume = -5;break;
            case 4: volume = 1f;break;
            case 5: volume = 6f;break;
        }
        fc.setValue(volume);
    }
}
