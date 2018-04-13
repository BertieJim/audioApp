package com.youdao.audio.audioapp;

import org.apache.http.Header;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.UnsupportedEncodingException;

public class LogIn extends AppCompatActivity {
    //选择是否自动登录
    private CheckBox auto_login;
    private SharedPreferences sp;
    // 用户名
    private EditText inputPhoneEt;
    //用户名
    private String phoneNums;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        inputPhoneEt = (EditText) findViewById(R.id.login_input_phone_et);
        //自动填充
        sp = this.getSharedPreferences("myinfo", Activity.MODE_PRIVATE);

        phoneNums=sp.getString("PhoneNum",null);
        inputPhoneEt.setText(phoneNums);
        if (phoneNums != null && sp.getBoolean("AUTO_ISCHECK",false)) //修改为true 原为false
        {
            //如果自动登录，则填充密码
            Get_Text(phoneNums);

        }
        auto_login = (CheckBox) findViewById(R.id.cb_auto);
        auto_login.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (auto_login.isChecked()) {
                    System.out.println("自动登录已选中");
                    sp.edit().putBoolean("AUTO_ISCHECK", true).commit();
                } else {
                    System.out.println("自动登录没有选中");
                    sp.edit().putBoolean("AUTO_ISCHECK", false).commit();
                }
            }
        });
    }
    public void LogIn_Failed(View view)
    {

        finish();
        System.exit(0);
    }
    public void LogIn_Success(View view) {
        phoneNums = inputPhoneEt.getText().toString();
        if (phoneNums.equals(""))
            Toast.makeText(getApplicationContext(), "用户名为空，请输入正确的用户名",
                    Toast.LENGTH_SHORT).show();
        else{
            sp = this.getSharedPreferences("myinfo", Activity.MODE_PRIVATE);
            sp.edit().putString("PhoneNum", phoneNums).commit();
            Get_Text(phoneNums);

        }
    }
    public void Get_Text(String phoneNums){
        AsyncHttpClient client = new AsyncHttpClient();
//        RequestParams params = new RequestParams();
//        params.add("name",phoneNums);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name",phoneNums);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ByteArrayEntity entity = null;
        try {
            entity = new ByteArrayEntity(jsonObject.toString().getBytes("UTF-8"));
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        client.post(getApplicationContext(),"http://api.ocr.youdao.com:7890/getOneText", entity,"", new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, JSONObject response) {
                try {
                    if(!response.getString("content").equals("None")){
                        sp.edit().putString("Is_Right", "true").commit();
                        sp.edit().putString("content", response.getString("content")).commit();
                        sp.edit().putString("userid", response.getString("userid")).commit();
                        sp.edit().putString("quotaid", response.getString("quotaid")).commit();
                        sp.edit().putString("checknum", response.getString("checknum")).commit();

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
                    Intent intent = new Intent();
                    intent.setClass(LogIn.this, MainActivity.class);

                    LogIn.this.finish();
                    startActivity(intent);
                    finish();
                    //两个参数分别表示进入的动画,退出的动画
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                }
                else
                    Toast.makeText(getApplicationContext(), "用户名错误或没有网络连接", Toast.LENGTH_SHORT).show();
            }
        }, 1500);
    }


}
