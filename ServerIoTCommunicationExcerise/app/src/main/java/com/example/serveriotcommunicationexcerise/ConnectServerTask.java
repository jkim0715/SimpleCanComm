package com.example.serveriotcommunicationexcerise;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class ConnectServerTask {
    int port;
    String IP;
    Socket socket = null;
    ArrayList<Socket> socketList;
    ServerSocket serverSocketTask;
    TextView textView;
    public ConnectServerTask(final int port, String ip, ArrayList<Socket> socketList, TextView textView) {
        this.port = port;
        this.IP = ip;
        this.socketList = socketList;
        this.textView = textView;

        Thread makeServerSocket = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(IP,port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        makeServerSocket.start();;
        try {
            makeServerSocket.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        serverSocketTask = new ServerSocket(socket,socketList);
        if(serverSocketTask!=null){
            serverSocketTask.execute();
        }
    }

    public Socket getSocket(){
        return socket;
    }
    class ServerSocket extends AsyncTask<Void,Object,Void> {

        Socket serverSocket = null;
        ArrayList<Socket> socketList;

        OutputStream out;
        DataOutputStream dout;

        InputStream in;
        DataInputStream din;

        public ServerSocket(Socket serverSocket, ArrayList<Socket> socketList) {
            this.serverSocket = serverSocket;
            this.socketList = socketList;
        }

        @Override
        protected void onProgressUpdate(Object... values) {


                String msg = (String)values[1];
                try {
                    textView.append("\nServer send Msg : " + msg);
                    sendMsg(msg,socketList);
                } catch (IOException e) {
                    e.printStackTrace();
                }

        }


        @Override
        protected Void doInBackground(Void... voids) {

            if(serverSocket!=null && serverSocket.isBound()){
                try {
                    while(true) {
                        in = serverSocket.getInputStream();
                        din = new DataInputStream(in);

                        String Msg = din.readUTF();
                        Object[] container = {new String("msg"), Msg};
                        publishProgress(container);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
        protected void sendMsg(String msg, ArrayList<Socket> list) throws IOException {
            final String Msg = msg;
            for(final Socket socket1:socketList){
                if(socket1!=null && socket1.isConnected()) {
                    Runnable sendRunable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                out = socket1.getOutputStream();
                                dout = new DataOutputStream(out);
                                dout.writeUTF(Msg);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    Thread sendThread = new Thread(sendRunable);
                    sendThread.start();
                }
            }
        }
    }
}
