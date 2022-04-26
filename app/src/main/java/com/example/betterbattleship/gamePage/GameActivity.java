package com.example.betterbattleship.gamePage;

import static common.utils.convertStateToJSON;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.example.betterbattleship.Player;
import com.example.betterbattleship.R;
import com.example.betterbattleship.SocketManager;

import org.json.JSONException;
import org.json.JSONObject;

import common.PlayerListSingleton;


public class GameActivity extends AppCompatActivity {

    private static final int ITEMS_PER_ROW = 5;
    private static final int TOTAL_NUM_TILES = 25;

    private RecyclerView gameRecyclerView;
    RecyclerView.LayoutManager layoutManager;

    private Button btnShoot;
    private Button btnMove;
    private GameViewAdapter adapter;
    private ArrayList<Integer> enemyPositions;
    private Player thisPlayer;
    int currentPosition;
    private boolean isTurn;
    private ArrayList<Player> players = PlayerListSingleton.getInstance().getPlayerList();
    TextView gameTextView;
    boolean gameOver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        gameTextView = (TextView)findViewById(R.id.screen_textView);

        BroadcastReceiver playerDeadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, @NonNull Intent intent) {
                String action = intent.getAction();
                if (action.equals("player_dead")) {
                    Toast.makeText(getApplicationContext(), "A player has been eliminated from the game.", Toast.LENGTH_SHORT).show();
                }
            }
        };

        BroadcastReceiver refreshGameReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, @NonNull Intent intent) {
                String action = intent.getAction();
                if (action.equals("refresh_game")) {
                    finish();
                    startActivity(getIntent());
                }
            }
        };

        BroadcastReceiver endGameReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, @NonNull Intent intent) {
                String action = intent.getAction();
                if (action.equals("kill_game")) {
                    Toast.makeText(getApplicationContext(), "Game has ended.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        };

        registerReceiver(playerDeadReceiver, new IntentFilter("player_dead"));
        registerReceiver(refreshGameReceiver, new IntentFilter("refresh_game"));
        registerReceiver(endGameReceiver, new IntentFilter("kill_game"));

        this.thisPlayer = getThisPlayer();
        this.currentPosition = getCurrentPosition(thisPlayer);
        this.isTurn = getTurn(thisPlayer);
        this.enemyPositions = getEnemyPositions();
        this.isTurn = getTurn(thisPlayer);
        this.gameOver = isGameOver();

        if(gameOver){
            if(isWinner()){
                gameTextView.setText("Game Over: You Won!");
            }
            else{
                gameTextView.setText("Game Over: You Lost");
            }
        }
        else if(currentPosition == -1){
            gameTextView.setText("Your have been eliminated");
            if(isTurn){
                playerIsDead();
            }
        }
        else{
            if(isTurn){
                gameTextView.setText("Your turn");
            }
            else{
                gameTextView.setText("Waiting for players to make move");
            }
        }

        initializeView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void initializeView() {

        gameRecyclerView = findViewById(R.id.game_recycler_view);
        layoutManager = new GridLayoutManager(this, ITEMS_PER_ROW);
        gameRecyclerView.setLayoutManager(layoutManager);

        adapter = new GameViewAdapter(this, TOTAL_NUM_TILES, currentPosition);
        gameRecyclerView.setAdapter(adapter);


        if(!gameOver) {
            btnShoot = findViewById(R.id.btn_shoot);
            btnMove = findViewById(R.id.btn_move);
            btnShoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isTurn) {
                        if (adapter.getSelectedTile() > 0) {
                            shoot(adapter.getSelectedTile());
                        } else {
                            Toast.makeText(getApplicationContext(), "No tile selected", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Not your turn", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            btnMove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isTurn) {
                        if (adapter.getSelectedTile() > 0) {
                            move(adapter.getSelectedTile());
                        } else {
                            Toast.makeText(getApplicationContext(), "No tile selected", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Not your turn", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        else{
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    finish();
                }
            }, 5000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }



    private Player getThisPlayer() {
        for(Player player: this.players){
            String host = PlayerListSingleton.getInstance().getOwnHostName();

            if (player.getHost().equals(PlayerListSingleton.getInstance().getOwnHostName())){
                return player;
            }
        }
        return null;
    }

    private int getCurrentPosition(Player player) {
        int[] coords = player.getCoordinates();
        return coords[0]*ITEMS_PER_ROW + coords[1];
    }
    private boolean getTurn(Player player){
        return player.getTurn();
    }
    private ArrayList<Integer> getEnemyPositions() { 
        ArrayList<Integer> enemies = new ArrayList<Integer>();
        for(Player player: this.players) {
            if (player.getHost() != PlayerListSingleton.getInstance().getOwnHostName()) {
                int[] coords = player.getCoordinates();
                enemies.add(coords[0]*ITEMS_PER_ROW + coords[1]);
            }
        }
        return enemies;
    }
    private void playerIsDead(){
        sendState();
    }


    private void sendState() {
        updateTurn();
        Log.e(null, "Now trying to send the state");
        JSONObject msg;
        try {
            msg = convertStateToJSON(this.players);
            msg.put("type", "newState");
            for (Player player : players) {
                try {
                    SocketManager.SocketWrite writer = new SocketManager.SocketWrite(msg, player.getHost());
                    writer.execute();

                    Intent intent = new Intent("refresh_game");
                    this.sendBroadcast(intent);
                }
                catch (Error e) {
                    Log.e("Send socket", "Failed to send msg");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateTurn(){
        int index = 0;
        Player currPlayer = players.get(index);
        while(!currPlayer.getHost().equals(PlayerListSingleton.getInstance().getOwnHostName())){
            index++;
            currPlayer = players.get(index);
        }
        currPlayer.setTurn(false);
        if(index < players.size()-1){
            players.get(index+1).setTurn(true);
        }
        else{
            players.get(0).setTurn(true);
        }
    }

    private void shoot(int selectedTile) {
        if(enemyPositions.contains(selectedTile)){
            Toast.makeText(getApplicationContext(), "Enemy eliminated", Toast.LENGTH_SHORT).show();
            for(Player player:players){
                int[] coord = player.getCoordinates();
                if(coord[0] == selectedTile/ITEMS_PER_ROW && coord[1] == selectedTile%ITEMS_PER_ROW){
                    player.killPlayer();
                }
            }
        }
        sendState();
    }

    private void move(int selectedTile) {
        if(enemyPositions.contains(selectedTile)) {
            Toast.makeText(getApplicationContext(), "Enemy eliminated", Toast.LENGTH_SHORT).show();
            for(Player player:players){
                int[] coord = player.getCoordinates();
                if(coord[0] == selectedTile/ITEMS_PER_ROW && coord[1] == selectedTile%ITEMS_PER_ROW){
                    player.killPlayer();
                }
            }
        }
        int[] newCoordinates = {selectedTile/ITEMS_PER_ROW, selectedTile%ITEMS_PER_ROW};
        this.thisPlayer.setCoordinates(newCoordinates);
        sendState();
    }

    private boolean isGameOver(){
        int playersAlive = 0;
        for(Player player:players){
            if(player.getCoordinates() != null){
                playersAlive++;
            }
        }
        if(playersAlive >1){
            return false;
        }
        else{
            return true;
        }
    }
    private boolean isWinner(){
        String ownHostName = PlayerListSingleton.getInstance().getOwnHostName();

        for(Player player:players){
            if (player.getHost().equals(ownHostName) && player.getCoordinates() != null){
                return true;
            }
        }
        return false;
    }
}