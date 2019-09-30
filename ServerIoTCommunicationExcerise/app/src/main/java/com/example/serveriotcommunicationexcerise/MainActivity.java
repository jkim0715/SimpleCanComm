package com.example.serveriotcommunicationexcerise;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    TextView textView;
    ArrayList<Socket> socketList;
    Socket serverSocket;
    ConnectIoTTask connectIoTTask = null;
    ConnectServerTask connectServerTask = null;
    int index;

    OutputStream out;
    DataOutputStream dout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView= findViewById(R.id.textView);
        socketList = new ArrayList<Socket>();

        connectServerTask = new ConnectServerTask(8888, "70.12.60.95",socketList,textView);

        serverSocket = connectServerTask.getSocket();
        if(serverSocket == null) Log.i("Server","socket is empty");
        try {
            connectIoTTask = new ConnectIoTTask(socketList,serverSocket,textView);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(connectIoTTask !=null) {
            connectIoTTask.acceptSocket();
            Log.i("IoT","client is ready");
        }
        index = 1;
    }

    public void Onclick(View v){
        for(final Socket socket:socketList){
            Runnable testSendIoTRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        out = socket.getOutputStream();
                        dout = new DataOutputStream(out);
                        dout.writeUTF("Test : android is send "+ index);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            };
            Thread testThread = new Thread(testSendIoTRunnable);
            testThread.start();
        }
        index++;
    }
}
