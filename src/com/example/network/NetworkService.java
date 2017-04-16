package com.example.network;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import com.example.opencvcamera.MainActivity;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.example.opencvcamera.R;

public class NetworkService extends Service implements Network{
	private Context context;
	private WifiManager mWifiManager;
	private ConnectivityManager manager;
	private Socket clientSocket=null;
	private NetworkBinder binder=new NetworkBinder();
	private boolean connected=false;
	private boolean connecting=false;
	
	private void connect(Handler h, String ip , int port){
		ConnectionThread connectThread=new ConnectionThread(h);
		connectThread.setIpAndPort(ip, port);
		new Thread(connectThread).start();
	}
	
	public void setContext(Context c){
		context=c;
	}
	public Socket getAvailableSocket(){
		return clientSocket;
	}
	public boolean isConnecting(){
		return connecting;
	}
	public boolean isConnected(){
		return connected;
	}
	@SuppressWarnings("deprecation")
	private boolean isWifiConnected(){
		NetworkInfo wifiInfo, mobileInfo, netInfo;
		mobileInfo= manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		netInfo=manager.getActiveNetworkInfo();
		if(netInfo==null)
			return false;
		else if(!wifiInfo.isConnected()){
			return false;
		}
		return true;
	}
	private void sendMessage(Handler h, int w){
		Message msg=h.obtainMessage();
		msg.what=w;
		h.sendMessage(msg);
	}
	@Override
	public void onConnect(final Handler h) {
		// TODO Auto-generated method stub
		if(!isWifiConnected()){
			sendMessage(h, 0);
			return;
		}
		if(clientSocket!=null){
			sendMessage(h, 2);
			return;
		}
		AlertDialog.Builder connectDialog=new AlertDialog.Builder(context);
		LayoutInflater inflater=LayoutInflater.from(context);
		final View v=View.inflate(context, R.layout.connectdialog, null);
		connectDialog.setView(v);
		connectDialog.setTitle("连接到主机");
		connectDialog.setPositiveButton("是", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				EditText editTextIpAddress=(EditText)v.findViewById(R.id.edit_ip_address);
				TextView textPortNumber=(TextView)v.findViewById(R.id.text_port_number);
				String ipAddress=editTextIpAddress.getText().toString();
				int port=Integer.valueOf(textPortNumber.getText().toString());
				connect(h, ipAddress, port);
			}
		});
		connectDialog.setNegativeButton("否", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				// TODO Auto-generated method stub
				sendMessage(h, 0);
			}
				
		});
		final AlertDialog dialog=connectDialog.create();
		dialog.setCancelable(false);
		dialog.show();
	}
	@Override
	public void onDisconnect() {
		// TODO Auto-generated method stub
		if(clientSocket!=null){
			try {
				clientSocket.close();
				clientSocket=null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		connected=false;
	}
	@Override
	public void onCreate(){
		manager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		super.onCreate();
	}
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return binder;
	}
	public class NetworkBinder extends Binder{
		public NetworkService getService(){
			return NetworkService.this;
		}
	}
	class ConnectionThread implements Runnable{
		Handler h;
		String address;
		int port;
		public ConnectionThread(Handler _h){
			h=_h;
		}
		public void setIpAndPort(String ip, int p){
			address=ip;
			port=p;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			int count=10;
			clientSocket=new Socket();
			if(connected){
				NetworkService.this.sendMessage(h, 1);
				return;
			}
			try {
				connecting=true;
				clientSocket.connect(new InetSocketAddress(address, port), 1500);
			} catch (IOException e) {
				e.printStackTrace();
				Log.i("Connection", "Failed to connect");
				sendMessage(h, 0);
				clientSocket=null;
				connecting=false;
				return;
			}
			connecting=false;
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			connected=true;
			sendMessage(h, 1);
		}
		
	}
}
