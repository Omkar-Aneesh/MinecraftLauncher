package client;

public class KeyEvents {
    Main main;
    
    public KeyEvents(Main main){this.main = main;}
    
    public void handleEvents(){keyPressEvent();keyReleaseEvent();}
    
    public void keyPressEvent(){}
    public void keyReleaseEvent(){}
}
