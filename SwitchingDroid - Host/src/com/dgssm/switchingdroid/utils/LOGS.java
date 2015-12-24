package com.dgssm.switchingdroid.utils;

import android.util.Log;

public class LOGS 
{
	public static boolean mIsEnabled = true;
	
	public static void d(String tag, String msg) 
	{
		if( mIsEnabled ) 
		{
			Log.d(tag, msg);
		}
	}
	
	
}
