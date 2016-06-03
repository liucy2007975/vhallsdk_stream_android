package com.vhall.stream;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/**
 * 欢迎页
 * @author huanan
 *
 */
public class LaunchActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launch);
	}

	// 流
	public void streamServiceClick(View view) {
		Intent intent = new Intent(this, MainActivity.class);
		Constants.sdk_type = Constants.TYPE_STREAM;
		startActivity(intent);
	}

}
