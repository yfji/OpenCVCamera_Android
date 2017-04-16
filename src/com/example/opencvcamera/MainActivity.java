package com.example.opencvcamera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.example.network.*;
import com.example.network.NetworkService.NetworkBinder;

public class MainActivity extends Activity {

	private Button buttonStart;
	private Button buttonConnect;
	private Button buttonDisconnect;
	private Network mNetworkService=null;
	private ServiceConnection conn;
	private Handler h;
	private boolean bound=false;
	private boolean destroyed=true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		buttonStart=(Button)findViewById(R.id.btn_start);
		buttonConnect=(Button)findViewById(R.id.btn_connect);
		buttonDisconnect=(Button)findViewById(R.id.btn_disconnect);
		buttonStart.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				Intent intent=new Intent();
				intent.putExtra("connected", false);
				intent.setClass(MainActivity.this, CameraActivity.class);
				startActivity(intent);
			}
			
		});
		
		buttonConnect.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent i=new Intent(MainActivity.this, NetworkService.class);
				if(destroyed)
					startService(i);
				if(!bound)	
					bindService(i, conn, Context.BIND_AUTO_CREATE);	
				else{
					((NetworkService)mNetworkService).setContext(MainActivity.this);
					mNetworkService.onConnect(h);
				}
				destroyed=false;
			}
			
		});
		
		buttonDisconnect.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(mNetworkService==null)
					return;
				if(((NetworkService)mNetworkService).isConnecting()){
					Toast.makeText(MainActivity.this, "请等待连接终止", Toast.LENGTH_SHORT).show();
					return;
				}
				Intent i=new Intent(MainActivity.this, NetworkService.class);
				mNetworkService.onDisconnect();
				if(bound){
					Log.i("NetworkService", "unbind service");
					unbindService(conn);
					bound=false;
				}
				if(!destroyed){
					Log.i("NetworkService", "kill service");
					stopService(i);
					mNetworkService=null;
				}
				Toast.makeText(MainActivity.this, "连接已断开", Toast.LENGTH_SHORT).show();
				destroyed=true;
			}
			
		});
		h=new Handler(){
			@Override
			public void handleMessage(Message msg){
				if(msg.what==0){
					AlertDialog.Builder confirmDialog=new AlertDialog.Builder(MainActivity.this);
					confirmDialog.setTitle("提示");
					confirmDialog.setMessage("主机连接失败，请检查网络连接");
					confirmDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							unbindService(conn);
							bound=false;
						}
					});
					final AlertDialog dialog=confirmDialog.create();
					dialog.setCancelable(false);
					dialog.show();
					
				}else if(msg.what==1){
					AlertDialog.Builder confirmDialog=new AlertDialog.Builder(MainActivity.this);
					confirmDialog.setMessage("主机连接成功，是否打开摄像头？");
					confirmDialog.setPositiveButton("是", new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							unbindService(conn);
							bound=false;
							Intent intent=new Intent();
							intent.putExtra("connected", true);
							intent.setClass(MainActivity.this, CameraActivity.class);
							startActivity(intent);	
						}
					});
					confirmDialog.setNegativeButton("否", new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							// TODO Auto-generated method stub
							unbindService(conn);
							bound=false;
						}
					});
					final AlertDialog dialog=confirmDialog.create();
					dialog.setCancelable(false);
					dialog.show();
				}else if(msg.what==2){
					Intent intent=new Intent();
					intent.putExtra("connected", true);
					intent.setClass(MainActivity.this, CameraActivity.class);
					startActivity(intent);
					unbindService(conn);
					bound=false;
				}
			}
		};
		
		conn=new ServiceConnection(){

			@Override
			public void onServiceConnected(ComponentName arg0,
					IBinder arg1) {
				// TODO Auto-generated method stub
				NetworkBinder binder=(NetworkBinder)arg1;
				mNetworkService=binder.getService();	
				((NetworkService)mNetworkService).setContext(MainActivity.this);
				Log.i("NetworkService", "Service bound");
				mNetworkService.onConnect(h);
				bound=true;
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				// TODO Auto-generated method stub
				//mNetworkService.onDisconnect();
				Log.i("NetworkService", "Service unbound");
				bound=false;
			}		
		};	
	}

	@Override
    protected void onDestroy(){
        super.onDestroy();
        if(bound)	
        	unbindService(conn);
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
