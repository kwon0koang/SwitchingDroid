package com.dgssm.switchingdroid.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

public class AudioRecordService  {

	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	private static final int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
	private static final int BytesPerElement = 2; // 2 bytes in 16bit format

	private AudioRecord m_audiorecorder;
	
	private AudioTCPthread audioThread = null;
	
	private Context mContext;
	public boolean mThreadStart = false;
	

	protected static final	String SERV_IP		=	"192.168.49.76";
	protected static final	int		  PORT		=	8500;
	
	Socket socket = null;

	public AudioRecordService(Context context){
		
		mContext = context;
		Log.e("AudioRecoding","TCPserver ServiceStart");
		 m_audiorecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
					RECORDER_SAMPLERATE, RECORDER_CHANNELS,
					RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);
		 m_audiorecorder.startRecording();
		
		 Log.e("AudioRecoding","TCPserver initailze");
		 
		 mThreadStart = true;
		this.audioThread = new AudioTCPthread();

	}
	
	public void AudioThreadStart() {
		
		new Thread(new Runnable() {           
            public void run() {       
                
            	try {
        			Log.d("TCP","TCPserver connecting");
        			InetAddress serverAddr = InetAddress.getByName(SERV_IP);
        			socket = new Socket(serverAddr, PORT);
        		
        		} catch (UnknownHostException e) {
        			// TODO Auto-generated catch block
        			Log.d("TCP","TCPserver error1");
        			Log.d("TCP",e+" ");
        			e.printStackTrace();
        		} catch(IOException	e){
        			Log.d("TCP","TCPserver error2");
        			Log.d("TCP",e+" ");
        			e.printStackTrace();
        		}
            	
                while (socket.isConnected()) {
                   
        			try {
        				Log.d("TCP","TCPserver execute");
        				//audioThread.execute();
        				
        				byte sData[] = new byte[BufferElements2Rec];
        				m_audiorecorder.read(sData, 0,BufferElements2Rec );
        				
        				DataOutputStream dos = new DataOutputStream( socket.getOutputStream() );
        				        				
        				synchronized (dos) {
        					dos.write( sData, 0, BufferElements2Rec );
        					dos.flush();
        				}
        				
        				Log.d("TCP", sData+"");
        				Log.d("TCP", sData.length+"");
        			
        				
        			}
            		catch (IOException e) {
            			// TODO Auto-generated catch block
            			e.printStackTrace();
            		}
                	
                }
            }
        }).start();
	}
	
	public void AudioThreadStop() {
		
		Log.d("TCP","server closed");
		try {
			socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

		
	
	public class AudioTCPthread extends AsyncTask<Void, Void, Void>{
		
		@Override
		protected Void doInBackground(Void... params){
			
			Log.d("TCP","TCPserver send data");
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
