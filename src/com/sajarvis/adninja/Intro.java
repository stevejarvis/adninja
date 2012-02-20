package com.sajarvis.adninja;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Intro extends Activity{
	private Button finish;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.intro);
		finish = (Button) findViewById(R.id.done);
		finish.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
}
