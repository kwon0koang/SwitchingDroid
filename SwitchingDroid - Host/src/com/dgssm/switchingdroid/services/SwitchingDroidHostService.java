package com.dgssm.switchingdroid.services;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.dgssm.switchingdroid.R;
import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.LOGS;

public class SwitchingDroidHostService extends Service 
{
	private static final String TAG = "WindWalkerHostService";
	
	// Context, System
	private Context mContext = null;
	private Handler mActivityHandler = null;
	private ServiceHandler mServiceHandler = new ServiceHandler();
	private ServiceListener mServiceListener = new ServiceListenerImpl();
	private final IBinder mBinder = new WindWalkerHostServiceBinder();
	
	// Network management
	private WifiMgrThread mWifiMgrThread = null;
	private WifiP2pInfo mWifiP2PInfo = null;
	
	private ServerSocketManager mServerSocketManager = null;
	private ClientSocketManager mClientSocketManager = null;
	
	// Background working
	private ScreenCaptureService screenCaptureService = null;
	
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
				.setContentTitle("SWITCHINGDROID - HOST")
				.setContentText("ing...")
				.setSmallIcon(R.drawable.ic_launcher)
				.setTicker("SWITCHINGDROID - HOST")
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
		if(mWifiMgrThread == null) {
			mWifiMgrThread = new WifiMgrThread(mContext, mServiceHandler, mServiceListener);
			mWifiMgrThread.start();
		}

		if (screenCaptureService == null) {
			screenCaptureService = new ScreenCaptureService(mContext, mServiceHandler);
			screenCaptureService.captureThreadStart();
		}
	}
	
	private void stopThread() {
		if(mWifiMgrThread != null && mWifiMgrThread.isAlive())
			mWifiMgrThread.interrupt();
		mWifiMgrThread.setKillSign(true);
		mWifiMgrThread = null;

		if (screenCaptureService != null) {
			screenCaptureService.captureThreadStop();
			screenCaptureService = null;
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
	public class WindWalkerHostServiceBinder extends Binder 
	{
		public SwitchingDroidHostService getService() {
			return SwitchingDroidHostService.this;
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
				case Constants.SERVICE_MSG_READ_SCREEN_IMAGE :
					Uri uri = Uri.parse("file://" + (String) msg.obj);
					Log.e(TAG, uri.toString());
					Intent serviceIntent = new Intent(mContext, FileTransferService.class);
				    serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
				    serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
				    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, "192.168.49.76");
			        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, msg.arg1);
//			        serviceIntent.putExtra(FileTransferService.EXTRAS_IMAGE_FILE, (byte[]) msg.obj);
			        mContext.startService(serviceIntent);
			        
			        break;
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
						msg.obj = arg4;							// Connection info (WifiP2pInfo)
						mWifiP2PInfo = (WifiP2pInfo)arg4;		// Remember this !!!
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
					sendCommandToRemote((Command)arg4);
					break;
				}
				
			} // End of switch(msgtype)
		}
	}	// End of class ServiceListenerImpl
	
	
	
	

	
	
	
	
}
