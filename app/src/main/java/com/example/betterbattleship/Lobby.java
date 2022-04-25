package com.example.betterbattleship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import common.PlayerListSingleton;

import static common.utils.convertStateToJSON;

public class Lobby extends AppCompatActivity {

    ArrayList<Player> playerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lobby);

        playerList = PlayerListSingleton.getInstance().getPlayerList();

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                if (action.equals("refresh_activity")) {
                    finish();
                    startActivity(getIntent());
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("refresh_activity"));

        Button startButton = (Button) findViewById(R.id.StartGame);
        startButton.setOnClickListener(v -> {startGame(v);});
    }

    public void startGame(View view) {
        Log.e(null, "Now starting the game");
        JSONObject msg;
        try {
            msg = convertStateToJSON(playerList);
            msg.put("type", "startGame");
            for (Player player : playerList) {
                try {
                    SocketManager.SocketWrite writer = new SocketManager.SocketWrite(msg, player.getHost());
                    writer.execute();
                }
                catch (Error e) {
                    Log.e("Send socket", "Failed to send msg");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}