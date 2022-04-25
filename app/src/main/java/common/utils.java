package common;

import com.example.betterbattleship.Player;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class utils {
    public static JSONObject convertStateToJSON(ArrayList<Player> players){
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
}
