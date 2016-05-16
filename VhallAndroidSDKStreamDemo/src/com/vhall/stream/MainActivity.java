package com.vhall.stream;

import com.vhall.netbase.constants.VhallHttpStream;
import com.vhall.netbase.constants.VhallHttpStream.ReqNewCallback;
import com.vinny.vinnylive.LiveParam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * 主界面
 * 
 * @author huanan
 *
 */
public class MainActivity extends Activity {

	EditText et_roomid, et_token, et_bitrate, et_delay;
	LinearLayout ll_password;
	TextView tv_roomid;
	RadioGroup rg_type;
	LiveParam param;
	int delay = 2;
	TextView tv_param;
	private ProgressDialog mProcessDialog;
	private EditText mediaFrameRate;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		et_roomid = (EditText) this.findViewById(R.id.et_roomid);
		tv_roomid = (TextView) this.findViewById(R.id.tv_roomid);
		et_token = (EditText) this.findViewById(R.id.et_token);
		et_bitrate = (EditText) this.findViewById(R.id.et_bitrate);
		et_delay = (EditText) this.findViewById(R.id.et_delay);
		mediaFrameRate = (EditText) this.findViewById(R.id.et_frame_rate);
		rg_type = (RadioGroup) this.findViewById(R.id.rg_type);
		tv_param = (TextView) this.findViewById(R.id.tv_param);
		
		mProcessDialog = new ProgressDialog(MainActivity.this);
		mProcessDialog.setCancelable(true);
		
		mProcessDialog.setCanceledOnTouchOutside(false);
		param = LiveParam.getParam(LiveParam.TYPE_HDPI);
		tv_param.setText(param.getParamStr());
		initData();
		rg_type.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rb_hdpi:
					param = LiveParam.getParam(LiveParam.TYPE_HDPI);
					tv_param.setText(param.getParamStr());
					break;
				case R.id.rb_xhdpi:
					param = LiveParam.getParam(LiveParam.TYPE_XHDPI);
					tv_param.setText(param.getParamStr());
					break;
				case R.id.rb_xxhdpi:
					param = LiveParam.getParam(LiveParam.TYPE_XXHDPI);
					tv_param.setText(param.getParamStr());
					break;

					
				default:
					break;
				}
			}
		});

	}
	
	/**
	 * 初始化显示和参数
	 */
	private void initData(){
		if(Constants.sdk_type == Constants.TYPE_STREAM){
			et_roomid.setText("");
			et_token.setText("");
		}
	}

	/**
	 * 校验
	 * @param level 发起RTMP直播等级为1 , 
	 * @param id
	 * @param token
	 * @param bitrate
	 * @param delay
	 * @return
	 */
	private boolean invalidate(int level, String id, String token, String bitrate, int delay , String mediaFrameRate) {

		if (TextUtils.isEmpty(id)) {
			Toast.makeText(this, "id不能为空", Toast.LENGTH_SHORT).show();
			return false;
		}
		
		if (TextUtils.isEmpty(token)){
			Toast.makeText(this, "Token不能为空", Toast.LENGTH_SHORT).show();
			return false;
		}

		if (level > 0) {
			if (TextUtils.isEmpty(bitrate)) {
				Toast.makeText(this, "码率不能为空", Toast.LENGTH_SHORT).show();
				return false;
			}
			
			if (TextUtils.isEmpty(mediaFrameRate)){
				Toast.makeText(this, "帧率不能为空", Toast.LENGTH_SHORT).show();
				return false;
			}
			if (delay < 2) {
				Toast.makeText(getApplicationContext(), "延时最低2秒", Toast.LENGTH_LONG).show();
				return false;
			}
		} 
		return true;
	}

	/**
	 * 竖屏发起RTMP
	 * @param v
	 */
	public void onBroadcast(View v) {
		String id = et_roomid.getText().toString();
		String token = et_token.getText().toString();
		//码率
		String bitrateStr = et_bitrate.getText().toString();
		String delayStr = et_delay.getText().toString();
		//帧率
		String mediaFrameRateStr = mediaFrameRate.getText().toString();
		try {
			int delay = Integer.parseInt(delayStr);
			int bitrate = Integer.parseInt(bitrateStr);
			bitrate = bitrate * 1024;
			if (!invalidate(1, id, token, bitrateStr, delay , mediaFrameRateStr))
				return;
			int framerate = Integer.parseInt(mediaFrameRateStr);
			param.setFrame_rate(framerate);
			param.orientation = LiveParam.Screen_orientation_portrait;
			param.video_bitrate = bitrate;
			param.buffer_time = delay;

			Intent intent = new Intent(MainActivity.this, BroadcastActivity.class);
			intent.putExtra("roomid", id);
			intent.putExtra("token", token);
			intent.putExtra("param", param);
			startActivity(intent);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 横屏发起RTMP
	 * @param v
	 */
	public void onBroadcastLand(View v) {
		String id = et_roomid.getText().toString();
		String token = et_token.getText().toString();
		String bitrateStr = et_bitrate.getText().toString();
		String delayStr = et_delay.getText().toString();
		String mediaFrameRateStr = mediaFrameRate.getText().toString();
		try {
			int delay = Integer.parseInt(delayStr);
			int bitrate = Integer.parseInt(bitrateStr);
			if (!invalidate(1, id, token, bitrateStr, delay , mediaFrameRateStr))
				return;
			int framerate = Integer.parseInt(mediaFrameRateStr);
			param.setFrame_rate(framerate);
			param.orientation = LiveParam.Screen_orientation_landscape;
			param.video_bitrate = bitrate;
			param.buffer_time = delay;
			Intent intent = new Intent(MainActivity.this, BroadcastActivity.class);
			intent.putExtra("roomid", id);
			intent.putExtra("token", token);
			intent.putExtra("param", param);
			startActivity(intent);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 观看直播RTMP
	 * @param v
	 */
	public void onWatch(View v) {
		if (Constants.sdk_type == Constants.TYPE_STREAM) {
			String id = et_roomid.getText().toString();
			if (TextUtils.isEmpty(id))
				return;
			getRtmpWatchUrl(id);
		}
	}

	/**
	 * 观看直播HLS
	 * @param v
	 */
	public void onWatchHLS(View v) {
		if (Constants.sdk_type == Constants.TYPE_STREAM) {
			String id = et_roomid.getText().toString();
			
			if (TextUtils.isEmpty(id)){
				return;
			}
			getDirectHLSURL(id);
		}
	}

	/**
	 * 观看回放HLS
	 * @param v
	 */
	public void onPlayBack(View v) {
		if (Constants.sdk_type == Constants.TYPE_STREAM) {
			String id = et_roomid.getText().toString();
			if (TextUtils.isEmpty(id)){
				return;
			}
			getVideoHLSURL(id);
		}
	}

	
	private void skipToWatch(String url,String msg_server,String msg_token) {
		Intent intent = new Intent(MainActivity.this, WatchActivity.class);
		intent.putExtra("url", url);
		intent.putExtra("msg_server", msg_server);
		intent.putExtra("msg_token", msg_token);
		intent.putExtra("param", param);
		startActivity(intent);
	}

	/**
	 * 跳转回放
	 * @param url
	 * @param type
	 */
	private void skipToHLS(String url, String type) {
		Intent intent = new Intent(MainActivity.this, WatchHLSActivity.class);
		intent.putExtra("url", url);
		intent.putExtra("type", type);
		intent.putExtra("param", param);
		startActivity(intent);
	}
	
	/**
	 * 流媒体获取RTMP观看地址
	 * @param v
	 */
	public void getRtmpWatchUrl(String v) {
		VhallHttpStream.rtmpWatchUrl(v , new ReqNewCallback() {

			@Override
			public void OnSuccess(final String data) {
				mProcessDialog.dismiss();
				skipToWatch(data,"","");
			}

			@Override
			public void OnFail(final String errorMsg) {
				mProcessDialog.dismiss();
				Toast.makeText(getApplicationContext(), "rtmpWatch地址获取失败", Toast.LENGTH_SHORT).show();
			}
		});
	}

	/**
	 * 请求网络直播HLS地址
	 */
	private void getDirectHLSURL(String id) {
		mProcessDialog.show();
		
		VhallHttpStream.getDirectUrl(id, new ReqNewCallback() {
			
			@Override
			public void OnSuccess(final String data) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mProcessDialog.dismiss();
						skipToHLS(data, "direct");
					}
				});
			}
			
			@Override
			public void OnFail(final String errorMsg) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mProcessDialog.dismiss();
						Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
		
	}

	/**
	 * 请求获取回放地址
	 * @param id
	 */
	private void getVideoHLSURL(final String id) {
		mProcessDialog.show();
		
		VhallHttpStream.getNewVideoUrl(id, new ReqNewCallback() {
			
			@Override
			public void OnSuccess(final String data) {
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						mProcessDialog.dismiss();
						skipToHLS(data, "video");
					}
				});
			}
			
			@Override
			public void OnFail(final String errorMsg) {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						mProcessDialog.dismiss();
						Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

}
