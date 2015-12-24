package com.dgssm.switchingdroid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dgssm.switchingdroid.services.Command;
import com.dgssm.switchingdroid.services.TouchService;
import com.dgssm.switchingdroid.services.SwitchingDroidClientService;
import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.RecycleUtils;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.display.SimpleBitmapDisplayer;



public class SwitchingDroidClientActivity extends Activity{

	private String TAG = "WindWalkerClientActivity";
	
	// Context, System
	private Context mContext;
	private SwitchingDroidClientService mService;
	public ActivityHandler mActivityHandler;
	
	// Global
	public static final String tag = "WindWalkerClientActivity";
	
	// Layout
	//----- Main Layout
	private TextView mTextNetworkInfo = null;
	private TextView mTextConnectionInfo = null;
	private TextView mTextDeviceInfo = null;
	private TextView mTextMessage = null;
	
	private ProgressBar mProgressBar;
	
	private LinearLayout layoutBtn;
	private Button btnVolumeUp;
	private Button btnVolumeDown;
	private Button btnSetting;
	
	private ImageView imageView = null;
	private ImageLoaderConfiguration config = null;
	private DisplayImageOptions options = null; 
	
	// Flag
	private int connFlag = Constants.CONNECT_TRY;
	private int isConnected = 0;
	
	public static int displayFlag = Constants.SUB_DISPLAY;
	
	private long backKeyPressedTime = 0;
	
	/*****************************************************
	*		Initialization methods
	******************************************************/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_wind_walker_client);
		
		//----- Initialize system param, data, settings
		initialize();
		
		//----- Layout
		mTextNetworkInfo = (TextView) findViewById(R.id.network_status);
		mTextConnectionInfo = (TextView) findViewById(R.id.connection_info);
		mTextDeviceInfo = (TextView) findViewById(R.id.device_info);
		mTextMessage = (TextView) findViewById(R.id.message_box);
		
		mProgressBar = (ProgressBar) findViewById(R.id.mProgressBar);
		
		imageView = (ImageView) findViewById(R.id.imageView);
		config = new ImageLoaderConfiguration.Builder(getApplicationContext())
											 .threadPriority(Thread.NORM_PRIORITY - 2)
											 .denyCacheImageMultipleSizesInMemory()
											 .diskCacheFileNameGenerator(new Md5FileNameGenerator())
											 .diskCacheSize(50 * 1024 * 1024)
											 .tasksProcessingOrder(QueueProcessingType.LIFO)
											 .build();
		
		ImageLoader.getInstance().init(config);
		options = new DisplayImageOptions.Builder()
										 .cacheInMemory(true)
										 .build();
		
		layoutBtn = (LinearLayout) findViewById(R.id.layoutBtn);
		btnVolumeUp = (Button) findViewById(R.id.btnVolumeUp);
		btnVolumeDown = (Button) findViewById(R.id.btnVolumeDown);
		btnSetting = (Button) findViewById(R.id.btnSetting);
		btnVolumeUp.setOnClickListener(mClickListener);
		btnVolumeDown.setOnClickListener(mClickListener);
		btnSetting.setOnClickListener(mClickListener);
		///////////////////////////////////////////////////////
		
		layoutBtn.setVisibility(View.INVISIBLE);
		
		///////////////////////////////////////////////////////
	}
	
	/*****************************************************
	*		Overrided methods
	******************************************************/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.wind_walker_, menu);
		return true;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){	
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(SwitchingDroidClientActivity.this, "버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            finish();
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		displayFlag = Constants.SUB_DISPLAY;
		
		if(isConnected == 1){
			TouchService.topViewVisible();		
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		displayFlag = Constants.MAIN_DISPLAY;
		
		TouchService.topViewInvisible();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		TouchService.wm.removeView(TouchService.topView);
		TouchService.wm.removeView(TouchService.btnSwitching);
		
		finalizeActivity();    
	}
	
	@Override
	public void onLowMemory (){		// onDestroy is not always called when applications are finished by Android system.
		super.onLowMemory();
		finalizeActivity();
	}
	
	//mClickListener###########################################################
	View.OnClickListener mClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			StringBuilder sb;

			String str;
			byte[] strBytes;
			Command cmd;
			sb = new StringBuilder();
        	sb.setLength(0);

			if(v == btnVolumeUp){
				Log.e(TAG, "Volume Up aaaaaaaaaaaaaaa");
				sb.append(Constants.VOLUME_UP+"");
			}
			else if(v == btnVolumeDown){
				Log.e(TAG, "Volume Down aaaaaaaaaaaaaaa");
				sb.append(Constants.VOLUME_DOWN+"");
			}
			else if(v == btnSetting){
				Log.e(TAG, "Setting aaaaaaaaaaaaaaa");
				sb.append(Constants.MENU_SETTING+"");
			}
			
			str = sb.toString();
			strBytes = str.getBytes();
			cmd = new Command(Constants.COMMAND_MESSAGE_STRING, strBytes.length, strBytes);
			TouchService.sendCommandToRemote(cmd);
		}
	};
		
	/*****************************************************
	*		Private methods
	******************************************************/
	private void initialize() 
	{
		mActivityHandler = new ActivityHandler();
		doBindService();
	}
	
	private void finalizeActivity() {
		mService.endConnection();
		doStopService();			// Do not stop service !! Always run on background.
		RecycleUtils.recursiveRecycle(getWindow().getDecorView());
		System.gc();
	}
	
	private void doBindService() {
		bindService( new Intent(this, SwitchingDroidClientService.class), mServiceConn, Context.BIND_AUTO_CREATE);
	}
	
	private void doStopService() {
		stopService(new Intent(this, SwitchingDroidClientService.class));
		unbindService(mServiceConn);
	}
	
	private String getNetworkStatusString(int status) {
		String strReturn = null;
		
		switch(status) {
			case Constants.STATUS_INITIALIZING:
				strReturn = "INITIALIZING";
				break;
			case Constants.STATUS_REGISTER_AND_DISCOVER:
				strReturn = "REGISTER AND DISCOVER";
				break;
			case Constants.STATUS_ON_REGISTER_AND_DISCOVER:
				strReturn = "ON REGISTER AND DISCOVER";
				break;
			case Constants.STATUS_ADD_LOCAL_SERVICE:
				strReturn = "ADD LOCAL SERVICE";
				break;
			case Constants.STATUS_ADDING_LOCAL_SERVICE:
				strReturn = "ADDING LOCAL SERVICE";
				break;
			case Constants.STATUS_ADD_LOCAL_SERVICE_SUCCESS:
				strReturn = "ADD LOCAL SERVICE SUCCESS";
				break;
			case Constants.STATUS_ADD_LOCAL_SERVICE_FAIL:
				strReturn = "ADD LOCAL SERVICE FAIL";
				break;
			case Constants.STATUS_ADD_SERVICE_REQUEST:
				strReturn = "ADD SERVICE REQUEST";
				break;
			case Constants.STATUS_ADDING_SERVICE_REQUEST:
				strReturn = "ADDING SERVICE REQUEST";
				break;
			case Constants.STATUS_ADD_SERVICE_REQUEST_SUCCESS:
				strReturn = "ADD SERVICE REQUEST SUCCESS";
				break;
			case Constants.STATUS_ADD_SERVICE_REQUEST_FAIL:
				strReturn = "ADD SERVICE REQUEST FAIL";
				break;
			case Constants.STATUS_START_DISCOVER_SERVICE:
				strReturn = "START DISCOVER SERVICE";
				break;
			case Constants.STATUS_DISCOVERING_SERVICE:
				strReturn = "DISCOVERING SERVICE";
				break;
			case Constants.STATUS_START_DISCOVER_SUCCESS:
				strReturn = "START DISCOVER SUCCESS";
				break;
			case Constants.STATUS_START_DISCOVER_FAIL:
				strReturn = "START DISCOVER FAIL";
				break;
			case Constants.STATUS_TEXT_RECORD_AVAILABLE:
				strReturn = "TEXT RECORD AVAILABLE";
				break;
			case Constants.STATUS_SERVICE_AVAILABLE:
				strReturn = "SERVICE AVAILABLE";
				break;
			case Constants.STATUS_CONNECT_P2P:
				strReturn = "CONNECT P2P";
				break;
			case Constants.STATUS_CONNECTING_PEER:
				strReturn = "CONNECTING PEER";
				break;
			case Constants.STATUS_CONNECT_SUCCESS:
				strReturn = "CONNECT SUCCESS";
				break;
			case Constants.STATUS_CONNECT_FAIL:
				strReturn = "CONNECT FAIL";
				break;
			case Constants.STATUS_CONNECTION_INFO_AVAILABLE:
				strReturn = "CONNECTION INFO AVAILABLE";
				break;
			case Constants.STATUS_CONNECTED_AS_GROUP_OWNER:
				strReturn = "CONNECTED AS GROUP OWNER";
				break;
			case Constants.STATUS_CONNECTED_AS_GROUP_CLIENT:
				strReturn = "CONNECTED AS GROUP CLIENT";
				break;
			case Constants.STATUS_DISCONNECTED:
				strReturn = "DISCONNECTED";
				break;
				
			case Constants.STATUS_RESET_WIFI:
				strReturn = "RESET WIFI";
				break;
			case Constants.STATUS_CHECK_WIFI_STATUS:
				strReturn = "CHECK WIFI STATUS";
				break;
				
			case Constants.STATUS_P2P_CONNECTION_SUCCESS:
				strReturn = "P2P CONNECTION SUCCESS";
				break;

			default:
				strReturn = "INVALID STATUS...";
				break;
				
		}
		
		return strReturn;
	}
	
	/*****************************************************
	*		Sub classes
	******************************************************/
	
	//---------------------------------------------------------------------
	//	Service connection
	//---------------------------------------------------------------------
	private ServiceConnection mServiceConn = new ServiceConnection() 
	{
		public void onServiceConnected(ComponentName className, IBinder binder) {
			mService = ((SwitchingDroidClientService.WindWalkerClientServiceBinder) binder).getService();
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
							&& getNetworkStatusString(status) != "CONNECTED AS GROUP CLIENT"){
						isConnected = 0;
						
						mProgressBar.setVisibility(View.VISIBLE);
						mTextNetworkInfo.setVisibility(View.VISIBLE);
						mTextConnectionInfo.setVisibility(View.VISIBLE);
						mTextDeviceInfo.setVisibility(View.VISIBLE);
						mTextMessage.setVisibility(View.VISIBLE);
						layoutBtn.setVisibility(View.INVISIBLE);
						TouchService.topViewInvisible();
						//TouchService.btnSwitchingInvisible();
						TouchService.btnSwitchingLoading();
						connFlag = Constants.CONNECT_RETRY;
					}
					//접속 성공
					else if(getNetworkStatusString(status) == "CONNECTED AS GROUP CLIENT"){
						isConnected = 1;
						
						mProgressBar.setVisibility(View.INVISIBLE);
						mTextNetworkInfo.setVisibility(View.INVISIBLE);
						mTextConnectionInfo.setVisibility(View.INVISIBLE);
						mTextDeviceInfo.setVisibility(View.INVISIBLE);
						mTextMessage.setVisibility(View.INVISIBLE);
						layoutBtn.setVisibility(View.VISIBLE);
						if(displayFlag == Constants.SUB_DISPLAY) {
							TouchService.topViewVisible();
						}
						//TouchService.btnSwitchingVisible();
						TouchService.btnSwitchingLoadingComplete();
						connFlag = Constants.CONNECT_TRY;
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
					
					sb = new StringBuilder();
					sb.append("Remote address :").append(info.groupOwnerAddress.toString());
					mTextDeviceInfo.setText(sb.toString());
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
					break;
				}
				case Constants.ACTIVITY_MSG_UPDATE_UI :
					Uri uri = Uri.parse("file://" + (String) msg.obj);
					Log.d(TAG, " + Activity message : Update UI" + uri.toString());
					// Bitmap bitmap = ImageLoader.getInstance().loadImageSync(uri.toString(), options);
					imageView.setImageBitmap(BitmapFactory.decodeFile((String) msg.obj));
					
					break;
			}
			
			super.handleMessage(msg);
		}
	}	// End of class MainHandler
	
	
	
}
