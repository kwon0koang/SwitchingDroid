package com.dgssm.switchingdroid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;

public class LogoActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.logo);
		
		Handler handler = new Handler(){
			public void handleMessage(Message msg){
				super.handleMessage(msg);;
				Intent intent = new Intent(LogoActivity.this, SwitchingDroidClientActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.fade, R.anim.hold);
				finish();
			}
		};
		
		handler.sendEmptyMessageDelayed(0, 2000);
	}
}
