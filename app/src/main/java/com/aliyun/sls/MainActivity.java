package com.aliyun.sls;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.aliyun.sls.android.sdk.LOGClient;
import com.aliyun.sls.android.sdk.Log;
import com.aliyun.sls.android.sdk.LogGroup;
import com.aliyun.sls.android.sdk.utils.IPService;


public class MainActivity extends AppCompatActivity {

    public final static int HANDLER_MESSAGE_UPLOAD_FAILED = 00011;
    public final static int HANDLER_MESSAGE_UPLOAD_SUCCESS = 00012;

    /**
     * 填入必要的参数
     */
    public String endpoint = "******";
    public String accesskeyID = "******";
    public String accessKeySecret = "******";
    public String project = "******";
    public String logStore = "******";
    public String source_ip = "";


    TextView logText;
    Button upload;

    private Handler handler = new Handler() {
        // 处理子线程给我们发送的消息。
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case IPService.HANDLER_MESSAGE_GETIP_CODE:
                    source_ip = (String) msg.obj;
                    logText.setText(source_ip);
                    return;
                case HANDLER_MESSAGE_UPLOAD_FAILED:
                    logText.setText((String) msg.obj);
                    return;
                case HANDLER_MESSAGE_UPLOAD_SUCCESS:
                    Toast.makeText(MainActivity.this,"upload success",Toast.LENGTH_SHORT).show();
                    return;
            }
            super.handleMessage(msg);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logText = (TextView) findViewById(R.id.ip);
        upload = (Button) findViewById(R.id.upload);
        try {
            IPService.getInstance().AsyncGetIp(IPService.DEFAULT_URL,handler);
        } catch (Exception e) {
            e.printStackTrace();
            logText.setText(e.getMessage());
        }
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleUploadLog(source_ip);
            }
        });

    }

    private void sampleUploadLog(@Nullable String ip) {
        if (TextUtils.isEmpty(ip)){
            Toast.makeText(MainActivity.this,"请先获取ip地址",Toast.LENGTH_SHORT).show();
            return;
        }
        final LOGClient logClient = new LOGClient(endpoint, accesskeyID,
                accessKeySecret, project);

        /* 创建logGroup */
        final LogGroup logGroup = new LogGroup("sls test", ip);

        /* 存入一条log */
        Log log = new Log();
        log.PutContent("bbb", "value_3");
        log.PutContent("aaa", "value_5");

        logGroup.PutLog(log);


        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    /* 发送log 会调用网络操作，需要在一个异步线程中完成*/
                    logClient.PostLog(logGroup, logStore);
                }catch (Exception e){
                    e.printStackTrace();
                    Message message = Message.obtain(handler);
                    message.what = HANDLER_MESSAGE_UPLOAD_FAILED;
                    message.obj = e.getMessage();
                    message.sendToTarget();
                    return;
                }
                Message message = Message.obtain(handler);
                message.what = HANDLER_MESSAGE_UPLOAD_SUCCESS;
                message.sendToTarget();
            }
        }).start();



    }
}