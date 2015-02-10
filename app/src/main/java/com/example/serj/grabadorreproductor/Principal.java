package com.example.serj.grabadorreproductor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;

import java.util.ArrayList;

public class Principal extends Activity implements SeekBar.OnSeekBarChangeListener{

    //CONSTANTES
    private final int GRABAR = 1;
    private final int GETMUSICA=2;
    public static final String BROADCAST_SEEKBAR = "com.example.serj.grabadorreproductor.sendseekbar";
    //VISTA
    private Button btPlayPause;
    private SeekBar seekBar;
    private ListView lv;
    private Adaptador ad;
    //LISTA
    private ArrayList<String> listaCanciones;
    //BROADCAST RECEIVER
    Intent serviceIntent;
    Intent intent;
    private int seekMax;
    private static int songEnded = 0;
    private boolean reproduciendo = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateUI(serviceIntent);
        }
    };

    /* ******************************************************* */
                      // MÉTODOS OVERRIDE //
    /* ******************************************************* */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if(requestCode == GRABAR || requestCode == GETMUSICA){
                // Añadir grabación a la lista
                Uri uri = data.getData();
                String ruta = getPath(uri);
                listaCanciones.add(getTitulo(ruta));
                Intent intent = new Intent(this, Reproductor.class);
                intent.putExtra("cancion", ruta);
                intent.setAction(Reproductor.ADD);
                startService(intent);
                ad.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        initComponents();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_principal, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, Reproductor.class));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.rec:
                return grabarAudio();
            case R.id.add:
                return add();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(broadcastReceiver, new IntentFilter(Reproductor.BROADCAST_ACTION));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
    }

    /* ******************************************************* */
                           // INTENTS //
    /* ******************************************************* */

    private boolean grabarAudio(){
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        startActivityForResult(intent, GRABAR);
        return true;
    }

    private boolean add(){
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GETMUSICA);
        return true;
    }

    /* ******************************************************* */
                   // CONTROL DEL MEDIA PLAYER //
    /* ******************************************************* */

    public void playPause(View v){
        if(Reproductor.canPause()){
            pause();
            reproduciendo = false;
            setBtPlayPause();
        }else{
            start();
            reproduciendo = true;
            setBtPlayPause();
        }
    }

    public void start() {
        Intent intent = new Intent(this, Reproductor.class);
        intent.setAction(Reproductor.PLAY);
        startService(intent);
    }

    public void pause() {
        Intent intent = new Intent(this, Reproductor.class);
        intent.setAction(Reproductor.PAUSE);
        startService(intent);
    }

    public void next(View v){
        Intent intent = new Intent(this, Reproductor.class);
        intent.setAction(Reproductor.NEXT);
        startService(intent);
        reproduciendo = true;
        setBtPlayPause();
    }

    public void previous(View v){
        Intent intent = new Intent(this, Reproductor.class);
        intent.setAction(Reproductor.PREVIOUS);
        startService(intent);
        reproduciendo = true;
        setBtPlayPause();
    }

    /* ******************************************************* */
                       // MÉTODOS AUXILIARES //
    /* ******************************************************* */

    public String getPath(Uri uri) {
        // Path real de un archivo
        Cursor cur = getContentResolver().query(uri,null, null, null, null);
        cur.moveToFirst();
        String path = cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA));
        cur.close();
        return path;
    }

    public static String getTitulo(String s){
        // Título sin la ruta
        return s.substring(s.lastIndexOf("/")+1);
    }

    private void initComponents(){
        serviceIntent = new Intent(this, Reproductor.class);
        intent = new Intent(BROADCAST_SEEKBAR);
        btPlayPause = (Button)findViewById(R.id.btPlayPause);
        lv = (ListView)findViewById(R.id.listView);
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        listaCanciones = new ArrayList<>();
        ad = new Adaptador(this, R.layout.listadetalle, listaCanciones);
        lv.setAdapter(ad);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(Principal.this, Reproductor.class);
                intent.setAction(Reproductor.PLAY_ITEM);
                intent.putExtra("posicion", i);
                startService(intent);
                reproduciendo = true;
                setBtPlayPause();
            }
        });
        registerForContextMenu(lv);
    }

    public void setBtPlayPause(){
        // Cambiar el botón de play/pause
        if(reproduciendo){
            btPlayPause.setBackgroundResource(android.R.drawable.ic_media_pause);
        }else{
            btPlayPause.setBackgroundResource(android.R.drawable.ic_media_play);
        }
    }

    /* ******************************************************* */
                           // SEEKBAR //
    /* ******************************************************* */

    private void updateUI(Intent serviceIntent) {
        String counter = serviceIntent.getStringExtra("counter");
        String mediamax = serviceIntent.getStringExtra("mediamax");
        String strSongEnded = serviceIntent.getStringExtra("song_ended");
        int seekProgress = Integer.parseInt(counter);
        seekMax = Integer.parseInt(mediamax);
        songEnded = Integer.parseInt(strSongEnded);
        seekBar.setMax(seekMax);
        seekBar.setProgress(seekProgress);
        if (songEnded == 1) {
            reproduciendo = true;
            setBtPlayPause();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int seekPos = seekBar.getProgress();
            intent.putExtra("seekpos", seekPos);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
