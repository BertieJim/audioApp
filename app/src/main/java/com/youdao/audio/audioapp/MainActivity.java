package com.youdao.audio.audioapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.youdao.audio.common.AudioChunkWrapper;
import com.youdao.audio.common.IAudioer;
import com.youdao.audio.player.AudioPlayerConfig;
import com.youdao.audio.player.AudioWavFilePlayer;
import com.youdao.audio.recorder.AudioDefaultPcmStreamRecorder;
import com.youdao.audio.recorder.AudioDefaultWavFileRecorder;
import com.youdao.audio.recorder.AudioErrorCode;
import com.youdao.audio.recorder.AudioRecordConfig;
import com.youdao.audio.recorder.OnAudioFrameRecordListener;
import com.youdao.audio.recorder.OnAudioRecordListener;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private View btnStart;
    private View btnStop;
    private View btnCancel;
    private View btnConfirm;
    private View btnDelete;
    private View btnUpload;
    private View btnPlay;

    private Button btnRecord;
    private Button btnPlayAudio;
    private Button btnUploadAudio;
    private Button btnCancelAudio;




    private IAudioer audioer;
    private AudioRecordConfig mRecordConfig;
    private AudioPlayerConfig mPlayConfig;

    private SharedPreferences sp;


    TextView txtText;
    TextView txtChecknum;

    String txtTextV = "Fault";

    String content;
    String checknum;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        txtText = (TextView)findViewById(R.id.txt_text);
        txtChecknum = (TextView)findViewById(R.id.txt_checknum);


        sp = this.getSharedPreferences("myinfo", Activity.MODE_PRIVATE);
        content = sp.getString("content",txtTextV);
        checknum = sp.getString("checknum",txtTextV);

        txtText.setText(content);
        txtChecknum.setText("Done:"+checknum);

        btnRecord = this.findViewById(R.id.btn_record);
        btnPlayAudio = this.findViewById(R.id.btn_playaudio);
        btnUploadAudio = this.findViewById(R.id.btn_uploadaudio);


        mRecordConfig = AudioRecordConfig.SAMPLE_8K_16BIT;
        mPlayConfig = AudioPlayerConfig.SAMPLE_8K_16BIT;
        btnPlayAudio.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                play_Record();
            }
        });

        btnUploadAudio.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                upload_Record();

                btnPlayAudio.setVisibility(View.INVISIBLE);
                btnUploadAudio.setVisibility(View.INVISIBLE);
//                Get_Text();
                btnRecord.setText("录音");
            }
        });


        btnRecord.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                float downX = 0;
                float downY = 0;
                float moveX = 0;
                float moveY = 0;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        delete_Record();
                        btnRecord.setText("正在录音");
                        start_Record();
                        downX = event.getX();
                        downY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(Math.abs(downY-event.getY())>300)
                        {
                            btnRecord.setText(" 取消 ");
                        }
                        else{
                            btnRecord.setText("正在录音");
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if(Math.abs(downY-event.getY())>300)
                        {
                            cancel_Record();
                            btnRecord.setText(" 录音 ");
                        }
                        else{
                            stop_Record();
                            btnRecord.setText(" 重新录音 ");
                            btnPlayAudio.setVisibility(View.VISIBLE);
                            btnUploadAudio.setVisibility(View.VISIBLE);

                        }
                        break;
                }
                return true;
            } });


    }
    public void stop_Record(){
        if (audioer != null) {
            audioer.stop();
        }
        //chong ming ming
        String musicPath = Environment.getExternalStorageDirectory() + "/test.wav";
        String newPath = Environment.getExternalStorageDirectory() + "/test2.wav";

        File oleFile = new File(musicPath);
        File newFile = new File(newPath);
        //执行重命名
        oleFile.renameTo(newFile);

    }
    public void cancel_Record(){
        audioer.release();
        audioer = null;
    }

    public void play_Record(){
        audioer = new AudioWavFilePlayer(mPlayConfig, null);
        audioer.start();
    }
    public void delete_Record(){
        if (audioer != null) {
            audioer.stop();
            audioer.release();
            audioer = null;
        }


    }

    public void upload_Record(){
        if (audioer != null) {
            audioer.stop();
        }
        audioer.release();
        audioer = null;
        //上传录音
        sp = this.getSharedPreferences("myinfo", Activity.MODE_PRIVATE);
        String userName =sp.getString("PhoneNum",null);
        String quotaid =sp.getString("quotaid",null);
        String musicPath = Environment.getExternalStorageDirectory() + "/test2.wav";
        String newPath = Environment.getExternalStorageDirectory() + "/"+userName+"_"+quotaid+".wav";

        File oleFile = new File(musicPath);
        File newFile = new File(newPath);
        //执行重命名
        oleFile.renameTo(newFile);
        //指定url路径
        String url = "http://api.ocr.youdao.com:7890/addAAudio";
        //封装文件上传的参数
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        //根据路径创建文件
        File file = new File(newPath);
        try {
            //放入文件
            params.put("file", file);

        } catch (Exception e) {
            // TODO: handle exception
            System.out.println("文件不存在----------");
        }

        client.post(url, params,new JsonHttpResponseHandler() {
            @SuppressLint("WrongConstant")
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if(!response.getString("content").equals("None")){

                        sp.edit().putString("Is_Right", "true").commit();
                        sp.edit().putString("content", response.getString("content")).commit();
                        sp.edit().putString("quotaid", response.getString("quotaid")).commit();
                        txtText.setText(response.getString("content"));
                        txtChecknum.setText("Done:"+response.getString("checknum"));
                        if (statusCode == 200) {
                            Toast.makeText(getApplicationContext(), "上传成功", Toast.LENGTH_SHORT)
                                    .show();
                        }


                    }
                    else{
                        Toast.makeText(getApplicationContext(), "出现错误，请联系管理员或重启", Toast.LENGTH_SHORT).show();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sp.edit().commit();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  String responseString, Throwable throwable) {
                // TODO Auto-generated method stub
                super.onFailure(statusCode, headers, responseString, throwable);
                Toast.makeText(getApplicationContext(), "出现错误，请联系管理员或重启", Toast.LENGTH_SHORT).show();
                Log.e("downTongxing", "23333333");
            }


        });
        //这里需要延时，足够去服务器验证密码
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (sp.getString("Is_Right","false").equals("true"))
                {
//                    Toast.makeText(getApplicationContext(), "执行了！", Toast.LENGTH_SHORT).show();
                    sp.edit().putString("Is_Right", "false").commit();
                    btnRecord.setEnabled(true);
                }
                else
                    Toast.makeText(getApplicationContext(), "上传失败，没有网络连接", Toast.LENGTH_SHORT).show();
            }
        }, 2100);
    }


    public void start_Record() {
        if (!isGrantExternalRW(MainActivity.this)) {
            Toast.makeText(MainActivity.this, "请在授权管理中授权外存读取权限", Toast.LENGTH_LONG).show();
        } else {
            audioer = new AudioDefaultWavFileRecorder(mRecordConfig, null, 2000, new OnAudioRecordListener() {


                @Override
                public void onAudioFrameRecorded(AudioChunkWrapper wrapper) {
//                        final double db = wrapper.maxAmplitudeRatio();
                    Log.d(TAG, "onAudioFrameRecorded -- ratio = " + wrapper.maxAmplitudeRatio());
                    final double db = wrapper.maxAmplitude();
                }

                @Override
                public void onReady() {
                    Log.d(TAG, "onReady --");
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "onStart -- ");
                }

                @Override
                public void onSilence(long time) {
                    Log.d(TAG, "onSilence -- time = " + time);
                }

                @Override
                public void onStop() {
                    Log.d(TAG, "onStop -- ");
                }

                @Override
                public void onRelease() {
                    Log.d(TAG, "onRelease -- ");
                }

                @Override
                public void onError(int errorCode) {
                    Log.d(TAG, "onRelease -- ");
                    if (errorCode == AudioErrorCode.ERROR_WAV_FILE_CREATE_FAIL) {
                        Log.d(TAG, "onRelease -- ERROR_WAV_FILE_CREATE_FAIL");
                    }
                }
            });
            audioer.start();
        }
    }



    public void Out(View view){
        if (audioer != null) {
            audioer.stop();
        }
        audioer = null;

        Toast.makeText(getApplicationContext(), "注销成功!",
                Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this,LogIn.class);

        sp = this.getSharedPreferences("myinfo", Activity.MODE_PRIVATE);
        sp.edit().putBoolean("AUTO_ISCHECK", false).commit();

        MainActivity.this.finish();
        startActivity(intent);
        finish();

    }


    public static boolean isGrantExternalRW(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO

            }, 1);

            return false;
        }

        return true;
    }

}
