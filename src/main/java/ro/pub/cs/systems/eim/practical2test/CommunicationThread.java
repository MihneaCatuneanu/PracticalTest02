package ro.pub.cs.systems.eim.practical2test;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;


public class CommunicationThread extends Thread {

    private final ServerThread serverThread;
    private final Socket socket;

    // Constructor of the thread, which takes a ServerThread and a Socket as parameters
    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    // run() method: The run method is the entry point for the thread when it starts executing.
    // It's responsible for reading data from the client, interacting with the server,
    // and sending a response back to the client.
    @Override
    public void run() {
        // It first checks whether the socket is null, and if so, it logs an error and returns.
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            // Create BufferedReader and PrintWriter instances for reading from and writing to the socket
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type!");

            // Read the city and informationType values sent by the client
            String name = bufferedReader.readLine();
            String informationType = bufferedReader.readLine();
            if (name == null || name.isEmpty() || informationType == null || informationType.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }

            // It checks whether the serverThread has already received the weather forecast information for the given city.
            HashMap<String, PokemonInformation> data = serverThread.getData();
            PokemonInformation pokemonInformation;
            if (data.containsKey(name)) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                pokemonInformation = data.get(name);
            } else {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                String pageSourceCode = "";

                // make the HTTP request to the web service
                HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS + name);
                HttpResponse httpGetResponse = httpClient.execute(httpGet);
                HttpEntity httpGetEntity = httpGetResponse.getEntity();
                if (httpGetEntity != null) {
                    pageSourceCode = EntityUtils.toString(httpGetEntity);
                }
                if (pageSourceCode == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                    return;
                } else Log.i(Constants.TAG, pageSourceCode);

                // Parse the page source code into a JSONObject and extract the needed information
                JSONObject content = new JSONObject(pageSourceCode);
                JSONArray abilitiesArray = content.getJSONArray(Constants.ABILITIES);
                JSONObject abilities;
                StringBuilder condition1 = new StringBuilder();
                for (int i = 0; i < abilitiesArray.length(); i++) {
                    abilities = abilitiesArray.getJSONObject(i);
                    condition1.append(abilities.getString(Constants.NAME)).append(" : ").append(abilities.getString(Constants.EMPTY_STRING));

                    if (i < abilitiesArray.length() - 1) {
                        condition1.append(";");
                    }
                }

                JSONObject content2 = new JSONObject(pageSourceCode);
                JSONArray typesArray = content2.getJSONArray(Constants.TYPES);
                JSONObject types;
                StringBuilder condition2 = new StringBuilder();
                for (int i = 0; i < typesArray.length(); i++) {
                    types = typesArray.getJSONObject(i);
                    condition2.append(types.getString(Constants.TYPE)).append(" : ").append(types.getString(Constants.EMPTY_STRING));

                    if (i < typesArray.length() - 1) {
                        condition2.append(";");
                    }
                }

                // Create a object with the information extracted from the JSONObject
                pokemonInformation = new PokemonInformation(pageSourceCode.toString() , pageSourceCode.toString());
                //pokemonInformation = new PokemonInformation(content,content2);

                // Cache the information for the given
                serverThread.setData(name, pokemonInformation);
            }

            if (pokemonInformation == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Weather Forecast Information is null!");
                return;
            }

            // Send the information back to the client
            String result;
            switch (informationType) {
                case Constants.ALL:
                    result = pokemonInformation.toString();
                    break;
                case Constants.ABILITIES:
                    result = pokemonInformation.getAbilitysPokemon();
                    break;
                case Constants.TYPES:
                    result = pokemonInformation.getTypesPokemon();
                    break;
                default:
                    result = "[COMMUNICATION THREAD] Wrong information type (all / temperature / wind_speed / condition / humidity / pressure)!";
            }

            // Send the result back to the client
            printWriter.println(result);
            printWriter.flush();
        } catch (IOException | JSONException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                socket.close();
            } catch (IOException ioException) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                if (Constants.DEBUG) {
                    ioException.printStackTrace();
                }
            }
        }
    }

}
