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
	private int length=1024;
	private int imageWidth;
	private int imageHeight;
	private int imageChannels;
	private boolean safelyExit=false;
	
	public ImageThread(OutputStream os, InputStream _is, Handler _h){
		ios=os;
		is=_is;
		h=_h;
	}
	public void stop(){
		bstop=true;
	}
	public boolean isStop(){
		return safelyExit;
	}
	public void allocate(int w, int h, int n){
		imageWidth=w;
		imageHeight=w;
		imageChannels=n;
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
		safelyExit=false;
		try {
			String header="sz:"+imageWidth+","+imageHeight+","+imageChannels;
			ios.write(header.getBytes());
			while(true){
				int rn=is.read(messageBuff);
				String messageStr=new String(messageBuff, 0, rn);
				Log.i("Socket", messageStr);
				if(messageStr.equals("image"))
					break;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Log.i(TAG, "Image Thread running");	
		while(!bstop){
			while(!newFrame){
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			finishFrame=false;
			Log.i("MatInfo", cacheMat.cols()+", "+cacheMat.rows()+", "+cacheMat.channels());
			Utils.matToBitmap(cacheMat, cacheImage);
			buff.rewind();
			cacheImage.copyPixelsToBuffer(buff);
	        byte[] byteArray = buff.array();
	        try {
				ios.write("image".getBytes());
				ios.flush();
				while(true){
					int rn=is.read(messageBuff);
					String messageStr=new String(messageBuff, 0, rn);
					Log.i("Socket", messageStr);
					if(messageStr.equals("image"))
						break;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
	        //send;
        	try {
        		int start=0;
        		int len=length;
        		while(true){
        			ios.write(byteArray, start, len);
        			ios.flush();
        			start+=len;
        			if(start>=byteArray.length)
        				break;
        			if(start+len>=byteArray.length){
        				len=byteArray.length-start;
        			}
        		}
			} catch (IOException e) {
				e.printStackTrace();
			}
        	Log.i("ImageThread", "Send one frame");
        	while(true){
        		try {
					int rn=is.read(messageBuff);
	        		if(messageBuff[0]=='o' && messageBuff[1]=='k'){
	        			String message=new String(messageBuff, 0, rn).substring(2);
	        			Log.i("Socket", message);
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
			safelyExit=true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		/*
		while(true){
    		try {
				int rn=is.read(messageBuff);
				String messageStr=new String(messageBuff, 0, rn);
        		if(messageStr.equals("end")){
        			safelyExit=true;
        			break;
        		}
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}*/
	}
}
