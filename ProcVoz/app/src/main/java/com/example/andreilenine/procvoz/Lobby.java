package com.example.andreilenine.procvoz;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

public class Lobby extends AppCompatActivity {

    private final static String LOG_TAG = "ProcVoz Record";
    private final String [] permissions = {Manifest.permission.RECORD_AUDIO};
    public final static String EXTRA_PLAYERS_NAMES = "names";
    public final static String EXTRA_PLAYERS_AUDIOS = "audios";
    public final static String EXTRA_ROUND = "round";
    public final static String EXTRA_SOURCE = "source";
    public final static String EXTRA_TURN = "turn";
    private boolean permissionToRecordAccepted = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        setContentView(R.layout.activity_lobby);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    public void searchMatch (View view){
        EditText et = (EditText) findViewById(R.id.nick);
        TextView tv = (TextView) findViewById(R.id.response);
        String nick = et.getText().toString();
        if (nick.compareTo("") == 0)
            tv.setText(R.string.empty_nick);
        else
            if (nick.length() > 8){
                tv.setText(R.string.big_nick);
                et.setText("");
            }
            else
            {
                tv.setText(R.string.searching);
                Button b = (Button) findViewById(R.id.search);
                b.setVisibility(View.GONE);
                ProgressBar pb = (ProgressBar) findViewById(R.id.waiting);
                pb.setVisibility(View.VISIBLE);

                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Log.d(LOG_TAG, ": " + e.getMessage());
                }
                clientRequest(nick);
            }
    }

    private void clientRequest (String nick) {
        //String url = "http://192.168.1.107:5000/connect";
        String url = "http://10.0.2.2:5000/connect";
        final JsonObject json = new JsonObject();
        json.addProperty("name", nick);

        Log.d(LOG_TAG," Trying to connect");

        Ion.with(context)
            .load(url)
            .setLogging("MyLogs", Log.DEBUG)
            .setJsonObjectBody(json)
            .asJsonObject()
            .setCallback(new FutureCallback<JsonObject>() {
                @Override
                public void onCompleted(Exception e, JsonObject result) {
                    serverResponse(e, result);
                }
            });
    }

    private void serverResponse(Exception e, JsonObject result) {
        TextView tv = (TextView) findViewById(R.id.response);
        if (e != null) {
            tv.setText(R.string.error);
            Log.d(LOG_TAG, ": " + e.getMessage());

            try {
                Thread.sleep(2000);
            }
            catch (InterruptedException e1) {
                Log.d(LOG_TAG, ": " + e1.getMessage());
            }

            tv.setText("");

            Button b = (Button) findViewById(R.id.search);
            b.setVisibility(View.VISIBLE);

            ProgressBar pb = (ProgressBar) findViewById(R.id.waiting);
            pb.setVisibility(View.GONE);
        }
        else {
            tv.setText(R.string.found);

            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e2) {
                Log.d(LOG_TAG, ": " + e2.getMessage());
            }

            tv.setText("");

            Button b = (Button) findViewById(R.id.search);
            b.setVisibility(View.VISIBLE);

            ProgressBar pb = (ProgressBar) findViewById(R.id.waiting);
            pb.setVisibility(View.GONE);

            JsonArray players = result.getAsJsonArray("players");
            Log.v("Res:", " 1" + players.get(0).getAsJsonObject().get("name").getAsString());

            int size = players.size();

            String names[] = new String[size];
            String audios[] = new String[size];

            for (int i = 0; i < size; i++) {
                names[i] = players.get(i).getAsJsonObject().get("name").getAsString();
                audios[i] = players.get(i).getAsJsonObject().get("audio").getAsString();
            }

            String turn = result.get("player_turn").getAsString();

            String source = result.get("player_src").getAsString();

            int round = result.get("round").getAsInt();

            Intent intent = new Intent(context, Match.class);
            intent.putExtra(EXTRA_PLAYERS_NAMES, names);
            intent.putExtra(EXTRA_PLAYERS_AUDIOS, audios);
            intent.putExtra(EXTRA_TURN, turn);
            intent.putExtra(EXTRA_SOURCE, source);
            intent.putExtra(EXTRA_ROUND, round);
            startActivity(intent);
        }
    }

    public void exit(View view) {
        finish();
    }

}
