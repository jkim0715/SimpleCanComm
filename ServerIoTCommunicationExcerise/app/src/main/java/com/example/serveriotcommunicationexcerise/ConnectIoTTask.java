package com.example.serveriotcommunicationexcerise;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectIoTTask {
    ArrayList<Socket> list;
    Socket serverSocket;
    TextView textView;
    ServerSocket socketServer;
    Handler addSocketHandler;
    public ConnectIoTTask(ArrayList<Socket> list, Socket serverSocket, TextView textView) throws IOException {
        this.list = list;
        this.serverSocket = serverSocket;
        this.textView = textView;
        socketServer = new ServerSocket(8888);
        addSocketHandler = new Handler();
    }
    protected void acceptSocket(){
        Thread accept = new Thread(new Runnable() {
            Socket newSocket;

            @Override
            public void run() {
                while(true){
                    try {
                        newSocket = socketServer.accept();
                        Log.i("IoT",newSocket.getInetAddress()+"is accepted");
                        ReceiveTask receiveTask = new ReceiveTask(newSocket);
                        receiveTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        list.add(newSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        accept.start();
    }
    class ReceiveTask extends AsyncTask<Void, String, Void> {
        Socket mySocket;

        OutputStream out;
        DataOutputStream dout;

        InputStream in;
        DataInputStream din;

        boolean flag;
        public ReceiveTask(Socket mySocket) {
            this.mySocket = mySocket;
            flag= true;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            final String msg = values[0];
            Runnable sendToServerRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        out = serverSocket.getOutputStream();
                        dout = new DataOutputStream(out);
                        dout.writeUTF(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread sendToServerThread = new Thread(sendToServerRunnable);
            sendToServerThread.start();
            textView.append("\n"+mySocket.getInetAddress()+msg);
        }



        @Override
        protected Void doInBackground(Void... voids) {
            try {
                while(flag){
                    in = mySocket.getInputStream();
                    din = new DataInputStream(in);
                    String Msg = din.readUTF();
                    Log.i("IoT",mySocket.getInetAddress()+" is send "+Msg);
                    publishProgress(Msg);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
