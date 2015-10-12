package com.motivejelly.node;

import java.util.List;

import com.gamingbeaststudio.developtoolkit.network.MessageDao;
import com.gamingbeaststudio.developtoolkit.network.TcpTools;
import com.gamingbeaststudio.developtoolkit.network.UdpTools;
import com.gamingbeaststudio.developtoolkit.tools.Tools;
import com.motivejelly.supportlibary.FrameInfo;
import com.motivejelly.supportlibary.MsgType;
import com.motivejelly.supportlibary.Ports;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

public class NodeController {

	Activity main;
	String nodeIp;
	String adsListVersion;
	List<String> missingFiles;

	SharedPreferences sp;
	Editor spE;
	WifiManager wm;
	Handler uiHandler;
	Handler fileHandler;
	HandlerThread ht;

	public NodeController(final SharedPreferences sp, final WifiManager wm,
			final Handler uiHandler, final Activity main) {

		this.uiHandler = uiHandler;
		this.sp = sp;
		this.wm = wm;
		this.main = main;
		spE = sp.edit();
		initController();
		activateReceiver();
	}

	private void initController() {

		isFirstRun();
		nodeIp = Tools.intToIp(wm.getConnectionInfo().getIpAddress());
		adsListVersion = sp.getString("AdsListVersion",
				Constants.DEFAULT_ADSLIST_VERSION);
	}

	private void isFirstRun() {

		if (sp.getBoolean("isFirstRun", true)) {
			initResources();
			spE.putBoolean("isFirstRun", false);
			spE.commit();
		}
	}

	private void initResources() {

		int t = Constants.TEST_ADS.length;
		for (int i = 0; i < t; i++) {
			String filePath = Constants.PACKAGE_NAME + "ads/";
			Tools.writeFileFromAssets(Constants.TEST_ADS[i], filePath, main);
		}
		t = Constants.TEST_QR.length;
		for (int i = 0; i < t; i++) {
			String filePath = Constants.PACKAGE_NAME + "qr/";
			;
			Tools.writeFileFromAssets(Constants.TEST_QR[i], filePath, main);
		}
		String filePath = Constants.PACKAGE_NAME;
		Tools.writeFileFromAssets("ads_list_"
				+ Constants.DEFAULT_ADSLIST_VERSION + ".json", filePath, main);
	}

	private void activateReceiver() {

		final Thread thread = new Thread() {
			public void run() {
				while (true) {
					try {
						final MessageDao msgD = UdpTools
								.receive(Ports.NODE_UDP_RECEIVE);
						final Message msg = new Message();
						msg.what = Integer.parseInt(msgD.getMsgType());
						msg.obj = msgD;
						uiHandler.sendMessage(msg);
					} catch (final Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		thread.setDaemon(true);
		thread.start();
	}

	// TODO Services
	public void online() {

		byte[] data = Tools.toByteArray(new MessageDao(nodeIp, String
				.valueOf(MsgType.N2F_ONLINE), null));
		UdpTools.send(Ports.NODE_UDP_SEND, data, data.length,
				Constants.BROADCAST, Ports.FRAME_UDP_RECEIVE);
	}

	public void checkFrameAdsListVersion(MessageDao msg) {

		FrameInfo fi = (FrameInfo) msg.getBody();
		byte[] data = Tools.toByteArray(new MessageDao(nodeIp, String
				.valueOf(MsgType.N2F_UPDATE_ADSLIST), Integer
				.parseInt(adsListVersion) > Integer.parseInt(fi
				.getAdsListVersion()) ? adsListVersion : null));
		UdpTools.send(Ports.NODE_UDP_SEND, data, data.length,
				msg.getSendUserIp(), Ports.FRAME_UDP_RECEIVE);
	}

	public void sendFile(final MessageDao msg) {

		Thread fileThread = new Thread() {

			public void run() {

				try {
					String path = Constants.PACKAGE_NAME;
					if (!((String) msg.getBody()).endsWith(".json")) {
						if (((String) msg.getBody()).startsWith("ad")) {
							path = path + "ads/";
						} else {
							path = path + "qr/";
						}
					}
					TcpTools.openClient(path, (String) msg.getBody(),
							msg.getSendUserIp(), Ports.FRAME_TCP_RECEIVE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		fileThread.setDaemon(true);
		fileThread.start();
	}

	public void setFrameId(String id, String ip) {

		byte[] data = Tools.toByteArray(new MessageDao(nodeIp, String
				.valueOf(MsgType.N2F_SET_NAME), id));
		UdpTools.send(Ports.NODE_UDP_SEND, data, data.length, ip,
				Ports.FRAME_UDP_RECEIVE);
	}

	public void allFramesSleep() {

		byte[] data = Tools.toByteArray(new MessageDao(nodeIp, String
				.valueOf(MsgType.N2F_SLEEP), null));
		UdpTools.send(Ports.NODE_UDP_SEND, data, data.length,
				Constants.BROADCAST, Ports.FRAME_UDP_RECEIVE);
	}

	public void allFramesWake() {

		byte[] data = Tools.toByteArray(new MessageDao(nodeIp, String
				.valueOf(MsgType.N2F_WAKE), null));
		UdpTools.send(Ports.NODE_UDP_SEND, data, data.length,
				Constants.BROADCAST, Ports.FRAME_UDP_RECEIVE);
	}

	public void callReceived(String ip) {

		byte[] data = Tools.toByteArray(new MessageDao(nodeIp, String
				.valueOf(MsgType.N2F_SERVICE_CALLED), null));
		UdpTools.send(Ports.NODE_UDP_SEND, data, data.length, ip,
				Ports.FRAME_UDP_RECEIVE);
	}

	public void callAnswered(String ip) {

		byte[] data = Tools.toByteArray(new MessageDao(nodeIp, String
				.valueOf(MsgType.N2F_SERVICE_ANSWER), null));
		UdpTools.send(Ports.NODE_UDP_SEND, data, data.length, ip,
				Ports.FRAME_UDP_RECEIVE);
	}
}