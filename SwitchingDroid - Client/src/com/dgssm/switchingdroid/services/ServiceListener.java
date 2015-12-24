package com.dgssm.switchingdroid.services;

public interface ServiceListener {
	public void OnReceiveCallback(int msgtype, int arg0, int arg1, String arg2, String arg3, Object arg4);
}
