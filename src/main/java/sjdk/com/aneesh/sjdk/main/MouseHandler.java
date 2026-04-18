package sjdk.com.aneesh.sjdk.main;

import java.awt.event.*;

public class MouseHandler implements MouseListener, MouseMotionListener, MouseWheelListener {

    GamePanel gp;

    public MouseHandler(GamePanel gp){
        this.gp = gp;
    }

    public int mouseX;
    public int mouseY;
    public int moveX;
    public int moveY;
    public int scroll = 0;
    public int button = 0;
    public boolean clicked = false;
    public boolean dragged = false;
    public boolean pressed = false;

    @Override
    public void mouseClicked(MouseEvent e) {
        button = e.getButton();

        clicked = true;
        int m = e.getX();
        int my = e.getY();
        mouseX = m;
        mouseY = my;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        button = e.getButton();

        pressed = true;
        int mq = e.getX();
        int myq = e.getY();
        if (gp.fullscreen) {
            mq = (int) (0.625 * e.getX());
            myq = (int) (0.625 * e.getY());
        }
        moveX = mq;
        moveY = myq;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragged = false;
        pressed = false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        dragged = true;
        int m = e.getX();
        int my = e.getY();
        if (gp.fullscreen) {
            m = (int) (0.625 * e.getX());
            my = (int) (0.625 * e.getY());
        }
        moveX = m;
        moveY = my;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int m = e.getX();
        int my = e.getY();
        if (gp.fullscreen) {
            m = (int) (0.625 * e.getX());
            my = (int) (0.625 * e.getY());
        }
        moveX = m;
        moveY = my;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scroll = e.getWheelRotation();
    }
}

