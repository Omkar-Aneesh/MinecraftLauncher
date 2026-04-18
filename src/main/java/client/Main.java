package client;

import sjdk.com.aneesh.sjdk.main.SetupEnv;

import java.awt.*;

public class Main {
    public static final SetupEnv env = new SetupEnv();

    public final KeyEvents keyEvents = new KeyEvents(this);
    public final MouseEvents mouseEvents = new MouseEvents(this);
    public int referenceWidth = 1920;
    public int referenceHeight = 1080;
    public static int tileSize;
    public int baseTileSize = 128;

    public static void main(String[] args) {
        Main main = new Main();

        env.gamePanel.addMouseWheelListener(main.mouseEvents);

        Dimension screenSize = new Dimension(1200, 800);

        env.undecorated = false;

        env.gamePanel.setScreenSize(screenSize.width, screenSize.height);

        double scaleX = (double)screenSize.width / main.referenceWidth;
        double scaleY = (double)screenSize.height / main.referenceHeight;

        double scale = Math.min(scaleX, scaleY);

        tileSize = (int)(main.baseTileSize * scale);

        env.setupEnv(main);

        env.gamePanel.g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        env.gamePanel.g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    public UI ui = new UI(this);

    public void draw(){
        env.gamePanel.setColor(Color.BLACK);
        env.gamePanel.fillRect(0, 0, 1200, 800);

        ui.draw();
    }

    public void update(){
        keyEvents.handleEvents();
        mouseEvents.handleEvents();
    }
}
