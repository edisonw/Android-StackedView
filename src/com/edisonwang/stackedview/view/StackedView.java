package com.edisonwang.stackedview.view;

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

  private boolean             isScrollingRight;

  private boolean             isPrepared;

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
    if (root != this) {
      addView(root);
    }
  }

  public StackedView(Context context, RelativeLayout root, int n) {
    super(context);
    initStackedViews(context, root, n);
    if (root != this) {
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
    if (root != this) {
      addView(root);
    }
  }

  public StackedView(Context context, AttributeSet attrs, RelativeLayout root, int n) {
    super(context);
    initStackedViews(context, root, n);
    if (root != this) {
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
    isPrepared = false;
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
    // Move
    RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
    params.leftMargin -= deltaX;
    params.rightMargin += deltaX;
    views[current].setLayoutParams(params);
    // Test
    if (params.leftMargin < 0) {
      isScrollingRight = true;
      prepareScrollingToRight();
    }
  }

  private void prepareScrollingToRight() {
    Log.i(TAG, "Prepared Scrolling To Right");
    RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
    params.leftMargin = 0;
    params.rightMargin = 0;
    views[current].setLayoutParams(params);
    if (current < size - 1) {
      params = (LayoutParams)views[current + 1].getLayoutParams();
      params.leftMargin = views[current].getWidth();
      params.rightMargin = -params.leftMargin;
      views[current].bringToFront();
      views[current + 1].setLayoutParams(params);
      views[current + 1].setVisibility(View.VISIBLE);
      views[current + 1].bringToFront();
    }
    if (current >= 1) {
      views[current - 1].setVisibility(View.GONE);
    }
    this.isPrepared = true;
  }

  private void fixZzIndexRight(float deltaX) {
    // Move
    if (current < size - 1) {
      RelativeLayout.LayoutParams params = (LayoutParams)views[current + 1].getLayoutParams();
      params.leftMargin -= deltaX;
      params.rightMargin += deltaX;
      views[current + 1].setLayoutParams(params);
      // Test
      if (params.leftMargin > views[current].getWidth()) {
        isScrollingRight = false;
        prepareScrollingToLeft();
      }
    } else {
      isScrollingRight = false;
      prepareScrollingToLeft();
    }

  }

  private void prepareScrollingToLeft() {
    Log.i(TAG, "Prepared Scrolling To Left");
    if (current > 0) {
      views[current - 1].bringToFront();
      views[current - 1].setVisibility(View.VISIBLE);
    }
    views[current].bringToFront();
    if (current < size - 1) {
      views[current + 1].setVisibility(View.GONE);
    }
    this.isPrepared = true;
  }

  private void fixZzIndex(float deltaX) {
    Log.i(TAG, "Moving and Fixing Index.");
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
    if (!isPrepared) {
      if (toRight) {
        isScrollingRight = true;
        prepareScrollingToRight();
      } else {
        if (toLeft) {
          isScrollingRight = false;
          prepareScrollingToLeft();
        }
      }
      this.isPrepared = true;
    }
    if (isPrepared && isScrollingRight) {
      Log.i(TAG, "Fix to right, current: " + current);
      fixZzIndexRight(deltaX);
    } else {
      Log.i(TAG, "Fix to left, current: " + current);
      fixZzIndexLeft(deltaX);
    }

  }

  /**
   * 
   * @return index if needs to snap, else return -1.
   */
  private int scrollTestInternal() {
    final int width = views[current].getWidth();
    if (isScrollingRight) {
      if (current >= size - 1) {
        return -1;
      }
      RelativeLayout.LayoutParams params = (LayoutParams)views[current + 1].getLayoutParams();
      if (Math.abs(params.leftMargin) <= width * (1 - threshold)) {
        return current < size - 1 ? current + 1 : current;
      }
    } else {
      RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
      if (Math.abs(params.leftMargin) > width * threshold) {
        return current > 0 ? current - 1 : current;
      }
    }
    return -1;
  }

  private synchronized void scrollTo(final int index) {
    if (isScrolling) {
      return;
    }
    Log.i(TAG, "Scrolling to from current: " + current + " to " + index + " (" + (isScrollingRight ? "Right" : "Left")
        + ")");
    isScrolling = true;
    new Thread(new Runnable() {

      @Override
      public synchronized void run() {
        final boolean isRestoring = current == index;
        if (isScrollingRight) {

          if (current + 1 > size - 1) {
            isPrepared = false;
            isScrolling = false;
            return;
          }
          Log.i(TAG, (isRestoring ? " Restoring " : " Scrolling ") + (isRestoring ? "Right" : "Left") + " Currnet: "
              + current);

          final RelativeLayout.LayoutParams params = (LayoutParams)views[current + 1].getLayoutParams();

          int totalDistance = isRestoring ? (views[current].getWidth() - params.leftMargin) : (params.leftMargin); // >0

          int distance = totalDistance / (isRestoring ? duration / 3 : duration);

          if (distance == 0) {
            distance = isRestoring ? 1 : -1;
          }
          boolean needsMore;
          if (isRestoring) {
            needsMore = params.leftMargin < views[current].getWidth();
          } else {
            needsMore = params.leftMargin > 0;
          }
          while (needsMore) {
            params.leftMargin += isRestoring ? distance : -distance;
            params.rightMargin = -params.leftMargin;
            if (isRestoring) {
              needsMore = params.leftMargin < views[current].getWidth();
            } else {
              needsMore = params.leftMargin > 0;
            }
            views[current + 1].post(new Runnable() {

              @Override
              public void run() {
                views[current + 1].setLayoutParams(params);
              }

            });
            try {
              Thread.sleep(1);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          views[current + 1].post(new Runnable() {

            @Override
            public void run() {
              // ScrollRight And Not restoring
              if (isRestoring) {
                RelativeLayout.LayoutParams params = (LayoutParams)views[current + 1].getLayoutParams();
                params.leftMargin = views[current].getWidth();
                params.rightMargin = -params.leftMargin;
                views[current + 1].setLayoutParams(params);
              } else {
                Log.i(TAG, "Set Current Index to " + index);
                current = index;
                RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
                params.leftMargin = 0;
                params.rightMargin = 0;
                views[current].setLayoutParams(params);
                views[current].bringToFront();
              }
              isPrepared = false;
              isScrolling = false;
            }

          });
        } else {
          if (current < 1) {
            isPrepared = false;
            isScrolling = false;
            return;
          }

          final RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();

          Log.i(TAG, (isRestoring ? "  Restoring " : "  Scrolling ") + (isRestoring ? "Left" : "Right"));

          int totalDistance = isRestoring ? (params.leftMargin) : (views[current - 1].getWidth() - params.leftMargin);

          int distance = totalDistance / (isRestoring ? duration / 3 : duration);
          if (distance == 0) {
            distance = isRestoring ? -1 : 1;
          }
          boolean needsMore;
          if (isRestoring) {
            needsMore = params.leftMargin > 0;
          } else {
            needsMore = params.leftMargin < views[current - 1].getWidth();
          }
          while (needsMore) {
            params.leftMargin += isRestoring ? -distance : distance;
            params.rightMargin = -params.leftMargin;
            if (isRestoring) {
              needsMore = params.leftMargin > 0;
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
              // ScrollRight And Not restoring
              if (isRestoring) {
                RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
                params.leftMargin = 0;
                params.rightMargin = 0;
                views[current].setLayoutParams(params);
              } else {
                Log.i(TAG, "Set Current Index to " + index);
                views[current].setVisibility(View.GONE);
                current = index;
                views[current].setVisibility(View.VISIBLE);
                views[current].bringToFront();
              }
              isPrepared = false;
              isScrolling = false;
            }

          });
        }
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
  public void addView(View child) {
    if (root == this) {
      addStackedView(child, true);
    } else {
      super.addView(child);
    }
  }

  public void addStackedView(View child, boolean attachToParent) {
    root.addView(child);
    initStackedViews(getContext(), getRoot(), current);
  }
}
