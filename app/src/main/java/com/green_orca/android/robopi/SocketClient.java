package com.green_orca.android.robopi;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * simple socket client to transmit prepared messages to RoboPi
 * Created by sven on 04.01.18.
 */

public class SocketClient {
    private String serverIp = "192.168.0.30"; //your computer IP address
    private int serverPort = 8888;
    private OnMessageReceived mMessageListener = null;
    private boolean mRun = false;
    Socket socket = null;

    private final String LOGTAG = "TCP Client";
    PrintWriter out;
    BufferedReader in;

    private boolean connected = false;

    public boolean isConnected(){
        return connected;
    }
    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public SocketClient(OnMessageReceived listener) {
        mMessageListener = listener;
    }

    /**
     * set connection parameters and connect
     * @param serverIp
     * @param port
     */
    public void connect(String serverIp, int port){
        this.serverIp = serverIp;
        this.serverPort = port;
        try {
            //here you must put your computer's IP address.
            InetAddress serverAddr = InetAddress.getByName(serverIp);

            Log.e(LOGTAG, "C: Connecting...");

            //create a socket to make the connection with the server
            socket = new Socket(serverAddr, serverPort);

            try {
                //send the message to the server
                out = new PrintWriter(
                        new BufferedWriter(
                                new OutputStreamWriter(socket.getOutputStream())), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;
                Log.e(LOGTAG, "C: Connected.");
                mMessageListener.messageReceived("connected");
            } catch (Exception ei) {
                Log.e(LOGTAG, "C: Failed to get I/O from socket"+ei.getMessage());
                connected = false;
            }
        }
        catch(Exception eo){
            Log.e(LOGTAG, "C: Connecting failed outside: "+eo.toString());
            connected = false;
        }

    }

    /**
     * transmits message to RoboPi instance (probably something like "L:42;R:42;")
     * @param msg
     */
    public void send(String msg){
        if (out == null) {
            this.connect(this.serverIp, this.serverPort);
        }
        try{
            out.print(msg);
            out.flush();
            Log.e(LOGTAG, "C: Msg sent.");
            mMessageListener.messageReceived("Msg sent");
            try{
                String serverMessage = in.readLine();
                if (serverMessage != null && mMessageListener != null) {
                    //call the method messageReceived from MyActivity class
                    mMessageListener.messageReceived(serverMessage);
                }
                serverMessage = null;
            }
            catch (Exception ex){
                Log.e(LOGTAG, "C: Couldn't read response. "+ex.toString());
            }

        }
        catch(Exception ex){
            Log.e(LOGTAG, "C: Sending failed "+ex.toString());
        }
    }

    /**
     * disconnects phone from RoboPi instance
     */
    public void dispose(){
        try{
            socket.close();
            connected = false;
        } catch(IOException ex){
            //could't care less :-)
            Log.e("SocketClient","Crash on dispose()");
        }
    }

    /**
     * Declare the interface.
     * TODO: The method messageReceived(String message) will be implemented in the MyActivity class at on asynckTask doInBackground
     */
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}
