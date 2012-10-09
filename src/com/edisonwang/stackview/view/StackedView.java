package com.edisonwang.stackview.view;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class StackedView extends RelativeLayout {

  public static final String  TAG       = "StackedViews";

  private float               mLastMotionX;

  private int                 mActivePointerId;

  private static final double threshold = 0.2;

  private static final int    duration  = 250;

  private View[]              views;

  private int                 size;

  private int                 current;

  private View                root;

  private boolean             isScrolling;

  public StackedView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initStackedViews(context, (RelativeLayout)findViewById(android.R.id.primary), 0);
  }

  public StackedView(Context context, RelativeLayout root, int n) {
    super(context);
    initStackedViews(context, root, n);
  }

  public View getRoot() {
    return root;
  }

  private void initStackedViews(Context context, RelativeLayout relativeLayout, int initialIndex) {
    int cCount = relativeLayout.getChildCount();
    this.views = new View[cCount];
    for (int i = 0; i < cCount; i++) {
      views[i] = relativeLayout.getChildAt(i);
    }
    size = views.length;
    setInitialViewIndex(initialIndex);
    isScrolling = false;
    root = relativeLayout;
  }

  private void setInitialViewIndex(int n) {
    if (n >= size) {
      throw new IllegalArgumentException("N is greater than the number of views");
    }
    for (int i = 0; i < size; i++) {
      views[i].setVisibility(i == n ? View.VISIBLE : View.GONE);
    }
    current = n;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    final int action = ev.getAction();
    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        mLastMotionX = ev.getX();
        mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
        break;
      }
      case MotionEventCompat.ACTION_POINTER_DOWN: {
        final int index = MotionEventCompat.getActionIndex(ev);
        final float x = MotionEventCompat.getX(ev, index);
        mLastMotionX = x;
        mActivePointerId = MotionEventCompat.getPointerId(ev, index);
        break;
      }
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    boolean callSuper = false;
    int action = ev.getAction();
    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        Log.i(TAG, "Down detected");
        mLastMotionX = ev.getX();
        mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
      }
      case MotionEventCompat.ACTION_POINTER_DOWN: {
        Log.i(TAG, "Pointer Down detected");
        callSuper = true;
        final int index = MotionEventCompat.getActionIndex(ev);
        final float x = MotionEventCompat.getX(ev, index);
        mLastMotionX = x;
        mActivePointerId = MotionEventCompat.getPointerId(ev, index);
        break;
      }
      case MotionEvent.ACTION_MOVE: {
        Log.i(TAG, "Move detected on page " + current);
        if ((!isScrolling) && mActivePointerId != -1) {
          // Scroll to follow the motion event
          final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
          final float x = MotionEventCompat.getX(ev, activePointerIndex);
          fixZzIndex(mLastMotionX - x);
          mLastMotionX = x;
        }
        break;
      }
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        Log.i(TAG, "Pointer Up or Cancel detected");
        mActivePointerId = -1;
        break;
      }
      case MotionEvent.ACTION_POINTER_UP: {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
        if (pointerId == mActivePointerId) {
          // This was our active pointer going up. Choose a new
          // active pointer and adjust accordingly.
          final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
          mLastMotionX = ev.getX(newPointerIndex);
          mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
          callSuper = true;
        }
        break;
      }
    }
    scrollToInternal();
    if (callSuper) {
      // TODO
    }
    return true;
  }

  // INTERNAL *************************************
  private void fixZzIndex(float deltaX) {
    RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
    if (deltaX > 0) {
      if (current == size - 1) {
        return;
      }
    } else {
      if (deltaX < 0 && current == 0) {
        return;
      }
    }
    params.leftMargin -= deltaX;
    params.rightMargin += deltaX;
    if (params.leftMargin < 0) {
      if (current < size - 1) {
        Log.i(TAG, "SHOW " + (current + 1));
        views[current + 1].setVisibility(View.VISIBLE);
        views[current + 1].bringToFront();
        views[current].bringToFront();
      }
      if (current > 0) {
        Log.i(TAG, "HIDE " + (current - 1));
        views[current - 1].setVisibility(View.GONE);
      }
    } else {
      if (current < size - 1) {
        Log.i(TAG, "HIDE " + (current + 1));
        views[current + 1].setVisibility(View.GONE);
      }
      if (current > 0) {
        Log.i(TAG, "SHOW " + (current - 1));
        views[current - 1].setVisibility(View.VISIBLE);
        views[current - 1].bringToFront();
        views[current].bringToFront();
      }
    }
    views[current].setLayoutParams(params);
  }

  /**
   * 
   * @return index if needs to snap, else return -1.
   */
  private int scrollTestInternal() {
    final int width = views[current].getWidth();
    RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
    if (Math.abs(params.leftMargin) > width * threshold) {
      if (params.leftMargin < 0) {
        return current < size - 1 ? current + 1 : current;
      } else {
        return current > 0 ? current - 1 : current;
      }
    }
    return -1;
  }

  private void scrollTo(final int index) {
    Log.i(TAG, "Scrolling to " + index);
    if (isScrolling) {
      return;
    }
    isScrolling = true;
    new Thread(new Runnable() {

      @Override
      public void run() {
        final RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
        int totalDistance = (current > index) ? (views[current].getWidth() - params.leftMargin)
            : (views[current].getWidth() - params.rightMargin);

        int distance = totalDistance / duration;
        while (Math.abs(params.leftMargin) < views[current].getWidth()) {
          params.leftMargin += (current > index) ? distance : -distance;
          params.rightMargin -= (current > index) ? distance : -distance;
          views[current].post(new Runnable() {

            @Override
            public void run() {
              views[current].setLayoutParams(params);
            }

          });
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
        views[current].post(new Runnable() {

          @Override
          public void run() {
            // views[current].setVisibility(View.GONE);
            RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
            params.leftMargin = 0;
            params.rightMargin = 0;
            views[current].setLayoutParams(params);
            current = index;
            fixZzIndex(0);
            isScrolling = false;
          }

        });
      }

    }).start();

  }

  private void scrollToInternal() {
    int nextIndex = scrollTestInternal();
    if (nextIndex >= 0) {
      scrollTo(nextIndex);
    }
  }
}
