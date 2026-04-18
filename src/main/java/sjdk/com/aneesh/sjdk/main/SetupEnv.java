package sjdk.com.aneesh.sjdk.main;

import client.Main;

import javax.swing.*;
import java.awt.*;

import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

public class SetupEnv{
    public static final JFrame window = new JFrame();
    Component locationComponent = null;
    boolean resizable = false;
    public boolean undecorated = false;
    public GamePanel gamePanel = new GamePanel();
    String title = "SJDK";

    public void setupEnv(Main main){
        gamePanel.start(main);

        window.setDefaultCloseOperation(EXIT_ON_CLOSE);
        window.setTitle(title);

        window.setResizable(resizable);
        window.setUndecorated(undecorated);
        window.add(gamePanel);

        window.pack();

        window.setLocationRelativeTo(locationComponent);
        window.setVisible(true);

        gamePanel.setupGame();
        gamePanel.startGameThread();
    }

}
