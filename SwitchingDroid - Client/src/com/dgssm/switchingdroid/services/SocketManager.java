package com.dgssm.switchingdroid.services;

import java.io.BufferedOutputStream;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.dgssm.switchingdroid.services.Command;
import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.LOGS;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * A simple server socket that accepts connection and writes some data on
 * the stream.
 */
public class SocketManager extends Thread {

    private static final String tag = "SocketManager";

	// Thread status
    
    
	// System
    private Context mContext;
    private Handler mHandler;
    
    
	// Network
    private final int THREAD_COUNT = 10;

    private Socket mSocket = null;
    private InputStream iStream;
    private OutputStream oStream;
    
    private byte[] mOutputBuffer;

    // Global
    private boolean mKillSign = false;

    
    
    
	/*****************************************************
	*		Initializing methods
	 * @throws IOException 
	******************************************************/
    public SocketManager(Context c, Handler h, Socket s) {
    	mContext = c;
    	mHandler = h;
    	mSocket = s;
    	
    	Log.d(tag, "+ ");
    	Log.d(tag, "+ Socket connected: Host = " + s.getInetAddress().getAddress());
    	Log.d(tag, "+ Socket connected: Host addr = " + s.getInetAddress().getHostAddress());
    	Log.d(tag, "+ Socket connected: Inet addr = " + s.getInetAddress().toString());
    	Log.d(tag, "+ ");
    }
    
    
	/*****************************************************
	*		Main loop
	******************************************************/
    @Override
    public void run() {
    	
    	// Wait for a while. Remote peer needs time to create socket.
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
        try {
	        iStream = mSocket.getInputStream();
	        oStream = mSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        
        byte[] buffer = new byte[2048];
        int cmd;
        int bytes;
        
        while(!Thread.interrupted()) {

            try {
            	Log.d(tag, "+ Socket : waiting for read");
            	
            	cmd = iStream.read();
                if (cmd > -1) {
                	
                	bytes = iStream.read();
                    if (bytes > -1) {
                    	if(bytes > 0) {
                // Read from the InputStream
                            int readSize = iStream.read(buffer);
                            if (readSize == bytes) {
                            	
                            	processCommand(cmd, bytes, buffer);
                            	
                            } else {
                            	Log.d(tag, "+ Socket : cannot find data or data size is incorrect");
                                continue;
                            }
                    	} else if(bytes == 0) {
                    		processCommand(cmd, 0, null);		// Command has no data. Just command only.
                    	}
                    } else {
                    	Log.d(tag, "+ Socket : Cannot read data size");
                        continue;
                    }
                    
                } else {
                	Log.d(tag, "+ Socket : Cannot read command");
                    continue;
                }

            } catch (IOException e) {
                Log.e(tag, "+ Socket : "+e.toString());
            }
                
            if(mKillSign) {
            	break;
            }
            
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//				break;
//			}
            
        }
        
    }	// End of run()
    
    
	/*****************************************************
	*		Private methods
	******************************************************/
    private void writeStream(byte[] buffer, OutputStream os) {
    	if(buffer == null || os == null) {
    		Log.d(tag, "+ Socket: buffer or output stream is null...");
    		return;
    	}
    	
        try {
            os.write(buffer);
            Log.d(tag, "+ Socket: send buffer to remote...");
            // os.flush();
        } catch (IOException e) {
            Log.d(tag, "+ Socket: Exception during write output stream");
        }
    }
    
    private void processCommand(int cmd, int size, byte[] buffer) {
        Log.d(tag, "+ Socket : received command = " + cmd);
    	switch(cmd) {
	    	case Constants.COMMAND_MESSAGE_STRING:
	    	{
	            String received = new String(buffer, 0, size);
	            // Send the obtained bytes to the UI Activity

	            Message msg = mHandler.obtainMessage(Constants.SERVICE_MSG_REMOTE_MESSAGE, -1, -1, received);
	            mHandler.sendMessage(msg);
	    	}
	    	break;
	    	
	    	default:
	    		Log.d(tag, "+ Socket : invalid command = " + cmd);
	    		break;
    	}
    }
    
    
	/*****************************************************
	*		Public methods
	******************************************************/
    
    public Socket getSocket() {
    	return mSocket;
    }
    
    public void setOutputBuffer(byte[] bArray) {
    	mOutputBuffer = bArray;
    }
    
    public boolean isSocketManagerAvailable() {
    	return (mSocket==null || mSocket.isClosed()) ? false : true;
    }
    
    public void endSocketManager() {
        try {
            if (mSocket != null && !mSocket.isClosed())
            	mSocket.close();
        } catch (IOException ioe) {
        }
        mSocket = null;
        mKillSign = true;
        this.interrupt();
    }
    
    public void writeBufferToStream(byte[] buffer) {
    	if(mSocket != null && oStream != null)
    		writeStream(buffer, oStream);
    }

    public void writeStringToStream(String str) {
    	if(mSocket != null && oStream != null) {
            try {
                // 소켓에서 얻어온 스트림을 이용하여 BufferedOutputStream 객체를 생성한다.
                BufferedOutputStream buffer_out_stream = new BufferedOutputStream(oStream); 
     
                // String 타입의 문자열을 byte 타입의 배열로 변환
                byte[] data = str.getBytes();
                // byte 배열의 길이를 구한다.
                int data_size = data.length;
     
                // 문자열 길이를 스트림에 쓴다.
                buffer_out_stream.write((byte)data_size);
                // 문자열 바이트 데이터를 스트림에 쓴다.
                buffer_out_stream.write(data);
                // 스트림에 저장해두었던 길이와 문자열 데이터를 서버에 전송한다.
                buffer_out_stream.flush();
            } catch (IOException ie) {
                // 소켓이 정상적으로 열려있지 않은 경우 발생
            }   
    	}
    }
    
    public void writeCommandToStream(Command cmd) {
    	if(mSocket != null && oStream != null && cmd != null) {
            try {
                // 소켓에서 얻어온 스트림을 이용하여 BufferedOutputStream 객체를 생성한다.
                BufferedOutputStream buffer_out_stream = new BufferedOutputStream(oStream); 
     
                // String 타입의 문자열을 byte 타입의 배열로 변환
                byte[] data = cmd.data;
                // byte 배열의 길이를 구한다.
                int data_size = cmd.length;
     
                // Write command
                buffer_out_stream.write((byte)cmd.command);
                // Write data length
                buffer_out_stream.write((byte)cmd.length);
                // Write data
                buffer_out_stream.write(cmd.data);
                // 스트림에 저장해두었던 길이와 데이터를 서버에 전송한다.
                buffer_out_stream.flush();
            } catch (IOException ie) {
                // 소켓이 정상적으로 열려있지 않은 경우 발생
            }   
    	}
    }

    
	/*****************************************************
	*		Sub classes
	******************************************************/
    

}


