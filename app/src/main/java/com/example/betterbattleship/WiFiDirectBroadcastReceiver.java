package com.example.betterbattleship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e("intent", intent.getAction());
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            Log.e("State is now: ", "" + state);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
            } else {
            }

            // Check to see if Wi-Fi is enabled and notify appropriate activity
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (manager != null) {
                manager.requestPeers(channel, peerListListener);
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

    private ArrayList<WifiP2pDevice> peers = new ArrayList<>();
    private WifiP2pManager.PeerListListener peerListListener = peerList -> {

        List<WifiP2pDevice> refreshedPeers = new ArrayList<>(peerList.getDeviceList());
        if (!refreshedPeers.equals(peers)) {
            peers.clear();
            peers.addAll(refreshedPeers);

            for (WifiP2pDevice peer : peers) {
                Log.e("PEERS: ", peer.deviceName);
            }


            // If an AdapterView is backed by this data, notify it
            // of the change. For instance, if you have a ListView of
            // available peers, trigger an update.
//                ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

            // Perform any other updates needed based on the new list of
            // peers connected to the Wi-Fi P2P network.
        }

        if (peers.size() == 0) {
            Log.d("Discovery of peers", "No devices found");
            return;
        }
    };

    public void sendSocket(JSONObject json, String host, int port) {
        Socket socket = new Socket();

        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), 500);

            try (OutputStreamWriter out = new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8)) {
                out.write(json.toString());
            }
        } catch (IOException e) {
            //catch logic
        }

        finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        //catch logic
                    }
                }
            }
        }
    }

    public static class SocketListen extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] objects) {
            while(true) {
                try {
                    ServerSocket serverSocket = new ServerSocket(8888);
                    Socket client = serverSocket.accept();
                    InputStream inputStream = client.getInputStream();
                    return null;

                } catch (IOException e) {
                    return null;
                }
            }
        }
    }
}
