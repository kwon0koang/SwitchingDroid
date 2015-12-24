package com.dgssm.switchingdroid.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.dgssm.switchingdroid.services.Command;
import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.LOGS;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

/**
 * A simple server socket that accepts connection and writes some data on
 * the stream.
 */
public class ServerSocketManager extends Thread {

    private static final String tag = "ServerSocketManager";

	// Thread status
    
    
	// System
    private Context mContext;
    private Handler mHandler;
    
    
	// Network
    private final int THREAD_COUNT = 10;

    private ServerSocket mServerSocket = null;
    private HashMap<String, SocketManager> mSocketList = new HashMap<String, SocketManager>();
    
    // Global
    private boolean mKillSign = false;
    

    
    
    
    
    
    
	/*****************************************************
	*		Initializing methods
	 * @throws IOException 
	******************************************************/
    public ServerSocketManager(Context c, Handler h) throws IOException {
    	mContext = c;
    	mHandler = h;
    	
    	makeServerSocket();
    }
    
    
	/*****************************************************
	*		Main loop
	******************************************************/
    @Override
    public void run() {
    	
        while(!Thread.interrupted()) {
            try {
                // A blocking operation.
            	// Initiate a SocketManager instance when there is a new connection
            	Log.d(tag, "+ ServerSocketManager: waiting for incoming request...");
            	SocketManager sm = new SocketManager(mContext, mHandler, mServerSocket.accept());
            	mSocketList.put(sm.getSocket().getInetAddress().getAddress().toString(), sm);
            	
            	sm.start();
            	
                Log.d(tag, "+ ServerSocket: add new SocketManager...");

            } catch (IOException e) {
            	endServerSocketManager();
                e.printStackTrace();
                break;
            }
        }
        
    }	// End of run()
    
    
	/*****************************************************
	*		Private methods
	******************************************************/

    
    
	/*****************************************************
	*		Public methods
	******************************************************/
    public void makeServerSocket() throws IOException {
        try {
        	mServerSocket = new ServerSocket(Constants.SERVER_PORT);
            Log.d(tag, "+ ServerSocket created...");
        } catch (IOException e) {
        	Log.d(tag, "+ Cannot make ServerSocket !!!");
            e.printStackTrace();
            closeAllSockets();
            throw e;
        }
    }
    
    public boolean isServerSocketAvailable() {
    	return (mServerSocket==null || mServerSocket.isClosed()) ? false : true;
    }
    
    public void endServerSocketManager() {
    	// Close every socket manager
    	closeAllSockets();
    	
        try {
            if (mServerSocket != null && !mServerSocket.isClosed())
            	mServerSocket.close();
            mServerSocket = null;
        } catch (IOException ioe) {
        	mServerSocket = null;
        }
        
    	this.interrupt();
    }
    
    public void closeAllSockets() {
    	Iterator<String> iterator = mSocketList.keySet().iterator();
    	while (iterator.hasNext()) {
    		SocketManager sm = mSocketList.get(iterator.next());
    		if( sm.isSocketManagerAvailable() ) {
    			sm.endSocketManager();
    		}
    	}
        mSocketList.clear();
    }
    
    public void sendMessageToRemote(String str) {
    	Iterator<String> iterator = mSocketList.keySet().iterator();
    	while (iterator.hasNext()) {
    		SocketManager sm = mSocketList.get(iterator.next());
    		if( sm.isSocketManagerAvailable() ) {
    			sm.writeStringToStream(str);
    		}
    	}
    }
    
    public void sendCommandToRemote(Command cmd) {
    	Iterator<String> iterator = mSocketList.keySet().iterator();
    	while (iterator.hasNext()) {
    		SocketManager sm = mSocketList.get(iterator.next());
    		if( sm.isSocketManagerAvailable() ) {
    			sm.writeCommandToStream(cmd);
    		}
    	}
    }
    
	/*****************************************************
	*		Sub classes
	******************************************************/
    
    
    
    
    
    
    

}