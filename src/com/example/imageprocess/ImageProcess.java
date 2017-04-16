package com.example.imageprocess;

import java.util.ArrayList;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import android.util.Log;

public class ImageProcess {

	public long mNativeObjAddr=0;
	public ImageProcess(){
		System.loadLibrary("ImageProcess");
		Log.i("Library", "Load library successfully");
	}
	public void findRegions(Mat input, Mat output, ArrayList<Rect> regions){
		long inputAddr=input.getNativeObjAddr();
		long outputAddr=output.getNativeObjAddr();
		nativeFindRegions(inputAddr, outputAddr, regions);
	}
	public void match(Mat input, Mat template, Rect roi){
		Mat patch=input.adjustROI(roi.y, roi.y+roi.height, roi.x, roi.x+roi.width);
		long inputAddr=patch.getNativeObjAddr();
		long templateAddr=template.getNativeObjAddr();
		nativeMatch(inputAddr, templateAddr);
	}
	
	public native void nativeFindRegions(long inputAddr, long outputAddr, ArrayList<Rect> regions);
	public native void nativeRoi(long inputAddr, long outputAddr, int x, int y, int w, int h);
	public native void nativeMatch(long inputAddr, long templateAddr);
}
