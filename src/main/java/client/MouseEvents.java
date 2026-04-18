package client;

import sjdk.com.aneesh.sjdk.main.MouseHandler;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class MouseEvents implements MouseWheelListener {
    Main main;

    int mouseX;
    int mouseY;

    boolean clicked = false;

    int scroll = 0;

    public MouseEvents(Main main){
        this.main = main;
    }

    public void handleEvents(){
        handleClickEvents();
        handleDragEvents();
        handlePressEvents();
        handleScrollEvents();
    }

    public void handleClickEvents(){
        MouseHandler mouseH = Main.env.gamePanel.mouseH;

        clicked = mouseH.dragged;

        mouseX = mouseH.moveX;
        mouseY = mouseH.moveY;

    }
    public void handlePressEvents(){}
    public void handleDragEvents(){}
    public void handleScrollEvents(){
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        scroll = e.getWheelRotation();
    }

    public boolean isMouseCollidingWith(int x, int y, int width, int height){
        boolean value = false;
        if (mouseX > x && mouseX < x + width &&
            mouseY > y && mouseY < y + height){
            value = true;
        }
        return value;
    }
}
