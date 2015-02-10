package com.example.serj.grabadorreproductor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class Reproductor extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnSeekCompleteListener {

    //MEDIAPLAYER
    private MediaPlayer mp;
    private enum Estados{
        idle,
        initialized,
        prepairing,
        prepared,
        started,
        paused,
        completed,
        sttoped,
        end,
        error
    };
    private static Estados estado;
    //LISTA
    private ArrayList<String> canciones = new ArrayList<>();
    private int actual;
    //COMANDOS
    public static final String PLAY="play";
    public static final String STOP="stop";
    public static final String ADD="add";
    public static final String PAUSE="pause";
    public static final String NEXT = "next";
    public static final String PREVIOUS = "previous";
    public static final String PLAY_ITEM = "item";
    private static final int NOTIFY_ID=1;
    private boolean reproducir;
    //SEEKBAR
    public static final String BROADCAST_ACTION = "com.example.serj.grabadorreproductor.seekprogress";
    int mediaPosition;
    int mediaMax;
    private static int songEnded;
    private final Handler handler = new Handler();
    Intent seekIntent;

    /* ******************************************************* */
                      // MÉTODOS OVERRIDE //
    /* ******************************************************* */

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int r = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(r==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            mp = new MediaPlayer();
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            mp.setOnSeekCompleteListener(this);
            estado = Estados.idle;
            seekIntent = new Intent(BROADCAST_ACTION);
        } else {
            stopSelf();
        }

    }

    @Override
    public void onDestroy() {
        if(mp!=null){
            //mp.reset();
            mp.release();
            mp = null;
            stopForeground(true);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(broadcastReceiver, new IntentFilter(Principal.BROADCAST_SEEKBAR));
        String action = intent.getAction();

        if(action.equals(PLAY)){
            play();
        }else if(action.equals(ADD)){
            String dato = intent.getStringExtra("cancion");
            add(dato);
        }else if(action.equals(STOP)){
            stop();
        }else if(action.equals(PAUSE)) {
            pause();
        }else if(action.equals(PREVIOUS)){
            if(canciones.size() > 0 ){
                previous();
            }
        }else if(action.equals(NEXT)){
            if(canciones.size() > 0){
                next();
            }
        }else if(action.equals(PLAY_ITEM)){
            int cancion = intent.getIntExtra("posicion", 0);
            playItem(cancion);
        }
        setupHandler();
        return super.onStartCommand(intent, flags, startId);
    }

    /* ******************************************************* */
                    // METODOS DE AUDIO //
    /* ******************************************************* */

    private void add(String cancion){
        this.canciones.add(cancion);
        actual = 0;
    }

    private void next(){
        if( canciones != null && actual+1 < canciones.size()){
            actual++;
            mp.reset();
            estado = Estados.idle;
            play();
        }
    }

    private void pause() {
        if(estado == Estados.started) {
            mp.pause();
            estado = Estados.paused;
        }
    }

    private void play(){
        if(canciones != null && canciones.size() > 0){
            reproducir = true;
            if(estado == Estados.error){
                estado = Estados.idle;
            }
            if(estado == Estados.idle){
                reproducir = true;
                try {
                    mp.setDataSource(canciones.get(actual));
                    estado = Estados.initialized;
                } catch (IOException e) {
                    estado= Estados.error;
                }
            }
            if(estado == Estados.initialized ||
                    estado == Estados.sttoped){
                reproducir = true;
                mp.prepareAsync();
                estado = Estados.prepairing;
            } else if(estado == Estados.prepairing) {
                reproducir = true;
            }
            if(estado == Estados.prepared ||
                    estado == Estados.paused ||
                    estado == Estados.completed ||
                    estado == Estados.started) {
                mp.start();
                estado = Estados.started;
            }
        }
    }

    private void previous(){
        if( canciones != null && actual-1 >= 0){
            if(mp.getCurrentPosition() < 1000){
                actual--;
                mp.reset();
                estado = Estados.idle;
                play();
            } else {
                mp.seekTo(0);
                play();
            }
        }
    }

    private void stop(){
        if(estado == Estados.prepared ||
                estado == Estados.started ||
                estado == Estados.paused ||
                estado == Estados.completed){
            mp.seekTo(0);
            mp.stop();
            estado = Estados.sttoped;
        }
        reproducir = false;
    }

    private void playItem(int pos){
        actual = pos;
        mp.reset();
        estado = Estados.idle;
        play();
    }

    public static boolean canPause(){
        if(estado == Estados.prepared ||
                estado == Estados.started){
            return true;
        }else{
            return false;
        }
    }

    /* ******************************************************* */
                // INTERFAZ AUDIO FOCUS CHANGED //
    /* ******************************************************* */

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                play();
                mp.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mp.setVolume(0.1f, 0.1f);
                break;
        }
    }

    /* ******************************************************* */
                // INTERFAZ COMPLETED LISTENER //
    /* ******************************************************* */

    @Override
    public void onCompletion(MediaPlayer mp) {
        estado = Estados.completed;
        next();
    }

    /* ******************************************************* */
                // INTERFAZ PREPARED LISTENER //
    /* ******************************************************* */

    @Override
    public void onPrepared(MediaPlayer mp) {
        estado = Estados.prepared;
        if(reproducir){
            mp.start();
            notificacion(canciones.get(actual).toString());
            estado = Estados.started;
        }
    }

    /* ******************************************************* */
                      // NOTIFICACIONES //
    /* ******************************************************* */

    private void notificacion(String songTitle){
        Intent notIntent = new Intent(this, Principal.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setTicker(Principal.getTitulo(songTitle))
                .setOngoing(true)
                .setContentTitle("Reproduciendo")
        .setContentText(Principal.getTitulo(songTitle));
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    /* ******************************************************* */
                        // SEEKBAR //
    /* ******************************************************* */

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (!mp.isPlaying()){
            play();
        }
    }

    private void setupHandler() {
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 100); // 100ms
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            LogMediaPosition();
            handler.postDelayed(this, 100); // 100ms
        }
    };

    private void LogMediaPosition() {
        if (mp.isPlaying()) {
            mediaPosition = mp.getCurrentPosition();
            mediaMax = mp.getDuration();
            seekIntent.putExtra("counter", String.valueOf(mediaPosition));
            seekIntent.putExtra("mediamax", String.valueOf(mediaMax));
            seekIntent.putExtra("song_ended", String.valueOf(songEnded));
            sendBroadcast(seekIntent);
        }
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        // Recibe la posición de la SeekBar si ha sido modificada en la Actividad
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekPos(intent);
        }
    };

    public void updateSeekPos(Intent intent) {
        // Hace seekTo la posición de la SeekBar en la Actividad
        int seekPos = intent.getIntExtra("seekpos", 0);
        if (mp.isPlaying()) {
            handler.removeCallbacks(sendUpdatesToUI);
            mp.seekTo(seekPos);
            setupHandler();
        }
    }
}
