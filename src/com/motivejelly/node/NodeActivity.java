package com.motivejelly.node;

import java.util.ArrayList;
import java.util.Collections;

import com.motivejelly.supportlibary.FrameInfo;
import com.motivejelly.supportlibary.MsgType;
import com.gamingbeaststudio.developtoolkit.network.MessageDao;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class NodeActivity extends Activity {

	Activity main;
	GridView framesPanel;
	TextView logcat;
	ImageView wake;
	ImageView sleep;
	ArrayList<FrameInfo> frames = new ArrayList<FrameInfo>();
	NodeController controller;

	@SuppressLint("HandlerLeak")
	public Handler uiHandler = new Handler() {

		@Override
		public void handleMessage(final Message msg) {

			if (msg != null && !(msg.obj instanceof MessageDao)) {
				return;
			}
			switch (msg.what) {
			case MsgType.F2N_CALL_SERVICE:
				callService((MessageDao) msg.obj);
				break;
			case MsgType.F2N_LOW_BATTERY:
				lowBattery((MessageDao) msg.obj);
				break;
			case MsgType.F2N_ONLINE:
				frameOnline((MessageDao) msg.obj);
				break;
			case MsgType.F2N_REQUEST_FILE:
				sendFile((MessageDao) msg.obj);
				break;
			}
		}
	};

	// TODO Runtime
	@Override
	protected void onCreate(final Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		updateApplication();
		initComponent();
		requestUpdatesFromServer();
		startService();
	}

	// TODO Update
	private void updateApplication() {

	}

	// TODO Initialize Component
	private void initComponent() {

		main = this;
		controller = new NodeController(getSharedPreferences("share",
				MODE_PRIVATE),
				(WifiManager) getSystemService(Context.WIFI_SERVICE),
				uiHandler, main);
		setContentView(R.layout.activity_node);
		framesPanel = (GridView) findViewById(R.id.gv_frames);
		logcat = (TextView) findViewById(R.id.tv_logcat);
		sleep = (ImageView) findViewById(R.id.iv_sleep);
		sleep.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				controller.allFramesSleep();
			}
		});
		wake = (ImageView) findViewById(R.id.iv_wake);
		wake.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				controller.allFramesWake();
			}
		});
		framesPanel.setAdapter(new FrameAdapter(this));
		framesPanel.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(final AdapterView<?> parent,
					final View view, final int position, final long id) {

				final FrameInfo fi = frames.get(position);
				if (fi.getStatus() == Constants.CALLING) {
					fi.setStatus(Constants.RUNNING);
					framesPanel.invalidateViews();
					controller.callAnswered(fi.getIp());
				} else {
					final EditText et = new EditText(main);
					et.setHint("Set FrameId");
					new AlertDialog.Builder(main)
							.setTitle(fi.getId())
							.setView(et)
							.setPositiveButton("Confirm",
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												final DialogInterface dialog,
												final int which) {

											if (!(et.getText() == null)
													&& et.getText().length() > 0)
												fi.setId(et.getText()
														.toString().trim());
											controller.setFrameId(fi.getId(),
													fi.getIp());
											Collections.sort(frames);
											framesPanel.invalidateViews();
										}
									}).setNegativeButton("Cancel", null).show();
				}
			}
		});

	}

	// TODO Updates Adslist Server
	private void requestUpdatesFromServer() {

	}

	// TODO Node Services
	private void startService() {

		controller.online();
	}

	private void frameOnline(final MessageDao msg) {

		logcat.setText(logcat.getText() + "Frame:"
				+ ((FrameInfo) msg.getBody()).getIp() + "("
				+ ((FrameInfo) msg.getBody()).getMac() + ") Found\n");
		controller.checkFrameAdsListVersion(msg);
		final FrameInfo fi = (FrameInfo) msg.getBody();
		frames.remove(fi);
		frames.add(fi);
		Collections.sort(frames);
		framesPanel.invalidateViews();
	}

	private void sendFile(final MessageDao msg) {

		logcat.setText(logcat.getText() + "Frame:" + msg.getSendUserIp()
				+ " Request File:" + (String) msg.getBody() + "\n");
		controller.sendFile(msg);
	}

	private void callService(final MessageDao msg) {

		final FrameInfo fi = (FrameInfo) msg.getBody();
		if (frames.indexOf(fi) == -1) {
			fi.setStatus(Constants.CALLING);
			frames.add(fi);
			Collections.sort(frames);
		} else {
			frames.get(frames.indexOf(fi)).setStatus(Constants.CALLING);
		}
		framesPanel.invalidateViews();
		controller.callReceived(fi.getIp());
	}

	private void lowBattery(final MessageDao msg) {

	}

	class FrameAdapter extends BaseAdapter {

		private final LayoutInflater inflater;

		FrameAdapter(final Context context) {

			inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {

			return frames.size();
		}

		@Override
		public Object getItem(final int position) {

			return frames.get(position);
		}

		@Override
		public long getItemId(final int position) {

			return position;
		}

		@SuppressLint("ViewHolder")
		@Override
		public View getView(final int position, View convertView,
				final ViewGroup parent) {

			convertView = inflater.inflate(R.layout._frame_info, parent, false);
			((TextView) convertView.findViewById(R.id.tv_frame_id))
					.setText(frames.get(position).getId());
			((ImageView) convertView.findViewById(R.id.iv_frame))
					.setImageResource(frames.get(position).getStatus() == 0 ? R.drawable.icon_frame_normal
							: R.drawable.icon_frame_calling);
			return convertView;
		}

		@Override
		public boolean areAllItemsEnabled() {

			return true;
		}

		@Override
		public boolean isEnabled(final int position) {

			return true;
		}
	}
}
