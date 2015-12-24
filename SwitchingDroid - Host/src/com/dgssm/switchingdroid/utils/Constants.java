package com.dgssm.switchingdroid.utils;

public class Constants {
	
	//---------- In code strings
	public static final String URLPrefix = "http://";
	
	
	//---------- Host service status
	public static final int STATUS_INITIALIZING = 0;

	public static final int STATUS_MAKING_CHANNEL = 1;
	public static final int STATUS_CHANNEL_DISCONNECT = 2;
	public static final int STATUS_CHANNEL_CONNECTED = 3;

	public static final int STATUS_DISCOVER_PEERS = 11;
	public static final int STATUS_DISCOVER_PEERS_FAILED = 12;
	public static final int STATUS_DISCOVER_PEERS_SUCCESS = 13;

	public static final int STATUS_REQUEST_PEERS = 21;			// After receive [WIFI_P2P_PEERS_CHAGNED_ACTION]
	public static final int STATUS_PEER_LIST_FAILED = 22;		// No peer found
	public static final int STATUS_PEER_LIST_RECEIVED = 23;

	public static final int STATUS_CONNECT_PEER = 31;
	public static final int STATUS_CONNECT_PEER_FAILED = 32;
	public static final int STATUS_CONNECT_PEER_SUCCESS = 33;

	public static final int STATUS_REQUEST_CONNECTION_INFO = 41;		// After receive [WIFI_P2P_CONNECTION_CHANGED_ACTION ]
	public static final int STATUS_CONNECTION_INFO_FAILED = 42;
	public static final int STATUS_CONNECTION_INFO_AVAILABLE = 43;		// receive on ConnectionInfoListener

	public static final int STATUS_CONNECTED_GROUP_AS_OWNER = 51;
	public static final int STATUS_CONNECTED_GROUP_AS_CLIENT = 52;
	public static final int STATUS_CONNECTED_TO_UNKNOWN = 53;
	
	public static final int STATUS_RESET_WIFI = 101;
	public static final int STATUS_CHECK_WIFI_STATUS = 102;
	public static final int STATUS_WAIT_AFTER_RESET_WIFI = 103;	//==> STATUS_INITIALIZING
	
	public static final int STATUS_P2P_CONNECTION_SUCCESS = 1000;
	
	
	
	//---------- Activity handler message type
	public static final int ACTIVITY_MSG_NETWORK_STATUS_NOTI = 11;
	public static final int ACTIVITY_MSG_DEVICE_INFO = 21;
	public static final int ACTIVITY_MSG_RESET_DEVICE_INFO = 28;
	public static final int ACTIVITY_MSG_CONNECTION_INFO = 31;
	public static final int ACTIVITY_MSG_RESET_CONNECTION_INFO = 38;

	public static final int ACTIVITY_MSG_REMOTE_MESSAGE = 51;
	
	
	//---------- Service listener message type
	public static final int SERVICE_MSG_NETWORK_STATUS_NOTI = 11;
	public static final int SERVICE_MSG_DEVICE_INFO = 21;
	public static final int SERVICE_MSG_RESET_DEVICE_INFO = 28;
	public static final int SERVICE_MSG_CONNECTION_INFO = 31;
	public static final int SERVICE_MSG_RESET_CONNECTION_INFO = 38;
	
	public static final int SERVICE_MSG_REMOTE_MESSAGE = 51;	// Display received string
	
	public static final int SERVICE_MSG_MAKE_SERVER_SOCKET = 101;
	public static final int SERVICE_MSG_MAKE_CLIENT_SOCKET = 102;
	public static final int SERVICE_MSG_CLOSE_SOCKET_MANAGER = 103;
	
	public static final int SERVICE_MSG_SEND_STRING_TO_REMOTE = 201;	// Send string to remote
	public static final int SERVICE_MSG_SEND_COMMAND_TO_REMOTE = 202;	// Send string to remote
	
	public static final int SERVICE_MSG_READ_SCREEN_IMAGE = 301;
	
	//---------- Delay time
	public static final long NETWORK_P2P_PEERS_CHANGE_TIMEOUT = 1*60*1000; 		// 3 minutes
	public static final long NETWORK_CONNECTION_REQUEST_TIMEOUT = 30*1000; 	// 3 minutes
	public static final long NETWORK_WAITING_WIFI_RESET = 10*1000; 		// 10 seconds
	
	
	//---------- Network
    public static final int SERVER_PORT = 7515;
	
	
	//---------- Network message Commands (Socket communication, 0 <= command <= 255)
	public static final int COMMAND_MESSAGE_STRING = 1;
	
	//---------- Touch		-kwon
	public static final int TOUCH_DOWN = 0;
	public static final int TOUCH_POINTER_DOWN = 5;
	public static final int TOUCH_UP = 1;
	public static final int TOUCH_POINTER_UP = 6;
	public static final int TOUCH_MOVE = 2;
	public static final int TOUCH_CANCEL = 3;
	public static final int TOUCH_CLICK = 555;
	
	//---------- Hardware Key		-kwon
	public static final int VOLUME_UP = 1991;
	public static final int VOLUME_DOWN = 1992;
	public static final int MENU_SETTING = 1993;

	//---------- Connect		-kwon
	public static final int CONNECT_TRY=1981;
	public static final int CONNECT_RETRY=1982;
	
}
