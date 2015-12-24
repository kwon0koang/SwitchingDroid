package com.dgssm.switchingdroid.utils;

public class Constants {
	
	//---------- In code strings
	
	
	//---------- Host service status
	public static final int STATUS_INITIALIZING = 0;
	
	public static final int STATUS_REGISTER_AND_DISCOVER = 1;
	public static final int STATUS_ON_REGISTER_AND_DISCOVER = 2;

	public static final int STATUS_ADD_LOCAL_SERVICE = 3;
	public static final int STATUS_ADDING_LOCAL_SERVICE = 4;
	public static final int STATUS_ADD_LOCAL_SERVICE_SUCCESS = 5;
	public static final int STATUS_ADD_LOCAL_SERVICE_FAIL = 6;		//==> STATUS_REGISTER_AND_DISCOVER

	public static final int STATUS_ADD_SERVICE_REQUEST = 11;
	public static final int STATUS_ADDING_SERVICE_REQUEST = 12;
	public static final int STATUS_ADD_SERVICE_REQUEST_SUCCESS = 13;
	public static final int STATUS_ADD_SERVICE_REQUEST_FAIL = 14;		//==> STATUS_ADD_SERVICE_REQUEST

	public static final int STATUS_START_DISCOVER_SERVICE = 21;
	public static final int STATUS_DISCOVERING_SERVICE = 22;
	public static final int STATUS_START_DISCOVER_SUCCESS = 23;
	public static final int STATUS_START_DISCOVER_FAIL = 24;		//==> STATUS_START_DISCOVER_SERVICE

	public static final int STATUS_TEXT_RECORD_AVAILABLE = 31;
	public static final int STATUS_SERVICE_AVAILABLE = 32;			//==> STATUS_CONNECT_P2P

	public static final int STATUS_CONNECT_P2P = 41;
	public static final int STATUS_CONNECTING_PEER = 42;
	public static final int STATUS_CONNECT_SUCCESS = 43;
	public static final int STATUS_CONNECT_FAIL = 44;				//==> STATUS_SERVICE_AVAILABLE

	public static final int STATUS_CONNECTION_INFO_AVAILABLE = 51;
	public static final int STATUS_CONNECTED_AS_GROUP_OWNER = 52;
	public static final int STATUS_CONNECTED_AS_GROUP_CLIENT = 53;
	public static final int STATUS_DISCONNECTED = 54;		//==> STATUS_REGISTER_AND_DISCOVER

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

	public static final int ACTIVITY_MSG_UPDATE_UI = 61;
	
	//---------- Service listener message type
	public static final int SERVICE_MSG_NETWORK_STATUS_NOTI = 11;
	public static final int SERVICE_MSG_DEVICE_INFO = 21;
	public static final int SERVICE_MSG_RESET_DEVICE_INFO = 28;
	public static final int SERVICE_MSG_CONNECTION_INFO = 31;
	public static final int SERVICE_MSG_RESET_CONNECTION_INFO = 38;
	
	public static final int SERVICE_MSG_REMOTE_MESSAGE = 51;
	
	public static final int SERVICE_MSG_MAKE_SERVER_SOCKET = 101;
	public static final int SERVICE_MSG_MAKE_CLIENT_SOCKET = 102;
	public static final int SERVICE_MSG_CLOSE_SOCKET_MANAGER = 103;
	
	public static final int SERVICE_MSG_SEND_STRING_TO_REMOTE = 201;	// Send string to remote
	public static final int SERVICE_MSG_SEND_COMMAND_TO_REMOTE = 202;	// Send string to remote
	
	public static final int SERVICE_MSG_UPDATE_UI = 301;
	
	
	//---------- Delay time
	public static final long NETWORK_P2P_CONNECT_TIMEOUT = 60*1000; 		// 1 minutes
	public static final long NETWORK_DISCOVERING_TIMEOUT = 2*60*1000; 		// 2 minutes
	public static final long NETWORK_WAITING_WIFI_RESET = 10*1000; 		// 10 seconds
	
	
	//---------- Network
	public static final String NETWORK_SERVICE_NAME = "WindWalker_";
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
	
	//---------- Switching		-kwon
	public static final int MAIN_DISPLAY=1971;
	public static final int SUB_DISPLAY=1972;
}
