package com.example.betterbattleship;

import android.content.BroadcastReceiver;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class Lobby extends AppCompatActivity {

    ArrayList<Player> playerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lobby);

        BroadcastReceiver receiver = getIntent().getParcelableExtra("Broadcast Receiver");
    }
}