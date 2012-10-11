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

  private RelativeLayout      root;

  private boolean             isScrolling;
  
  private boolean             isScrollingRight; //Current+1 on top.

  public StackedView(Context context) {
    super(context);
    initStackedViews(context, this, 0);
  }

  public StackedView(Context context, int n) {
    super(context);
    initStackedViews(context, this, 0);
  }

  public StackedView(Context context, RelativeLayout root) {
    super(context);
    initStackedViews(context, root, 0);
    if(root!=this){
      addView(root);
    }
  }

  public StackedView(Context context, RelativeLayout root, int n) {
    super(context);
    initStackedViews(context, root, n);
    if(root!=this){
      addView(root);
    }
  }

  public StackedView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initStackedViews(context, this, 0);
  }

  public StackedView(Context context, AttributeSet attrs, int n) {
    super(context);
    initStackedViews(context, this, n);
  }

  public StackedView(Context context, AttributeSet attrs, RelativeLayout root) {
    super(context);
    initStackedViews(context, root, 0);
    if(root!=this){
      addView(root);
    }
  }

  public StackedView(Context context, AttributeSet attrs, RelativeLayout root, int n) {
    super(context);
    initStackedViews(context, root, n);
    if(root!=this){
      addView(root);
    }
  }

  public void setRoot(RelativeLayout r) {
    root = r;
    initStackedViews(getContext(), root, 0);
  }

  public RelativeLayout getRoot() {
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
    isScrollingRight = false;
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
        scrollToInternal(false);
        break;
      }
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        Log.i(TAG, "Pointer Up or Cancel detected");
        mActivePointerId = -1;
        scrollToInternal(true);
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
    if (callSuper) {
      // TODO
    }
    return true;
  }

  // INTERNAL *************************************

  private void fixZzIndexLeft(float deltaX) {
    boolean toLeft = deltaX < 0;
    RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();

    if (params.leftMargin - deltaX < 0) {
      toLeft = true;
    }
    int index = current;
    if (toLeft) {
      index = current + 1;
      params = (LayoutParams)views[index].getLayoutParams();
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
  private void fixZzIndexRight(float deltaX) {
    
  }
  private void fixZzIndex(float deltaX) {
    boolean toLeft = deltaX < 0;
    boolean toRight = deltaX > 0;
    if (toRight) {
      if (current >= size - 1) {
        return;
      }
    } else {
      if (toLeft && current == 0) {
        return;
      }
    }
    /**
     * 2 Cases here.
     * 
     * ---If isScrollingRight = false
     * Slide the view in current index to the RIGHT with POSITIVE left margin on current index, reveals the current-1 on bottom.
     * ---No need to test anything here.
     * Slide the view in current index to the LEFT  with POSITIVE left margin on current index, reveals the current-1 on bottom.
     * 
     * 
     * ---If positive left margin on the current index turn negative, we set it to 0 immediately and let current+1 to have POSITIVE left margin = its width.
     * ---Crossing this line sets isScrollingLeft to true.
     * 
     * ---If iScrollingRight = true;
     * Slide the view in current+1 index to the RIGHT with POSITIVE left margin on current+1 index, reveals current on bottom. 
     * ---It does not go over.   
     * 
     * 
     */
    if(isScrollingRight){
      fixZzIndexRight(deltaX);
    }else{
      fixZzIndexLeft(deltaX);
    }
    
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
    if (isScrolling) {
      return;
    }
    Log.i(TAG, "Scrolling to " + index);
    isScrolling = true;
    new Thread(new Runnable() {

      @Override
      public void run() {
        final RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
        final boolean isRestoring = current == index;
        final boolean toLeft = (isRestoring) ? params.leftMargin > 0 : current > index;

        Log.i(TAG, (isRestoring ? "Restoring " : " Scrolling ") + (toLeft ? "Left" : "Right"));

        int totalDistance = toLeft ? (views[current].getWidth() - params.rightMargin)
            : (views[current].getWidth() - params.leftMargin);

        int distance = totalDistance / (isRestoring ? duration / 3 : duration);
        if (distance == 0) {
          distance = toLeft ? -1 : 1;
        }
        boolean needsMore;
        if (isRestoring) {
          needsMore = params.leftMargin != 0;
        } else {
          needsMore = Math.abs(params.leftMargin) < views[current].getWidth();
        }
        while (needsMore) {
          params.leftMargin += toLeft ? distance : -distance;
          params.rightMargin -= toLeft ? distance : -distance;
          if (isRestoring) {
            needsMore = (toLeft) ? params.leftMargin <= 0 : params.leftMargin >= 0;
          } else {
            needsMore = Math.abs(params.leftMargin) < views[current].getWidth();
          }
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
            RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
            params.leftMargin = 0;
            params.rightMargin = 0;
            views[current].setLayoutParams(params);
            Log.i(TAG, "Set Current Index to " + index);
            current = index;
            if (!isRestoring) {
              fixZzIndex(0);
            }
            isScrolling = false;
          }

        });
      }

    }).start();

  }

  private void scrollToInternal(boolean canceled) {
    int nextIndex = scrollTestInternal();
    if (!canceled) {
      if (nextIndex >= 0) {
        scrollTo(nextIndex);
      }
    } else {
      scrollTo(current);
    }
  }

  @Override
  public void invalidate() {
    this.initStackedViews(getContext(), root, current);
    super.invalidate();
  }
  
  @Override
  public void addView(View child){
    if(root==this){
      addStackedView(child,true);
    }else{
      super.addView(child);
    }
  }

  public void addStackedView(View child,boolean attachToParent) {
    root.addView(child);
    initStackedViews(getContext(), getRoot(), current);
  }
}
