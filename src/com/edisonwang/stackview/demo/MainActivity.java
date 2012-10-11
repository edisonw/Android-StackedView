package com.edisonwang.stackview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.edisonwang.curlviewgroup.R;
import com.edisonwang.stackview.view.StackedView;

public class MainActivity extends Activity {


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(new StackedView(this,(RelativeLayout)getLayoutInflater().inflate(R.layout.activity_main,null),1));
  }
}
