package com.example.opencvcamera;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ImageThread implements Runnable{
	private static String TAG="ImageThread";
	private OutputStream ios;
	private InputStream is;
	private boolean bstop=false;
	private boolean newFrame=false;
	private boolean finishFrame=true;
	private ByteBuffer buff;
	private Mat cacheMat;
	private Bitmap cacheImage;
	private byte[] messageBuff;
	private Handler h;
	
	public ImageThread(OutputStream os, InputStream _is, Handler _h){
		ios=os;
		is=_is;
		h=_h;
	}
	public void stop(){
		bstop=true;
	}
	public void allocate(int w, int h, int n){
		buff = ByteBuffer.allocate(w*h*n);
		cacheMat=new Mat(h, w, CvType.CV_8UC4);
		cacheImage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		messageBuff=new byte[1024];
	}
	public void sendFrame(Mat mat){
		if(finishFrame){
			cacheMat=mat;
			newFrame=true;
		}
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!bstop){
			Log.i(TAG, "Image Thread running");	
			while(!newFrame){
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			finishFrame=false;
			Log.i("MatInfo", cacheMat.cols()+", "+cacheMat.rows()+", "+cacheMat.channels());
			Utils.matToBitmap(cacheMat, cacheImage);
			cacheImage.copyPixelsToBuffer(buff);
	        byte[] byteArray = buff.array();
	        try {
				ios.write("image".getBytes());
				while(true){
					int rn=is.read(messageBuff);
					messageBuff[rn]='\0';
					Log.i("Socket", new String(messageBuff));
					if(new String(messageBuff).equals("image"))
						break;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	        //send;
        	try {
				ios.write(byteArray, 0, byteArray.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
        	Log.i("ImageThread", "Send one frame");
        	while(true){
        		try {
					int rn=is.read(messageBuff);
					messageBuff[rn]='\0';
	        		if(messageBuff[0]=='o' && messageBuff[1]=='k'){
	        			String message=new String(messageBuff).substring(2);
	        			Message msg=h.obtainMessage();
	        			msg.what=1;
	        			msg.obj=message;
	        			h.sendMessage(msg);
	        			break;
	        		}
				} catch (IOException e) {
					e.printStackTrace();
				}
        		
        	}
        	try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	        newFrame=false;
	        finishFrame=true;
		}
		try {
			ios.write("stop".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(true){
    		try {
				int rn=is.read(messageBuff);
				messageBuff[rn]='\0';
        		if(new String(messageBuff).equals("end")){
        			break;
        		}
			} catch (IOException e) {
				e.printStackTrace();
			}
    		
    	}
	}
}
