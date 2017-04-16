package com.example.opencvcamera;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.TextView;

import com.example.imageprocess.ImageProcess;
import com.example.network.NetworkService;
import com.example.network.NetworkService.NetworkBinder;

public class CameraActivity extends Activity implements CvCameraViewListener2{
	
	private CameraBridgeViewBase   mOpenCvCameraView;
	private BaseLoaderCallback  mLoaderCallback;
	private static String TAG="OpenCV Camera";
	private Mat rgbMat;
	private ImageProcess processor;
	
	private ImageThread imageThread;
	private NetworkService mNetworkService;
	private ServiceConnection conn;
	private Socket clientSocket=null;
	private boolean connected=false;
	private boolean bound=false;
	private boolean infoUpdated=false;
	private OutputStream imageOutputStream;
	private InputStream infoInputStream;
	private Handler h;
	private Bitmap mCacheBitmap;
	private TextView mTextMessage;
	private String message="";
	private int previewWidth;
	private int previewHeight;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.camera_view);
		mTextMessage=(TextView)findViewById(R.id.text_message);
		
		mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.fd_activity_surface_view);
		//mOpenCvCameraView.setVisibility(SurfaceView.GONE); 
		mOpenCvCameraView.setCvCameraViewListener(this);
        
		mLoaderCallback = new BaseLoaderCallback(this) {
			@Override
			public void onManagerConnected(int status) {
			    switch (status) {
			        case LoaderCallbackInterface.SUCCESS:
			        {
			            Log.i(TAG, "OpenCV loaded successfully");			        
			            System.loadLibrary("opencv_java");	
			            processor=new ImageProcess();
			            mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE); 
			            mOpenCvCameraView.enableView();
			        } 
			        break;
			        default:
			        {
			        	Log.i(TAG, "Failed to load OpenCV");
			            super.onManagerConnected(status);
			        }
			        break;
			    }
			}
		};	
		conn=new ServiceConnection(){

			@Override
			public void onServiceConnected(ComponentName arg0,
					IBinder arg1) {
				// TODO Auto-generated method stub
				Log.i("NetworkService","CameraActivity Service bound");
				NetworkBinder binder=(NetworkBinder)arg1;
				mNetworkService=binder.getService();
				clientSocket=mNetworkService.getAvailableSocket();
				if(clientSocket!=null){
					sendMessage(h, 0);
					bound=true;
				}
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				// TODO Auto-generated method stub
				//mService=null;
				bound=false;
			}		
		};	
		h=new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what==0){
					Log.i(TAG, "Connection built: "+clientSocket.toString());
					try {
						imageOutputStream=clientSocket.getOutputStream();
						infoInputStream=clientSocket.getInputStream();
						connected=true;
						imageThread=new ImageThread(imageOutputStream, infoInputStream, h);
						imageThread.allocate(previewWidth, previewHeight, 4);
						new Thread(imageThread).start();
					} catch (IOException e) {				
						e.printStackTrace();
						connected=false;
					}
				}
				else if(msg.what==1){
					message=(String)msg.obj;
					mTextMessage.setText(message);
				}
			}
		};
		Intent intent=getIntent();
		connected=intent.getBooleanExtra("connected", false);
		if(connected){
			Log.i(TAG, "Use online mode");
			Intent i=new Intent(CameraActivity.this, NetworkService.class);
			bindService(i, conn, Context.BIND_AUTO_CREATE);	
		}
		else{
			Log.i(TAG, "Use native mode");
		}
	}
	private void sendMessage(Handler h, int w){
		Message msg=h.obtainMessage();
		msg.what=w;
		h.sendMessage(msg);
	}
	@Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
        if(connected)
        	imageThread.stop();
        if(bound)
        	unbindService(conn);
    }
    @Override
    public void onCameraViewStarted(int width, int height) {
    	rgbMat = new Mat(height, width, CvType.CV_8UC4);
    	previewWidth=width;
    	previewHeight=height;   	
    }
    @Override
    public void onCameraViewStopped() {
    	rgbMat.release();
    }
    @Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		// TODO Auto-generated method stub
    	rgbMat=inputFrame.rgba();  
    	//Mat contour=new Mat();
    	//processor.findRegions(rgbMat, contour, null);
    	if(clientSocket!=null && clientSocket.isConnected()){
    		imageThread.sendFrame(rgbMat);
    	}
    	//mTextMessage.setText(message);
		return rgbMat;
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
