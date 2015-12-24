package com.dgssm.switchingdroid.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dgssm.switchingdroid.utils.Constants;

public class ScreenCaptureService {
	// Debug
	private final static	String			TAG				= "Screen Capture Service";
	
	private					CaptureThread	task1	= null;
//	private					CaptureThread	task2	= null;
//	private					CaptureThread	task3	= null;
//	private					CaptureThread	task4	= null;
//	private					CaptureThread	task5	= null;
//	private					CaptureThread	task6	= null;
//	private					CaptureThread	task7	= null;
//	private					CaptureThread	task8	= null;
//	private					CaptureThread	task9	= null;
//	private					CaptureThread	task10	= null;
	
	private					Handler serviceHandler = null;
	
	private					byte[]	data = null;
	private					Bitmap bitmap = null;
	private					ByteArrayOutputStream baos = null;
//	private					Context context = null;
	
	public ScreenCaptureService(Context context, Handler serviceHandler) {
//		this.context = context;
		this.serviceHandler = serviceHandler;
		baos = new ByteArrayOutputStream();
		this.task1 = new CaptureThread(0, 9000);
//		this.task2 = new CaptureThread(1, 9001);
//		this.task3 = new CaptureThread(2, 9002);
//		this.task4 = new CaptureThread(3);
//		this.task5 = new CaptureThread(4);
//		this.task6 = new CaptureThread(5);
//		this.task7 = new CaptureThread(6);
//		this.task8 = new CaptureThread(7);
//		this.task9 = new CaptureThread(8);
//		this.task10 = new CaptureThread(9);
	}
	
	public void captureThreadStart() {
		task1.start();
//		task2.start();		
//		task3.start();
//		task4.start();
//		task5.start();
//		task6.start();
//		task7.start();
//		task8.start();
//		task9.start();
//		task10.start();
	}
	
	public void captureThreadResume() {
		task1.resume();
	}
	
	public void captureThreadStop() {
		task1.stop();
//		task2.stop();
//		task3.stop();
//		task4.stop();
//		task5.stop();
//		task6.stop();
//		task7.stop();
//		task8.stop();
//		task9.stop();
//		task10.stop();
	}
	
	public void captureThreadSuspend() {
		task1.suspend();
	}
	
	private void sendToReadScreenCommand(String filePath, int port) {
		Message msg = serviceHandler.obtainMessage(Constants.SERVICE_MSG_READ_SCREEN_IMAGE, port, 0, filePath);
		serviceHandler.sendMessage(msg);
	}
	
	private class CaptureThread implements Runnable {  		
  		private final 	int 	RUNNING 	= 0;
  		private final 	int 	SUSPENDED 	= 1;
  		private final 	int 	STOPPED 	= 2;
  		private 		int 	state 		= RUNNING;
  		
  		private 		Thread 	thread 		= null;
  		
  		private			String	filePath	= null;
  		
  		private			int		port		= 0;
  		private			int		number		= 0;
  		
  		/** Constructor **/
  		public CaptureThread(int number, int port) {
  			this.filePath = Environment.getExternalStorageDirectory() + "/" + number + ".png";
  			this.port = port;
  			thread = new Thread(this);
  		}
  		
  		/** Override Method **/
  		@Override
  		public void run() {  			
  			while (true) {
  				if (checkState()) {
  					Log.d( TAG, "capture thread stop" );
  					
  					thread = null;
  					break;
  				}
  				
  				number = (number + 1) % 5;
  				filePath = Environment.getExternalStorageDirectory() + "/" + number + ".png";
  				captureScreen(filePath);
  				sendToReadScreenCommand(filePath, port);
  			}
  		}
  		
  		/** User Define Method **/
  		private void captureScreen(String filePath) {		
  	 		// capture screen about adb shell commands
  	 		try {
//  	 			Runtime.getRuntime().exec("su -c screencap -p /sdcard/SwitchingDroid/" + fileName);
  	 			Process process = Runtime.getRuntime().exec("su -c screencap -p " + filePath);
  	 			process.waitFor();
  	 			Log.d(TAG, "capture complete!");
  	 			  	 		
//  	 			Uri uri = Uri.parse("file://" + filePath);
//  	 			Log.e(TAG, uri.toString());
//  				Intent serviceIntent = new Intent(context, FileTransferService.class);
//  			    serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
//  			    serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
//  			    serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, "192.168.49.76");
//  		        serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, port);
//  		        context.startService(serviceIntent);
  	 			
  	 			// Test
//  	 			BitmapFactory.Options options = new BitmapFactory.Options();
//  	 			options.inJustDecodeBounds = true;
//  	 			BitmapFactory.decodeFile(filePath, options);
//  	 			int scale = (int) Math.pow(2, (int) Math.round(Math.log(d))
//  	 			bitmap = BitmapFactory.decodeFile(filePath);
//  	 			bitmap.compress(CompressFormat.JPEG, 20, baos);
//  	 			data = baos.toByteArray();
//  	 			baos.close();
//  	 			bitmap = null;
//  	 			bitmap.recycle();
  	 		} catch (InterruptedException ie) {
  	 			ie.printStackTrace();
  	 		} catch (IOException ioe) {
  				ioe.printStackTrace();
  			} 		
  		}
  		
  		private synchronized void setState(int state) {  			
  			this.state = state;
  			
  			if (this.state == RUNNING) {
  				notify();
  			} else {
  				thread.interrupt();
  			}
  		}
  		
  		private synchronized boolean checkState() {
  			while (state == SUSPENDED) {
  				try {
  					wait();
  				} catch ( InterruptedException ie ) {
  					ie.printStackTrace();
  				}
  			}
  			
  			return state == STOPPED;
  		}
  		
  		private void start() {
  			thread.start();
  			setState(RUNNING);
  		}
  		
  		private void resume() {
  			setState(RUNNING);
  		}
  		
  		private void suspend() {
  			setState(SUSPENDED);
  		}
  		
  		private void stop() {  			
  			setState(STOPPED);
  		}
	}
	
	// End of CaptureThread
}

// End ScreenCaptureService