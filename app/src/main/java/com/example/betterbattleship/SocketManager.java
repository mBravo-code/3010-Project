package com.example.betterbattleship;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;

import common.PlayerListSingleton;

import static common.utils.convertStateToJSON;
import static common.utils.getPlayerFromList;

public class SocketManager {

    private static final int SERVER_PORT = 8888;
    public static class SocketWrite extends AsyncTask {

        JSONObject json;
        InetSocketAddress socketAddress;
        private static final int SERVER_PORT = 8888;

        public SocketWrite(JSONObject json, String host){
            this.json = json;
            socketAddress = new InetSocketAddress(host, SERVER_PORT);
            Log.e(null, "Spawned an socket writer in one of the threads");
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            Log.e(null, "Writer thread executed");
            Socket socket = new Socket();

            if (socketAddress.getHostName().equals(PlayerListSingleton.getInstance().getOwnHostName())) {
                Log.e(null, "Trying to send message to self");
                return null;
            }

            try {
                socket.bind(null);
                Log.e(null, "Now connecting to socket" + socketAddress.toString());
                socket.connect(socketAddress, 500);

                try (OutputStreamWriter out = new OutputStreamWriter(
                        socket.getOutputStream(), Charset.forName("UTF-8"))) {
                    out.write(json.toString());
                    Log.e(null, "Wrote " + json.toString() + " to " + socketAddress);
                }
                catch (Exception e) {
                    Log.e(null, "Exception occurred " + e.toString());
                }
            } catch (Exception e) {
                Log.e(null, "Exception occurred " + e.toString());
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

        public SocketListen(Context context){
            this.context = context;
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected Object doInBackground(Object[] objects) {

            while(true) {
                try {
                    ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                    Log.e(null, "Now listening on port" + SERVER_PORT);
                    Socket client = serverSocket.accept();
                    InputStream inputStream = client.getInputStream();

                    try {
                        JSONObject message = inputStreamToJson(inputStream);
                        Log.d("Message", "Received message: " + message.toString());
                        String type = message.getString("type");
                        String hostname = client.getInetAddress().getHostName();
                        switch (type) {
                            case "joinGame":
                                addPlayerToGame(hostname, client.getPort());
                                Log.e(null, "Player added");
                                break;
                            case "consensus":
                                ArrayList<Player> newState = getArrayListFromMessage(message);
                                modifyConsensus(hostname, newState);
                                break;
                            case "newState":
                                replaceState(message);
                                sendConsensus(hostname);
                                Intent consensusIntent = new Intent("do_consensus");
                                context.sendBroadcast(consensusIntent);
                                break;
                            case "startGame":
                                startGameHandler(message);
                                break;
                            case "endGame":
                                Intent endIntent = new Intent("kill_game");
                                context.sendBroadcast(endIntent);
                        }
                    } catch (Exception e) {
                        Log.e("Socket received", "Invalid message received from socket." + e.toString());
                    }
                    serverSocket.close();
                } catch (IOException e) {
                    Log.e(null, e.toString());
                }
            }
        }

        private ArrayList<Player> getArrayListFromMessage(JSONObject message){
            ArrayList<Player> newState = new ArrayList();
            try {
                JSONArray array = message.getJSONArray("state");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject personJson = array.getJSONObject(i);
                    boolean turn = personJson.getBoolean("turn");

                    int[] coordinatesArray = new int[2];
                    String stringCoords = personJson.getString("coordinates");
                    if(!stringCoords.equals("null")) {
                        String[] withoutbrack = stringCoords.split("\\[|\\]");
                        String[] numberStrs = withoutbrack[1].split(",");
                        coordinatesArray[0] = Integer.parseInt(numberStrs[0]);
                        coordinatesArray[1] = Integer.parseInt(Character.toString(numberStrs[1].charAt(1)));
                    }
                    else{
                        coordinatesArray = null;
                    }

                    String host = personJson.getString("host");
                    int port = personJson.getInt("port");
                    Player newPerson = new Player(turn, coordinatesArray, host, port);
                    newState.add(newPerson);
                }
            }
            catch( Exception e){
                Log.e(null, e.toString());
                Log.e(null, "Unable to create arraylist from message");
            }
            return newState;
        }

        private void replaceState(JSONObject message){
            ArrayList<Player> newState = getArrayListFromMessage(message);
            ArrayList<Player> oldState = PlayerListSingleton.getInstance().getPlayerList();


            for (Player p : oldState){
                Player newPlayerState = getPlayerFromList(newState, p.getHost());
                if(p.getCoordinates() != null && newPlayerState.getCoordinates() == null){
                    Intent intent = new Intent("player_dead");
                    context.sendBroadcast(intent);
                }
            }

            PlayerListSingleton.getInstance().setPlayerList(newState);
            Intent intent = new Intent("refresh_game");
            context.sendBroadcast(intent);
        }

        private void startGameHandler(JSONObject message){
            try {
                String ownHostname = message.getString("hostname");
                PlayerListSingleton.getInstance().setOwnHostName(ownHostname);
                ArrayList<Player> brandNewState = getArrayListFromMessage(message);
                PlayerListSingleton.getInstance().setPlayerList(brandNewState);
                Intent startGameIntent = new Intent("startGame");
                context.sendBroadcast(startGameIntent);
                Log.e(null, "Broadcast sent");
            }
            catch (Exception e){
                Log.e(null, "Error getting hostname from jsonobject");
            }
        }

        private void sendConsensus(String hostname){
            ArrayList<Player> playerList = PlayerListSingleton.getInstance().getPlayerList();
            JSONObject msg;
            try {
                msg = convertStateToJSON(playerList);
                msg.put("type", "consensus");
                for (Player player : playerList) {
                    // Dont send consensus to person that sent newstate
                    if(player.getHost().equals(hostname))
                        continue;
                    try {
                        SocketManager.SocketWrite writer = new SocketManager.SocketWrite(msg, player.getHost());
                        writer.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                    catch (Error e) {
                        Log.e("Send socket", "Failed to send msg");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void addPlayerToGame(String host, int port){
            PlayerListSingleton.getInstance().addNewPlayer(host, port);
            refreshActivity();
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        private void modifyConsensus(String source, ArrayList<Player> state){
            PlayerListSingleton.getInstance().getConsensus().put(source, state);
        }

        private JSONObject inputStreamToJson(InputStream in) throws IOException, JSONException {
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            return new JSONObject(responseStrBuilder.toString());
        }

        private void refreshActivity(){
            Intent intent = new Intent("refresh_activity");
            context.sendBroadcast(intent);
        }
    }
}
