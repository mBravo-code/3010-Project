package com.example.betterbattleship;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.example.betterbattleship.gamePage.GameActivity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.betterbattleship.databinding.ActivityMainBinding;

import android.content.BroadcastReceiver;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import org.json.JSONException;
import org.json.JSONObject;

import common.PlayerListSingleton;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    WifiP2pManager manager;
    Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;

    private static final String TAG = "Wifi test=========";
    private int PERMISSIONS_REQUEST_LOCATION = 100;
    private static final int SERVER_PORT = 8888;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BroadcastReceiver startGameReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, @NonNull Intent intent) {
                String action = intent.getAction();
                if (action.equals("startGame")) {
                    PlayerListSingleton.getInstance().initializeLastPositions();
                    Intent gameItent = new Intent(getBaseContext(), GameActivity.class);
                    startActivity(gameItent);
                }
            }
        };
        registerReceiver(startGameReceiver, new IntentFilter("startGame"));

        // Manually request location permission. Needed to use discovery
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,ACCESS_FINE_LOCATION)) {

                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
                }
            }
        }

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (!initP2p()) {
            finish();
        }

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e(null, "Found peers");
            }

            @Override
            public void onFailure(int reason) {
                switch (reason){
                    case WifiP2pManager.BUSY: Log.e(null, "Did not find peers. Reason: Busy"); break;
                    case WifiP2pManager.ERROR: Log.e(null, "Did not find peers. Reason: Error"); break;
                    case WifiP2pManager.P2P_UNSUPPORTED: Log.e(null, "Did not find peers. Reason: Unsupported"); break;
                }

            }
        });

        // Adds the header

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);


        Button hostButton = findViewById(R.id.HostButton);
        hostButton.setOnClickListener(v -> createGame(v));

        Button joinButton = findViewById(R.id.JoinButton);
        joinButton.setOnClickListener(v -> joinGame(v));

        PlayerListSingleton.getInstance().listen(getApplicationContext());

        PlayerListSingleton.getInstance().initializePlayerList();
        PlayerListSingleton.getInstance().initializeConsensus();

        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();
                if (action.equals("do_consensus")) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            MainActivity.Consensus c = new MainActivity.Consensus (getApplicationContext());
                            c.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
                    }, 1000);
                }
            }
        };

        IntentFilter newIntentFilter = new IntentFilter();
        newIntentFilter.addAction("do_consensus");
        registerReceiver(broadcastReceiver, newIntentFilter);
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);

    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private boolean initP2p() {
        // Device capability definition check
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }
        // Hardware capability check
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.");
            return false;
        }
        if (!wifiManager.isP2pSupported()) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.");
            return false;
        }
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.");
            return false;
        }
        channel = manager.initialize(this, getMainLooper(), null);
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.");
            return false;
        }
        return true;
    }


    public void createGame(View view){
        ArrayList<WifiP2pDevice> peerList = ((WiFiDirectBroadcastReceiver ) receiver).getListOfPeers();
        String ownHost;
        for (WifiP2pDevice peer : peerList) {

            // add exceptions
            if (peer.primaryDeviceType.equals("7-0050F204-1") ||  peer.primaryDeviceType.equals("3-0050F204-1") || peer.deviceName.equals("DIRECT-63-HP DeskJet 2700 series"))
                continue;

            WifiP2pConfig config = new WifiP2pConfig();

            // We want to be the group owner if we create the game
            config.groupOwnerIntent = 15;
            config.deviceAddress = peer.deviceAddress;

            try {
                manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.e("CONNECTION TO PEER", "SUCCESSFULLY CONNECTED TO PEER" + peer.deviceName);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e("CONNECTION TO PEER", "UNABLE TO CONNECT TO PEER" + peer.deviceName + " Reason is: " + reason);
                    }
                });
            }
            catch (Error e){
                Log.e("Connection", "not doing that for sure");
            }
        }

        if (!((WiFiDirectBroadcastReceiver) receiver).isConnected())
            return;

        // add self to playerlist:
        ownHost = ((WiFiDirectBroadcastReceiver) receiver).getGroupIP().getHostAddress();
        PlayerListSingleton.getInstance().addNewPlayer(ownHost, SERVER_PORT);
        PlayerListSingleton.getInstance().setOwnHostName(ownHost);

        Intent intent = new Intent(this, Lobby.class);
        startActivity(intent);
    }

    private void joinGame(View view){
        Log.e(null, "Now trying to join the game");

        if(((WiFiDirectBroadcastReceiver)receiver).getGroupIP() != null) {
            String hostAddress = ((WiFiDirectBroadcastReceiver) receiver).getGroupIP().getHostAddress();
            Log.e("hostAddress", hostAddress);

            try {
                JSONObject request = new JSONObject();
                request.put("type", "joinGame");
                Log.e("Executing", hostAddress);
                // Sends request to join
                SocketManager.SocketWrite writer = new SocketManager.SocketWrite(request, hostAddress);
                writer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                Log.e("Executing", hostAddress);

            } catch (JSONException e) {
                Log.e("JSONERROR", e.toString());
            }
            Toast.makeText(getApplicationContext(), "Waiting for host to start game", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "No games currently being hosted", Toast.LENGTH_SHORT).show();
        }
    }

    public static class Consensus extends AsyncTask {

        private Context context;

        public Consensus(Context context) {
            this.context = context;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            if (!isConsensusValid()){
                killGame();
            }
            return null;
        }

        private boolean isConsensusValid() {
            ArrayList<Player> myState = PlayerListSingleton.getInstance().getPlayerList();
            Hashtable<String, ArrayList<Player>> consensusList = PlayerListSingleton.getInstance().getConsensus();
            Set<String> setOfKeys = consensusList.keySet();
            boolean traitorFound = false;
            Log.e(null, "My state is: " + myState.toString());
            Enumeration<String> keys = consensusList.keys();
            while(keys.hasMoreElements()){
                Log.e(null, "consensus list is: " + consensusList.get(keys.nextElement()));
            }
            for (String key : setOfKeys) {
                if (traitorFound)
                    break;
                ArrayList<Player> otherPlayerState = consensusList.get(key);
                if (!myState.equals(otherPlayerState))
                    traitorFound = true;
            }
            Log.e(null, "Traitor found? " + traitorFound);
            return !traitorFound;
        }

        private void killGame() {
            Intent intent = new Intent("kill_game");
            context.sendBroadcast(intent);
        }
    }

}