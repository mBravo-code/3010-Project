package com.example.betterbattleship;

public class Player {
    boolean turn;
    public int[] coordinates;
    String host;
    int port;

    public Player(boolean turn, int[] coordinates, String host, int port) {
        this.turn = turn;
        this.coordinates = coordinates;
        this.host = host;
        this.port = port;
    }

    public boolean getTurn() {
        return turn;
    }
    public void setTurn(boolean turn) {
        this.turn = turn;
    }

    public int[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(int[] newCoodinates) {
        this.coordinates = newCoodinates;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void killPlayer(){
        coordinates = null;
    }
}