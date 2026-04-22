package client;

import org.json.JSONObject;
import sjdk.com.aneesh.sjdk.main.GamePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Scanner;

public class UI {
    GamePanel gp;
    Main main;

    boolean inVersionSelectionMode = false;
    boolean inNewInstallationMode = false;
    boolean inModLoaderSelectionMode = false;
    boolean inModLoaderVersionSelectionMode = false;
    boolean inOptionsPanel = false;
    boolean progressBar = false;
    boolean installingMinecraft = false;

    boolean enteringInstallationName = false;
    boolean enteringUserName = false;

    boolean versionExists = false;

    boolean showUsernameCursor = true;
    boolean showInstallationNameCursor = true;

    String username = "Aneesh015";
    String installationName = "";
    String version = "";
    String modLoader = "";
    String modLoaderVersion = "";

    int versionIndexForOptions;
    int versionYForOptions;

    int installDone;
    int installTotal;
    double installSpeed;
    long installEta;

    String[] modLoaderNameList = new String[3];

    StringBuilder usernameBuffer = new StringBuilder();
    StringBuilder installationNameBuffer = new StringBuilder();

    int cursorIntervalCounter = 0;

    int maxCursorIntervals = 30;

    int versionOffset = 0;

    int versionSelectionPanelWidth = 491;
    int versionSelectionPanelHeight = 481;

    int modLoaderSelectionPanelWidth = 491;
    int modLoaderSelectionPanelHeight = 121;

    int modLoaderVersionSelectionPanelWidth = 491;
    int modLoaderVersionSelectionPanelHeight = 481;

    int optionsPanelWidth = 100;
    int optionPanelHeight = 40;

    BufferedImage screen2 = new BufferedImage(versionSelectionPanelWidth, versionSelectionPanelHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = screen2.createGraphics();

    BufferedImage screen3 = new BufferedImage(modLoaderSelectionPanelWidth, modLoaderSelectionPanelHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g3 = screen3.createGraphics();

    BufferedImage screen4 = new BufferedImage(modLoaderVersionSelectionPanelWidth, modLoaderVersionSelectionPanelHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g4 = screen4.createGraphics();

    BufferedImage screen5 = new BufferedImage(optionsPanelWidth, optionPanelHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g5 = screen5.createGraphics();

    MinecraftLauncher minecraftLauncher = new MinecraftLauncher();
    MinecraftInstaller minecraftInstaller = new MinecraftInstaller();
    ForgeInstaller forgeInstaller = new ForgeInstaller();
    ForgeRunner forgeRunner = new ForgeRunner();

    ArrayList<String> versionList = new ArrayList<>();
    ArrayList<String> modLoaderList = new ArrayList<>();
    ArrayList<String> versionPlayList = new ArrayList<>();
    ArrayList<String> versionNameList = new ArrayList<>();

    BufferedImage optionImage;

    public UI(Main main){
        gp = Main.env.gamePanel;
        this.main = main;

        versionList.addAll(extractVersions());

        modLoaderNameList[0] = "Vanilla";
        modLoaderNameList[1] = "Forge";
        modLoaderNameList[2] = "Fabric";

        loadVersionPlayList();

        optionImage = loadImage("3dots");
    }

    public BufferedImage loadImage(String imagePath){
        BufferedImage image = null;

        try {
            InputStream is = getClass().getResourceAsStream("/" + imagePath + ".png");
            image = ImageIO.read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return image;
    }

    public void draw(){
        drawVersionPlayList();
        drawNewInstallationButton();

        if (progressBar){
            drawProgressBar();
        }
//        drawUserNameBox();
//        drawPlayButton();
//        drawVersionSelector();
//        System.out.println("[" + installDone + "/" + installTotal + "] " +
//                (int)((installDone * 100.0) / installTotal) + "% | " +
//                String.format("%.2f MB/s", installSpeed) +
//                " | ETA: " + installEta + "s");
    }

    public void loadVersionPlayList(){
        try{
            File file = new File("minecraft/versionNames.txt");
            if (file.exists()){

                Scanner scanner = new Scanner(file);
                versionPlayList.clear();
                versionNameList.clear();
                while (scanner.hasNextLine()){
                    String string = scanner.nextLine();
                    if (!string.isEmpty()){
                        String[] lists = string.split(":");

                        versionPlayList.add(lists[0]);
                        versionNameList.add(lists[1]);
                    }
                }
            }
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void drawProgressBar(){
        int width = 1199;
        int height = 40;
        int x = 0;
        int y = 800 - height - 1;

        double progressPercentage = (installDone * 100.0) / installTotal;

        if (progressPercentage == 100){
            progressPercentage = 0;
        }

        int progressX = 0;
        int progressY = y;
        double progressWidth = (width / 100.0) * progressPercentage;
        int progressHeight = 40;

        gp.setColor(Color.GREEN);
        gp.fillRect(progressX, progressY, (int) progressWidth, progressHeight);

        gp.setColor(Color.WHITE);
        gp.drawRect(x, y, width, height);

        gp.set_font(gp.getFont().deriveFont(30f));

        String str = "";

        if (minecraftInstaller.currentSituationString.isEmpty()){
            if (installingMinecraft) {
                str = "[" + installDone + "/" + installTotal + "] " + (int) progressPercentage + "% | " +
                        String.format("%.2f MB/s", installSpeed) + " | ETA: " + installEta + "s";
            } else {
                str = MinecraftLauncher.currentSituationString;
            }
        } else {
            str = minecraftInstaller.currentSituationString;
        }

        if (modLoader.equals("Forge")){
            if (forgeRunner.currentSituationString.isEmpty()) {
                if (installingMinecraft) {
                    str = "[" + installDone + "/" + installTotal + "] " + (int) progressPercentage + "% | " +
                            String.format("%.2f MB/s", installSpeed) + " | ETA: " + installEta + "s";
                } else {
                    str = forgeInstaller.currentSituationString;
                }
            } else {
                str = forgeRunner.currentSituationString;
            }
        }

        int strX = (1200/2) - (getStringWidth(str)/2);
        int strY = progressY + 30;

        gp.drawString(str, strX, strY);
    }

    public void drawNewInstallationWindow(){
        int width = 500;
        int height = 400;
        int x = (1200/2) - (width/2);
        int y = (800/2) - (height/2);

        gp.setColor(Color.GRAY);
        gp.fillRect(x, y, width, height);

        int padding = 10;

//        drawVersionSelector(x, y, width, height);
        int lastBoxHeight = drawInstallationNameBox(x, y, width, height);padding += lastBoxHeight + 10; padding += lastBoxHeight + 10; padding += lastBoxHeight + 10;
        drawInstallButton(x, y, width, height, lastBoxHeight + padding); padding -= lastBoxHeight + 10;
        drawModLoaderVersionSelector(x, y, width, height, lastBoxHeight + padding); padding -= lastBoxHeight + 10;
        lastBoxHeight = drawModLoaderSelector(x, y, width, height, lastBoxHeight + padding); padding -= lastBoxHeight + 10;
        drawVersionSelector(x, y, width, height, lastBoxHeight + padding);

        if (gp.mouseH.pressed){
            if (!main.mouseEvents.isMouseCollidingWith(x, y, width, height) && !enteringInstallationName &&
                !inVersionSelectionMode && !inModLoaderSelectionMode && !inModLoaderVersionSelectionMode){
                inNewInstallationMode = false;

//                username = "";
//                usernameBuffer.delete(0, usernameBuffer.length());
//                version = "";
//                modLoader = "";
//                installationName = "";
            }
        }
    }

    public void drawNewInstallationButton(){
        int x = 20;
        int y = 20;
        int width = 200;
        int height = 30;

        gp.setColor(Color.GRAY);
        gp.fillRect(x, y, width, height);

        gp.setColor(Color.WHITE);
        gp.set_font(gp.getFont().deriveFont(20f));

        String text = "New Installation";

        int strX = x + (width / 2) - (getStringWidth(text) / 2);

        gp.drawString(text, strX, y + 20);

        if (gp.mouseH.pressed){
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height)){
                inNewInstallationMode = true;
                gp.mouseH.pressed = false;
            }
        }

        if (inNewInstallationMode){
            drawNewInstallationWindow();
        }
    }
    public int drawInstallationNameBox(int boxX, int boxY, int boxWidth, int boxHeight){
        int width = boxWidth - 10;
        int height = 40;
        int x = boxX + (boxWidth/2) - (width/2);
        int y = boxY + 10;

        gp.setColor(Color.GRAY);
        gp.fillRect(x, y, width, height);
        gp.setColor(Color.black);
        gp.drawRect(x, y, width, height);

        String InstallationNameStr = installationName;
        gp.setColor(Color.WHITE);

        if (installationName.isEmpty() && !enteringInstallationName){
            InstallationNameStr = "Name Your Installation";
            gp.setColor(Color.DARK_GRAY);
        }

        if (enteringInstallationName){
            InstallationNameStr = "";
        }

        gp.set_font(gp.getFont().deriveFont(30f));
        gp.drawString(InstallationNameStr, x, y + 30);

        if (gp.mouseH.pressed){
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height)){
                enteringInstallationName = true;
                gp.mouseH.pressed = false;
            } else {
                enteringInstallationName = false;
                installationName = installationNameBuffer.toString();
            }
        }

        cursorIntervalCounter ++;

        if (cursorIntervalCounter > maxCursorIntervals){
            cursorIntervalCounter = 0;
            showInstallationNameCursor = !showInstallationNameCursor;
        }

        if (enteringInstallationName){
            gp.setColor(Color.white);
            gp.set_font(gp.getFont().deriveFont(30f));

            if (gp.keyH.keyPressed){
                showInstallationNameCursor = true;
                if (gp.keyPressCode == KeyEvent.VK_BACK_SPACE){
                    if (!installationNameBuffer.isEmpty()) {
                        installationNameBuffer.deleteCharAt(installationNameBuffer.length() - 1);
                    }
                } else if (gp.keyPressCode == KeyEvent.VK_ENTER) {
                    installationName = installationNameBuffer.toString();
                    enteringInstallationName = false;
                } else if (gp.keyPressCode == KeyEvent.VK_ESCAPE) {
                    installationName = installationNameBuffer.toString();
                    enteringUserName = false;
                }else if (Character.isLetterOrDigit(gp.keyPressChar) || gp.keyPressChar == '.' || gp.keyPressChar == '-'){
                    installationNameBuffer.append(gp.keyPressChar);
                }
                gp.keyH.keyPressed = false;
            }

            int cursorWidth = 2;
            int cursorHeight = height - 2;
            int cursorX = x + getStringWidth(installationNameBuffer.toString()) + cursorWidth + 2;
            int cursorY = y + 1;

            gp.drawString(installationNameBuffer.toString(), x, y + 30);

            if (showInstallationNameCursor){
                gp.setColor(Color.WHITE);
                gp.fillRect(cursorX, cursorY, cursorWidth, cursorHeight);
            }
        }
        return height;
    }

    public void drawUserNameBox(int boxX, int boxY, int boxWidth, int boxHeight){
        int width = boxWidth - 10;
        int height = 40;
        int x = boxX + (boxWidth/2) - (width/2);
        int y = boxY + 10;

        gp.setColor(Color.GRAY);
        gp.fillRect(x, y, width, height);
        gp.setColor(Color.black);
        gp.drawRect(x, y, width, height);

        String usernameStr = username;

        gp.setColor(Color.WHITE);

        if (username.isEmpty() && !enteringUserName){
            usernameStr = "Enter Your Username";
            gp.setColor(Color.DARK_GRAY);
        }

        if (enteringUserName){
            usernameStr = "";
        }

        gp.set_font(gp.getFont().deriveFont(30f));
        gp.drawString(usernameStr, x, y + 30);

        if (gp.mouseH.pressed){
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height)){
                enteringUserName = true;
                gp.mouseH.pressed = false;
            } else {
                enteringUserName = false;
                username = usernameBuffer.toString();
            }
        }

        cursorIntervalCounter ++;

        if (cursorIntervalCounter > maxCursorIntervals){
            cursorIntervalCounter = 0;
            showUsernameCursor = !showUsernameCursor;
        }

        if (enteringUserName){
            gp.setColor(Color.darkGray);
            gp.set_font(gp.getFont().deriveFont(30f));

            if (gp.keyH.keyPressed){
                showUsernameCursor = true;
                if (gp.keyPressCode == KeyEvent.VK_BACK_SPACE){
                    if (!usernameBuffer.isEmpty()) {
                        usernameBuffer.deleteCharAt(usernameBuffer.length() - 1);
                    }
                } else if (gp.keyPressCode == KeyEvent.VK_ENTER) {
                    username = usernameBuffer.toString();
                    enteringUserName = false;
                } else if (gp.keyPressCode == KeyEvent.VK_ESCAPE) {
                    username = usernameBuffer.toString();
                    enteringUserName = false;
                }else if (Character.isLetterOrDigit(gp.keyPressChar)){
                    usernameBuffer.append(gp.keyPressChar);
                }
                gp.keyH.keyPressed = false;
            }

            int cursorWidth = 2;
            int cursorHeight = height - 2;
            int cursorX = x + getStringWidth(usernameBuffer.toString()) + cursorWidth + 1;
            int cursorY = y + 1;

            gp.drawString(usernameBuffer.toString(), x, y + 30);

            if (showUsernameCursor){
                gp.fillRect(cursorX, cursorY, cursorWidth, cursorHeight);
            }
        }
    }
    public int drawVersionSelector(int boxX, int boxY, int boxWidth, int boxHeight, int lastBoxHeight){
        int width = boxWidth - 10;
        int height = 40;
        int x = boxX + (boxWidth/2) - (width/2);
        int y = boxY + lastBoxHeight + 10;

        gp.setColor(Color.GRAY);
        gp.fillRect(x, y, width, height);
        gp.setColor(Color.black);
        gp.drawRect(x, y, width, height);

        gp.set_font(gp.getFont().deriveFont(30f));

        String string;

        if (version.isEmpty()){
            string = "Select A Version";
            gp.setColor(Color.DARK_GRAY);
        } else {
            string = version;
            gp.setColor(Color.WHITE);
        }
        gp.drawString(string, x, y + 30);

        if (Main.env.gamePanel.mouseH.pressed) {
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height)) {
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, versionSelectionPanelWidth, versionSelectionPanelHeight);
                inVersionSelectionMode = !inVersionSelectionMode;
                Main.env.gamePanel.mouseH.pressed = false;
            } else if(!main.mouseEvents.isMouseCollidingWith(x, y + height, versionSelectionPanelWidth, versionSelectionPanelHeight)) {
                inVersionSelectionMode = false;
            }
        }
        if (inVersionSelectionMode){
            versionOffset -= main.mouseEvents.scroll * height;
            main.mouseEvents.scroll = 0;

            if (versionOffset >= 0){
                versionOffset = 0;
            }
            if (versionOffset < -versionList.size() * height){
                versionOffset = versionList.size() * height;
            }

            int BoxY = versionOffset;

            for (int i = 0; i < versionList.size(); i ++){
                g2.setColor(Color.GRAY);
                g2.fillRect(0, BoxY, width, height);
                g2.setColor(Color.BLACK);
                g2.drawRect(0, BoxY, width, height);

                g2.setColor(Color.WHITE);
                g2.setFont(g2.getFont().deriveFont(30f));
                g2.drawString(versionList.get(i), 0, BoxY + 30);

                if (Main.env.gamePanel.mouseH.pressed) {
                    if (Main.env.gamePanel.mouseH.moveY > y + height && Main.env.gamePanel.mouseH.moveY < y + height + versionSelectionPanelHeight) {
                        if (main.mouseEvents.isMouseCollidingWith(x, BoxY + y + height, width, height)) {
                            version = versionList.get(i);
                            File file = new File("minecraft/versions");
                            String[] fileList = file.list();
                            if (fileList != null) {
                                for (String s : fileList) {
                                    if (version.equals(s)) {
                                        versionExists = true;
                                        break;
                                    }
                                }
                            }
                            inVersionSelectionMode = false;
                            Main.env.gamePanel.mouseH.pressed = false;
                        }
                    }
                }
//                Main.env.gamePanel.mouseH.pressed = false;

                BoxY += height;
            }

            gp.drawImage(screen2, x, y + height);
        }
        return height;
    }
    public int drawModLoaderSelector(int boxX, int boxY, int boxWidth, int boxHeight, int lastBoxHeight){
        int width = boxWidth - 10;
        int height = 40;
        int x = boxX + (boxWidth/2) - (width/2);
        int y = boxY + lastBoxHeight + 10;

        gp.setColor(Color.GRAY);
        gp.fillRect(x, y, width, height);
        gp.setColor(Color.black);
        gp.drawRect(x, y, width, height);

        gp.set_font(gp.getFont().deriveFont(30f));

        String string;

        if (modLoader.isEmpty()){
            string = "Select A Mod Loader";
            gp.setColor(Color.darkGray);
        } else {
            string = modLoader;
            gp.setColor(Color.WHITE);
        }
        gp.drawString(string, x, y + 30);

        if (Main.env.gamePanel.mouseH.pressed) {
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height) && !inVersionSelectionMode) {
                g3.setColor(Color.BLACK);
                g3.fillRect(0, 0, modLoaderSelectionPanelWidth, modLoaderSelectionPanelHeight);
                inModLoaderSelectionMode = !inModLoaderSelectionMode;
                Main.env.gamePanel.mouseH.pressed = false;
            } else if (!main.mouseEvents.isMouseCollidingWith(x, y + height, modLoaderSelectionPanelWidth, modLoaderSelectionPanelHeight) && !inVersionSelectionMode){
                inModLoaderSelectionMode = false;
            }
        }
        if (inModLoaderSelectionMode){
//            versionOffset -= main.mouseEvents.scroll * height;
//            main.mouseEvents.scroll = 0;

//            if (versionOffset >= 0){
//                versionOffset = 0;
//            }
//            if (versionOffset < -versionList.size() * height){
//                versionOffset = versionList.size() * height;
//            }

//            int BoxY = versionOffset;
            int BoxY = 0;

            for (int i = 0; i < modLoaderNameList.length; i ++){
                g3.setColor(Color.GRAY);
                g3.fillRect(0, BoxY, width, height);
                g3.setColor(Color.BLACK);
                g3.drawRect(0, BoxY, width, height);

                g3.setColor(Color.WHITE);
                g3.setFont(g2.getFont().deriveFont(30f));
                g3.drawString(modLoaderNameList[i], 0, BoxY + 30);

                if (Main.env.gamePanel.mouseH.pressed) {
                    if (Main.env.gamePanel.mouseH.moveY > y + height && Main.env.gamePanel.mouseH.moveY < y + height + versionSelectionPanelHeight) {
                        if (main.mouseEvents.isMouseCollidingWith(x, BoxY + y + height, width, height)) {
                            modLoader = modLoaderNameList[i];
//                            File file = new File("minecraft/versions");
//                            String[] fileList = file.list();
//                            if (fileList != null) {
//                                for (String s : fileList) {
//                                    if (version.equals(s)) {
//                                        versionExists = true;
//                                        break;
//                                    }
//                                }
//                            }
                            inModLoaderSelectionMode = false;
                            Main.env.gamePanel.mouseH.pressed = false;
                        }
                    }
                }
//                Main.env.gamePanel.mouseH.pressed = false;

                BoxY += height;
            }

            gp.drawImage(screen3, x, y + height);
        }
        return height;
    }
    public int drawModLoaderVersionSelector(int boxX, int boxY, int boxWidth, int boxHeight, int lastBoxHeight){
        int width = boxWidth - 10;
        int height = 40;
        int x = boxX + (boxWidth/2) - (width/2);
        int y = boxY + lastBoxHeight + 10;

        if (modLoaderList.isEmpty()){
            gp.setColor(Color.DARK_GRAY);
        } else {
            gp.setColor(Color.GRAY);
        }
        gp.fillRect(x, y, width, height);
        gp.setColor(Color.black);
        gp.drawRect(x, y, width, height);

        gp.set_font(gp.getFont().deriveFont(30f));
        String string;
        if (modLoaderList.isEmpty()){
            string = "No Mod Loader Versions";
            gp.setColor(Color.GRAY);
        } else if (modLoaderVersion.isEmpty()){
            string = "Select Mod Loader Version";
            gp.setColor(Color.darkGray);
        } else {
            string = modLoaderVersion;
            gp.setColor(Color.white);
        }
        gp.drawString(string, x, y + 30);

        if (Main.env.gamePanel.mouseH.pressed) {
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height) && !inVersionSelectionMode && !inModLoaderSelectionMode && !modLoaderList.isEmpty()) {
                g4.setColor(Color.BLACK);
                g4.fillRect(0, 0, modLoaderVersionSelectionPanelHeight, modLoaderVersionSelectionPanelHeight);
                inModLoaderVersionSelectionMode = !inModLoaderVersionSelectionMode;
                Main.env.gamePanel.mouseH.pressed = false;
            } else {
                inModLoaderVersionSelectionMode = false;
            }
        }
        if (inModLoaderVersionSelectionMode){
//            versionOffset -= main.mouseEvents.scroll * height;
//            main.mouseEvents.scroll = 0;

//            if (versionOffset >= 0){
//                versionOffset = 0;
//            }
//            if (versionOffset < -versionList.size() * height){
//                versionOffset = versionList.size() * height;
//            }

//            int BoxY = versionOffset;
            int BoxY = 0;

            for (int i = 0; i < modLoaderList.size(); i ++){
                g4.setColor(Color.GRAY);
                g4.fillRect(0, BoxY, width, height);
                g4.setColor(Color.BLACK);
                g4.drawRect(0, BoxY, width, height);

                g4.setColor(Color.WHITE);
                g4.setFont(g2.getFont().deriveFont(30f));
                g4.drawString(modLoaderList.get(i), 0, BoxY + 30);

                if (Main.env.gamePanel.mouseH.pressed) {
                    if (Main.env.gamePanel.mouseH.moveY > y + height && Main.env.gamePanel.mouseH.moveY < y + height + versionSelectionPanelHeight) {
                        if (main.mouseEvents.isMouseCollidingWith(x, BoxY + y + height, width, height)) {
                            modLoaderVersion = modLoaderList.get(i);
//                            File file = new File("minecraft/versions");
//                            String[] fileList = file.list();
//                            if (fileList != null) {
//                                for (String s : fileList) {
//                                    if (version.equals(s)) {
//                                        versionExists = true;
//                                        break;
//                                    }
//                                }
//                            }
                            inModLoaderVersionSelectionMode = false;
                            Main.env.gamePanel.mouseH.pressed = false;
                        }
                    }
                }
//                Main.env.gamePanel.mouseH.pressed = false;

                BoxY += height;
            }

            gp.drawImage(screen4, x, y + height);
        }
        return height;
    }

    public void drawInstallButton(int boxX, int boxY, int boxWidth, int boxHeight, int lastBoxHeight){
        int width = boxWidth - 30;
        int height = 40;
        int x = boxX + (boxWidth/2) - (width/2);
        int y = boxY + lastBoxHeight + 40;

        gp.setColor(Color.YELLOW);
        gp.fillRect(x, y, width, height);

        gp.set_font(gp.getFont().deriveFont(30f));
        gp.setColor(Color.BLACK);

        String string;

        string = "Install";

        gp.drawString(string, x + (width/2) - (getStringWidth(string)/2), y + 30);

        if (Main.env.gamePanel.mouseH.pressed && !inVersionSelectionMode) {
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height)) {
                if (modLoader.equals("Vanilla")) {
                    minecraftInstaller.setProcessListener((done, total, speed, eta) -> {
                        SwingUtilities.invokeLater(() -> {
                            this.installDone = done;
                            this.installTotal = total;
                            this.installSpeed = speed;
                            this.installEta = eta;
                        });
                    });

                    installingMinecraft = true;
                    new Thread(() -> {
                        try {
                            minecraftInstaller.install(version);
                            FileWriter fileWriter = new FileWriter("minecraft/versionNames.txt", true);
                            fileWriter.write(version + ":" + installationName + "\n");
                            fileWriter.close();
                            progressBar = false;
                            installingMinecraft = false;
                            loadVersionPlayList();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                } else if (modLoader.equals("Forge")) {
                    forgeInstaller.setProcessListener((done, total, speed, eta) -> {
                        SwingUtilities.invokeLater(() -> {
                            this.installDone = done;
                            this.installTotal = total;
                            this.installSpeed = speed;
                            this.installEta = eta;
                        });
                    });

                    installingMinecraft = true;
                    new Thread(() -> {
                        try {
                            String forgeVersion = forgeInstaller.install(version);
                            forgeRunner.run(version, forgeVersion);
                            FileWriter fileWriter = new FileWriter("minecraft/versionNames.txt", true);
                            fileWriter.write(version + "-forge-" + forgeVersion + ":" + installationName + "\n");
                            fileWriter.close();
                            progressBar = false;
                            installingMinecraft = false;
                            loadVersionPlayList();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).start();
                }

                Main.env.gamePanel.mouseH.pressed = false;
                inNewInstallationMode = false;
                progressBar = true;
            }
        }
    }

    public void drawVersionPlayList(){
        int width = 1200;
        int height = 600;
        int x = 10;
        int y = 100;

        int pBWidth = 80;
        int pBHeight = 40;
        int pBX = 1200 - pBWidth - 100;

        for (int i = 0; i < versionPlayList.size(); i ++){
            gp.setColor(Color.WHITE);
            gp.drawRect(-1, y, width + 1, 70);

            gp.setColor(Color.WHITE);
            gp.set_font(gp.getFont().deriveFont(40f));

            gp.drawString((i + 1) + ".", x, y + 50);
            gp.drawString(versionNameList.get(i), x + 120, y + 50);

            int strWidth = getStringWidth(versionNameList.get(i));

            gp.set_font(gp.getFont().deriveFont(20f));
            gp.drawString(versionPlayList.get(i), x + 120 + strWidth + 10, y + 50);

            int pBY = y + (70/2) - (pBHeight/2);

            drawPlayButton(pBX, pBY, pBWidth, pBHeight, 30, versionPlayList.get(i));

            gp.drawImage(optionImage, pBX + pBWidth + 50, pBY, 10, pBHeight);

            if (gp.mouseH.pressed){
                if (main.mouseEvents.isMouseCollidingWith(pBX + pBWidth + 50, pBY, 10, pBHeight) && !inNewInstallationMode && !inOptionsPanel){
                    inOptionsPanel = true;
                    versionIndexForOptions = i;
                    versionYForOptions = pBY;
                    gp.mouseH.pressed = false;
                }
            }

            y += 60;
        }

        if (inOptionsPanel){
            int oPWidth = optionsPanelWidth;
            int oPHeight = optionPanelHeight;
            int oPX = pBX + pBWidth + 50 - oPWidth + 30;
            int oPY = versionYForOptions;

            g5.setColor(Color.black);
            g5.fillRect(0, 0, oPWidth, oPHeight);

            int rectX = 0;
            int rectY = 0;
            int rectWidth = optionsPanelWidth;
            int rectHeight = 40;

            g5.setColor(Color.GRAY);
            g5.fillRect(rectX, rectY, rectWidth, rectHeight);
            g5.setColor(Color.BLACK);
            g5.drawRect(rectX, rectY, rectWidth,rectHeight);

            String str = "Delete";

            g5.setColor(Color.WHITE);
            g5.setFont(g5.getFont().deriveFont(30f));
            g5.drawString(str, (rectWidth/2) - (getStringWidth(str)/2), rectY + 30);

            if (gp.mouseH.pressed){
                if (main.mouseEvents.isMouseCollidingWith(oPX + rectX, oPY + rectY, rectWidth, rectHeight)){
                    deleteVersion(versionPlayList.get(versionIndexForOptions));
                    inOptionsPanel = false;
                    loadVersionPlayList();
                }
                if (!main.mouseEvents.isMouseCollidingWith(oPX, oPY, oPWidth, oPHeight)){
                    inOptionsPanel = false;
                }
                gp.mouseH.pressed = false;
            }

            gp.drawImage(screen5, oPX, oPY, oPWidth, oPHeight);

        }
    }

    public void deleteVersion(String version){
        try (var paths = Files.walk(Paths.get("minecraft", "versions", version))){
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);

            File file = new File("minecraft/versionNames.txt");
            Scanner scanner = new Scanner(file);

            ArrayList<String> versionList = new ArrayList<>();

            while (scanner.hasNextLine()){
                String line = scanner.nextLine();
                if (!line.isEmpty()){
                    String[] split = line.split(":");
                    if (!split[0].equals(version)){
                        versionList.add(line);
                    }
                }
            }

            StringBuilder versionListStr = new StringBuilder();

            for (String str: versionList){
                versionListStr.append(str + "\n");
            }

            FileWriter fileWriter = new FileWriter("minecraft/versionNames.txt");
            fileWriter.write(versionListStr.toString());
            fileWriter.close();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public void drawPlayButton(int x, int y, int width, int height, float fontSize, String version){
        gp.setColor(Color.GREEN);

        gp.fillRect(x, y, width, height);

        gp.setColor(Color.WHITE);
        gp.set_font(gp.getFont().deriveFont(fontSize));

        String str = "Play!";

        gp.drawString(str, x + (width/2) - (getStringWidth(str)/2), (int) (y + fontSize));

        if (gp.mouseH.pressed){
            if (main.mouseEvents.isMouseCollidingWith(x, y, width, height) && !inOptionsPanel && !inNewInstallationMode){

                progressBar = true;
                new Thread(() -> {
                    try {
                        gp.mouseH.pressed = false;
                        minecraftLauncher.run(version, username);
                        progressBar = false;
                    } catch (Exception e){
                        throw new RuntimeException(e);
                    }
                }).start();;
            }
        }
    }

    public int getStringWidth(String string){
        return (int) gp.g2.getFontMetrics().getStringBounds(string, gp.g2).getWidth();
    }

    public ArrayList<String> extractVersions() {
        ArrayList<String> list = new ArrayList<>();

        try {
            JSONObject manifest = readJson("https://launchermeta.mojang.com/mc/game/version_manifest.json");

            for (Object o: manifest.getJSONArray("versions")){
                JSONObject v = (JSONObject) o;
                list.add(v.getString("id"));
            }
        } catch (Exception e){
            System.out.println(e);
        }

        return list;
    }

    public JSONObject readJson(String url) throws Exception {
        try (InputStream in = new URL(url).openStream()){
            return new JSONObject(new String(in.readAllBytes()));
        }
    }
}
