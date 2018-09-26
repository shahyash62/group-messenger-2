package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static android.os.SystemClock.sleep;
import static edu.buffalo.cse.cse486586.groupmessenger2.GroupMessengerActivity.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = "shahyash";
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    static boolean f0 = true, f1 = true, f2 = true, f3 = true, f4 = true;
    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    static String minePort;
    static int A = 0;
    static int P = 0;
    static int N;
    static HashMap<String, Integer> holdBack = new HashMap<String, Integer>();
    static String[] msgCount = new String[35];
    static int key = 0;

//reference: https://stackoverflow.com/questions/26230225/hashmap-getting-first-key-value
    private static Map<String, Integer> sortMapByValues(Map<String, Integer> aMap) {

        Set<Entry<String,Integer>> mapEntries = aMap.entrySet();

/*        System.out.println("Values and Keys before sorting ");
        for(Entry<String,Float> entry : mapEntries) {
            System.out.println(entry.getValue() + " - "+ entry.getKey());
        }*/

        // used linked list to sort, because insertion of elements in linked list is faster than an array list.
        List<Entry<String,Integer>> aList = new LinkedList<Entry<String,Integer>>(mapEntries);

        // sorting the List
        Collections.sort(aList, new Comparator<Entry<String,Integer>>() {

            @Override
            public int compare(Entry<String, Integer> ele1,
                               Entry<String, Integer> ele2) {

                return ele1.getValue().compareTo(ele2.getValue());
            }
        });

        // Storing the list into Linked HashMap to preserve the order of insertion.
        Map<String,Integer> aMap2 = new LinkedHashMap<String, Integer>();
        for(Entry<String,Integer> entry: aList) {
            aMap2.put(entry.getKey(), entry.getValue());
        }

        return aMap2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        final EditText editText = (EditText) findViewById(R.id.editText1);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        minePort = myPort;
        switch (Integer.parseInt(myPort)) {
            case 11108:
                N = 0;
                break;
            case 11112:
                N = 1;
                break;
            case 11116:
                N = 2;
                break;
            case 11120:
                N = 3;
                break;
            case 11124:
                N = 4;
                break;
        }
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
        /*String msg = "message: " + myPort;
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);*/
        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                    String msg = editText.getText().toString() + "\n";
                    editText.setText("");
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            ServerSocket serverSocket = sockets[0];
            Socket clientSocket;
            DataInputStream msgIn;
            String msgReceived;
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
            uriBuilder.scheme("content");
            Uri mUri = uriBuilder.build();
            ContentResolver mContentResolver = getContentResolver();
            try {
                clientSocket = serverSocket.accept();
                msgIn = new DataInputStream(clientSocket.getInputStream());
                while((msgReceived = msgIn.readUTF()) != null) {
                    if (msgReceived.contains(":")) {
                        Log.i(TAG, "doInBackground: 1st time msg received: " + msgReceived);
                        String[] splitMsg = msgReceived.split(":");
                        if (!minePort.equals(splitMsg[2])) {
                            int PS = Math.max(A, P) + N;
                            P = PS;
                            String msgToSend;
                            msgToSend = splitMsg[0] + "~" + Integer.toString(PS) + "~" + splitMsg[2];
                            holdBack.put(splitMsg[0] + "_ND", PS); //message added in queue as ND
                            Log.i(TAG, "doInBackground: 1st time message put in hqueue: " + splitMsg[0] + "_ND");
                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, "proposal");
                        }
                    }
                    else if (msgReceived.contains("~")) {
                        Log.i(TAG, "doInBackground: hfs msg received:" + msgReceived);
                        String[] splitMsg = msgReceived.split("~");
                        boolean msgFound = false;
                        Integer s = holdBack.get(splitMsg[0] + "_ND");
                        s = Math.max(s, Integer.parseInt(splitMsg[1]));
                        holdBack.put(splitMsg[0]+"_ND", s);
                        for (int x = 0; x < msgCount.length; x++) {
                            String[] tempM;
                            if (msgCount[x] != null) {
                                tempM = msgCount[x].split("_");
                            }
                            else {
                                tempM = new String[1];
                                tempM[0] = "";
                            }
                            Log.i(TAG, "doInBackground: comparing strings: " + splitMsg[0] +" & " + tempM[0]);
                            if (splitMsg[0].equals(tempM[0])) {
                                msgFound = true;
                                Log.i(TAG, "doInBackground: message found!");
                                int count = Integer.parseInt(tempM[2]) + 1;
                                Log.i(TAG, "doInBackground: message count = " + count);
                                if (count == 3) {
                                    Log.i(TAG, "doInBackground: count reached 5");
                                    //msgCount[x] = tempM[0] + "_D_" + Integer.toString(count);
                                    msgCount[x] = null;
                                    Log.i(TAG, "doInBackground: message D: " + splitMsg[0]);
                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, splitMsg[0]+"_"+Integer.toString(s), "done");
                                }
                                else {
                                    msgCount[x] = tempM[0] + "_ND_" + Integer.toString(count);
                                }
                                break;
                            }
                        }
                        if (!msgFound) {
                            Log.i(TAG, "doInBackground: message not found in queue");
                            for (int i = 0; i < msgCount.length; i++) {
                                if (msgCount[i] == null) {
                                    msgCount[i] = splitMsg[0] + "_ND_0";
                                    Log.i(TAG, "doInBackground: initial message stored in queue: " + msgCount[i]);
                                    break;
                                }
                            }
                        }
                    }
                    else if (msgReceived.contains("_")) {
                        String[] splitMsg = msgReceived.split("_");
                        holdBack.put(splitMsg[0] + "_D", holdBack.remove(splitMsg[0]+"_ND"));
                        holdBack.put(splitMsg[0] + "_D", Integer.parseInt(splitMsg[1]));
                        A = Math.max(A, Integer.parseInt(splitMsg[1]));
                        boolean flag = true;
                        while (flag) {
                            Map<String, Integer> sortedMap = sortMapByValues(holdBack);
                            //sortedMap.values().toArray()[0];
                            if (sortedMap.isEmpty()) {
                                break;
                            }
                            Map.Entry<String, Integer> entry = sortedMap.entrySet().iterator().next();
                            String firstItem = entry.getKey();
                            //Float value=entry.getValue();
                            String[] testMsg = firstItem.split("_");
                            Log.i(TAG, "doInBackground: done message received: " + msgReceived);
                            if (testMsg[1].equals("D")) {
                                ContentValues mNewValues = new ContentValues();
                                //Log.i(TAG, "doInBackground: break1");
                                mNewValues.put("key", Integer.toString(key));
                                //Log.i(TAG, "doInBackground: break2");
                                mNewValues.put("value", testMsg[0]);
                                //Log.i(TAG, "doInBackground: break3");
                                mContentResolver.insert(mUri, mNewValues);
                                Log.i(TAG, "doInBackground: Message Stored: " + testMsg[0] + " with key: " + key);
                                holdBack.remove(testMsg[0] + "_D");
                                key++;
                            }
                            else {
                                flag = false;
                            }
                        }
                    }
                    clientSocket = sockets[0].accept();
                    msgIn = new DataInputStream(clientSocket.getInputStream());
                }

            } catch (IOException e) {
                Log.i(TAG, "doInBackground: Exception in reading message\n" + e);
            }

            //Log.i(TAG, "Recieved message: " + message);
            return null;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            DataOutputStream msgOut;
            try {
                if (msgs[1].equals("proposal")) {
                    String msgToSend;
                    String[] splitMsg = msgs[0].split("~");
                    msgToSend = splitMsg[0] + "~" + splitMsg[1];
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(splitMsg[2]));
                    msgOut = new DataOutputStream(socket.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();
                    Log.i(TAG, "doInBackground: hfs msg sent: " + msgToSend);
                }
                else if (msgs[1].equals("done")) {
                    String msgToSend = msgs[0];
                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT0));
                    msgOut = new DataOutputStream(socket0.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT1));
                    msgOut = new DataOutputStream(socket1.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT2));
                    msgOut = new DataOutputStream(socket2.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT3));
                    msgOut = new DataOutputStream(socket3.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT4));
                    msgOut = new DataOutputStream(socket4.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();
                    Log.i(TAG, "doInBackground: done msg sent:" + msgToSend);
                }
                else {
                    int max = Math.max(A, P) + N;
                    String msgToSend = msgs[0] + ":" + Integer.toString(max) + ":" + msgs[1];
                    holdBack.put(msgs[0]+"_ND", max);

                    Socket socket0 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT0));
                    msgOut = new DataOutputStream(socket0.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket1 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT1));
                    msgOut = new DataOutputStream(socket1.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket2 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT2));
                    msgOut = new DataOutputStream(socket2.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket3 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT3));
                    msgOut = new DataOutputStream(socket3.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();

                    Socket socket4 = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT4));
                    msgOut = new DataOutputStream(socket4.getOutputStream());
                    msgOut.writeUTF(msgToSend);
                    msgOut.flush();
                    Log.i(TAG, "doInBackground: done msg sent:" + msgToSend);
                    Log.i(TAG, "doInBackground: 1st time msg sent: " + msgToSend);
                }
            /*} catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");*/
            } catch (Exception e) {
                Log.e(TAG, "ClientTask socket IOException");
            }

            return null;
        }
    }
}