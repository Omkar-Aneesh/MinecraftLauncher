package sjdk.com.aneesh.sjdk.main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean keyPressed;

    GamePanel gp;

    public KeyHandler(GamePanel gp){
        this.gp = gp;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        keyPressed = true;
        gp.keyPressChar = e.getKeyChar();
        gp.keyPressCode = e.getKeyCode();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keyPressed = false;
        gp.keyReleaseChar = e.getKeyChar();
        gp.keyReleaseCode = e.getKeyCode();
    }
}
