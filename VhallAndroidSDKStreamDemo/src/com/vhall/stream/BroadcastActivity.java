package com.vhall.stream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.vhall.netbase.constants.ZReqEngine;
import com.vhall.netbase.constants.VhallMediaController;
import com.vinny.vinnylive.AudioRecordThread;
import com.vinny.vinnylive.CameraNewView;
import com.vinny.vinnylive.CameraNewView.CameraCallback;
import com.vinny.vinnylive.ConnectionChangeReceiver;
import com.vinny.vinnylive.LiveObs;
import com.vinny.vinnylive.LiveObs.LiveCallback;
import com.vinny.vinnylive.LiveParam;
/**
 * 发直播界面
 * 
 * @author huanan
 *
 */
@SuppressWarnings("deprecation")
public class BroadcastActivity extends Activity {

	private static final String TAG = "BroadcastActivity";
	private boolean isPublishing, isAudioing;
	private ProgressDialog mProcessDialog;
	private Button mPublishBtn, mFlashBtn, mAudioBtn;
	private TextView tv_upload_speed;
	private AudioRecordThread mAudioRecordThread;
	// private GestureDetector mGestureDetector;
	private ConnectionChangeReceiver mConnectionChangeReceiver;
	private CameraNewView mCameraView;
	private VhallMediaController controller;

	LiveParam param = null;
	String roomid = null;
	String access_token = null;
	String stream_token = null;
	String url = null;
	
	ZReqEngine.Attend attend;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		param = (LiveParam) getIntent().getSerializableExtra("param");
		// param.live_publish_type = 2;//0推流，1推流回调，2只回调不推流
		roomid = getIntent().getStringExtra("roomid");
		param.setStream_name(roomid);
		Log.v(TAG, "" + param.getParamStr());
		access_token = getIntent().getStringExtra("token");
		if (param.orientation == LiveParam.Screen_orientation_portrait){
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}else{
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);			
		}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//		LightnessControl.setLightnessMax(BroadcastActivity.this);
		setContentView(R.layout.activity_broadcast);
		 getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
	                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mCameraView = (CameraNewView) this.findViewById(R.id.cameraview);
		mCameraView.init(param, this, new RelativeLayout.LayoutParams(0, 0));
		mCameraView.setmCameraCallback(new CameraCallback() {

			@Override
			public void onFirstFrame(String path) {
				Log.e(TAG, "" + path);
			}
		});
		
		/** 是否正在直播*/
		isPublishing = false;
		isAudioing = true;
		mAudioRecordThread = null;
		AudioRecordThread.mIsAudioRecording = true;
		mConnectionChangeReceiver = null;
		controller = new VhallMediaController(BroadcastActivity.this);
		controller.newInstance(mLiveCallBack ,true);
		
		mProcessDialog = new ProgressDialog(BroadcastActivity.this);
		mProcessDialog.setCancelable(true);
		mProcessDialog.setCanceledOnTouchOutside(false);

		mAudioBtn = (Button) findViewById(R.id.audioBtn);
		mFlashBtn = (Button) findViewById(R.id.flashBtn);
		mPublishBtn = (Button) findViewById(R.id.publish_btn);
		tv_upload_speed = (TextView) this.findViewById(R.id.tv_upload_speed);
		
		/**
		 * 点击直播
		 */
		mPublishBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isPublishing) {
					stopBroadcast();
				} else {
					/* Check 3G or WiFi */
					int netState = ConnectionChangeReceiver.ConnectionDetect(BroadcastActivity.this);
					switch (netState) {
					case ConnectionChangeReceiver.NET_ERROR:
						break;
					case ConnectionChangeReceiver.NET_UNKNOWN: {
						param.crf = LiveParam.CRF_WIFI;
						startBrocast();
					}
						break;
					case ConnectionChangeReceiver.NET_2G3G: {
						param.crf = LiveParam.CRF_2G3G;
						startBrocast();
					}
						break;
					case ConnectionChangeReceiver.NET_WIFI: {
						param.crf = LiveParam.CRF_WIFI;
						startBrocast();
					}
						break;
					default:
						break;
					}
				}
			}
		});
	}

	/**
	 * 发起直播
	 */
	private void startPublish() {
		if (param == null)
			return;
		boolean isSuccesss = controller.setParam(param.getParamStr());
		if (!isSuccesss){
			showAlert("直播参数错误");
			finish();
		}
		if (TextUtils.isEmpty(url) || TextUtils.isEmpty(stream_token) || TextUtils.isEmpty(roomid)) {
			showAlert("参数为空");
			return;
		}
		String publishUrl = url + "?token=" + stream_token + "/" + roomid;
		//String urlStr = url+ "?token=" + "/" + "790edd292fdf941ffcdba412f0a88304";
		//Log.v(TAG, "publishUrl == " + publishUrl);
		/** 连接推流地址*/
		boolean isConnect = controller.star(publishUrl);
		Log.v(TAG , "Url == " + publishUrl);
		if(isConnect){
			mProcessDialog.show();
			if (attend != null){
				attend.attend();
			}
		}else{
			Toast.makeText(this, "开始直播失败", Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	/**
	 * 直播过程中的回调
	 */
	private LiveCallback mLiveCallBack = new LiveCallback() {

		@Override
		public void notifyVideoData(byte[] data) {
		}

		@Override
		public int notifyAudioData(byte[] data, int size) {
			return 0;
		}

		@Override
		public void notifyEvent(int resultCode, String content) {
			if (handler != null) {
				try {
					Message message = new Message();
					message.what = resultCode;
					message.obj = content;
					handler.sendMessage(message);
				} catch (Exception e) {

				}
			}
		}

		@Override
		public void onH264Video(byte[] data, int size, int type) {
			//Log.e("h264data", "长度--------------->" + data.length + "类型--------------->" + type);
		}
	};
	
	/**
	 * 直播回调处理
	 */
	@SuppressLint("HandlerLeak")
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case LiveObs.OK_PublishConnect: {
				mProcessDialog.dismiss();
				isPublishing = true;
				mPublishBtn.setText("停止直播");
				mCameraView.startPublish();
				startAudioCapture();
			}
				break;
			case LiveObs.ERROR_PublishConnect: {
				mProcessDialog.show();
				stopPublish();
				int netState = ConnectionChangeReceiver.ConnectionDetect(BroadcastActivity.this);
				switch (netState) {
				case ConnectionChangeReceiver.NET_ERROR:
					showAlert("没有可以使用的网络");
					break;
				default:
					showAlert("服务器连接失败");
					break;
				}
			}
				break;
			case LiveObs.ERROR_Send: {
				mProcessDialog.show();
				stopPublish();
				int netState = ConnectionChangeReceiver.ConnectionDetect(BroadcastActivity.this);
				switch (netState) {
				case ConnectionChangeReceiver.NET_ERROR:
					showAlert("没有可以使用的网络");
					break;
				default:
					showAlert("网断了，请重试！");
					break;
				}
			}
				break;
			case LiveObs.INFO_Speed_Upload: {
				String content = (String) msg.obj;
				tv_upload_speed.setText(content+"kbps");
				// Log.e(TAG, "上传速度: " + content + "kbps");
			}
				break;
			case LiveObs.ERROR_Param:
				//Log.e(TAG, "ERROR_Param------------------------------------->"+param.getParamStr());
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	/**
	 * 切换摄像头
	 * @param v
	 */
	@SuppressLint("NewApi")
	public void onChangeCamera(View v) {
		int mCameraCount = 0;
		mCameraCount = Camera.getNumberOfCameras();
		if (mCameraCount <= 1) {
			Toast.makeText(getApplicationContext(), "只有一个摄像头!", Toast.LENGTH_LONG).show();
			return;
		}
		if (mCameraView != null) {
			mCameraView.changeCamera();
		}
	}

	/**
	 * 切换闪光灯
	 * @param v
	 */
	public void onFlash(View v) {
		if (mCameraView != null) {
			boolean ret = mCameraView.changeFlash();
			if (ret) {
				mFlashBtn.setText("关闭闪光灯");
			} else {
				mFlashBtn.setText("开启闪光灯");
			}
		}
	}

	/**
	 * 切换静音
	 * @param v
	 */
	public void onAudioSwitch(View v) {
		if (isAudioing) {
			closeAudio();
			isAudioing = false;
			mAudioBtn.setText("已静音");
		} else {
			openAudio();
			isAudioing = true;
			mAudioBtn.setText("静音");
		}
	}
	private void openAudio() {
		AudioRecordThread.openAudio();
	}
	private void closeAudio() {
		AudioRecordThread.closeAudio();
	}

	/**
	 * 停止直播
	 */
	private void stopPublish() {
		if (isPublishing) {
			mProcessDialog.dismiss();
			mCameraView.stopPublish();
			stopAudioCapture();
			/** 断开连接地址*/
			controller.stop();
		}
		isPublishing = false;
		mPublishBtn.setText("开始直播");
		tv_upload_speed.setText("");
		if (attend != null)
			attend.disAttend();
	}
	
	/**
	 * 开启音频录制线程
	 */

	private void startAudioCapture() {
		if (mAudioRecordThread == null) {
			mAudioRecordThread = new AudioRecordThread();
			mAudioRecordThread.init();
			mAudioRecordThread.start();
		}
	}
	/**
	 * 关闭音频录制线程
	 */
	private void stopAudioCapture() {
		if (mAudioRecordThread != null) {
			mAudioRecordThread.stopThread();
			mAudioRecordThread = null;
		}
	}

	private void registerConnectionChangeReceiver() {
		if (mConnectionChangeReceiver == null) {
			mConnectionChangeReceiver = new ConnectionChangeReceiver();
		}
		registerReceiver(mConnectionChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private void unregisterConnectionChangeReceiver() {
		unregisterReceiver(mConnectionChangeReceiver);
	}

	private void showAlert(String info) {
		if (this.isFinishing())
			return;
		new AlertDialog.Builder(BroadcastActivity.this).setTitle("错误").setMessage(info)
				.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialoginterface, int i) {
					}
				}).show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerConnectionChangeReceiver();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		mCameraView.destroyCamera();
		stopPublish();
	}

	@Override
	protected void onStop() {
		super.onStop();
		stopPublish();
		unregisterConnectionChangeReceiver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/**
	 * 开始直播前获取地址
	 */
	private void startBrocast() {
		if (!mCameraView.mIsPreviewing) {
			Toast.makeText(getApplicationContext(), "预览失败，无法直播！", Toast.LENGTH_LONG).show();
			return;
		}
		if (Constants.sdk_type == Constants.TYPE_STREAM) {//流式，地址固定，token区分
			url = LiveParam.rtmpPublishBaseUrl;
			stream_token = access_token;
			startPublish();
		}
	}

	
	/**
	 * 结束直播请求
	 */
	private void stopBroadcast() {
		if (Constants.sdk_type == Constants.TYPE_STREAM) {//流式，直接结束
			stopPublish();
		}
	}

}