package com.edisonwang.stackedview.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RelativeLayout;

import com.edisonwang.stackedview.R;
import com.edisonwang.stackedview.view.StackedView;

public class MainActivity extends Activity {


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(new StackedView(this,(RelativeLayout)getLayoutInflater().inflate(R.layout.activity_main,null),1).setTopPage(1));
  }
}
