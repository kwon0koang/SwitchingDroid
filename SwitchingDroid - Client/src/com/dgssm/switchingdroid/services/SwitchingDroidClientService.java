package com.dgssm.switchingdroid.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ImageView;

import com.dgssm.switchingdroid.R;
import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.LOGS;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class SwitchingDroidClientService extends Service 
{
	private static final String TAG = "WindWalkerClientService";
	
	// Context, System
	private Context mContext = null;
	private Handler mActivityHandler = null;
	private ServiceHandler mServiceHandler = new ServiceHandler();
	private ServiceListener mServiceListener = new ServiceListenerImpl();
	private final IBinder mBinder = new WindWalkerClientServiceBinder();
	
	// Network management
	private WifiMgrThread mWifiMgrThread = null;
	private WifiP2pInfo mWifiP2PInfo = null;
	
	private ServerSocketManager mServerSocketManager = null;
	private ClientSocketManager mClientSocketManager = null;
	
	// Background working
	private TouchService mTouchService = null;
	private AudioTreckService mAudioTreckService = null;

	// Global
	private boolean isGroupOwner = false;
	
	private NotificationManager mNotiManager;
	private Notification mNoti;
	private int notiId = 777;

	/*****************************************************
	*		Overrided methods
	******************************************************/
	@Override
	public void onCreate() {
		mContext = getApplicationContext();
		initialize();
		
		mNotiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);		

		mNoti = new NotificationCompat.Builder(getApplicationContext())
				.setContentTitle("SWITCHINGDROID - CLIENT")
				.setContentText("ing...")
				.setSmallIcon(R.drawable.ic_launcher)
				.setTicker("SWITCHINGDROID - CLIENT")
				.setAutoCancel(false)
				.build();

		mNotiManager.notify(notiId, mNoti);
		startForeground(notiId, mNoti);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		initialize();
		
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onDestroy() {
		Log.e(TAG, "�꽌鍮꾩뒪 �뵒�뒪�듃濡쒖씠 aaaaaaaaaaaaa");
		mNotiManager.cancel(notiId);
		
		finalizeService();
	}
	
	@Override
	public void onLowMemory (){		// onDestroy is not always called when applications are finished by Android system.
		finalizeService();
	}

	/*****************************************************
	*		Private methods
	******************************************************/
	private void initialize() {
		startThread();
	}
	
	private void finalizeService() {
		stopThread();
	}
	
	private void startThread() {
		if(mWifiMgrThread == null)
		{
			mWifiMgrThread = new WifiMgrThread(mContext, mServiceHandler, mServiceListener);
			mWifiMgrThread.start();
		}	
		if(mTouchService == null) {
			mTouchService = new TouchService(mContext, mServiceHandler, mServiceListener);
		}
		if(mAudioTreckService == null)
		{
			mAudioTreckService = mAudioTreckService.getInstance();
			Log.e("Audio", "testtesttest");
			
			mAudioTreckService.AudioThreadStart();
		}
	}
	
	private void stopThread() {
		if(mWifiMgrThread != null && mWifiMgrThread.isAlive())
			mWifiMgrThread.interrupt();
		mWifiMgrThread.setKillSign(true);
		mWifiMgrThread = null;
		
		if(mAudioTreckService != null) {
			mAudioTreckService.AudioThreadStop();
			mAudioTreckService = null;
		}
	}
	
	private void makeServerSocketManager() {
		try {
			if(mServerSocketManager == null) {
				LOGS.d(TAG, "# makeServerSocketManager()...");
				mServerSocketManager = new ServerSocketManager(mContext, mServiceHandler);
				mServerSocketManager.start();
			} else {
				LOGS.d(TAG, "# Server socket already exists...");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void closeServerSocketManager() {
		if(mServerSocketManager != null) {
			if(mServerSocketManager.isServerSocketAvailable() == false) {
				LOGS.d(TAG, "# closeServerSocketManager()... Server socket is not started ");
				mServerSocketManager = null;
			} else {
				LOGS.d(TAG, "# closeServerSocketManager()... closing server socket.");
				mServerSocketManager.endServerSocketManager();
				mServerSocketManager = null;
			}
		}
	}
	
	private void makeClientSocketManager() {
		try {
			if(mWifiP2PInfo != null) {
				LOGS.d(TAG, "# Make client socket...");
				mClientSocketManager = new ClientSocketManager(mContext, mServiceHandler, mWifiP2PInfo.groupOwnerAddress);
				mClientSocketManager.initialize();
			} else {
				LOGS.d(TAG, "# Cannot find remote device info...");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void closeClientSocketManager() {
		if(mClientSocketManager != null) {
			if(mClientSocketManager.isClientSocketAvailable() == false) {
				mClientSocketManager = null;
			} else {
				mClientSocketManager.endClientSocketManager();
				mClientSocketManager = null;
			}
		}
	}
	
	private void closeEverySocketManager() {
		closeServerSocketManager();
		closeClientSocketManager();
	}
	
	private void sendStringToRemote(String str) {
		if(isGroupOwner) {
			if(mServerSocketManager != null && mServerSocketManager.isServerSocketAvailable())
				mServerSocketManager.sendMessageToRemote(str);
		} else {
			if(mClientSocketManager != null && mClientSocketManager.isClientSocketAvailable())
				mClientSocketManager.sendMessageToRemote(str);
		}
	}
	
	private void sendCommandToRemote(Command cmd) {
		if(isGroupOwner) {
			if(mServerSocketManager != null && mServerSocketManager.isServerSocketAvailable())
				mServerSocketManager.sendCommandToRemote(cmd);
		} else {
			if(mClientSocketManager != null && mClientSocketManager.isClientSocketAvailable())
				mClientSocketManager.sendCommandToRemote(cmd);
		}
	}
	
	/*****************************************************
	*		Public methods (Service API for Activity)
	******************************************************/
//	public boolean addScrap(ParsedResultObject pro) {
//		return mContentManager.addScrap(pro);
//	}
//	public boolean deleteScrap(ParsedResultObject pro) {
//		return mContentManager.deleteScrap(pro);
//	}
//	
//	public boolean forceUpdateAll() {
//		return mContentManager.forcedUpdate();
//	}
//	public void reserveUpdateAll() {
//		mContentManager.reserveUpdateAll();
//	}
	
	public void setActivityHandler(Handler h) {
		mActivityHandler = h;
	}
	
	public void endConnection() {
		mWifiMgrThread.endConnection();
		// need to close every socket
		closeEverySocketManager();
	}
	
	/*****************************************************
	*		Sub classes
	******************************************************/
	public class WindWalkerClientServiceBinder extends Binder 
	{
		public SwitchingDroidClientService getService() {
			return SwitchingDroidClientService.this;
		}
	}
	
	class ServiceHandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what) {
				case Constants.SERVICE_MSG_REMOTE_MESSAGE:
				{
					if(mActivityHandler != null) {
						Message send_msg = mActivityHandler.obtainMessage(Constants.ACTIVITY_MSG_REMOTE_MESSAGE);
						send_msg.obj = msg.obj;		// message string
						mActivityHandler.sendMessage(send_msg);
					}
					break;
				}
			}
			
			super.handleMessage(msg);
		}
	}	// End of class MainHandler
	
	class ServiceListenerImpl implements ServiceListener 
	{
		
		@Override
		public void OnReceiveCallback(int msgtype, int arg0, int arg1, String arg2, String arg3, Object arg4)
		{
			
			switch(msgtype) {
			
				// Received from WiFi manager thread
				case Constants.SERVICE_MSG_NETWORK_STATUS_NOTI:
				{
					if(mActivityHandler != null) {
						Message msg = mActivityHandler.obtainMessage(Constants.ACTIVITY_MSG_NETWORK_STATUS_NOTI);
						msg.arg1 = arg0;		// Network status code
						mActivityHandler.sendMessage(msg);
					}
					break;
				}
				case Constants.SERVICE_MSG_CONNECTION_INFO:
				{
					if(mActivityHandler != null) {
						Message msg = mActivityHandler.obtainMessage(Constants.ACTIVITY_MSG_CONNECTION_INFO);
						msg.obj = arg4;						// Connection info (WifiP2pInfo)
						mWifiP2PInfo = (WifiP2pInfo)arg4;	// Remember this !!!
						mActivityHandler.sendMessage(msg);
					}
					break;
				}
				case Constants.SERVICE_MSG_RESET_CONNECTION_INFO:
				{
					if(mActivityHandler != null) {
						Message msg = mActivityHandler.obtainMessage(Constants.ACTIVITY_MSG_RESET_CONNECTION_INFO);
						mActivityHandler.sendMessage(msg);
					}
					break;
				}
				case Constants.SERVICE_MSG_DEVICE_INFO:
				{
					if(mActivityHandler != null) {
						Message msg = mActivityHandler.obtainMessage(Constants.ACTIVITY_MSG_DEVICE_INFO);
						msg.obj = arg4;		// Device info
						mActivityHandler.sendMessage(msg);
					}
					break;
				}
				case Constants.SERVICE_MSG_RESET_DEVICE_INFO:
				{
					if(mActivityHandler != null) {
						Message msg = mActivityHandler.obtainMessage(Constants.ACTIVITY_MSG_RESET_DEVICE_INFO);
						mActivityHandler.sendMessage(msg);
					}
					break;
				}
				
				case Constants.SERVICE_MSG_MAKE_SERVER_SOCKET:
				{
					if(mWifiP2PInfo != null) {
						isGroupOwner = true;
						makeServerSocketManager();
					} else {
						LOGS.d(TAG, "# Cannot find remote device info...");
					}
					break;
				}
				case Constants.SERVICE_MSG_MAKE_CLIENT_SOCKET:
				{
					if(mWifiP2PInfo != null) {
						isGroupOwner = false;
						makeClientSocketManager();
					} else {
						LOGS.d(TAG, "# Cannot find remote device info...");
					}
					break;
				}
				case Constants.SERVICE_MSG_CLOSE_SOCKET_MANAGER:
				{
					closeEverySocketManager();
					break;
				}
				
				// Received from WorkThread
				case Constants.SERVICE_MSG_SEND_STRING_TO_REMOTE:
				{
					sendStringToRemote(arg2);
					break;
				}
				case Constants.SERVICE_MSG_SEND_COMMAND_TO_REMOTE:
				{
					Log.d("SERVICE_MSG_SEND_COMMAND_TO_REMOTE", "aaaaaaaaaaaaaaa");
					
					//
					/*StringBuilder sb = new StringBuilder();
					sb.append("클占쏙옙占싱억옙트占쏙옙占쏙옙占쏙옙占쏙옙占승메쏙옙占쏙옙占쏙옙");
					
					// Send this to remote
					if(sb.length() > 1) {
						String str = sb.toString();
						byte[] strBytes = str.getBytes();
						Command cmd = new Command(Constants.COMMAND_MESSAGE_STRING, strBytes.length, strBytes);
						sendCommandToRemote(cmd);
					}*/
					//
					
					
					sendCommandToRemote((Command)arg4);
					break;
				}
				case Constants.SERVICE_MSG_UPDATE_UI :
					if (mActivityHandler != null) {
						Message msg = mActivityHandler.obtainMessage(Constants.ACTIVITY_MSG_UPDATE_UI, arg2);
						mActivityHandler.sendMessage(msg);
					}
					
					break;
				
			} // End of switch(msgtype)
		}
	}	// End of class ServiceListenerImpl
	
	/** FileServerThread Class **/
	public static class FileServerThread implements Runnable {
		// Debug
		private static final	String	TAG	= "FileServerThread";
		
		// State
		private final		 	int 	RUNNING 	= 0;
  		private final 			int 	SUSPENDED 	= 1;
  		private final 			int 	STOPPED 	= 2;
  		private 				int 	state 		= RUNNING;
  		
  		// Thread
  		private 				Thread 	thread 		= null;
  		
  		// Socket
		private ServerSocket serverSocket = null;
		private	int	port = 0;
		
		// File
		private String filePath = null;
		
		// Callback
        private ServiceListener serviceListener = null;
        
        private int number = 0;
		
        /** Constructor **/
		public FileServerThread(ServiceListener serviceListener, int port, int number) {
			this.thread = new Thread(this);
			this.port = port;
			// this.filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + number + ".png";
			this.serviceListener = serviceListener;
		}
		
		/** Override Method **/
		@Override
		public void run() {
			try {
				serverSocket = new ServerSocket(port);
				Log.d(TAG, "Server: Socket opened");
				
				while (true) {
	  				if (checkState()) {
	  					Log.d(TAG, "capture thread stop");
	  					
	  					thread = null;
	  					break;
	  				}
	  				
  					Socket client = serverSocket.accept();
  					Log.d(TAG, "Server: connection done");
  					
  					number = (number + 1) % 5;
  					filePath = Environment.getExternalStorageDirectory() + "/" + number + ".png";
  					final File f = new File(filePath);

  					Log.d(TAG, "server: copying files " + f.toString());
//  	                File dirs = new File(f.getParent());
//  	                if (!dirs.exists())
//  	                    dirs.mkdirs();
//  	                f.createNewFile();
  					
  	                InputStream inputStream = client.getInputStream();
//  					try {
//  						inputStream = client.getInputStream();
//  						
//  						Bitmap bitmap = BitmapFactory.decodeStream(bufferedInputStream);
//  		            	Log.d(TAG, "inputStrem -> bitmap");
//
//  		            	sendToUpdateUiCommand(bitmap);
//  					} catch (IOException ioe) {
//  						ioe.printStackTrace();
//  					} finally {
//  						Log.d(TAG, "client close()");
//  						client.close();
//  					}
  					
  					copyFile(inputStream, new FileOutputStream(f));
  					sendToUpdateUiCommand(f.getAbsolutePath());
  					client.close();
				}
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		
		/** User Define Method **/
		private void copyFile(InputStream inputStream, OutputStream out) {
	        byte buf[] = new byte[1024];
	        int len;
	        try {
	            while ((len = inputStream.read(buf)) != -1) {
	                out.write(buf, 0, len);
	            }
	            
	            out.close();
	            inputStream.close();
	        } catch (IOException e) {
	            Log.d("FileTransferAsyncTask", e.toString());
	        }
	    }
		
		public void start() {
  			thread.start();
  			setState(RUNNING);
  		}
  		
  		public void resume() {
  			setState(RUNNING);
  		}
  		
  		public void suspend() {
  			setState(SUSPENDED);
  		}
  		
  		public void stop() {  			
  			setState(STOPPED);
  		}
  		
		private synchronized void sendToUpdateUiCommand(String path) {
			if (path != null) {
				serviceListener.OnReceiveCallback(Constants.SERVICE_MSG_UPDATE_UI, 0, 0, path, null, null);
				Log.d(TAG, " + Service message : Update UI " + path);
			}
			else {
				Log.e(TAG, "path is null");
			}
		}
		
  		private synchronized void setState(int state) {  			
  			this.state = state;
  			
  			if (this.state == RUNNING) {
  				notify();
  			} else {
  				thread.interrupt();
  			}
  		}
  		
  		private synchronized boolean checkState() {
  			while (state == SUSPENDED) {
  				try {
  					wait();
  				} catch ( InterruptedException ie ) {
  					ie.printStackTrace();
  				}
  			}
  			
  			return state == STOPPED;
  		}
	}
	
	// End of FileServerThread	
}
