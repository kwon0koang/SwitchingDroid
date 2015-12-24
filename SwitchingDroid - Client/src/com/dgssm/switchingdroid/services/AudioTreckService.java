package com.dgssm.switchingdroid.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class AudioTreckService{
	
	private AudioTrack m_audiotrack;
	
	int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
	int BytesPerElement = 2; // 2 bytes in 16bit format
	boolean Touch_flag = true;
	
	private AudioTCPthread audioThread = null;
	private Handler handler = null;
	protected static final	int		  PORT		=	8500;
	
	Socket socket = null;
	ServerSocket serverSocket = null;
	
	private static AudioTreckService Singleton= null;
	
	private AudioTreckService()
	{
	    m_audiotrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 8000 /* 1 second buffer */,
                AudioTrack.MODE_STREAM);    
	    
	    m_audiotrack.play();   
		Log.e("Audio", "Audioservice Start");
		audioThread = new AudioTCPthread();
		Log.e("Audio", "AsyncTesk Install");
		
	}
	
	public static synchronized AudioTreckService getInstance(){
		
		Log.e("Singleton", "START");
		if( Singleton == null)
		{
			Singleton = new  AudioTreckService();
		}
		return Singleton;
	}
	
	public void InputListener(boolean state)
	{
		Touch_flag = state;
	}
	
	void AudioThreadStart() {
		Log.d("TCP","TCPserver start");
		Singleton.Singleton_AudioThreadStart();
		
	}
		
	private void Singleton_AudioThreadStart() {
		
		Log.d("TCP","Runnable");
		
		new Thread(new Runnable() {           
            public void run() {       
                
            	try {
    				Log.d("TCP","TCPserver connecting");
    				serverSocket = new ServerSocket(PORT);
    				socket = serverSocket.accept();
    					
    			} catch (UnknownHostException e) {
    				// TODO Auto-generated catch block
    				Log.d("TCP","TCPserver error1");
    				e.printStackTrace();
    			} catch(IOException	e){
    				Log.d("TCP","TCPserver error1");
    				e.printStackTrace();
    			}
            	while(socket.isConnected())
            	{ 
            		try {
            			Log.d("TCP","TCPserver waiting");
            			DataInputStream dis = new DataInputStream( socket.getInputStream() );
            			byte[] sData = new byte[BufferElements2Rec];
            			//byte[] sData= null;
            			dis.read(sData,0,BufferElements2Rec);
            			
            			if(sData != null && Touch_flag) {
            				Log.d("TCP", "Async Thread");
            				m_audiotrack.write(sData, 0, BufferElements2Rec);
            			}
            			
            			//Log.d("TCP", "execute");
            			//audioThread.execute(sData);
						
						
						//Message msg = Message.obtain(handler, 1, sData);
						//Log.d("Handler", "Obtain");
						//Log.d("Handler", msg+"");
						//handler.sendMessage(msg);
						//Log.d("Handler", "Send Message");
						
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
            	}
            
            }
        }).start();
	}
	public void AudioThreadStop(){
		Singleton.Singleton_AudioThreadStop();
	}
	
	private void Singleton_AudioThreadStop(){
		
		Log.d("TCP","server closed");
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
public class AudioTCPthread extends AsyncTask<byte[],Void,Void>{
		
	
		
		@Override
		protected Void doInBackground(byte[]... data){
			
			
			//byte[] Data = new byte[BufferElements2Rec];
			//Data = String.getBytes();
			//String[] temp = params;
			
			
			if(data[0] != null) {
				Log.d("TCP", "Async Thread");
				m_audiotrack.write(data[0], 0, BufferElements2Rec);
			}
			return null;
		}
		
		protected void onPostExecute(){
			
		}
		
		
		public void WriteSocket(DataOutputStream data,byte[] buff) throws IOException{
			//	data send
			data.write(buff);
		}
		public void ReadSock(DataInputStream	data) throws IOException{
			//	data recieve
			byte[] datafile = null;
			
			data.read(datafile);
		
		}
	}
}
