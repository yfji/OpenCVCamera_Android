package com.example.network;

import android.os.Handler;

public interface Network {
	public void onConnect(Handler h);
	public void onDisconnect();
}
