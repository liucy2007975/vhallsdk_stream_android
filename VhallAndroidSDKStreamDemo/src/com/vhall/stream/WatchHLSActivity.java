package com.vhall.stream;

import java.util.Timer;
import java.util.TimerTask;

import com.vhall.playersdk.player.impl.HlsRendererBuilder;
import com.vhall.playersdk.player.impl.VhallHlsPlayer;
import com.vhall.playersdk.player.util.Util;
import com.vinny.vinnylive.LiveParam;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * 观看回放
 * 
 * @author huanan
 *
 */
public class WatchHLSActivity extends Activity {

	private static final String TAG = "MediaPlayerDemo";
	private VhallHlsPlayer mMediaPlayer;
	private VhallPlayerListener mVhallPlayerListener;
	private long playerCurrentPosition = 0L; // 度播放的当前标志，毫秒
	private long playerDuration;// 播放资源的时长，毫秒
	private String playerDurationTimeStr = "00:00:00";
	private RelativeLayout mRelativeVideoSize;
	
	String type = "";
	String video_url = "";
	LiveParam param;
	private Matrix matrix;

	SurfaceView surface;
	SurfaceHolder holder;
	LinearLayout ll_actions;
	ImageView iv_play;
	SeekBar seekbar;
	TextView tv_pos;
	ProgressBar pb;
	Button button;
	Timer timer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.play_back);
		surface = (SurfaceView) this.findViewById(R.id.surface);
		holder = surface.getHolder();
		
		ll_actions = (LinearLayout) this.findViewById(R.id.ll_actions);
		mRelativeVideoSize = (RelativeLayout) this.findViewById(R.id.relative_video_size);
		iv_play = (ImageView) this.findViewById(R.id.iv_play);
		button = (Button) this.findViewById(R.id.button1);
		seekbar = (SeekBar) this.findViewById(R.id.seekbar);
		tv_pos = (TextView) this.findViewById(R.id.tv_pos);
		pb = (ProgressBar) this.findViewById(R.id.pb);
		type = getIntent().getStringExtra("type");
		video_url = getIntent().getStringExtra("url");
		param = (LiveParam) getIntent().getSerializableExtra("param");
		
		Log.v(TAG, "" + param.getParamStr());
		Log.v(TAG, "type == " + type + "video_url == " + video_url+" param == " + param);
		iv_play.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TextUtils.isEmpty(video_url))
					return;
				if (mMediaPlayer == null) {
					playVideo(video_url);
					iv_play.setImageResource(R.drawable.icon_play_pause);
				} else {
					if (mMediaPlayer.isPlaying()) {
						// mMediaPlayer.pause();
						mMediaPlayer.setPlayWhenReady(false);
						iv_play.setImageResource(R.drawable.icon_play_play);
					} else {
						// mMediaPlayer.start();
						mMediaPlayer.setPlayWhenReady(true);
						iv_play.setImageResource(R.drawable.icon_play_pause);
					}

				}
			}
		});
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else {
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			}
		});
		
		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			/** 停止拖动*/
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(mMediaPlayer == null){
					return;
				}
				playerCurrentPosition = seekBar.getProgress();
				int secondPostition = seekBar.getSecondaryProgress();
				int per = mMediaPlayer.getBufferedPercentage();
				pb.setVisibility(View.INVISIBLE);
				mMediaPlayer.seekTo(playerCurrentPosition);
				//mMediaPlayer.start();
				/*if (mMediaPlayer != null) {
					mMediaPlayer.seekTo(playerCurrentPosition);
					mMediaPlayer.start();
				} else {
					// TODO
				}*/
			}

			/** 开始拖动*/
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				tv_pos.setText(converLongTimeToStr(progress) + "/" + playerDurationTimeStr);
			}
		});

		if (type.equals("direct")) {
			iv_play.performClick();
		} else if (type.equals("video")) {
			ll_actions.setVisibility(View.VISIBLE);
		}

		// getReq();

	}
	private void initTimer() {
		if (timer != null)
			return;
		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {

				runOnUiThread(new Runnable() {

					@Override
					public void run() {

						if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
							playerCurrentPosition = mMediaPlayer.getCurrentPosition();
							
							seekbar.setProgress((int) playerCurrentPosition);
							String playerCurrentPositionStr = converLongTimeToStr(playerCurrentPosition);
							tv_pos.setText(playerCurrentPositionStr + "/" + playerDurationTimeStr);
						}
					}
				});

			}
		}, 1000, 1000);
	}

	/**
	 * 创建播放器,并播放
	 * 
	 * @param path
	 */
	private void playVideo(String path) {
		try {
			if (path == "") {
				return;
			}
			// Create a new media player and set the listeners
			String userAgent = Util.getUserAgent(this, "VhallAPP");
			mVhallPlayerListener = new VhallPlayerListener();
			mMediaPlayer = new VhallHlsPlayer(new HlsRendererBuilder(this, userAgent, path));
			mMediaPlayer.addListener(mVhallPlayerListener);
			mMediaPlayer.seekTo(playerCurrentPosition);
			mMediaPlayer.prepare();
			mMediaPlayer.setSurface(holder.getSurface());
			if (!this.isFinishing()) {
				mMediaPlayer.setPlayWhenReady(true);
			} else {
				releaseMediaPlayer();
			}

		} catch (Exception e) {
			Log.e(TAG, "error: " + e.getMessage(), e);
		}
	}

	
	/**
	 * 自定义播放器监听事件处理
	 */

	private class VhallPlayerListener implements VhallHlsPlayer.Listener {
		@Override
		public void onStateChanged(boolean playWhenReady, int playbackState) {
			switch (playbackState) {
			case VhallHlsPlayer.STATE_IDLE:
				Log.e(TAG, "--------------------->STATE_IDLE");
				break;
			case VhallHlsPlayer.STATE_PREPARING:
				pb.setVisibility(View.VISIBLE);
				Log.e(TAG, "--------------------->STATE_PREPARING");
				break;
			case VhallHlsPlayer.STATE_BUFFERING:
				pb.setVisibility(View.VISIBLE);
				Log.e(TAG, "--------------------->STATE_BUFFERING");
				break;
			case VhallHlsPlayer.STATE_READY:
				Log.e(TAG, "--------------------->STATE_READY");
				pb.setVisibility(View.GONE);
				if (type.equals("video")) {
					playerDuration = mMediaPlayer.getDuration();
					playerDurationTimeStr = converLongTimeToStr(playerDuration);
					seekbar.setMax((int) playerDuration);
					initTimer();
				}
				break;
			case VhallHlsPlayer.STATE_ENDED:
				Log.e(TAG, "--------------------->STATE_ENDED");
				pb.setVisibility(View.GONE);
				releaseMediaPlayer();
				break;
			default:
				break;
			}

		}

		@Override
		public void onError(Exception e) {
			releaseMediaPlayer();
		}

		@Override
		public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
				float pixelWidthHeightRatio) {
			Log.v(TAG , "SSSSSSSSSSSS");
			if (width == 0 || height == 0) {
				return;
			}
			Log.e(TAG, "width:" + width + "---" + "height:" + height);
			videoWidth = width;
			videoHeight = height;
			transform(videoWidth ,videoHeight);
			
		}
	}

	/**
	 * 释放播放器
	 */
	private void releaseMediaPlayer() {
		if (mMediaPlayer != null) {
			mMediaPlayer.release();
			mMediaPlayer = null;
			playerCurrentPosition = 0;
			iv_play.setImageResource(R.drawable.icon_play_play);
		}
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        transform(videoWidth , videoHeight);
        Log.v(TAG , "XXXXXXXXXXXX");
    }


	/**
	 * 将长整型值转化成字符串
	 * 
	 * @param time
	 * @return
	 */
	public static String converLongTimeToStr(long time) {
		int ss = 1000;
		int mi = ss * 60;
		int hh = mi * 60;

		long hour = (time) / hh;
		long minute = (time - hour * hh) / mi;
		long second = (time - hour * hh - minute * mi) / ss;

		String strHour = hour < 10 ? "0" + hour : "" + hour;
		String strMinute = minute < 10 ? "0" + minute : "" + minute;
		String strSecond = second < 10 ? "0" + second : "" + second;
		if (hour > 0) {
			return strHour + ":" + strMinute + ":" + strSecond;
	 	} else {
			return "00:" + strMinute + ":" + strSecond;
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		releaseMediaPlayer();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseMediaPlayer();
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	private int screenWidth = 0;
	private int screenHeight = 0;
	private int videoWidth = 0;
	private int videoHeight = 0;
	
	public void transform(int videoWidth, int videoHeight){
		boolean hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK);
		boolean hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME);
		
		screenWidth = ScreenSizeUtil.getScreenWidth(this);
        screenHeight = ScreenSizeUtil.getScreenHeight(this);
        Log.v(TAG, "transformVideo, videoWidth =" + videoWidth);
        Log.v(TAG, "transformVideo, videoHeight =" + videoHeight);
        int fixWidth = 0;
        int fixHeight = 0;
        /** 竖屏观看*/
        if (this.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT){
        	
        		if (videoWidth < videoHeight){
        			Log.v(TAG,"竖屏发起直播~竖屏观看");
        			fixHeight = screenHeight;
    				fixWidth = (screenHeight * videoWidth) / videoHeight;
    				Log.v(TAG,"fixWidth === " + fixWidth + "fixHeight === " + fixHeight);
    				
    				if (fixWidth > screenWidth || fixHeight > screenHeight){
    					float wRatio = (float)fixWidth/(float)screenHeight;
    					float hRatio = (float)fixHeight/(float)screenWidth; 
    					Log.v(TAG,"宽超出比例" + wRatio + "高超出比例" + hRatio);
    					//选择大的一个进行缩放   
    		            float maxScale = Math.max(wRatio, hRatio);  
    		            Log.v(TAG,"选择的缩放" + maxScale);
    		            int result = (int) (screenWidth * maxScale);
    		            android.view.ViewGroup.LayoutParams param =  mRelativeVideoSize.getLayoutParams();
    		            param.width = result;
    		            param.height = fixHeight;
    		            mRelativeVideoSize.setLayoutParams(param);
    				}
    				surface.getHolder().setFixedSize(fixWidth, fixHeight);
        		}else {
        			Log.v(TAG,"横屏发起直播~~~~横屏观看");
        			fixWidth = screenWidth;
    				fixHeight = (videoHeight * screenWidth) / videoWidth;
    				surface.getHolder().setFixedSize(fixWidth, fixHeight);
        		}
        }else { /** 横屏观看*/
        	if (videoWidth > videoHeight) {
        		Log.v(TAG,"横屏发起直播~横屏观看");
        		fixWidth = screenHeight;
        		fixHeight = (screenHeight * videoHeight) /  videoWidth;

        		if (hasBackKey && hasHomeKey) {
        			Log.v(TAG, "没有虚拟键盘" );
        		} else {
        			fixWidth += 18;
        		}
				Log.v(TAG,"fixWidth === " + fixWidth + "fixHeight === " + fixHeight);
				surface.getHolder().setFixedSize(fixWidth, fixHeight);	
				if (fixWidth > screenWidth || fixHeight > screenHeight){
					Log.v(TAG,"是超出了");
					float wRatio = (float)fixWidth/(float)screenWidth;
					float hRatio = (float)fixHeight/(float)screenHeight; 
					Log.v(TAG,"宽超出比例" + wRatio + "高超出比例" + hRatio);
					//选择大的一个进行缩放   
		            float maxScale = Math.max(wRatio, hRatio);  
		            Log.v(TAG,"选择的缩放" + maxScale);
		            int vWidth = (int)Math.ceil((float)fixWidth/maxScale);   
		            int vHeight = (int)Math.ceil((float)fixHeight/maxScale);  
		            Log.v(TAG,"最新的宽" + vWidth + "最新的高" + vHeight);
		            android.view.ViewGroup.LayoutParams param =  mRelativeVideoSize.getLayoutParams();
		            param.width = fixWidth;
		            param.height = fixHeight;
		            mRelativeVideoSize.setLayoutParams(param);
		            Log.v(TAG , " mRelativeVideoSize.width = " + mRelativeVideoSize.getLayoutParams().width);
		            Log.v(TAG , " mRelativeVideoSize.height = " + mRelativeVideoSize.getLayoutParams().height);
		            return;
				}
				android.view.ViewGroup.LayoutParams param =  mRelativeVideoSize.getLayoutParams();
	            param.width = fixWidth;
	            param.height = fixHeight;
	            mRelativeVideoSize.setLayoutParams(param);
	            Log.v(TAG , " mRelativeVideoSize.width = " + mRelativeVideoSize.getLayoutParams().width);
	            Log.v(TAG , " mRelativeVideoSize.height = " + mRelativeVideoSize.getLayoutParams().height);
			}else{
				fixHeight = screenWidth;
				fixWidth = (screenWidth * videoWidth) / videoHeight;
				surface.getHolder().setFixedSize(fixWidth, fixHeight);
			}
        }
        //fixWidth = videoWidth * fixHeight / videoHeight;
        //surface.getHolder().setFixedSize(fixWidth, fixHeight);
		/*if(videoWidth > screenWidth || videoHeight > screenHeight){   
			Log.v(TAG,"XXXXXXXXXXXX");
            //如果video的宽或者高超出了当前屏幕的大小，则要进行缩放   
  
            float wRatio = (float)videoWidth/(float)screenWidth;   
  
            float hRatio = (float)videoHeight/(float)screenHeight;   
            
            //选择大的一个进行缩放   
            float maxScale = Math.max(wRatio, hRatio);   
            Log.v(TAG, "transformVideo, maxScale=" + maxScale);
            int vWidth = (int)Math.ceil((float)videoWidth/maxScale);   
            int vHeight = (int)Math.ceil((float)videoHeight/maxScale);                 
            //设置surfaceView的布局参数   
            surface.setLayoutParams(new LinearLayout.LayoutParams(vWidth, vHeight));   
            //然后开始播放视频   
        }  */ 
		
	}
	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
