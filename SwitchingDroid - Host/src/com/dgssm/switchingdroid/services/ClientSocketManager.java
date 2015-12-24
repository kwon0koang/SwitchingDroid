package com.dgssm.switchingdroid.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
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
public class ClientSocketManager  {

    private static final String tag = "ClientSocketManager";

	// Thread status
    
    
	// System
    private Context mContext;
    private Handler mHandler;
    
    
	// Network
    private InetAddress mAddress;
    private SocketManager mSocketManager = null;
    
    // Global
    

    
    
    
    
    
    
	/*****************************************************
	*		Initializing methods
	 * @throws IOException 
	******************************************************/
    public ClientSocketManager(Context c, Handler h, InetAddress addr) {
    	mContext = c;
    	mHandler = h;
    	mAddress = addr;
    	Log.d(tag, "+ ClientSocket: initializing");
    }
    
    
	/*****************************************************
	*		Main loop
	******************************************************/

    
    
	/*****************************************************
	*		Private methods
	******************************************************/

    
    
	/*****************************************************
	*		Public methods
	******************************************************/
    public void initialize() {
    	
        Socket socket = new Socket();
        try {
        	Log.d(tag, "+ ClientSocket: making socket");
            socket.bind(null);
            socket.connect(new InetSocketAddress(mAddress.getHostAddress(), Constants.SERVER_PORT), 10*1000);
            Log.d(tag, "+ ClientSocket: socket connected");
            mSocketManager = new SocketManager(mContext, mHandler, socket);
            mSocketManager.start();
            //new Thread(mSocketManager).start();		// Not runnable
        } catch (IOException e) {
        	mSocketManager = null;
        	
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
        
    }
    
    public boolean isClientSocketAvailable() {
    	if(mSocketManager == null)
    		return false;
    	
    	return mSocketManager.isSocketManagerAvailable();
    }
    
    public void endClientSocketManager() {
        try {
            if (mSocketManager != null && mSocketManager.isSocketManagerAvailable())
            	mSocketManager.endSocketManager();
        } catch (Exception e) {
        }
        mSocketManager = null;
    }
    
    public void sendMessageToRemote(String str) {
    	if(mSocketManager != null)
    		mSocketManager.writeStringToStream(str);
    }
    
    public void sendCommandToRemote(Command cmd) {
    	if(mSocketManager != null)
    		mSocketManager.writeCommandToStream(cmd);
    }
    
	/*****************************************************
	*		Sub classes
	******************************************************/
    
    
}
    