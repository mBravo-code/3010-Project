package com.example.betterbattleship;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.betterbattleship.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

import android.content.BroadcastReceiver;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.widget.Button;

import java.net.Socket;
import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CHANGE_WIFI_STATE;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    WifiP2pManager manager;
    Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;
    ArrayList<Player> players;

    private static final String TAG = "Wifi test=========";
    private int PERMISSIONS_REQUEST_LOCATION = 100;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize wifi stuff

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

        // Everything down here is boilerplate stuff

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button hostButton = (Button) findViewById(R.id.HostButton);
        hostButton.setOnClickListener(v -> createGame(v));

        Button joinButton = (Button) findViewById(R.id.JoinButton);
        joinButton.setOnClickListener(v -> joinGame(v));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, players);
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

                        // create listener socket
                        WiFiDirectBroadcastReceiver.SocketListen listener = new WiFiDirectBroadcastReceiver.SocketListen(getApplicationContext(), players);
                        listener.execute();
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
    }

    public void startGame(View view) {
        Log.e(null, "Now starting the game");
        ((WiFiDirectBroadcastReceiver )receiver).startGame();
    }

    public void sendState(View view) {
        Log.e(null, "Now trying sending state");
        ((WiFiDirectBroadcastReceiver )receiver).sendState();
    }

    private void joinGame(View view){
        Log.e(null, "Now trying to join the game");
        ((WiFiDirectBroadcastReceiver )receiver).sendRequestToJoin();
    }

}