package com.green_orca.android.robopi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;

/**
 * simple socket client to transmit prepared messages to RoboPi
 * Created by sven on 04.01.18.
 */

public class BluetoothClient {
    private OnMessageReceived mMessageListener = null;

    private BluetoothSocket mmSocket = null;
    private BluetoothDevice mmDevice;

    private static final String MY_UUID = "";
    private final String LOGTAG = "Bluetooth Client";

    PrintWriter out;
    BufferedReader in;

    String serverMAC = "";
    int serverPort = 0;

    private boolean connected = false;

    public boolean isConnected(){
        return connected;
    }
    /**
     *  Constructor of the class. OnMessagedReceived listens for the messages received from server
     */
    public BluetoothClient(OnMessageReceived listener) {
        mMessageListener = listener;

    }

    /**
     * set connection parameters and connect
     * @param device
     */
    public void connect(BluetoothDevice device, int serverPort){
        // Cancel discovery because it otherwise slows down the connection.
        //mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            BluetoothSocket tmp = null;
            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                java.util.UUID uuid = java.util.UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(LOGTAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            mmSocket.connect();
            connected = true;

        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                Log.e(LOGTAG, "Could connect client", connectException);
                mmSocket.close();
            } catch (IOException closeException) {
                Log.e(LOGTAG, "Could not close the client socket", closeException);
            }
            connected = false;
            return;
        }
        this.mmDevice = device;
        this.serverPort = serverPort;
        this.connected = true;
        Log.e(LOGTAG, "Socket's create() method succeeded");
    }

    /**
     * transmits message to RoboPi instance (probably something like "L:42;R:42;")
     * @param msg
     */
    public void send(String msg){
        if (!isConnected()){
            Log.d(LOGTAG,"reconnecting socket before sending");
            if (this.mmDevice!=null)
                this.connect( this.mmDevice, this.serverPort);
            else
                Log.d(LOGTAG, "no suitable bluetooth device to connect to");
        }

        try{
            if (out == null) {
                Log.d(LOGTAG,"fetching out printwriter before sending");
                out = new PrintWriter(mmSocket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(mmSocket.getInputStream()));
            }
            out.print(msg);
            out.flush();
            Log.e(LOGTAG, "C: Msg sent.");
            mMessageListener.messageReceived("Msg sent");
            try{
                String serverMessage = in.readLine();
                Log.d(LOGTAG, "received response: "+serverMessage);
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
            mmSocket.close();
            connected = false;
        } catch(IOException ex){
            //could't care less :-)
            Log.e("SocketClient","Crash on dispose()");
        }
    }

    /**
     * Declare the interface.
     * TODO: The method messageReceived(String message)
     * will be implemented in the MyActivity class at on asynckTask doInBackground
     */
    public interface OnMessageReceived {
        public void messageReceived(String message);
    }
}
