package com.example.serialdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;

import android_serialport_api.SerialPort;

public class SerialDemoActivity extends AppCompatActivity {
    private boolean ttymxc4_exit = true;
    private Button btn_ttymxc4_onff;
    private TextView tv_ttymxc4_send, tv_ttymxc4_receive;
    private static int time_delay = 1 * 1000;

    private static final int ttymxc4_send = 0x0C;
    private static final int ttymxc4_receive = 0x10;
    private byte[] sinal_four = {4};
    private int ttymxc4_send_size = 0;
    private int ttymxc4_receive_size = 0;

    //串口
    protected SerialPort mSerialPort;
    protected InputStream mInputStream;
    protected OutputStream mOutputStream;
    private ReadThread mReadThread;

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();

            while (!isInterrupted()) {
                int size;
                try {
                    Log.v("debug", "接收线程已经开启");
                    byte[] buffer = new byte[3];

                    if (mInputStream == null)
                        return;
                    size = mInputStream.read(buffer);
                    Log.v("debug", "接收线程已经开启 =size=>>" + size);

                    if (size > 0) {
                        ttymxc4_receive_size += size;
                        mHandler.sendEmptyMessage(ttymxc4_receive);
                    }
                    //SystemClock.sleep(500);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }


    private final MyHandler mHandler = new MyHandler(this);

    class MyHandler extends Handler {
        private WeakReference<SerialDemoActivity> mActivityRef;

        public MyHandler(SerialDemoActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SerialDemoActivity activity = mActivityRef.get();
            switch (msg.what) {
                case ttymxc4_send:
                    activity.tv_ttymxc4_send.setText(String.valueOf(activity.ttymxc4_send_size));
                    break;
                case ttymxc4_receive:
                    activity.tv_ttymxc4_receive.setText(String.valueOf(activity.ttymxc4_receive_size));
                    break;
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        btn_ttymxc4_onff = findViewById(R.id.btn_ttymxc4_onff);
        tv_ttymxc4_send = findViewById(R.id.tv_ttymxc4_send);
        tv_ttymxc4_receive = findViewById(R.id.tv_ttymxc4_receive);

        try {
            mSerialPort = new SerialPort(new File("/dev/ttymxc4"), 9600, 0);//这里串口地址和比特率记得改成你板子要求的值。
            mInputStream = mSerialPort.getInputStream();
            mOutputStream = mSerialPort.getOutputStream();
           mReadThread = new ReadThread();
            mReadThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClick_ttymxc4(View view) {
        if (ttymxc4_exit) {
            ttymxc4_exit = false;
            btn_ttymxc4_onff.setText("ttymxc4 关");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!ttymxc4_exit) {
                        SystemClock.sleep(time_delay);
                        try {
                            mOutputStream.write(sinal_four);
                            ttymxc4_send_size += sinal_four.length;
                            mHandler.sendEmptyMessage(ttymxc4_send);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } else {
            ttymxc4_exit = true;
            btn_ttymxc4_onff.setText("ttymxc4 开");
        }
    }
}
