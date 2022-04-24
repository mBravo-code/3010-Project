package com.example.betterbattleship;

import java.util.ArrayList;

public interface MessageHandler {
    public void run(Object firstObject, Object secondObject, Object thirdObject);
}

class joinGame implements MessageHandler {
    public void run(Object firstObject, Object secondObject, Object thirdObject){
        String host = (String) firstObject;
        int port = (int) secondObject;
        ArrayList<Player> players = (ArrayList<Player>) thirdObject;
        Player newPlayer = new Player(false, new int[] {0,0}, host, port);
        players.add(newPlayer);
    }
}
