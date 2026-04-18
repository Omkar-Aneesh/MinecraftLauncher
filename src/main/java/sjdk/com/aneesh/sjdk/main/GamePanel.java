package sjdk.com.aneesh.sjdk.main;

import client.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class GamePanel extends JPanel implements Runnable{
    public BufferedImage screen;
    public Graphics2D g2;
    public client.Main main;

    public int tileSize = 120;
    public int screenWidth = 540;
    public int screenHeight = 360;
    public int screenWidth2 = 540;
    public int screenHeight2 = 360;

    public boolean doubleBuffered = true;
    public Color backgroundColor = Color.black;

    Thread gameThread;
    int FPS = 60;

    public char keyPressChar;
    public int keyPressCode;

    public char keyReleaseChar;
    public int keyReleaseCode;

    public boolean fullscreen = false;

    public void setScreenSize(int width, int height){
        screenWidth = width;
        screenHeight = height;

        screenWidth2 = width;
        screenHeight2 = height;
    }

    public KeyHandler keyH = new KeyHandler(this);
    public MouseHandler mouseH = new MouseHandler(this);

    public void start(Main main) {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setDoubleBuffered(doubleBuffered);
        this.setBackground(backgroundColor);
        this.setFocusable(true);
        this.addKeyListener(keyH);
        this.addMouseListener(mouseH);
        this.addMouseMotionListener(mouseH);
        this.main = main;
    }

    public void setFullScreen(){
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        gd.setFullScreenWindow(SetupEnv.window);

        screenWidth2 = SetupEnv.window.getWidth();
        screenHeight2 = SetupEnv.window.getHeight();
    }

    public void setupGame(){
        screen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        g2 = (Graphics2D) screen.getGraphics();
    }
    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (gameThread != null){
            double drawInterval = 1000000000 / FPS;
            double nextDrawTime = System.nanoTime() + drawInterval;
            while (gameThread != null){
                update();
                draw();
                drawToScreen();
                try {
                    double remainingTime = nextDrawTime - System.nanoTime();
                    remainingTime = remainingTime / 1000000;

                    if (remainingTime < 0) {
                        remainingTime = 0;
                    }

                    Thread.sleep((long) remainingTime);

                    nextDrawTime += drawInterval;
                } catch (InterruptedException e){
                    throw new RuntimeException();
                }
            }
        }
    }

    public void update(){
        main.update();
    }

    public void draw(){
        g2.setColor(backgroundColor);
        g2.fillRect(0, 0, screenWidth, screenHeight);

        main.draw();
    }

    public void setColor(Color color){
        g2.setColor(color);
    }

    public void fillRect(int x, int y, int width, int height){
        g2.fillRect(x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height){
        g2.drawRect(x, y, width, height);
    }

    public void fillOval(int x, int y, int width, int height){
        g2.fillOval(x, y, width, height);
    }

    public void drawOval(int x, int y, int width, int height){
        g2.drawOval(x, y, width, height);
    }

    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints){
        g2.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints){
        g2.drawPolygon(xPoints, yPoints, nPoints);
    }

    public void drawImage(BufferedImage image, int x, int y){
        g2.drawImage(image, x, y, null);
    }

    public void drawImage(BufferedImage image, int x, int y, int width, int height){
        g2.drawImage(image, x, y, width, height, null);
    }
    public void drawLine(int startX, int startY, int endX, int endY){
        g2.drawLine(startX, startY, endX, endY);
    }
    public void set_font(Font font){
        g2.setFont(font);
    }
    public void drawString(String string, int x, int y){
        for (String line: string.split("\n")){
            g2.drawString(line, x, y);
            y += g2.getFont().getSize() + 10;
        }
    }

    public void drawToScreen(){
        Graphics g = getGraphics();
        g.drawImage(screen, 0, 0, screenWidth2, screenHeight2, null);
        g.dispose();
    }

    public BufferedImage loadImage(String path){
        BufferedImage image = null;

        try {
            image = ImageIO.read(getClass().getResourceAsStream(path));
        } catch (IOException e){
            e.printStackTrace();
        }

        return image;
    }

    public File loadFile(String path){
        File file = null;

        file = new File(path);

        return file;
    }

    public String extractStringFromFile(File file){
        String fileString = "";

        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()){
                fileString += scanner.nextLine() + "\n";
            }
            scanner.close();
            fileString = fileString.replaceAll("\n$", "");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return fileString;
    }
    public void writeToFile(String path, String string){
        try {
            FileWriter file = new FileWriter(path);
            file.write(string);
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean keyPressEvent(char character){
        return keyPressChar == character;
    }

    public boolean keyReleaseEvent(char character){
        return keyReleaseChar == character;
    }

    public void disposeKeyEvents(){
        keyPressChar = '\0';
        keyPressCode = 0;

        keyReleaseChar = '\0';
        keyReleaseCode = 0;
    }

    public boolean mouseClick(int x, int y, int width, int height){
        boolean click = false;

        if (mouseH.clicked){
            if (mouseH.mouseX > x && mouseH.mouseX < x + width &&
                    mouseH.mouseY > y && mouseH.mouseY < y + height){
                click = true;
                mouseH.clicked = false;
            }
        }

        return click;
    }

    public boolean mouseClick(int x, int y, int width, int height, int button){
        boolean click = false;

        if (mouseH.clicked && mouseH.button == button){
            if (mouseH.mouseX > x && mouseH.mouseX < x + width &&
                    mouseH.mouseY > y && mouseH.mouseY < y + height){
                click = true;
            }
        }

        return click;
    }

    public boolean mouseScroll(int dir){
        boolean scrl = false;

        if (mouseH.scroll == dir){
            scrl = true;
        }

        return scrl;
    }
}
