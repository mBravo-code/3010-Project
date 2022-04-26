package common;

import com.example.betterbattleship.Player;

import java.util.ArrayList;
import java.util.Hashtable;

public class PlayerListSingleton {

    private ArrayList<Player> playerList;
    public ArrayList<Player> getPlayerList() {return playerList;}
    public void initializePlayerList() {playerList = new ArrayList<>();}
    public void setPlayerList(ArrayList<Player> newPlayerList) {playerList = newPlayerList;}

    public Hashtable<String, ArrayList<Player>> consensus;
    public Hashtable<String, ArrayList<Player>> getConsensus() {return consensus;}
    public void initializeConsensus() {consensus = new Hashtable<>();}
    public void setConsensus(Hashtable<String, ArrayList<Player>> newConsensus) {consensus = newConsensus;}

    private String ownHostName;
    public void setOwnHostName(String newHostName) {ownHostName = newHostName;}
    public String getOwnHostName() {return ownHostName;}

    private static final PlayerListSingleton singleton = new PlayerListSingleton();

    public static PlayerListSingleton getInstance() {return singleton;}
}
