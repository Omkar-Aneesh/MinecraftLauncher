package client;

import javax.swing.*;
import java.io.File;

public class FilePicker {
    public String choose(){
        String path = "minecraft/toUpload/skin.png";

        JFileChooser chooser = new JFileChooser();

        int result = chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION){
            File file = chooser.getSelectedFile();
            path = file.getAbsolutePath();
        }

        return path;
    }
}
