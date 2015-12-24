package com.dgssm.switchingdroid.services;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SwitchingDroidStartServiceReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent) 
	{
		Intent service = new Intent(context, SwitchingDroidHostService.class );
		context.startService(service);
	}
}
