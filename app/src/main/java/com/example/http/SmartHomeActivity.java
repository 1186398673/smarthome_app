package com.example.http;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SmartHomeActivity extends AppCompatActivity {

    private String token;
    private String query_device_property_url;
    private String set_device_property_url;

    private ImageButton btnSwitchlight,btnSwitchair,btnSwitchaddwater;

    private TextView text_humidity,text_Smoke,text_temperature,text_brightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_smart_home);
        token=getIntent().getStringExtra("token");
        query_device_property_url=getIntent().getStringExtra("query_device_property_url");
        set_device_property_url=getIntent().getStringExtra("set_device_property_url");
        Get();
        btnSwitchlight=findViewById(R.id.btnSwitchlight);
        btnSwitchlight.setOnClickListener(v->{
            btnSwitchlight.setSelected(!btnSwitchlight.isSelected());
            POST();
        });
        btnSwitchair=findViewById(R.id.btnSwitchair);
        btnSwitchair.setOnClickListener(v->{
            btnSwitchair.setSelected(!btnSwitchair.isSelected());
            POST();
        });
        btnSwitchaddwater=findViewById(R.id.btnSwitchaddwater);
        btnSwitchaddwater.setOnClickListener(v->{
            btnSwitchaddwater.setSelected(!btnSwitchaddwater.isSelected());
            POST();
        });
        text_humidity=findViewById(R.id.humidity);
        text_Smoke=findViewById(R.id.Smoke);
        text_temperature=findViewById(R.id.temperature);
        text_brightness=findViewById(R.id.brightness);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void Get() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(query_device_property_url)
                            .header("Authorization", token)
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            Log.e("post", "请求失败: " + e.getMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            try (ResponseBody body = response.body()) {

                                if (response.isSuccessful()) {
                                    String responseData = body.string();
                                    Log.d("post", "响应: " + responseData);
                                    parseJsonWithNative(responseData);
                                } else {
                                    Log.e("post", "请求错误: " + response.code() + " " + response.message());
                                }
                            }
                        }
                    });


                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }//延时一秒获取一次亮度

                }
            }
        }).start();


    }
    private void POST() {

        OkHttpClient client = new OkHttpClient();

        String jsonBody = "{"
                + "\"product_id\":\"L8EG92Ib6s\","
                + "\"device_name\":\"esp32led02\","
                + "\"params\":"
                + "  {"
                + "    \"LightSwitch\":"+btnSwitchlight.isSelected()+","
                + "    \"addwater\":"+btnSwitchaddwater.isSelected()+","
                + "    \"airswitch\":"+btnSwitchair.isSelected()
                + "  }"
                + "}";

        Request request = new Request.Builder()
                .url(set_device_property_url)
                .header("Authorization", token)
                .post(RequestBody.create(jsonBody, MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("post", "请求失败: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody body = response.body()) {
                    if (response.isSuccessful()) {
                        String responseData = body.string();
                        Log.d("post", "响应: " + responseData);
                    } else {
                        Log.e("post", "请求错误: " + response.code() + " " + response.message());
                    }
                }
            }
        });

    }

    private void parseJsonWithNative(String jsonStr) {
        try {
            JSONObject rootObj = new JSONObject(jsonStr);
            int code = rootObj.getInt("code");
            String msg = rootObj.getString("msg");

            if (code == 0) {
                JSONArray dataArray = rootObj.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject dataObj = dataArray.getJSONObject(i);
                    String identifier = dataObj.getString("identifier");
                    String name = dataObj.getString("name");
                    // 处理可能缺失的字段（用has()判断）
                    String value = dataObj.has("value") ? dataObj.getString("value") : "无";
                    String description = dataObj.has("description") ? dataObj.getString("description") : "无";

                    Log.d("NativeParse", "名称：" + name + " | 标识：" + identifier + " | 值：" + value);
                    // 3. 字符串比较必须用equals()，不能用==（==比较地址，equals比较内容）
                    if ("Brightness".equals(identifier)) {
                        // 4. 确保UI操作在主线程（原生解析若在子线程，需用runOnUiThread）
                        runOnUiThread(() -> text_brightness.setText( value));
                    } else if ("Smoke".equals(identifier)) {
                        runOnUiThread(() -> text_Smoke.setText( value+ "mg/m3"));
                    } else if ("temperature".equals(identifier)) {
                        runOnUiThread(() -> text_temperature.setText(value + "℃")); // 增加单位更直观
                    } else if ("humidity".equals(identifier)) {
                        runOnUiThread(() -> text_humidity.setText(value + "%")); // 增加单位
                    }

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("NativeParseError", "解析失败：" + e.getMessage());
        }
    }
}