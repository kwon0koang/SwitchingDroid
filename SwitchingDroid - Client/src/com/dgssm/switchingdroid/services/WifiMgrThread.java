package com.dgssm.switchingdroid.services;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.util.Log;

import com.dgssm.switchingdroid.services.SwitchingDroidClientService.FileServerThread;
import com.dgssm.switchingdroid.utils.Constants;
import com.dgssm.switchingdroid.utils.LOGS;

public class WifiMgrThread extends Thread {
	
	private static final String tag = "WifiMgrThread";
	
	// Thread status
	private int mStatus = Constants.STATUS_INITIALIZING;
	private boolean mKillSign = false;
	
	// System
	private Context mContext;
	private SwitchingDroidClientService.ServiceHandler mServiceHandler;
	private ServiceListener mServiceListener;
	
	// Network
    private WifiP2pManager mWifiMgr;
    
    private WifiP2pDnsSdServiceRequest mServiceRequest;

    // TXT RECORD properties
    public static final String TXTRECORD_PROP_AVAILABLE = "available";
    public static final String SERVICE_INSTANCE = "_windwalkerclient";
    public static final String SERVICE_REG_TYPE = "_presence._tcp";
    
    static final int SERVER_PORT = 7515;

    public static final int MESSAGE_READ = 0x400 + 1;
    public static final int MY_HANDLE = 0x400 + 2;
    
    
    
    public boolean mIsWifiP2pEnabled = false;
    public boolean mOnResetWifi = false;

    private Channel mChannel;
    
    private final IntentFilter mIntentFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = null;

    public WifiMgrConnectionInfoListener mConnectionInfoListener = new WifiMgrConnectionInfoListener();
    LinkedList<WiFiP2pService> mServiceList = new LinkedList<WiFiP2pService>();
    
    private WiFiP2pService mCurrentSelectedService = null;
    
    // Global
	private long mDelayTime = -1;
	
	private FileServerThread fileServerThread1 = null;
    
	
	/*****************************************************
	*		Initializing methods
	******************************************************/
	
	public WifiMgrThread(Context c, SwitchingDroidClientService.ServiceHandler h, ServiceListener l) {
		mContext = c;
		mServiceHandler = h;
		mServiceListener = l;
		
        initializeThread();
		
        // add necessary intent values to be matched.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);			// Indicates whether Wi-Fi Peer-To-Peer (P2P) is enabled
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);			// Indicates that the available peer list has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);		// Indicates the state of Wi-Fi P2P connectivity has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);	// Indicates this device's configuration details have changed.

        initializeNetwork();
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
		
		switch(mStatus) {
		
			case Constants.STATUS_INITIALIZING:
				// 1. Need to initialize network and make channel
	            LOGS.d(tag, "# STATUS_INITIALIZING : Making channel...");

	            initializeNetwork();
				break;
			case Constants.STATUS_REGISTER_AND_DISCOVER:
				// 1-1. Make channel again
				LOGS.d(tag, "# STATUS_REGISTER_AND_DISCOVER : ...");
				
				startRegistrationAndDiscovery();
				break;
				
				
			case Constants.STATUS_ADD_LOCAL_SERVICE:
				LOGS.d(tag, "# STATUS_ADD_LOCAL_SERVICE : ...");
	            
				addLocalService();
				break;
			case Constants.STATUS_ADDING_LOCAL_SERVICE:
				LOGS.d(tag, "# STATUS_ADDING_LOCAL_SERVICE : ...");
	            
				setStatus(Constants.STATUS_ADD_SERVICE_REQUEST);
				break;
			case Constants.STATUS_ADD_SERVICE_REQUEST:
				LOGS.d(tag, "# STATUS_ADD_SERVICE_REQUEST : ...");
				
				addServiceRequest();
				break;
			case Constants.STATUS_ADDING_SERVICE_REQUEST:
				LOGS.d(tag, "# STATUS_ADDING_SERVICE_REQUEST : ...");
	            
				setStatus(Constants.STATUS_START_DISCOVER_SERVICE);
				break;
			case Constants.STATUS_START_DISCOVER_SERVICE:
				LOGS.d(tag, "# STATUS_START_DISCOVER_SERVICE : ...");
				
				discoverService();
				break;
			case Constants.STATUS_DISCOVERING_SERVICE:
				LOGS.d(tag, "# STATUS_DISCOVERING_SERVICE : discovering...");
	        	
				/* Do not terminate discovering stage..
				 * This causes infinite discovering failure.
				 * 
				if(System.currentTimeMillis() - mDelayTime > Constants.NETWORK_DISCOVERING_TIMEOUT) {
					// No response for a long time, retry from the start.
	            	setStatus(Constants.STATUS_ADD_LOCAL_SERVICE);
	            	cancelConnectionRequest();
	            	resetConnectionInfo();
	            	resetDeviceInfo();
	            	setCurrentTime();
				}
				*/
				break;
			case Constants.STATUS_ADD_LOCAL_SERVICE_FAIL:
			case Constants.STATUS_ADD_SERVICE_REQUEST_FAIL:
			case Constants.STATUS_START_DISCOVER_FAIL:
				// This state causes infinite failure while discovering
				// Reset wifi service and initialize again.
            	//cancelConnectionRequest();
            	resetConnectionInfo();
            	resetDeviceInfo();
            	setStatus(Constants.STATUS_RESET_WIFI);
				break;
				
			case Constants.STATUS_CONNECT_P2P:
				LOGS.d(tag, "# STATUS_CONNECT_P2P : Find WindWalker device and make connection...");
	        	
				if(mCurrentSelectedService != null)
					connectP2p(mCurrentSelectedService);
				else
					setStatus(Constants.STATUS_START_DISCOVER_SERVICE);
				break;
			case Constants.STATUS_CONNECTING_PEER:
				LOGS.d(tag, "# STATUS_CONNECTING_PEER : connecting...");
	        	
				if(System.currentTimeMillis() - mDelayTime > Constants.NETWORK_P2P_CONNECT_TIMEOUT) {
					// No response for a long time, retry from the start.
	            	setStatus(Constants.STATUS_ADD_LOCAL_SERVICE);
	            	cancelConnectionRequest();
	            	resetConnectionInfo();
	            	resetDeviceInfo();
	            	setCurrentTime();
				}
				break;
				
			case Constants.STATUS_CONNECTED_AS_GROUP_OWNER:
				// make server socket
				makeServerSocketManager();
				mStatus = Constants.STATUS_P2P_CONNECTION_SUCCESS;
				break;
			case Constants.STATUS_CONNECTED_AS_GROUP_CLIENT:
				// make client socket
				makeClientSocketManager();
				mStatus = Constants.STATUS_P2P_CONNECTION_SUCCESS;
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
				
			case Constants.STATUS_P2P_CONNECTION_SUCCESS:
				break;
				
			default:
				break;
		}

	}	// End of manageNetwork()
	
	
	
	
	/*****************************************************
	*		Private methods
	******************************************************/
	
	private void initializeThread() {
		// This code will be executed only once.

	}
	
	private void finalizeThread() {
		// mContext.unregisterReceiver(mReceiver);
	}
	
	private void registerWifiBroadcastReceiver() {
        // Register broadcast receiver
        mReceiver = new WiFiDirectBroadcastReceiver(mWifiMgr, mChannel, this);
        mContext.registerReceiver(mReceiver, mIntentFilter);
	}
	
	private void initializeNetwork() {
		NsdServiceInfo serviceInfo  = new NsdServiceInfo();
		// The name is subject to change based on conflicts
		serviceInfo.setServiceName(Constants.NETWORK_SERVICE_NAME);
		
        // Get wifi p2p service
        mWifiMgr = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        
        // Get channel
        // TODO: Turn on WiFi if fail to initializing
        mChannel = mWifiMgr.initialize(mContext, mContext.getMainLooper(), null);
        registerWifiBroadcastReceiver();
        registerListeners();
        
        //setStatus(Constants.STATUS_REGISTER_AND_DISCOVER);
        setStatus(Constants.STATUS_ADD_LOCAL_SERVICE);
	}
    
    private void registerListeners() {
        /*
         * Register listeners for DNS-SD services. These are callbacks invoked
         * by the system when a service is actually discovered.
         */
    	mWifiMgr.setDnsSdResponseListeners(mChannel, new DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName,
                            String registrationType, WifiP2pDevice srcDevice) {

                        // A service has been discovered. Is this our app?
                        if (instanceName.equalsIgnoreCase(SERVICE_INSTANCE)) {
                            // update the UI and add the item the discovered device.
                        	WiFiP2pService service = new WiFiP2pService();
                            service.device = srcDevice;
                            service.instanceName = instanceName;
                            service.serviceRegistrationType = registrationType;
                            
                            boolean isDuplicated = false;
                            for(int i=0; i<mServiceList.size(); i++) {
                            	WiFiP2pService temp = mServiceList.get(i);
                            	if(temp.device.deviceAddress.equals(service.device.deviceAddress)) {
                            		isDuplicated = true;
                            	}
                            }
                            if(!isDuplicated) {
                                mServiceList.add(service);
                            }
                            mCurrentSelectedService = service;

                            setCurrentTime();
                            setStatus(Constants.STATUS_CONNECT_P2P);
                            sendDeviceInfo(srcDevice);
                            
                            Log.d(tag, " ");
                            Log.d(tag, "instanceName : " + instanceName);
                            Log.d(tag, "deviceName : " + service.device.deviceName);
                            Log.d(tag, "onBonjourServiceAvailable : " + instanceName);
                            Log.d(tag, "registrationType : " + registrationType);
                            Log.d(tag, " ");
                         }
                    }
                }, new DnsSdTxtRecordListener() {
                    /**
                     * A new TXT record is available. Pick up the advertised
                     * buddy name.
                     */
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomainName, Map<String, String> record,
                            WifiP2pDevice device) {
                        Log.d(tag, device.deviceName + " is " + record.get(TXTRECORD_PROP_AVAILABLE));
                        Log.d(tag, "fullDomainName " + fullDomainName);
                        //sendDeviceInfo(device);
                    }
                });
    }
    
    /**
     * Registers a local service and then initiates a service discovery
     */
    private void startRegistrationAndDiscovery() {
    	mStatus = Constants.STATUS_ON_REGISTER_AND_DISCOVER;
    	addLocalService();
        discoverService();
    }
    
    private void addLocalService() {
    	setStatus(Constants.STATUS_ADDING_LOCAL_SERVICE);
    	
        Map<String, String> record = new HashMap<String, String>();
        record.put(TXTRECORD_PROP_AVAILABLE, "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(
                SERVICE_INSTANCE, SERVICE_REG_TYPE, record);
        
        mWifiMgr.addLocalService(mChannel, service, new ActionListener() {
            @Override
            public void onSuccess() {
                LOGS.d(tag, "Added Local Service");
                // mStatus = Constants.STATUS_ADD_LOCAL_SERVICE_SUCCESS;
            }
            @Override
            public void onFailure(int error) {
            	LOGS.d(tag, "Failed to add a service. code: "+error);
            	setStatus(Constants.STATUS_ADD_LOCAL_SERVICE_FAIL);
            }
        });
    }
    
    private void addServiceRequest() {
    	setStatus(Constants.STATUS_ADDING_SERVICE_REQUEST);
    	
        // After attaching listeners, create a service request and initiate discovery.
        mServiceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        mWifiMgr.addServiceRequest(mChannel, mServiceRequest, new ActionListener() {
            @Override
            public void onSuccess() {
            	Log.d(tag, "Added service discovery request");
            	// mStatus = Constants.STATUS_ADD_SERVICE_REQUEST_SUCCESS;
            }
            @Override
            public void onFailure(int arg0) {
            	Log.d(tag, "Failed adding service discovery request. reason code = "+arg0);
            	setStatus(Constants.STATUS_ADD_SERVICE_REQUEST_FAIL);
            }
        });
    }
    
    private void discoverService() {
    	setStatus(Constants.STATUS_DISCOVERING_SERVICE);
    	
        mServiceList.clear();
        mCurrentSelectedService = null;
        resetConnectionInfo();
        // resetDeviceInfo();
        
        mWifiMgr.discoverServices(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
            	Log.d(tag, "Service discovery initiated");
            	// mStatus = Constants.STATUS_START_DISCOVER_SUCCESS;
            }
            @Override
            public void onFailure(int arg0) {
            	Log.d(tag, "Service discovery failed");
            	setStatus(Constants.STATUS_START_DISCOVER_FAIL);
            }
        });
        setCurrentTime();
    }
	
	private void sendNetworkStatus(int status) {
		// setCurrentTime();
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
	
	public void disconnect() {
		cancelConnectionRequest();
		removeGroup();
	}
	
	private void cancelConnectionRequest() {
		mWifiMgr.cancelConnect(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
            	LOGS.d(tag, "# Aborted connection request successfully...");
            }
            @Override
            public void onFailure(int reasonCode) {
            	LOGS.d(tag, "# Abort connection failed... reason = "+reasonCode);
            }
        });
	}
	
	private void removeGroup() {
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
		mServiceList.clear();
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
	
	private void makeServerSocketManager() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_MAKE_SERVER_SOCKET, 0, 0, null, null, null);
	}
	
	private void makeClientSocketManager() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_MAKE_CLIENT_SOCKET, 0, 0, null, null, null);
	}
	
	private void closeSocketManager() {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_CLOSE_SOCKET_MANAGER, 0, 0, null, null, null);
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
    
    public class WifiMgrConnectionInfoListener implements ConnectionInfoListener {
    	@Override
    	public void onConnectionInfoAvailable(final WifiP2pInfo info) {
            /*
             * The group owner accepts connections using a server socket and then spawns a
             * client socket for every client. This is handled by {@code GroupOwnerSocketHandler}
             */
    		// InetAddress from WifiP2pInfo struct.
    		String hostAddr = info.groupOwnerAddress.getHostAddress();
    		String strIsGroupOwner = ", Group Owner : " + ((info.isGroupOwner == true) ? "yes" : "no");
    		LOGS.d(tag, "# Group Owner IP - " + hostAddr);
    		LOGS.d(tag, "# " + strIsGroupOwner);
    		
    		// After the group negotiation, we can determine the group owner.
            if (info.groupFormed && info.isGroupOwner) {
            	LOGS.d(tag, "# STATUS_CONNECTED_GROUP_AS_OWNER : ");
            	
                try {
                	Thread handler = null;
                } catch (Exception e) {
                    Log.d(tag, "Failed to create a server thread - " + e.getMessage());
                    return;
                }
                // This is important info.
                // Socket finds remote address from this.
                sendConnectionInfo(info);
            	
                setStatus(Constants.STATUS_CONNECTED_AS_GROUP_OWNER);
            } 
            else if (info.groupFormed) {
            	// The other device acts as the client. In this case,
            	// you'll want to create a client thread that connects to the group owner.
            	// Make client socket
            	
                // This is important info.
                // Socket finds remote address from this.
                sendConnectionInfo(info);
                
            	setStatus(Constants.STATUS_CONNECTED_AS_GROUP_CLIENT);
            	
             	if (fileServerThread1 == null) {
            		fileServerThread1 = new FileServerThread(mServiceListener, 9000, 0);
            		fileServerThread1.start();
            	}
            }
            else {	// exception???
            	setStatus(Constants.STATUS_START_DISCOVER_SERVICE);
            	cancelConnectionRequest();
            	resetConnectionInfo();
            	resetDeviceInfo();
            	
            	if (fileServerThread1 != null) {
            		fileServerThread1.stop();
            		fileServerThread1 = null;
            	}
            }
    	}
    }
    
    public void connectP2p(WiFiP2pService service) {
    	setStatus(Constants.STATUS_CONNECTING_PEER);
    	
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = service.device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        
        // Check if duplicated
        if (mServiceRequest != null) {
            mWifiMgr.removeServiceRequest(mChannel, mServiceRequest, new ActionListener() {
                @Override
                public void onSuccess() {
                }
                @Override
                public void onFailure(int arg0) {
                }
            });
        }

        mWifiMgr.connect(mChannel, config, new ActionListener() {
            @Override
            public void onSuccess() {
                LOGS.d(tag, "Connecting to service");
            }
            @Override
            public void onFailure(int errorCode) {
            	LOGS.d(tag, "Failed connecting to service");
            	setStatus(Constants.STATUS_START_DISCOVER_SERVICE);
            	cancelConnectionRequest();
            	resetConnectionInfo();
            	resetDeviceInfo();
            	setCurrentTime();
            }
        });
        setCurrentTime();
    }
    
    /**
     * A structure to hold service information.
     */
    public class WiFiP2pService {
        WifiP2pDevice device;
        String instanceName = null;
        String serviceRegistrationType = null;
    }

    
}	// End of class DataExtractThread







