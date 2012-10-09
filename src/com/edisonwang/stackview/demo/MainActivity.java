package com.edisonwang.stackview.demo;

import com.edisonwang.curlviewgroup.R;
import com.edisonwang.stackview.view.StackedView;

import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;
import android.app.Activity;

public class MainActivity extends Activity {

  private StackedView sv;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    View main = getLayoutInflater().inflate(R.layout.activity_main,null);
    sv = new StackedView(this,(RelativeLayout)main.findViewById(R.id.parent),1);
    sv.addView(main);
    setContentView(sv);
  }
}
