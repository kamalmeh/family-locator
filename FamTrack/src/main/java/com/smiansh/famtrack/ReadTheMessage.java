package com.smiansh.famtrack;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Locale;

public class ReadTheMessage extends Service implements TextToSpeech.OnInitListener {

    private TextToSpeech tts = null;
    private String msg = "";

    public ReadTheMessage() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        msg = intent.getStringExtra("MESSAGE");
        tts = new TextToSpeech(this, this);
        tts.setLanguage(Locale.ENGLISH);
        Log.i("READ_THE_MESSAGE", msg);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // OnInitListener impl
    public void onInit(int status) {
        tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "The Utterence ID");
    }
}