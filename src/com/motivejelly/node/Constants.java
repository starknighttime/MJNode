package com.motivejelly.node;

import android.os.Environment;

public class Constants {
	public static final String PACKAGE_NAME = Environment
			.getExternalStorageDirectory().getAbsolutePath()
			+ "/Android/data/com.motivejelly.nodedemo/";
	public static final String BROADCAST = "255.255.255.255";
	public static final String DEFAULT_ADSLIST_VERSION = "20151003";
	public static final String[] TEST_ADS = { "ad10032015000002.jpg",
			"ad10032015000001.mp4" };
	public static final String[] TEST_QR = { "qr10032015000002.jpg",
			"qr10032015000001.jpg" };
	public static final int RUNNING = 0;
	public static final int CALLING = 1;
}