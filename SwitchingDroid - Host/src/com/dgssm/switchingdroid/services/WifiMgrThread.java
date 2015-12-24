package com.dgssm.switchingdroid.services;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.LOGS;

public class WifiMgrThread extends Thread {
	
	private static final String tag = "WifiMgrThread";
	
	// Thread status
	private int mStatus = Constants.STATUS_INITIALIZING;
	private boolean mKillSign = false;
	
	// System
	private Context mContext;
	private SwitchingDroidHostService.ServiceHandler mServiceHandler;
	private ServiceListener mServiceListener;
	
	// Network
    private WifiP2pManager mWifiMgr;

    private boolean mIsWifiP2pEnabled = false;
    private boolean mRetryChannel = false;
    public boolean mOnResetWifi = false;

    private Channel mChannel;
    private WifiMgrChannelListener mChannelListener = new WifiMgrChannelListener();
    
    private final IntentFilter mIntentFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = null;

    private WifiMgrDiscoverActionListener mDiscoverActionListener = new WifiMgrDiscoverActionListener();
    
    public List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();
    public WifiMgrPeerListListener mPeerListListener = new WifiMgrPeerListListener();
    public WifiMgrPeerConnectActionListener mPeerConnectActionListener = new WifiMgrPeerConnectActionListener();
    
    public WifiMgrConnectionInfoListener mConnectionInfoListener = new WifiMgrConnectionInfoListener();
    
    private AudioRecordService	 audioRecordService   = null;
	
    
    // Global
	private long mDelayTime = -1;
    
	
	// new thing
	public static int peer_connect = 0;
	
	/*****************************************************
	*		Initializing methods
	******************************************************/
	
	public WifiMgrThread(Context c, SwitchingDroidHostService.ServiceHandler h, ServiceListener l) {
		mContext = c;
		mServiceHandler = h;
		mServiceListener = l;
		mStatus = Constants.STATUS_INITIALIZING;
		
        // add necessary intent values to be matched.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);			// Indicates whether Wi-Fi Peer-To-Peer (P2P) is enabled
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);			// Indicates that the available peer list has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);		// Indicates the state of Wi-Fi P2P connectivity has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);	// Indicates this device's configuration details have changed.

        // Get wifi p2p service
        mWifiMgr = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        
        // Get channel
        mChannel = mWifiMgr.initialize(mContext, mContext.getMainLooper(), mChannelListener);
        registerWifiBroadcastReceiver();
        setStatus(Constants.STATUS_MAKING_CHANNEL);
	}

	
	
	
	
	/*****************************************************
	*		Main loop
	******************************************************/
	
	@Override
	public void run() 
	{
		while(!Thread.interrupted())
		{
			manageNetwork();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			
			if(mKillSign)
				break;
			
		}	// End of while() loop
		
		// Finalize
		finalizeThread();
		
	}	// End of run()
	
	
	
	private void manageNetwork() {
		Log.e(tag ,peer_connect + " aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		
		switch(mStatus) {
		
			case Constants.STATUS_INITIALIZING:
				// 1. Need to make channel
	            LOGS.d(tag, "# STATUS_INITIALIZING : Making channel...");
	        	
	            mRetryChannel = false;
	            mChannel = mWifiMgr.initialize(mContext, mContext.getMainLooper(), mChannelListener);
	            //closeSocketManager();		// Close socket manager. Because device's network info may be updated.
	            
	            setStatus(Constants.STATUS_CHANNEL_CONNECTED);
				break;
			case Constants.STATUS_CHANNEL_DISCONNECT:
				// 1-1. Make channel again
				LOGS.d(tag, "# STATUS_CHANNEL_DISCONNECT : Make channel again...");
				
				mChannel = null;
				mRetryChannel = false;
				disconnect();
				setStatus(Constants.STATUS_INITIALIZING);
				break;
				
				
			case Constants.STATUS_CHANNEL_CONNECTED:
				// 2. Discover peers
				LOGS.d(tag, "# STATUS_CHANNEL_CONNECTED : Channel connected. Discover peers...");
	            
	            mWifiMgr.discoverPeers(mChannel, mDiscoverActionListener);
				resetConnectionInfo();
				resetDeviceInfo();
	            setStatus(Constants.STATUS_DISCOVER_PEERS);
				break;
			case Constants.STATUS_DISCOVER_PEERS_FAILED:
				// 2-1. No peer found or errors occurred, discover peers again
				LOGS.d(tag, "# STATUS_DISCOVER_PEERS_FAILED : No peer found or errors occurred, discover peers again...");
				
				setStatus(Constants.STATUS_RESET_WIFI);
				break;
			case Constants.STATUS_DISCOVER_PEERS_SUCCESS:
				// 3-2. If WIFI_P2P_PEERS_CHANGED_ACTION doesn't arrive, retry discovering.
				LOGS.d(tag, "# STATUS_DISCOVER_PEERS_SUCCESS : ...");
				if(System.currentTimeMillis() - mDelayTime > Constants.NETWORK_P2P_PEERS_CHANGE_TIMEOUT) {
					cancelConnectionRequest();
					setStatus(Constants.STATUS_DISCOVER_PEERS_FAILED);
				}
				break;
				
				
	        // 3. If discover succeeded, WIFI_P2P_PEERS_CHANGED_ACTION will be arrived on BroadcastReceiver
	        //    and then call requestPeers() to extract peer list
			   case Constants.STATUS_PEER_LIST_RECEIVED:
		              // 4. PeerListListener.onPeersAvailable() receives peer list.
		              //    Find WindWalker client peer and make connection 
		            LOGS.d(tag, "# STATUS_PEER_LIST_RECEIVED : Find WindWalker client peer and make connection...");
		            boolean isOk_flag = false;
		              WifiP2pDevice device = mPeers.get(0);
		            
		      
		              for(int i=0; i<mPeers.size(); i++){
		                 //WifiP2pDevice dev = mPeers.get(i);
		       
		                 device = mPeers.get(i);
		                
		                 //if(!device.deviceAddress.equals("32:cd:a7:3d:7e:5c") || device.deviceAddress.equals("00:08:ca:8b:3f:e3"))
		                 if(device.deviceAddress.equals("8e:3a:e3:94:a7:f8"))	//�겢�씪�씠�뼵�듃 �꽖�꽌�뒪7 �븘�땲硫� 臾댁떆
		                 {
		                	 isOk_flag = true;
		                	 break; 
		                 }
		                 else
		                 {
		                       	continue;
		                 }
		              }
		              
		              
		              if(!isOk_flag)
		              {
		            	  setStatus(Constants.STATUS_PEER_LIST_FAILED);
		            	  break;
		              }
		              else
		              {
		            	  peer_connect = 1;
		            	  sendDeviceInfo(device) ;
			              WifiP2pConfig config = new WifiP2pConfig();
			              config.deviceAddress = device.deviceAddress;
			              config.wps.setup = WpsInfo.PBC;
			              
			              // TODO: use [device.deviceName] and [device.primaryDeviceType]
			              mWifiMgr.connect(mChannel, config, mPeerConnectActionListener);
			              setStatus(Constants.STATUS_CONNECT_PEER);
			             
		              }
		            
		            break;
			case Constants.STATUS_PEER_LIST_FAILED:
				// 4-1. No peer found or errors occurred. Discover peers again.
				LOGS.d(tag, "# STATUS_PEER_LIST_FAILED : No peer found or errors occurred. Discover peers again...");
				
				mPeers.clear();
				cancelConnectionRequest();
				setStatus(Constants.STATUS_CHANNEL_CONNECTED);
				break;
			case Constants.STATUS_CONNECT_PEER_SUCCESS:
				// 4-2. If connection request takes time too long, disconnect this request.
				if(System.currentTimeMillis() - mDelayTime > Constants.NETWORK_CONNECTION_REQUEST_TIMEOUT) {
					resetDeviceInfo();
					disconnect();
					setStatus(Constants.STATUS_PEER_LIST_RECEIVED);
				}
				break;
			case Constants.STATUS_CONNECT_PEER_FAILED:
				// 4-3. Cannot connect to peer. Discover peers again.
				LOGS.d(tag, "# STATUS_CONNECT_PEER_FAILED : No peer found or errors occurred. Discover peers again...");
				
				disconnect();
				mStatus = Constants.STATUS_CONNECT_PEER_SUCCESS;		// Wait for a while to check whether Connect request is lost or not.
				setCurrentTime();
				break;
				
				
			// 5. Connected !!
			case Constants.STATUS_CONNECTED_GROUP_AS_OWNER:
				makeServerSocketManager();
				mStatus = Constants.STATUS_P2P_CONNECTION_SUCCESS;
				break;
			case Constants.STATUS_CONNECTED_GROUP_AS_CLIENT:
				makeClientSocketManager();
				mStatus = Constants.STATUS_P2P_CONNECTION_SUCCESS;
				break;
			case Constants.STATUS_CONNECTED_TO_UNKNOWN:
				LOGS.d(tag, "# STATUS_CONNECTED_TO_UNKNOWN : Unknown peer. Discover peers again...");
				
				resetConnectionInfo();
				resetDeviceInfo();
				disconnect();
				setStatus(Constants.STATUS_CHANNEL_CONNECTED);
				break;
				
				
				
			// 6. WiFi direct connection established. Do nothing.				
			case Constants.STATUS_P2P_CONNECTION_SUCCESS:
				
//				if(audioRecordService == null) {
//					Log.e("Audio","AudioService Start");
//					audioRecordService = new AudioRecordService(mContext);
//					audioRecordService.AudioThreadStart();
//				}
				
				break;
				
				
				
				
			case Constants.STATUS_RESET_WIFI:
				mOnResetWifi = true;
				turnOffWifi();
				setStatus(Constants.STATUS_CHECK_WIFI_STATUS);
				break;
			case Constants.STATUS_CHECK_WIFI_STATUS:
				turnOnWifi();
				if( isWifiOn() ) {
					mOnResetWifi = false;
					setStatus(Constants.STATUS_WAIT_AFTER_RESET_WIFI);
				}
				break;
			case Constants.STATUS_WAIT_AFTER_RESET_WIFI:
				if(System.currentTimeMillis() - mDelayTime > Constants.NETWORK_WAITING_WIFI_RESET) {
					setStatus(Constants.STATUS_INITIALIZING);
				}
				break;
				
			default:
				break;
		}

	}	// End of manageNetwork()
	
	
	
	
	
	
	
	
	
	
	
	
	/*****************************************************
	*		Private methods
	******************************************************/
	
	private void initializeThread() {
		
	}
	
	private void finalizeThread() {
		// mContext.unregisterReceiver(mReceiver);
	}
	
	private void registerWifiBroadcastReceiver() {
        // Register broadcast receiver
        mReceiver = new WiFiDirectBroadcastReceiver(mWifiMgr, mChannel, this);
        mContext.registerReceiver(mReceiver, mIntentFilter);
	}
	
	////////////////////////////////////////////////////////////
	// Call service callback
	//
	private void sendNetworkStatus(int status) {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_NETWORK_STATUS_NOTI, status, 0, null, null, null);
	}
	
	private void setCurrentTime() {
		mDelayTime = System.currentTimeMillis();
	}
	
	private void sendConnectionInfo(Object obj) {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_CONNECTION_INFO, 0, 0, null, null, obj);
	}
	
	private void resetConnectionInfo() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_RESET_CONNECTION_INFO, 0, 0, null, null, null);
	}
	
	private void sendDeviceInfo(Object obj) {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_DEVICE_INFO, 0, 0, null, null, obj);
	}
	
	private void resetDeviceInfo() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_RESET_DEVICE_INFO, 0, 0, null, null, null);
	}
	
	private void makeServerSocketManager() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_MAKE_SERVER_SOCKET, 0, 0, null, null, null);
	}
	
	private void makeClientSocketManager() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_MAKE_CLIENT_SOCKET, 0, 0, null, null, null);
	}
	
	private void closeSocketManager() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_CLOSE_SOCKET_MANAGER, 0, 0, null, null, null);
	}
	//
	// End of service callback
	////////////////////////////////////////////////////////////
	
	private void removeGroup() {
		if(mChannel == null || mWifiMgr == null)
			return;
		
		mWifiMgr.removeGroup(mChannel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(tag, "removeGroup failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
            	Log.d(tag, "removeGroup succeeded.");
            }

        });
	}
	
	private void cancelConnectionRequest() {
		if(mChannel == null || mWifiMgr == null)
			return;
		
		mWifiMgr.cancelConnect(mChannel, new ActionListener() {

            @Override
            public void onSuccess() {
            	LOGS.d(tag, "# Aborted connection request successfully...");
            }

            @Override
            public void onFailure(int reasonCode) {
            	LOGS.d(tag, "# Abort connection failed...");
            }
        });
	}
	
	private void disconnect() {
		cancelConnectionRequest();
		removeGroup();
		
		if(audioRecordService != null) {
			audioRecordService.AudioThreadStop();
			audioRecordService = null;
		}
	}
	
	private void turnOnWifi() {
		final WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		
		if(wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLING
				|| wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
			// Do nothing. Just wait.
		} else if(wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
			// Do nothing. Just wait.
		} else {
			wifi.setWifiEnabled(true);
			Log.d(tag, "# Turn on WiFi.");
		}
	}
	
	private void turnOffWifi() {
		final WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		wifi.setWifiEnabled(false);
		Log.d(tag, "# Turn off WiFi.");
	}
	
	private boolean isWifiOn() {
		final WifiManager wifi = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		return (wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED ? true : false);
	}
	
	
	
	
	
	
	/*****************************************************
	*		Public methods
	******************************************************/
	
	public int getStatus() {
		return mStatus;
	}
	
	public void setStatus(int status) {
		mStatus = status;
		setCurrentTime();
		sendNetworkStatus(mStatus);
	}
	
	public void setKillSign(boolean is) {
		mKillSign = is;
		finalizeThread();
	}
	
	public void endConnection() {
		disconnect();
	}
	
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.mIsWifiP2pEnabled = isWifiP2pEnabled;
    }
	
	
    
    
    
	/*****************************************************
	*		Sub classes
	******************************************************/
    public class WifiMgrChannelListener implements ChannelListener {
        @Override
        public void onChannelDisconnected() {
        	mStatus = Constants.STATUS_CHANNEL_DISCONNECT;
            // we will try once more
            if (mWifiMgr != null && !mRetryChannel) {
            	LOGS.d(tag, "# STATUS_CHANNEL_DISCONNECT : Channel lost. Trying again...");
                mRetryChannel = true;
                mChannel = mWifiMgr.initialize(mContext, mContext.getMainLooper(), this);
            } 
            else {
            	LOGS.d(tag, "# STATUS_CHANNEL_DISCONNECT : Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P...");
                mChannel = null;
                mRetryChannel = false;
                setStatus(Constants.STATUS_INITIALIZING);
            }
        }
    }
	
    public class WifiMgrDiscoverActionListener implements ActionListener {

        @Override
        public void onSuccess() {
            // Code for when the discovery initiation is successful goes here.
            // No services have actually been discovered yet, so this method
            // can often be left blank.  Code for peer discovery goes in the
            // onReceive method, detailed below.
        	LOGS.d(tag, "# STATUS_DISCOVER_PEERS_SUCCESS : Discovery Initiated...");
        	
        	// Do not use setStatus()... we dont need to notify UI thread.
        	mStatus = Constants.STATUS_DISCOVER_PEERS_SUCCESS;
        	setCurrentTime();
        }

        @Override
        public void onFailure(int reasonCode) {
            // Code for when the discovery initiation fails goes here.
            // Alert the user that something went wrong.
        	LOGS.d(tag, "# STATUS_DISCOVER_PEERS_FAILED : Discovery Failed...");
            setStatus(Constants.STATUS_DISCOVER_PEERS_FAILED);
        }
    }
    
    public class WifiMgrPeerListListener implements PeerListListener {
    	@Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
    		mStatus = Constants.STATUS_PEER_LIST_RECEIVED;
            mPeers.clear();
            mPeers.addAll(peerList.getDeviceList());
            Log.e(tag,"UUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU          "+mPeers.size());
            
            if (mPeers.size() < 1) {
                LOGS.d(tag, "# STATUS_PEER_LIST_FAILED : No devices found...");
                setStatus(Constants.STATUS_PEER_LIST_FAILED);
                return;
            }
    	}
    }
    
    public class WifiMgrPeerConnectActionListener implements ActionListener {

        @Override
        public void onSuccess() {
        	// WiFiDirectBroadcastReceiver will notify us. Ignore for now.
        	LOGS.d(tag, "# STATUS_CONNECT_PEER_SUCCESS : Peer connection established !!");
            setStatus(Constants.STATUS_CONNECT_PEER_SUCCESS);
        }

        @Override
        public void onFailure(int reasonCode) {
            // Code for when the discovery initiation fails goes here.
            // Alert the user that something went wrong.
        	LOGS.d(tag, "# STATUS_CONNECT_PEER_FAILED : Peer connection Failed : " + reasonCode);
            setStatus(Constants.STATUS_CONNECT_PEER_FAILED);
        }
    }
    
    public class WifiMgrConnectionInfoListener implements ConnectionInfoListener {
    	@Override
    	public void onConnectionInfoAvailable(final WifiP2pInfo info) {

    		// InetAddress from WifiP2pInfo struct.
    		String hostAddr = info.groupOwnerAddress.getHostAddress();
    		String strIsGroupOwner = ", Group Owner : " + ((info.isGroupOwner == true) ? "yes" : "no");
    		LOGS.d(tag, "# Group Owner IP - " + hostAddr);
    		LOGS.d(tag, "# " + strIsGroupOwner);
    		
    		sendConnectionInfo(info);
    		
    		// After the group negotiation, we can determine the group owner.
            if (info.groupFormed && info.isGroupOwner) {
            	// Do whatever tasks are specific to the group owner.
            	// One common case is creating a server thread and accepting
            	// incoming connections.
            	LOGS.d(tag, "# STATUS_CONNECTED_GROUP_AS_OWNER : ");
            	setStatus(Constants.STATUS_CONNECTED_GROUP_AS_OWNER);
            	
                // This is important info.
                // Socket finds remote address from this.
                sendConnectionInfo(info);
                
                if(audioRecordService == null) {
					Log.e("Audio","AudioService Start");
					audioRecordService = new AudioRecordService(mContext);
					audioRecordService.AudioThreadStart();
				}
            } 
            else if (info.groupFormed) {
            	// The other device acts as the client. In this case,
            	// you'll want to create a client thread that connects to the group owner.
            	LOGS.d(tag, "# STATUS_CONNECTED_GROUP_AS_CLIENT : ");
            	setStatus(Constants.STATUS_CONNECTED_GROUP_AS_CLIENT);
            	
                // This is important info.
                // Socket finds remote address from this.
                sendConnectionInfo(info);
            }
            else {	// exception???
            	setStatus(Constants.STATUS_CONNECTED_TO_UNKNOWN);
            	
            	if(audioRecordService != null) {
					Log.e("Audio","AudioService Stop");
					audioRecordService.AudioThreadStop();
					audioRecordService = null;
				}
            }
    	}
    }
    
    
}	// End of class DataExtractThread







