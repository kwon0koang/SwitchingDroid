package com.dgssm.switchingdroid.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class FileTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "com.example.android.wifidirect.SEND_FILE";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
    public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";
//    public static final String EXTRAS_IMAGE_FILE = "file";

    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
    	Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
        	Socket socket = new Socket();
        	
        	String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
//            byte data[] = intent.getExtras().getByteArray(EXTRAS_IMAGE_FILE);
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);
        	
            try {            	
                Log.d("FileTransfer", "Opening client socket - ");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                Log.d("FileTransfer", "Client socket - " + socket.isConnected());
                OutputStream stream = socket.getOutputStream(); 
                ContentResolver cr = context.getContentResolver();
                InputStream is = null;
                try {
                    is = cr.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    Log.d("FileTransfer", e.toString());
                }
                
                copyFile(is, stream);
                
//                FileInputStream fis = null;
//                try {
//                	fis = new FileInputStream(new File(filePath));
//                } catch (FileNotFoundException e) {
//                    Log.d("FileTransfer", e.toString());
//                }
//
//                copyFile(fis, stream);
                
//                try {
//                	ByteArrayInputStream bais = new ByteArrayInputStream(data);
//                	InputStream inputStream = cr.openInputStream(Uri.parse(fileUri));
//                	copyFile(bais, stream);
//                    Log.d("FileTransfer", "Client: Data written");
//                } catch (Exception e) {
//                	e.printStackTrace();
//				}
            } catch (IOException e) {
                Log.e("FileTransfer", e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    private void copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);

            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d("FileTransferAsyncTask", e.toString());
        }
    }
}
