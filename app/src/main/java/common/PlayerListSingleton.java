package common;

import com.example.betterbattleship.Player;

import java.util.ArrayList;

public class PlayerListSingleton {

    private ArrayList<Player> playerList;
    public ArrayList<Player> getPlayerList() {return playerList;}
    public void setPlayerList(ArrayList<Player> newPlayerList) {playerList = newPlayerList;}

    private static final PlayerListSingleton singleton = new PlayerListSingleton();

    public static PlayerListSingleton getInstance() {return singleton;}
}
