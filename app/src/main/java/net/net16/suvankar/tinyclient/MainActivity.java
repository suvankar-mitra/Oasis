package net.net16.suvankar.tinyclient;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import net.net16.suvankar.tinyclient.utils.IPv4AddressValidator;
import net.net16.suvankar.tinyclient.utils.SeekBarTouchOn;

import org.apache.commons.io.FileUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    ImageButton play;
    ImageButton next;
    ImageButton prev;
    SeekBar seekBar;
    TextView curTime;
    TextView totTime;
    TextView songTitle;
    ImageView albumArt;


    private String server_host = "";
    private File file;
    private boolean isFileLoaded;
    private boolean isPlaying;
    private int lastPosition;

    MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        songTitle = (TextView) findViewById(R.id.textView);
        play = (ImageButton) findViewById(R.id.play);
        next = (ImageButton) findViewById(R.id.next);
        prev = (ImageButton) findViewById(R.id.prev);
        curTime = (TextView) findViewById(R.id.currentTime);
        totTime = (TextView) findViewById(R.id.totalTime);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        albumArt = (ImageView) findViewById(R.id.albumArt);
        mPlayer = new MediaPlayer();

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(file==null)
                    return;
                if(isPlaying && mPlayer.isPlaying()){
                    play.setImageResource(R.drawable.ic_play_circle_filled_black_24dp);
                    lastPosition = mPlayer.getCurrentPosition();
                    mPlayer.pause();
                    isPlaying = false;
                }
                else {
                    mPlayer.reset();
                    mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    try {
                        Uri uri = Uri.fromFile(file);
                        Log.d("TAG","URI---"+uri);
                        mPlayer.setDataSource(getApplicationContext(), uri);
                        mPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mPlayer.seekTo(lastPosition);
                    mPlayer.start();
                    isPlaying = true;
                    play.setImageResource(R.drawable.ic_pause_circle_filled_black_24dp);

                }
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPlayer!=null && mPlayer.isPlaying()){
                    mPlayer.stop();
                    mPlayer.release();
                }
                SyncServer syncServer = new SyncServer();
                syncServer.execute(server_host);
            }
        });

        prev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPlayer!=null && mPlayer.isPlaying()){
                    mPlayer.stop();
                    mPlayer.release();
                }
                SyncServer syncServer = new SyncServer();
                syncServer.execute(server_host,"prev");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh: {

                if(mPlayer != null){
                    try{
                        if(mPlayer.isPlaying())
                            mPlayer.stop();
                        mPlayer.release();
                    }catch (IllegalStateException e){}
                }

                //popup dialog
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Enter IP address of server");

                // Set up the input
                final EditText input = new EditText(MainActivity.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText("192.168.0.7");
                builder.setView(input);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //we are not doing anything here because
                        //we need to stay put if the ip given by
                        //user is invalid, this method closes the
                        //dialog box right away if "ok" is clicked
                    }
                });
                final AlertDialog alert = builder.create();
                alert.show();
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    //this is the default dialog controller, we will close
                    //the dialog only if the user enters a valid ip
                    @Override
                    public void onClick(View view) {
                        server_host = input.getText().toString();
                        //dialog.dismiss();
                        if (IPv4AddressValidator.isValid(server_host)) {
                            Log.d("TAG","This is a valid ip");
                            alert.dismiss();
                            //we got the ip from user, now let's get the file from the server
                            SyncServer syncServer = new SyncServer();
                            syncServer.execute(server_host);
                        }
                        else {
                            //Toast.makeText(MainActivity.this, "That is not a valid IP", Toast.LENGTH_SHORT);
                            showToast("That is not a valid IP");
                            Log.d("TAG","That is not a valid IP");
                        }
                    }
                });
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showToast(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show(); // For the context I tried using getBaseContext, ActivityName.this also
    }

    public void startPlayback() {
        if(!isFileLoaded || file==null){
            return;
        }


        try {
            if(file.exists()){
                Log.d("TAG","File exists");
            }

            //load the album art
            try{
                loadAlbumArt();
            }catch (Exception e){}

            Uri uri = Uri.fromFile(file);
            Log.d("TAG","URI---"+uri);
            mPlayer = new MediaPlayer();
            mPlayer.reset();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(getApplicationContext(), uri);
            mPlayer.prepare();

            seekBar.setMax(mPlayer.getDuration());
            int minute = mPlayer.getDuration() / 60000;
            int sec = mPlayer.getDuration() - minute * 60000;
            sec = sec / 1000;
            if(sec<=9)
                totTime.setText(minute+":0"+sec);
            else
                totTime.setText(minute+":"+sec);

            mPlayer.start();
            /**
             * After each playback complete call the async task to load the next song
             * from the server
             */
            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    SyncServer syncServer = new SyncServer();
                    syncServer.execute(server_host);
                }
            });
            isPlaying = true;
            play.setImageResource(R.drawable.ic_pause_circle_filled_black_24dp);

            final Timer timer = new Timer();
            final SeekBarTouchOn seekBarTouchOn = new SeekBarTouchOn();

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                    //curTime.setText(i+"");  //set to string, otherwise it will throw res not found exception
                    int minute = i / 60000;
                    int sec = i - minute * 60000;
                    sec = sec / 1000;
                    if(sec<=9)
                        curTime.setText(minute+":0"+sec);
                    else
                        curTime.setText(minute+":"+sec);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    seekBarTouchOn.setTouchOn(true);
                }

                @Override
                public void onStopTrackingTouch(final SeekBar seekBar) {
                    lastPosition = seekBar.getProgress();
                    mPlayer.seekTo(seekBar.getProgress());
                    seekBarTouchOn.setTouchOn(false);
                }
            });

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(isPlaying) {
                                //timer.setText((mediaPlayer.getCurrentPosition() / 1000) + " / " + (mediaPlayer.getDuration() / 1000));
                                try{
                                    if(mPlayer!=null && mPlayer.isPlaying() && !seekBarTouchOn.isTouchOn())
                                        seekBar.setProgress(mPlayer.getCurrentPosition());
                                } catch (IllegalStateException e){}
                            }
                        }
                    });
                }
            },0,100);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAlbumArt() {
        if(file==null)
            return;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getAbsolutePath());
        //String albumName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        byte[] bytes = mmr.getEmbeddedPicture();
        Bitmap bitmap = null;
        if(bytes!=null) {
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        if(bitmap == null || bitmap.getByteCount()<=0) {
            //album art not found
            albumArt.setImageResource(R.drawable.album_art);
        }
        else{
            albumArt.setImageBitmap(bitmap);
        }
        mmr.release();
    }

    private class SyncServer extends AsyncTask<String, String, String> {

        ProgressDialog mDialog;
        private final int PORT = 5000;

        @Override
        protected String doInBackground(String... strings) {
            Log.d("TAG","Starting async task");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDialog = new ProgressDialog(MainActivity.this);    //make sure this runs on MainActivity.this
                    mDialog.setMessage("Downloading content, please wait...");
                    mDialog.setCancelable(false);
                    mDialog.show();
                }
            });

            server_host = strings[0];
            String prev = null;
            if(strings.length>1){
                prev = strings[1];
                Log.d("TAG",""+prev);
            }

            try {
                Socket socket = new Socket(server_host, PORT);

                DataInputStream dis = new DataInputStream(socket.getInputStream());
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

                if(prev==null)
                    dos.writeUTF("ready");
                else
                    dos.writeUTF("prev");   //I want previous song

                final String msg = dis.readUTF();
                if (msg != null) {
                    publishProgress(msg);
                }
                if(file!=null && file.exists()) {
                    file.delete();
                }
                file = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + File.separator, msg);
                if (!file.exists()) {
                    file.createNewFile();
                }
                Log.d("TAG", file.getAbsolutePath());
                FileUtils.copyInputStreamToFile(dis, file);
                dis.close();
                dos.close();
                socket.close();
                isFileLoaded = true;    //file load complete
            } catch (IOException e) {
                if(mDialog!=null) {
                    mDialog.dismiss();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                        dialog.setMessage("Unable to contact server, make sure the IP address is correct.")
                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                }).show();
                    }
                });
                //showToast("Server is offline or wrong IP address inserted!");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            songTitle.setText(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(mDialog!=null) {
                mDialog.dismiss();
            }

            //start the media playback
            startPlayback();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
        if(file!=null)
            file.delete();
    }
}


