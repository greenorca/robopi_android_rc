package com.green_orca.android.robopi;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.util.HashMap;

public class MainActivity extends Activity implements SocketClient.OnMessageReceived{

    class CustomRunnable implements Runnable{

        String message;
        Handler handler;

        public CustomRunnable(String msg, Handler handler){
            this.message = msg;
        }

        public void run(){
            asyncSendMessage(message);
            handler.postDelayed(this, 200);
        }

    }

    SocketClient socket;
    EditText logger;
    private final String LOGTAG = "MainActivity";
    private final int FULLSPEED = 80;
    private HashMap<String,String> directions = null;
    private Button connectButton;

    /**
     * make sure to disconnect from RoboPi whenever activity is stopped
     */
    @Override
    protected void onPause(){
        if (socket.isConnected()){
            Log.e(LOGTAG,"C: sending xxx to finish");
            asyncSendMessage("xxx");
        }
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        directions = new HashMap<String, String>();

        directions.put("VOR-R", String.format("L%d;R%d;", 0, FULLSPEED));
        directions.put("VOR", String.format("L%d;R%d;", FULLSPEED, FULLSPEED));
        directions.put("VOR-L", String.format("L%d;R%d;", FULLSPEED, 0));
        directions.put("DREH-L", String.format("L%d;R%d;", FULLSPEED, -FULLSPEED));
        directions.put("RÜCK-L", String.format("L%d;R%d;", -FULLSPEED, 0));
        directions.put("RÜCK", String.format("L%d;R%d;", -FULLSPEED, -FULLSPEED));
        directions.put("RÜCK-R", String.format("L%d;R%d;", 0, -FULLSPEED));
        directions.put("DREH-R", String.format("L%d;R%d;", -FULLSPEED, FULLSPEED));
        directions.put("STOP", String.format("L%d;R%d;", 0,0));

        directions.put("KOPF LINKS", "H:-50;0;");
        directions.put("KOPF RECHTS", "H:50;0;");

        socket = new SocketClient(this);

        logger = findViewById(R.id.editText);

        RadioButton radioBtnRemoteControl = (RadioButton) findViewById(R.id.radioButtonRC);
        RadioButton radioBtnFollow = (RadioButton) findViewById(R.id.radioButtonFollow);

        //final Button tButton = (Button) findViewById(R.id.buttonFwd);

        TableLayout tblLayout = (TableLayout)findViewById(R.id.tblMovementButtons);
        Log.e(LOGTAG, "tblLayout-Kids: "+tblLayout.getChildCount());
        for (int i=0; i < tblLayout.getChildCount(); i++) {
            TableRow tRow = (TableRow) tblLayout.getChildAt(i);
            for (int j=0; j < tRow.getChildCount(); j++)
            try {
                final Button tButton = (Button) tRow.getChildAt(j);
                tButton.setOnTouchListener(new View.OnTouchListener() {

                    private Handler mHandler;

                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                if (mHandler != null)
                                    return true;
                                mHandler = new Handler();
                                mHandler.postDelayed(myAction, 200);
                                break;
                            case MotionEvent.ACTION_UP:
                                if (mHandler == null) return true;
                                mHandler.removeCallbacks(myAction);
                                mHandler = null;
                                break;
                        }
                        return false;
                    }

                    Runnable myAction = new Runnable() {
                        @Override
                        public void run() {
                            asyncSendMessage(directions.get(tButton.getText().toString()));
                            mHandler.postDelayed(this, 500);
                        }
                    };

                });
            }
            catch (Exception ex){
                Log.e(LOGTAG, ex.toString());
            }
        }

        connectButton = (Button)findViewById(R.id.buttonConnect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((Button)view).getText().toString().equals("Connect")) {
                    try {
                        connect();
                        ((Button)view).setText("Disconnect");
                    }
                    catch(Exception ex){

                    }
                }
                else {
                    Log.e(LOGTAG, "disconnecting...");
                    try {
                        socket.dispose();
                        ((Button) view).setText("Connect");
                    }
                    catch(Exception ex){

                    }
                }
            }
        });

    }

    /**
     * connect to RoboPi
     */
    private void connect(){
        EditText edi = (EditText) findViewById(R.id.textFieldIP);
        final String ip = edi.getText().toString();
        AsyncTask t = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                socket.connect(ip, 8888);
                return null;
            }
        };
        t.execute();

    }

    /**
     * handler for all direction movement buttons
     * @param v
     */
    public void doStuff(View v){
        String btnText = (String) ((Button)v).getText();
        String msg = directions.get(btnText);
        asyncSendMessage(msg);
    }

    /**
     * actually sending prepared messages
     * @param msg
     */
    private void asyncSendMessage(String msg){
        AsyncTask sendDataTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                try{
                    SocketClient socket = (SocketClient)objects[0];
                    String msg = (String)objects[1];
                    socket.send(msg);
                } catch(Exception ex){
                    Log.e(LOGTAG, "AsynTask failed: "+ex.toString());
                }
                return null;
            }
        };
        Log.e(LOGTAG,"sending:: "+msg);
        Object[] params = {socket,msg};
        sendDataTask.execute(params);
    }

    @Override
    public void messageReceived(final String message) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logger.getText().append(message+System.lineSeparator());
            }
        });
    }
}
