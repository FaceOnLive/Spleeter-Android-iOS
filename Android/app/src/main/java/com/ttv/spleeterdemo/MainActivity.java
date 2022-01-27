package com.ttv.spleeterdemo;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ttv.spleeter.SpleeterSDK;
import com.ttv.spleeterdemo.databinding.ActivityMainBinding;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";

    private Context context;
    private ActivityMainBinding binding;
    private File mLastFile;
    private int mProcessing = 0;
    private AudioTrack mAudioTrack = null;
    private int mBufferSize = 0;

    Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == 1){
                int progress = msg.arg1;
                binding.txtProgress.setText(String.format("%d%%", progress));
            } else if(msg.what == 2){
                int progress = msg.arg1;
                binding.txtPlayProgress.setText(String.format("Play: %d%%", progress));
            } else{
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        int frameRate = 44100;
        int minBufferSize =
                AudioTrack.getMinBufferSize(
                        frameRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        mBufferSize = (3 * (minBufferSize / 2)) & ~3;
        Log.e("TestEngine", "Audio minBufferSize = " + minBufferSize + " " + mBufferSize);

        mAudioTrack =
                new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        frameRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        mBufferSize,
                        AudioTrack.MODE_STREAM);

        SpleeterSDK.createInstance(context).create();

        binding.btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileChooser fileChooser = new FileChooser(MainActivity.this, "Select wav file", FileChooser.DialogType.SELECT_FILE, mLastFile);
                FileChooser.FileSelectionCallback callback = new FileChooser.FileSelectionCallback() {

                    @Override
                    public void onSelect(File file) {
                        mLastFile = file;

                        try {
                            final String wavPath = file.getPath();
                            final String outPath = file.getParent() + "/out";
                            File f = new File(file.getParent(), "out");
                            if (!f.exists()) {
                                f.mkdirs();
                            }

                            File file1 = new File(file.getParent(), "out");
                            String[] myFiles;

                            myFiles = file1.list();
                            for (int i=0; i<myFiles.length; i++) {
                                File myFile = new File(file1, myFiles[i]);
                                myFile.delete();
                            }

                            binding.txtIn.setText("Input File: " + wavPath);
                            binding.txtOut.setText("Output Folder: " + outPath);
                            mProcessing = 1;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    SpleeterSDK.getInstance().process(wavPath, outPath);

                                    mProcessing = 0;
                                }
                            }).start();
                            new ProgressTask().execute("", "", "");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                fileChooser.show(callback);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (permission()) {
            openNewScreen();
        } else {
            RequestPermission_Dialog();
        }
    }

    public boolean permission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            int write = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
            int read = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void openNewScreen() {

    }

    public void RequestPermission_Dialog() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", new Object[]{getApplicationContext().getPackageName()})));
                startActivityForResult(intent, 2000);
            } catch (Exception e) {
                Intent obj = new Intent();
                obj.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(obj, 2000);
            }
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    boolean storage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean read = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (storage && read) {
                        openNewScreen();
                    } else {
                        //permission denied
                    }
                }
                break;
        }
    }

    public class ProgressTask extends AsyncTask<String, String, String> {
        private void setProgress(int progress) {
            Message message = new Message();
            message.what = 1;
            message.arg1 = progress;
            mHandler.sendMessage(message);
        }

        private void setPlayState(int progress) {
            Message message = new Message();
            message.what = 2;
            message.arg1 = progress;
            mHandler.sendMessage(message);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            binding.btnOpen.setEnabled(false);
            mAudioTrack.play();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                setProgress(0);
                setPlayState(0);

                int playSize = 0;
                int offset = 0;
                while(true) {
                    short[] playbuffer = new short[8192];
                    float[] stemRatio = new float[5];
                    stemRatio[0] = 2 * binding.seekBar1.getProgress() / (float)binding.seekBar1.getMax();
                    stemRatio[1] = 2 * binding.seekBar2.getProgress() / (float)binding.seekBar2.getMax();
                    stemRatio[2] = 2 * binding.seekBar3.getProgress() / (float)binding.seekBar3.getMax();
                    stemRatio[3] = 2 * binding.seekBar4.getProgress() / (float)binding.seekBar4.getMax();
                    stemRatio[4] = 2 * binding.seekBar5.getProgress() / (float)binding.seekBar5.getMax();

                    int ret = SpleeterSDK.getInstance().playbuffer(playbuffer, offset, stemRatio);
                    if(ret == 0) {
                        break;
                    } else if(ret < 0) {
                        Thread.sleep(30);
                    } else {
                        if(playSize == 0) {
                            playSize = SpleeterSDK.getInstance().playsize();
                        }

                        offset += ret;
                        mAudioTrack.write(playbuffer, 0, playbuffer.length);
                        Log.e("TestEngine", "write " + playbuffer.length);

                        int progress = SpleeterSDK.getInstance().progress();

                        setProgress(progress);
                        setPlayState((offset / 4) * 100 / playSize);
                    }
                }

                setProgress(100);
                setPlayState(100);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mAudioTrack.stop();

            float[] stemRatio = new float[5];
            stemRatio[0] = 2 * binding.seekBar1.getProgress() / (float)binding.seekBar1.getMax();
            stemRatio[1] = 2 * binding.seekBar2.getProgress() / (float)binding.seekBar2.getMax();
            stemRatio[2] = 2 * binding.seekBar3.getProgress() / (float)binding.seekBar3.getMax();
            stemRatio[3] = 2 * binding.seekBar4.getProgress() / (float)binding.seekBar4.getMax();
            stemRatio[4] = 2 * binding.seekBar5.getProgress() / (float)binding.seekBar5.getMax();

//            SpleeterSDK.getInstance().saveAllStem("/mnt/sdcard/split");
//            SpleeterSDK.getInstance().saveOne("/mnt/sdcard/one.wav", stemRatio);

            binding.btnOpen.setEnabled(true);
            Toast.makeText(context, "Processing done!", Toast.LENGTH_SHORT).show();
        }
    }
}