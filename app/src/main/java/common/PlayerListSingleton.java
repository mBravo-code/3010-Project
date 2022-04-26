package common;

import android.util.Log;

import com.example.betterbattleship.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Random;

public class PlayerListSingleton {

    private final int BOARD_SIZE = 5;
    private ArrayList<int[]> coordinatesList = new ArrayList<>();
    private ArrayList<Player> playerList;
    public ArrayList<Player> getPlayerList() {return playerList;}
    public void initializePlayerList() {playerList = new ArrayList<>();}
    public void setPlayerList(ArrayList<Player> newPlayerList) {playerList = newPlayerList;}

    public void addNewPlayer(String hostname, int port) {
        int newCoordinates[];
        Random generator = new Random();
        do {
            newCoordinates = new int[]{generator.nextInt(BOARD_SIZE), generator.nextInt(BOARD_SIZE)};
        } while (doesCoordinateExist(newCoordinates));
        boolean isLeader = false;
        if (playerList.isEmpty())
            isLeader = true;
        Player newPlayer = new Player(isLeader, newCoordinates, hostname, port);
        if (doesPlayerExist(newPlayer))
            Log.e(null, "Tried to add a player that already exists! Player is: " + newPlayer.getHost());
        else
            playerList.add(newPlayer);
    }

    private boolean doesCoordinateExist(int[] newCoordinates){
        for (int[] coordinate : coordinatesList) {
            if(Arrays.equals(coordinate, newCoordinates))
                return true;
        }
        return false;
    }

    private boolean doesPlayerExist(Player newPlayer){
        for (Player player : playerList) {
            if (player.getHost().equals(newPlayer.getHost()))
                return true;
        }
        return false;
    }

    public Hashtable<String, ArrayList<Player>> consensus;
    public Hashtable<String, ArrayList<Player>> getConsensus() {return consensus;}
    public void initializeConsensus() {consensus = new Hashtable<>();}
    public void setConsensus(Hashtable<String, ArrayList<Player>> newConsensus) {consensus = newConsensus;}

    private String ownHostName;
    public void setOwnHostName(String newHostName) {ownHostName = newHostName;}
    public String getOwnHostName() {return ownHostName;}

    private ArrayList<Integer> lastPositions;
    public void initializeLastPositions() {lastPositions = new ArrayList<>();}
    public ArrayList<Integer> getLastPositions() {return lastPositions;}
    public void setLastPositions(ArrayList<Integer> newLastPositions) {lastPositions = newLastPositions;}

    private static final PlayerListSingleton singleton = new PlayerListSingleton();

    public static PlayerListSingleton getInstance() {return singleton;}
}
