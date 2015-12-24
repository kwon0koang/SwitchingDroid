package com.dgssm.switchingdroid;

import net.pocketmagic.android.eventinjector.Events;
import net.pocketmagic.android.eventinjector.Events.InputDevice;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dgssm.switchingdroid.services.TouchData;
import com.dgssm.switchingdroid.services.SwitchingDroidHostService;
import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.RecycleUtils;


public class SwitchingDroidHostActivity extends Activity{

	private String TAG = "WindWalkerHostActivity";
	
	
	// Context, System
	private Context mContext;
	private SwitchingDroidHostService mService;
	public ActivityHandler mActivityHandler;
	
	// Layout
	//----- Main Layout
	private TextView mTextNetworkInfo = null;
	private TextView mTextConnectionInfo = null;
	private TextView mTextDeviceInfo = null;
	private TextView mTextMessage = null;

	private int prevTouch=999;

	private Button btnTest;
	
	private Events events;
	
	private ProgressBar mProgressBar;
	
	private AudioManager mAudioManager;

	private int connFlag = Constants.CONNECT_TRY;
	/*****************************************************
	*		Initialization methods
	******************************************************/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_wind_walker_host);
		
		//----- Initialize system param, data, settings
		initialize();
		
		//----- Layout
		mTextNetworkInfo = (TextView) findViewById(R.id.network_status);
		mTextConnectionInfo = (TextView) findViewById(R.id.connection_info);
		mTextDeviceInfo = (TextView) findViewById(R.id.device_info);
		mTextMessage = (TextView) findViewById(R.id.message_box);
		
		mProgressBar = (ProgressBar) findViewById(R.id.mProgressBar);
		
		mAudioManager =  (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		//터치 테스트
		Events.intEnableDebug(1);
		
		events = new Events();
		events.Init();
		
		for (InputDevice idev:events.m_Devs) {
			idev.Open(true);
		}

		
	}
	
	/*****************************************************
	*		Overrided methods
	******************************************************/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.wind_walker_host, menu);
		return true;
	}
	
	//백버튼 눌렀을 때,
	//서비스 onDestroy 호출됨
	//그래서 백키 후킹해서 홈이벤트로 대체
	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		//HOME
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){	//화면변환시(가로,세로) Reload방지
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		finalizeActivity();
	}
	
	@Override
	public void onLowMemory (){		// onDestroy is not always called when applications are finished by Android system.
		super.onLowMemory();
		finalizeActivity();
	}

	
	/*****************************************************
	*		Private methods
	******************************************************/
	private void initialize() 
	{
		mActivityHandler = new ActivityHandler();
		doBindService();
	}
	
	private void finalizeActivity() {
		//mService.endConnection();
		//doStopService();			// Do not stop service !! Always run on background.
		RecycleUtils.recursiveRecycle(getWindow().getDecorView());
		System.gc();
	}
	
	private void doBindService() {
		bindService( new Intent(this, SwitchingDroidHostService.class), mServiceConn, Context.BIND_AUTO_CREATE);
	}
	
	private void doStopService() {
		stopService(new Intent(this, SwitchingDroidHostService.class));
		unbindService(mServiceConn);
	}
	
	private String getNetworkStatusString(int status) {
		String strReturn = null;
		
		switch(status) {
			case Constants.STATUS_INITIALIZING:
				strReturn = "STATUS INITIALIZING";
				break;
			case Constants.STATUS_MAKING_CHANNEL:
				strReturn = "STATUS MAKING CHANNEL";
				break;
			case Constants.STATUS_CHANNEL_DISCONNECT:
				strReturn = "STATUS CHANNEL DISCONNECT";
				break;
			case Constants.STATUS_CHANNEL_CONNECTED:
				strReturn = "STATUS CHANNEL CONNECTED";
				break;
			case Constants.STATUS_DISCOVER_PEERS:
				strReturn = "STATUS DISCOVER PEERS";
				break;
			case Constants.STATUS_DISCOVER_PEERS_FAILED:
				strReturn = "STATUS DISCOVER PEERS FAILED";
				break;
			case Constants.STATUS_DISCOVER_PEERS_SUCCESS:
				strReturn = "STATUS DISCOVER PEERS SUCCESS";
				break;
			case Constants.STATUS_REQUEST_PEERS:
				strReturn = "STATUS REQUEST PEERS";
				break;
			case Constants.STATUS_PEER_LIST_FAILED:
				strReturn = "STATUS PEER LIST FAILED";
				break;
			case Constants.STATUS_PEER_LIST_RECEIVED:
				strReturn = "STATUS PEER LIST RECEIVED";
				break;
			case Constants.STATUS_CONNECT_PEER:
				strReturn = "STATUS CONNECT PEER";
				break;
			case Constants.STATUS_CONNECT_PEER_FAILED:
				strReturn = "STATUS CONNECT PEER FAILED";
				break;
			case Constants.STATUS_CONNECT_PEER_SUCCESS:
				strReturn = "STATUS CONNECT PEER SUCCESS";
				break;
			case Constants.STATUS_REQUEST_CONNECTION_INFO:
				strReturn = "STATUS REQUEST CONNECTION INFO";
				break;
			case Constants.STATUS_CONNECTION_INFO_FAILED:
				strReturn = "STATUS CONNECTION INFO FAILED";
				break;
			case Constants.STATUS_CONNECTION_INFO_AVAILABLE:
				strReturn = "STATUS CONNECTION INFO AVAILABLE";
				break;
			case Constants.STATUS_CONNECTED_GROUP_AS_OWNER:
				strReturn = "STATUS CONNECTED GROUP AS OWNER";
				break;
			case Constants.STATUS_CONNECTED_GROUP_AS_CLIENT:
				strReturn = "STATUS CONNECTED GROUP AS CLIENT";
				break;
			case Constants.STATUS_CONNECTED_TO_UNKNOWN:
				strReturn = "STATUS CONNECTED TO UNKNOWN";
				break;
			case Constants.STATUS_P2P_CONNECTION_SUCCESS:
				strReturn = "STATUS P2P CONNECTION SUCCESS";
				break;
			default:
				strReturn = "INVALID STATUS...";
				break;
				
		}
		
		return strReturn;
	}
	
	private boolean isTouchServiceRunning(Context ctx, String s_service_name) {
    	ActivityManager manager = (ActivityManager) ctx.getSystemService(Activity.ACTIVITY_SERVICE);
    	for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
    	    if (s_service_name.equals(service.service.getClassName())) {
    	        return true;
    	    }
    	}
    	return false;
	}

	
	
	
	/*****************************************************
	*		Public methods
	******************************************************/
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	/*****************************************************
	*		Sub classes
	******************************************************/
	
	//---------------------------------------------------------------------
	//	Service connection
	//---------------------------------------------------------------------
	private ServiceConnection mServiceConn = new ServiceConnection() 
	{
		public void onServiceConnected(ComponentName className, IBinder binder) {
			mService = ((SwitchingDroidHostService.WindWalkerHostServiceBinder) binder).getService();
			mService.setActivityHandler(mActivityHandler);
		}

		public void onServiceDisconnected(ComponentName className) {
			mService.setActivityHandler(null);
			mService = null;
		}
	};
	
	public class ActivityHandler extends Handler 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what) {
				case Constants.ACTIVITY_MSG_NETWORK_STATUS_NOTI:
				{
					int status = msg.arg1;		// Network status code
					mTextNetworkInfo.setText(getNetworkStatusString(status));
					
					//접속 시도
					if(connFlag == Constants.CONNECT_TRY 
							&& getNetworkStatusString(status) != "STATUS CONNECTED GROUP AS OWNER"){
						mProgressBar.setVisibility(View.VISIBLE);
						connFlag = Constants.CONNECT_RETRY;
					}
					//접속 성공
					else if(getNetworkStatusString(status) == "STATUS CONNECTED GROUP AS OWNER"){
						mProgressBar.setVisibility(View.INVISIBLE);
						connFlag = Constants.CONNECT_TRY;
						
						//HOME
						Intent intent = new Intent(Intent.ACTION_MAIN);
						intent.addCategory(Intent.CATEGORY_HOME);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					}
					
					break;
				}
				case Constants.ACTIVITY_MSG_CONNECTION_INFO:
				{
					WifiP2pInfo info = (WifiP2pInfo)msg.obj;		// Connection info
					StringBuilder sb = new StringBuilder();
					sb.append("Connection info : \n").append("IsGroupFormed=").append(info.groupFormed)
						.append(", IsGroupOwner=").append(info.isGroupOwner)
						.append(", Owner IP= ").append(info.groupOwnerAddress.getHostAddress());
					mTextConnectionInfo.setText(sb.toString());
					break;
				}
				case Constants.ACTIVITY_MSG_RESET_CONNECTION_INFO:
				{
					mTextConnectionInfo.setText("connection_info");
					break;
				}
				case Constants.ACTIVITY_MSG_DEVICE_INFO:
				{
					WifiP2pDevice info = (WifiP2pDevice)msg.obj;		// Connection info
					StringBuilder sb = new StringBuilder();
					sb.append("Device info : \n").append(info.toString());
					mTextDeviceInfo.setText(sb.toString());
					break;
				}
				case Constants.ACTIVITY_MSG_RESET_DEVICE_INFO:
				{
					mTextDeviceInfo.setText("device_info");
					break;
				}
				
				case Constants.ACTIVITY_MSG_REMOTE_MESSAGE:
				{
					String info = (String)msg.obj;
					mTextMessage.setText("Received msg: " + info);
					
					String[] imsi = info.split("#");
					double[] tData = new double[6];
					for(int m=0; m<imsi.length; m++){
						tData[m] = Double.parseDouble(imsi[m]);
						Log.d(tData[m] + "", "aaaaaaaaaaaaaaa");
					}

					TouchData eData = new TouchData(0, 0, 0);
			    	eData.what = (int)tData[0];
					eData.x = (int)(tData[1]*1.41);
					eData.y = (int)(tData[2]*1.46);
					
					//touch ##########################################
					//down
					if(eData.what == Constants.TOUCH_DOWN){
						Log.d(TAG, eData.what + " 터치 다운 aaaaaaaaaaaaaaa");
						events.m_Devs.get(5).mTouchDown(eData.x, eData.y);
					}
					//move
					else if(eData.what == Constants.TOUCH_MOVE){
						Log.d(TAG, eData.what + " 무브 aaaaaaaaaaaaaaa");
						events.m_Devs.get(5).mTouchMove(eData.x, eData.y);
					}
					//up
					else if(eData.what == Constants.TOUCH_UP){
						Log.d(TAG, eData.what + " 터치 업 aaaaaaaaaaaaaaa");
						events.m_Devs.get(5).mTouchUp(eData.x, eData.y);
					}//touch end ##########################################
					
					//prev touch setting
					prevTouch = eData.what;
					
					
					
					
					
					//hardware key ###############################
					if(eData.what == Constants.VOLUME_UP){
						Log.d(TAG, "volume up aaaaaaaaaaaaaaa");
						events.m_Devs.get(0).SendKey(115, true); //key down
						events.m_Devs.get(0).SendKey(115, false); //key up
						mAudioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
					}
					else if(eData.what == Constants.VOLUME_DOWN){
						Log.d(TAG, "volume down aaaaaaaaaaaaaaa");
						events.m_Devs.get(0).SendKey(114, true); //key down
						events.m_Devs.get(0).SendKey(114, false); //key up
						mAudioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
					}
					else if(eData.what == Constants.MENU_SETTING){
						Log.d(TAG, "menu setting aaaaaaaaaaaaaaa");
						Toast.makeText(SwitchingDroidHostActivity.this, "Setting", Toast.LENGTH_SHORT).show();
					}
					//hardware key end ###############################

					
					break;
				}
				
			}
			
			super.handleMessage(msg);
		}
	}	// End of class MainHandler
	
}





