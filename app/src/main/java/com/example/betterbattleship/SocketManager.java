package com.example.betterbattleship;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;

import common.PlayerListSingleton;

public class SocketManager {

    private static final int SERVER_PORT = 8888;
    public static class SocketWrite extends AsyncTask {

        JSONObject json;
        InetSocketAddress socketAddress;
        private static final int SERVER_PORT = 8888;

        public SocketWrite(JSONObject json, InetAddress host){
            this.json = json;
            socketAddress = new InetSocketAddress(host, SERVER_PORT);
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

        public SocketListen(Context context){
            this.context = context;
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


                } catch (IOException e) {
                }
            return null;
            }
        }

        private void addPlayerToGame(InetAddress host, int port){
            Player newPlayer = new Player(false, new int[] {0,0}, host, port);
            PlayerListSingleton.getInstance().getPlayerList().add(newPlayer);
            Intent intent = new Intent("refresh_activity");
            context.sendBroadcast(intent);
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
