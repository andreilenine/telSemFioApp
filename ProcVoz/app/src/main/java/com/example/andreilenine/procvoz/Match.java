package com.example.andreilenine.procvoz;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

public class Match extends AppCompatActivity {

    private boolean recordStart = true;
    private boolean isRecording = false;
    private boolean done = false;

    private boolean playStart = true;
    private boolean isPlaying = false;

    private boolean match = true;

    private AudioTrack mPlayer = null;

    private AudioRecord mRecorder = null;

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private int bufferSize;

    private int playAudioSize;

    private ArrayList<Short> audioShorts = null;

    private Thread recordingThread = null;

    private Thread playingThread = null;

    private Thread checkUpdateThread = null;

    private final static String LOG_TAG = "ProcVoz Record";

    private ArrayList <String> names;

    private ArrayList <String> audios;

    private String turn;

    private String source;

    private int round;

    private int sourceIndex;

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_match);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        getData();
        generateUI();
    }

    @Override
    public void onStart() {
        super.onStart();
        checkUpdateThread = new Thread(new Runnable() {
            public void run() {
                checkUpdate(names, audios, source, turn, round);
            }
        }, "UpdateCheck Thread");
        checkUpdateThread.start();
    }

    @Override
    public void onStop() {
        super.onStop();

        match = false;
        recordingThread = null;
        playingThread = null;
        checkUpdateThread = null;

        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
            recordingThread = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void generateUI() {
        MyCustomAdapter adapter = new MyCustomAdapter(getApplicationContext(), names, source, turn);

        ListView lv = (ListView) findViewById(R.id.players_list);
        lv.setAdapter(adapter);
    }

    private void getData() {
        Intent intent = getIntent();

        String tempNames[] = intent.getStringArrayExtra(Lobby.EXTRA_PLAYERS_NAMES);
        names = new ArrayList<>(tempNames.length);
        Collections.addAll(names, tempNames);

        String tempAudios[] = intent.getStringArrayExtra(Lobby.EXTRA_PLAYERS_NAMES);
        audios = new ArrayList<>(tempAudios.length);
        Collections.addAll(audios, tempAudios);

        source = intent.getStringExtra(Lobby.EXTRA_SOURCE);

        turn = intent.getStringExtra(Lobby.EXTRA_TURN);

        round = intent.getIntExtra(Lobby.EXTRA_ROUND, 1);
    }

    private void checkUpdate(ArrayList <String> tempNames, ArrayList <String> tempAudios, String tempSource,
                             String tempTurn, int tempRound) {
        String url = "http://10.0.2.2:5000/refresh";
        while (match) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.d(LOG_TAG, ": " + e.getMessage());
            }

            Ion.with(context)
                .load(url)
                .setLogging("MyLogs", Log.DEBUG)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        serverResponse(e, result);
                    }
                });

            if (!names.equals(tempNames) || !audios.equals(tempAudios) || !source.equals(tempSource) ||
                    !turn.equals(tempTurn) || round != tempRound) {
                tempNames = names;
                tempAudios = audios;
                tempSource = source;
                tempTurn = turn;
                tempRound = round;

                Log.d(LOG_TAG," Updated UI");

                Message msg = handler.obtainMessage(1);
                handler.sendMessage(msg);
            }
        }
    }

    private void serverResponse(Exception e, JsonObject result) {
        if (e != null)
            Log.d(LOG_TAG, ": " + e.getMessage());
        else {
            JsonArray players = result.getAsJsonArray("players");
            Log.v("Res:", " " + players.get(0).getAsJsonObject().get("name").getAsString());

            int size = players.size();

            String tempNames[] = new String[size];
            String tempAudios[] = new String[size];

            for (int i = 0; i < size; i++) {
                tempNames[i] = players.get(i).getAsJsonObject().get("name").getAsString();
                tempAudios[i] = players.get(i).getAsJsonObject().get("audio").getAsString();
            }

            Collections.addAll(names, tempNames);
            Collections.addAll(audios, tempAudios);

            turn = result.get("player_turn").getAsString();

            source = result.get("player_src").getAsString();

            round = result.get("round").getAsInt();

        }
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            generateUI();
        }
    };

    public void record(View view) {
        if (!isPlaying) {
            if (recordStart) {
                startRecording();
            } else {
                stopRecording();
            }
            recordStart = !recordStart;
        }
    }

    public void play(View view) {
        if (!isRecording && done) {
            if (playStart) {
                startPlaying();
            } else {
                stopPlaying();
            }
            playStart = !playStart;
        }
    }

    private void startPlaying() {
        for (int i = 0; i < audios.size(); i++)
            if (names.get(i).equals(source)) {
                sourceIndex = i;
                if (audios.get(i) != null) {
                    playAudioSize = audios.get(sourceIndex).length()/2;
                    mPlayer = new AudioTrack(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build(),
                            new AudioFormat.Builder()
                                    .setEncoding(RECORDER_AUDIO_ENCODING)
                                    .setSampleRate(RECORDER_SAMPLERATE / 2)
                                    .build(),
                            bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

                    mPlayer.play();
                    isPlaying = true;

                    playingThread = new Thread(new Runnable() {
                        public void run() {
                            mPlayer.write(shortFromString(audios.get(sourceIndex)), 0, playAudioSize);
                        }
                    }, "AudioPlayer Thread");
                    playingThread.start();
                }
                break;
            }
    }

    private void stopPlaying() {
        isPlaying = false;
        playingThread = null;

        mPlayer.stop();
        mPlayer.release();
        mPlayer = null;
    }

    private void startRecording() {
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);

        mRecorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            public void run() {
                generateAudio();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
        done = false;
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        isRecording = false;

        while (!done) ;

        recordingThread = null;

        int recAudioSize;

        if ((recAudioSize = audioShorts.size()) > 0) {
            for (int i = 0; i < audios.size(); i++)
                if (names.get(i).equals(turn)) {
                    String audio = new String (byteFromSHORT(audioShorts, recAudioSize));
                    audios.set(i, audio);
                    sendAudio(audio);
                    break;
                }
        }
    }

    private void sendAudio (String audio){
        String url = "http://10.0.2.2:5000/send_audio";
        final JsonObject json = new JsonObject();
        json.addProperty("audio", audio);

        Log.d(LOG_TAG," Trying to send audio");

        Ion.with(context)
                .load(url)
                .setLogging("MyLogs", Log.DEBUG)
                .setJsonObjectBody(json)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(Exception e, JsonObject result) {
                        if (e != null)
                            Log.d(LOG_TAG, ": " + e.getMessage());
                    }
                });
    }

    private void generateAudio() {
        short data[] = new short[bufferSize];
        audioShorts = new ArrayList<>();
        int readShorts;

        while (isRecording && ((readShorts = mRecorder.read(data, 0, bufferSize)) > 0))
            Collections.addAll(audioShorts, SHORTFromShort(data, readShorts));
        done = true;
    }

    private Short[] SHORTFromShort (short[] sData, int sArraySize) {
        Short[] Shorts = new Short[sArraySize];
        for (int i = 0; i < sArraySize; i++)
            Shorts[i] = sData[i];
        return Shorts;
    }

    private byte[] byteFromSHORT (ArrayList <Short> audioInShorts, int SArraySize) {
        ByteBuffer Bytes = ByteBuffer.allocate(SArraySize*2);
        for (int i = 0; i <SArraySize; i++)
            Bytes.putShort(i*2, audioInShorts.get(i));
        return Bytes.array();
    }

    private short[] shortFromString (String audio) {
        short[] shorts = new short[audio.length()/2];
        byte[] bytes = audio.getBytes();
        ByteBuffer Bytes = ByteBuffer.wrap(bytes);
        for (int i = 0; i < shorts.length; i++)
            shorts[i] = Bytes.getShort(2*i);
        return shorts;
    }

    public void exit(View view) {
        String url = "http://10.0.2.2:5000/disconnect";
        Log.d(LOG_TAG," Trying to disconnect");

        Ion.with(context)
                .load(url)
                .setLogging("MyLogs", Log.DEBUG);
        onStop();
        finish();
    }

}