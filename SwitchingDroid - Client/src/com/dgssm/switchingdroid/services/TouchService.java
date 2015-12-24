package com.dgssm.switchingdroid.services;


import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.dgssm.switchingdroid.R;
import com.dgssm.switchingdroid.SwitchingDroidClientActivity;
import com.dgssm.switchingdroid.utils.Constants;

public class TouchService {
	
	
	private static final String TAG = "TouchService";
	
	// Thread status
	private boolean mKillSign = false;
	
	// System
	private Context mContext;
	private SwitchingDroidClientService.ServiceHandler mServiceHandler;
	private static ServiceListener mServiceListener;
	
	// View
	public static TextView topView;		
	public static Button btnSwitching;
	public static AnimationDrawable btnSwitchingAnim;

	public static WindowManager wm;
	public static WindowManager.LayoutParams topViewParams;
	public static WindowManager.LayoutParams btnSwitchingParams;
	
	private StringBuilder builder = new StringBuilder();
	private int pointerId;
	private int[] what = new int[10];
	private float[] x = new float[10];
	private float[] y = new float[10];
	private boolean[] touched = new boolean[10];

	private int MAX_DISPLAY_X = 1080;
	private int MAX_DISPLAY_Y = 1558;
	
	private float START_X, START_Y;	
	private int PREV_X, PREV_Y;	
	private int MAX_X = -1, MAX_Y = -1;
	private int prevTouch = 0;
	
	private AudioTreckService audioTreckService = null;
	
	/*****************************************************
	*		Initializing methods
	******************************************************/
	
	public TouchService(Context c, SwitchingDroidClientService.ServiceHandler h, ServiceListener l) {
		mContext = c;
		mServiceHandler = h;
		mServiceListener = l;
		
		audioTreckService = audioTreckService.getInstance();
		
		initializeThread();
	}

	
	
	

	
	
	// Call service callback////////////////////////////////////////////////////////////
	public static void sendMessageToRemote(String str) {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_SEND_STRING_TO_REMOTE, 0, 0, str, null, null);
	}
	
	public static void sendCommandToRemote(Command cmd) {
		mServiceListener.OnReceiveCallback(Constants.SERVICE_MSG_SEND_COMMAND_TO_REMOTE, 0, 0, null, null, cmd);
	}
	
	
	
	
	
	
	
	
	
	
	private void initializeThread() {
		new Handler().post(new Runnable() {
			public void run() {
				topView = new TextView(mContext);
				topView.setBackgroundColor(Color.argb(0, 00, 100, 00));
				topView.setOnTouchListener(mTouchListener);
				topView.setWidth(MAX_DISPLAY_X);
				topView.setHeight(1558);
				topViewParams = new WindowManager.LayoutParams(
					WindowManager.LayoutParams.WRAP_CONTENT, 	WindowManager.LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.TYPE_PHONE,		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,				
					PixelFormat.TRANSLUCENT);													
				topViewParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
				
				btnSwitching = new Button(mContext);
				btnSwitching.setBackgroundResource(R.drawable.btn_switching_loading_anim_list);
				btnSwitchingAnim = (AnimationDrawable) btnSwitching.getBackground();
				btnSwitchingAnim.start();
				btnSwitching.setOnTouchListener(mTouchListener);
				btnSwitchingParams = new WindowManager.LayoutParams(
					WindowManager.LayoutParams.WRAP_CONTENT, 	WindowManager.LayoutParams.WRAP_CONTENT,
					WindowManager.LayoutParams.TYPE_PHONE,		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,				
					PixelFormat.TRANSLUCENT);													
				btnSwitchingParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
				btnSwitchingParams.width=122;
				btnSwitchingParams.height=122;
				
				wm = (WindowManager) mContext.getSystemService(mContext.WINDOW_SERVICE);
				wm.addView(topView, topViewParams);
				wm.addView(btnSwitching, btnSwitchingParams);
				
				topView.setVisibility(View.INVISIBLE);
				//btnSwitching.setVisibility(View.INVISIBLE);
				
				//================================================

			}
		});
		
		
	}
	
	
	
	
	
	
	// Visible || Invisible ///////////////////////////////////////////////////////////////////////////////////////////
	public static void topViewVisible()				{	topView.setVisibility(View.VISIBLE);			}
	public static void topViewInvisible()			{	topView.setVisibility(View.INVISIBLE);			}
	//public static void btnSwitchingVisible()		{	btnSwitching.setVisibility(View.VISIBLE);		}
	//public static void btnSwitchingInvisible()	{	btnSwitching.setVisibility(View.INVISIBLE);	}
	public static void btnSwitchingLoading()		{	
		btnSwitching.setBackgroundResource(R.drawable.btn_switching_loading_anim_list);
		btnSwitchingAnim = (AnimationDrawable) btnSwitching.getBackground();
		btnSwitchingAnim.start();
	}
	public static void btnSwitchingLoadingComplete()	{	
		btnSwitching.setBackgroundResource(R.drawable.btn_switching_anim_list);
		btnSwitchingAnim = (AnimationDrawable) btnSwitching.getBackground();
		btnSwitchingAnim.start();
	}
	
	
	
	
	
	
	
	
	
	
	//mTouchListener ////////////////////////////////////////////////////////////////////////////////////////////////////
	View.OnTouchListener mTouchListener = new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			//v == topView//////////////////////
			if(v == topView){
				int action = event.getAction() & MotionEvent.ACTION_MASK;
		        int pointerIndex = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK);
		        pointerIndex = pointerIndex >> MotionEvent.ACTION_POINTER_ID_SHIFT;
		        int pointerId = event.getPointerId(pointerIndex);
		        
		        
		        StringBuilder sb;

				String str;
				byte[] strBytes;
				Command cmd;
				
		        
		        switch(action)
		        {
		        case MotionEvent.ACTION_DOWN:				//첫占쏙옙째 占쏙옙치
		        case MotionEvent.ACTION_POINTER_DOWN:	//占싸뱄옙째 占쏙옙치
		        	what[pointerId] = action;
		        	touched[pointerId] = true;
		        	
		        	//x醫뚰몴 �뀑�똿
		        	if((int)event.getX(pointerIndex) >= MAX_DISPLAY_X)
		        		x[pointerId] = MAX_DISPLAY_X;
		        	else if((int)event.getX(pointerIndex) <= 0)
		        		x[pointerId] = 0;
		        	else
		        		x[pointerId] = (int)event.getX(pointerIndex);
		        	//y醫뚰몴 �뀑�똿
		        	if((int)event.getY(pointerIndex) >= MAX_DISPLAY_Y)
		        		y[pointerId] = MAX_DISPLAY_Y;
		        	else if((int)event.getY(pointerIndex) <= 0)
		        		y[pointerId] = 0;
		        	else
		        		y[pointerId] = (int)event.getY(pointerIndex);

		        	sb = new StringBuilder();
		        	sb.setLength(0);
		        	
		        	for(int i=0; i<2; i++)
			        {
			        	sb.append(what[i] + "#" + x[i] + "#" + y[i] + "#");
			        }
		        	
					str = sb.toString();
					strBytes = str.getBytes();
					cmd = new Command(Constants.COMMAND_MESSAGE_STRING, strBytes.length, strBytes);
					sendCommandToRemote(cmd);
					
		            break;
		        case MotionEvent.ACTION_UP:						//첫占쏙옙째 占쏙옙치 占쏙옙占쏙옙占쏙옙 占쏙옙占�
		        case MotionEvent.ACTION_POINTER_UP:		//占싸뱄옙째 占쏙옙치 占쏙옙占쏙옙占쏙옙 占쏙옙占�
		        case MotionEvent.ACTION_CANCEL:
		        	what[pointerId] = action;
		        	touched[pointerId] = false;
		        	
		        	//x醫뚰몴 �뀑�똿
		        	if((int)event.getX(pointerIndex) >= MAX_DISPLAY_X)
		        		x[pointerId] = MAX_DISPLAY_X;
		        	else if((int)event.getX(pointerIndex) <= 0)
		        		x[pointerId] = 0;
		        	else
		        		x[pointerId] = (int)event.getX(pointerIndex);
		        	//y醫뚰몴 �뀑�똿
		        	if((int)event.getY(pointerIndex) >= MAX_DISPLAY_Y)
		        		y[pointerId] = MAX_DISPLAY_Y;
		        	else if((int)event.getY(pointerIndex) <= 0)
		        		y[pointerId] = 0;
		        	else
		        		y[pointerId] = (int)event.getY(pointerIndex);
		        		
		        	
		        	sb = new StringBuilder();
		        	sb.setLength(0);
		        	
		        	for(int i=0; i<2; i++)
			        {
			        	sb.append(what[i] + "#" + x[i] + "#" + y[i] + "#");
			        }
		        	
					str = sb.toString();
					strBytes = str.getBytes();
					cmd = new Command(Constants.COMMAND_MESSAGE_STRING, strBytes.length, strBytes);
					sendCommandToRemote(cmd);
					
		            break;
		        case MotionEvent.ACTION_MOVE:
		            int pointerCount = event.getPointerCount();
		            for(int i=0; i<pointerCount; i++)
		            {
		            	pointerIndex = i;
		            	pointerId = event.getPointerId(pointerIndex);
		            	what[pointerId] = action;
		            	
			        	//x醫뚰몴 �뀑�똿
			        	if((int)event.getX(pointerIndex) >= MAX_DISPLAY_X)
			        		x[pointerId] = MAX_DISPLAY_X;
			        	else if((int)event.getX(pointerIndex) <= 0)
			        		x[pointerId] = 0;
			        	else
			        		x[pointerId] = (int)event.getX(pointerIndex);
			        	//y醫뚰몴 �뀑�똿
			        	if((int)event.getY(pointerIndex) >= MAX_DISPLAY_Y)
			        		y[pointerId] = MAX_DISPLAY_Y;
			        	else if((int)event.getY(pointerIndex) <= 0)
			        		y[pointerId] = 0;
			        	else
			        		y[pointerId] = (int)event.getY(pointerIndex);

			        	sb = new StringBuilder();
			        	
			        	for(int j=0; j<2; j++)
				        {
				        	sb.append(what[j] + "#" + x[j] + "#" + y[j] + "#");
				        }
			        	
						str = sb.toString();
						strBytes = str.getBytes();
						cmd = new Command(Constants.COMMAND_MESSAGE_STRING, strBytes.length, strBytes);
						sendCommandToRemote(cmd);
		            }
		            
		            break;
		        }
			}
			//v == topView end//////////////////////
			
			//v == btnSwitching//////////////////////
			else if(v == btnSwitching){
				switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					prevTouch = Constants.TOUCH_DOWN; 
					
					if(MAX_X == -1)	setMaxPosition();
					START_X = event.getRawX();	
					START_Y = event.getRawY();
					PREV_X = btnSwitchingParams.x;
					PREV_Y = btnSwitchingParams.y;
					
					break;
					
				case MotionEvent.ACTION_MOVE:
					prevTouch = Constants.TOUCH_MOVE;
					
					int x = (int)(event.getRawX() - START_X);
					int y = (int)(event.getRawY() - START_Y);
					
					btnSwitchingParams.x = PREV_X - x;
					btnSwitchingParams.y = PREV_Y - y;
					
					optimizePosition();
					wm.updateViewLayout(btnSwitching, btnSwitchingParams);
					
					break;
					
				case MotionEvent.ACTION_UP:
					//if click
					if(prevTouch == MotionEvent.ACTION_DOWN){
						switch(SwitchingDroidClientActivity.displayFlag){
						case Constants.SUB_DISPLAY:
							//HOME
							Intent homeIntent = new Intent(Intent.ACTION_MAIN);
							homeIntent.addCategory(Intent.CATEGORY_HOME);
							homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							mContext.startActivity(homeIntent);
							
							audioTreckService.InputListener(false);
							
							
							
							
							
							
							
							
							break;
						case Constants.MAIN_DISPLAY:
							Intent appIntent = new Intent(mContext, SwitchingDroidClientActivity.class);
							appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							appIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
							mContext.startActivity(appIntent);
							
							audioTreckService.InputListener(true);
							
							break;
						}
					}
					
					//if not click
					else{
						btnSnapEffect();
					}
					
					break;
				}
			}
			//v == btnSwitching end//////////////////////
			
	        return true;
		}
	};//mTouchListener end##########################################################

	private void setMaxPosition() {
		DisplayMetrics matrix = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(matrix);
		
		MAX_X = matrix.widthPixels - btnSwitching.getWidth();
		MAX_Y = matrix.heightPixels - btnSwitching.getHeight();
	}

	private void optimizePosition() {
		if(btnSwitchingParams.x > MAX_X) btnSwitchingParams.x = MAX_X;
		if(btnSwitchingParams.y > MAX_Y) btnSwitchingParams.y = MAX_Y;
		if(btnSwitchingParams.x < 0) btnSwitchingParams.x = 0;
		if(btnSwitchingParams.y < 0) btnSwitchingParams.y = 0;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//btnSnap 관련==================================================================================
	private void btnSnapEffect(){
		//go left
		if(btnSwitchingParams.x <= MAX_X / 2){
			btnSnapHandler.post(btnSnapLeftRunnable);
		}
		//go right
		else if(btnSwitchingParams.x > MAX_X / 2){
			btnSnapHandler.post(btnSnapRightRunnable);
		}
	}
	
	private Handler btnSnapHandler = new Handler();
	private Runnable btnSnapLeftRunnable = new Runnable() {
		@Override
		public void run() {
			btnSwitchingParams.x -= 40;
			wm.updateViewLayout(btnSwitching, btnSwitchingParams);
			if(btnSwitchingParams.x > 0) {
				btnSnapHandler.postDelayed(btnSnapLeftRunnable, 5);
			}
			else if(btnSwitchingParams.x < 0) {
				btnSwitchingParams.x = 0;
			}
		}
	};
	private Runnable btnSnapRightRunnable = new Runnable() {
		@Override
		public void run() {
			btnSwitchingParams.x += 40;
			wm.updateViewLayout(btnSwitching, btnSwitchingParams);
			if(btnSwitchingParams.x < MAX_X) {
				btnSnapHandler.postDelayed(btnSnapRightRunnable, 5);
			}
			else if(btnSwitchingParams.x > MAX_X) {
				btnSwitchingParams.x = MAX_X;
			}
		}
	};
	//btnSnap 관련 end==================================================================================

	
}







