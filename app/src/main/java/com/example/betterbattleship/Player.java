package com.example.betterbattleship;

import java.net.InetAddress;

public class Player {
    boolean turn;
    public int coordinates[];
    InetAddress host;
    int port;

    public Player(boolean turn, int coordinates[], InetAddress host, int port) {
        this.turn = turn;
        this.coordinates = coordinates;
        this.host = host;
        this.port = port;
    }

    public boolean getTurn() {
        return turn;
    }

    public int[] getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(int[] newCoodinates) {
        this.coordinates = newCoodinates;
    }

    public InetAddress getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void killPlayer(){
        coordinates = null;
    }
}