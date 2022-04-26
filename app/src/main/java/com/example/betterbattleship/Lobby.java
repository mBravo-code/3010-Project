package com.example.betterbattleship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import com.example.betterbattleship.gamePage.GameActivity;

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

        Log.e(null, "Playerlist is now: " + playerList.toString());

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

        IntentFilter newIntentFilter = new IntentFilter();
        newIntentFilter.addAction("refresh_activity");
        registerReceiver(broadcastReceiver, newIntentFilter);

        Button startButton = (Button) findViewById(R.id.StartGame);
        startButton.setOnClickListener(view -> startGame(view));

        populatePlayerList();
    }

    public void startGame(View view) {

        Log.e(null, "Now starting the game");
        JSONObject msg;
        try {
            msg = convertStateToJSON(playerList);
            msg.put("type", "startGame");
            for (Player player : playerList) {
                msg.put("hostname", player.getHost());
                Log.e(null, "Adding player" + player.toString());
                try {
                    Log.e(null, "Message prepared is " + msg.toString());
                    SocketManager.SocketWrite writer = new SocketManager.SocketWrite(msg, player.getHost());
                    writer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);;
                }
                catch (Error e) {
                    Log.e("Send socket", "Failed to send msg");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PlayerListSingleton.getInstance().initializeLastPositions();
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void populatePlayerList(){
        TableLayout tableLayout = findViewById(R.id.Player_Table);
        for (Player p : playerList) {
            TableRow newRow = new TableRow(this);
            TextView playerNameView = new TextView(this);
            playerNameView.setText(p.getHost());
            newRow.addView(playerNameView);
            tableLayout.addView(newRow);
        }
    }

}