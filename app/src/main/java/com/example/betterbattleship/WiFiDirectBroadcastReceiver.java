package com.example.betterbattleship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity;
    private ArrayList<Player> players;
    private boolean isConnected;

    private static final int SERVER_PORT = 8888;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity, ArrayList<Player> players) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
        this.players = players;
        isConnected = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e("intent", intent.getAction());

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            Log.e("State is now: ", "" + state);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                //NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                //this.isConnected = networkInfo.isConnected();
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

            if (manager == null) {
                return;
            }


        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

    public void sendRequestToJoin() {

        // We are connected with the other device, request connection
        // info to find group owner IP
        manager.requestConnectionInfo(channel, connectionListener);

        if (isConnected) {

            Log.e(null, "ConnectionListener executed");

            try {
                JSONObject request = new JSONObject();
                request.put("type", "joinGame");

                // Sends request to join
                WiFiDirectBroadcastReceiver.SocketWrite writer = new WiFiDirectBroadcastReceiver.SocketWrite(request, hostAddress, SERVER_PORT);
                writer.execute();
            } catch (JSONException e) {
                Log.e("JSONERROR", e.toString());
            }
        }
    }


    public void startGame() {
        JSONObject msg;
        try {
            msg = convertStateToJSON(this.players);
            msg.put("type", "startGame");
            for (Player player : players) {
                try {
                    WiFiDirectBroadcastReceiver.SocketWrite writer = new WiFiDirectBroadcastReceiver.SocketWrite(msg, player.getHost(), player.port);
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

    public void sendState() {
        JSONObject msg;
        try {
            msg = convertStateToJSON(this.players);
            msg.put("type", "newState");
            for (Player player : players) {
                try {
                    WiFiDirectBroadcastReceiver.SocketWrite writer = new WiFiDirectBroadcastReceiver.SocketWrite(msg, player.getHost(), player.port);
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

    private JSONObject convertStateToJSON(ArrayList<Player> players){
        JSONArray allDataArray = new JSONArray();

        for(int i = 0; i < players.size(); i++) {
            JSONObject eachData = new JSONObject();
            try {
                eachData.put("turn", players.get(i).getTurn());
                eachData.put("coordinates", players.get(i).coordinates);
                eachData.put("host", players.get(i).getHost());
                eachData.put("port", players.get(i).getPort());
            } catch ( JSONException e) {
                e.printStackTrace();
            }
            allDataArray.put(eachData);
        }

        JSONObject state = new JSONObject();
        try {
            state.put("state", allDataArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return state;
    }

    private ArrayList<WifiP2pDevice> peers = new ArrayList<>();

    private WifiP2pManager.PeerListListener peerListListener = peerList -> {

        List<WifiP2pDevice> refreshedPeers = new ArrayList<>(peerList.getDeviceList());
        if (!refreshedPeers.equals(peers)) {
            peers.clear();
            peers.addAll(refreshedPeers);


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

    public ArrayList<WifiP2pDevice> getListOfPeers(){
        return peers;
    }

    // methods for handling host address
    private InetAddress hostAddress;

    private WifiP2pManager.ConnectionInfoListener connectionListener = info -> {
        hostAddress = info.groupOwnerAddress;
        this.isConnected = info.groupFormed;
    };

    private InetAddress getGroupIP(){
        return hostAddress;
    }

    public class SocketWrite extends AsyncTask {

        JSONObject json;
        InetSocketAddress socketAddress;

        public SocketWrite(JSONObject json, InetAddress host, int port){
            this.json = json;
            socketAddress = new InetSocketAddress(host, port);
        }

        @Override
        protected Object doInBackground(Object[] objects) {

            Socket socket = new Socket();

            try {
                socket.bind(null);
                Log.e(null, "Now connecting to socket" + socketAddress.toString());
                socket.connect(socketAddress, 500);

                try (OutputStreamWriter out = new OutputStreamWriter(
                        socket.getOutputStream(), Charset.forName("UTF-8"))) {
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
            return null;
        }
    }

    public static class SocketListen extends AsyncTask {

        private Context context;
        ArrayList<Player> players;

        public SocketListen(Context context, ArrayList<Player> players){
            this.context = context;
            this.players = players;
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            while(true) {
                try {
                    ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                    Log.e(null, "Now listening on port" + SERVER_PORT);
                    Socket client = serverSocket.accept();
                    InputStream inputStream = client.getInputStream();

                    try{
                        JSONObject message = inputStreamToJson(inputStream);
                        Log.d("Message", "Received message: " + message.toString());
                        String type = message.getString("type");
                        switch(type){
                            case "joinGame":
                                addPlayerToGame(client.getInetAddress(), client.getPort());
                                Log.e(null, "Player added");
                                break;
                            case "consensus":
                                keepConsensus();
                                break;
                        }
                    } catch (Exception e){
                        Log.e("Socket received", "Invalid message received from socket." + e.toString());
                    }

                    return null;

                } catch (IOException e) {
                    return null;
                }
            }
        }

        private void addPlayerToGame(InetAddress host, int port){
            Player newPlayer = new Player(false, new int[] {0,0}, host, port);
            this.players.add(newPlayer);
        }

        private void keepConsensus(){
            return;
        }

        private JSONObject inputStreamToJson(InputStream in) throws IOException, JSONException {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            return new JSONObject(responseStrBuilder.toString());
        }
    }

}
