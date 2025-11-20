package com.example.http;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {
    private  String productid;
    private  String devicename;
    private  String devicekey;
    private String resoureproduct;

    private String set_device_property_url = "http://iot-api.heclouds.com/thingmodel/set-device-property";

    private String query_device_property_url;
    private String token;

    private boolean responseok=false;
    private EditText EditText_devid,EditText_devname,EditText_devpassword;
    private Button button_signin;


    private SharedPreferences msharedPreferences;

    private SharedPreferences.Editor editor;

    private  String key_productid="key_productid";
    private  String key_devicename="key_devicename";
    private  String key_devicekey="key_devicekey";


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        EditText_devid=findViewById(R.id.devid);
        EditText_devname=findViewById(R.id.devname);
        EditText_devpassword=findViewById(R.id.devpassword);
        button_signin=findViewById(R.id.sign_in);
        String prefsName = getString(R.string.prefs_name);
        msharedPreferences= getSharedPreferences(prefsName, MODE_PRIVATE);
        editor= msharedPreferences.edit();
        productid=msharedPreferences.getString(key_productid,"L8EG92Ib6s");
        EditText_devid.setText(productid);
        devicename=msharedPreferences.getString(key_devicename,"esp32led02");
        EditText_devname.setText(devicename);
        devicekey=msharedPreferences.getString(key_devicekey,"RlhPdFdJQ0ZNRDJlekd1OHlQUExPU2xoZFVPV0JJQVc=");
        EditText_devpassword.setText(devicekey);







        button_signin.setOnClickListener(v -> {
            // 点击时才获取输入的文本
            String inputProductId = EditText_devid.getText().toString().trim();
            String inputDevName = EditText_devname.getText().toString().trim();
            String inputDevKey = EditText_devpassword.getText().toString().trim();




            // 验证输入不为空
            if (inputProductId.isEmpty() || inputDevName.isEmpty() || inputDevKey.isEmpty()) {
                Toast.makeText(MainActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }

            // 更新全局变量
            productid = inputProductId;
            devicename = inputDevName;
            devicekey = inputDevKey;
            resoureproduct = "products/" + productid + "/devices/" + devicename;
            query_device_property_url = "http://iot-api.heclouds.com/thingmodel/query-device-property?product_id=" + productid + "&device_name=" + devicename;

            // 重新生成 Token（因为输入可能变化）
            try {
                token = Token.get(resoureproduct, devicekey);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Token生成失败", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return;
            }

            // 发起验证请求（这里用同步请求，因为需要等待结果）
            new Thread(() -> {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(query_device_property_url)
                        .header("Authorization", token)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        String responseData = response.body().string();
                        if (isDataNotEmpty(responseData)) {
                            responseok=true;
                            editor.putString(key_productid, productid);
                            editor.putString(key_devicename, devicename);
                            editor.putString(key_devicekey, devicekey);
                            editor.apply();
                            // 验证成功，跳转到下一个页面
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                                Intent intent=new Intent(MainActivity.this, SmartHomeActivity.class);
                                intent.putExtra("token",token);
                                intent.putExtra("query_device_property_url",query_device_property_url);
                                intent.putExtra("set_device_property_url",set_device_property_url);
                                startActivity(intent);

                            });
                        } else {
                            responseok=false;
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "登录失败：" + response.code(), Toast.LENGTH_SHORT).show());
                        }
                    }

                } catch (IOException e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }).start();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    private boolean isDataNotEmpty(String responseData) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);

            // 检查data字段是否为null或空
            Object dataValue = jsonObject.opt("data");
            return dataValue != null && !jsonObject.isNull("data");

        } catch (Exception e) {
            Log.e("login", "JSON解析失败: " + e.getMessage());
            return false;
        }
    }




}