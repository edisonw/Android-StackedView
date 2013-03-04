package com.edisonwang.stackedview.view;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class StackedView extends RelativeLayout {

  public static final boolean  DEBUG            = true;

  private static final String  TAG              = "StackedViews";

  private double               threshold        = 0.2;

  private int                  duration         = 250;

  private float                mLastMotionX;

  private int                  mActivePointerId;

  private View[]               views;

  private int                  size;

  private int                  current;

  private RelativeLayout       root;

  private boolean              isScrolling;

  private boolean              isScrollingRight;

  private boolean              isPrepared;

  private ScrollerRunner       scroller;

  private int                  topPage;

  private OnPageChangeListener onPageChangeListener;

  private boolean              scrollingByTouch = true;

  public StackedView(Context context) {
    super(context);
    initStackedViews(context, this, 0);
  }

  public StackedView(Context context, int initialPage) {
    super(context);
    initStackedViews(context, this, initialPage);
  }

  public StackedView(Context context, RelativeLayout root) {
    super(context);
    initStackedViews(context, root, 0);
    if (root != this) {
      addView(root);
    }
  }

  public StackedView(Context context, RelativeLayout root, int initialPage) {
    super(context);
    initStackedViews(context, root, initialPage);
    if (root != this) {
      addView(root);
    }
  }

  public StackedView(Context context, AttributeSet attrs) {
    super(context, attrs);
    initStackedViews(context, this, 0);
  }

  public StackedView(Context context, AttributeSet attrs, int initialPage) {
    super(context);
    initStackedViews(context, this, initialPage);
  }

  public StackedView(Context context, AttributeSet attrs, RelativeLayout root) {
    super(context);
    initStackedViews(context, root, 0);
    if (root != this) {
      addView(root);
    }
  }

  public StackedView(Context context, AttributeSet attrs, RelativeLayout root, int initialPage) {
    super(context);
    initStackedViews(context, root, initialPage);
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

  public StackedView setTopPage(int topPage) {
    this.topPage = topPage;
    return this;
  }

  public int getTopPage() {
    return topPage;
  }

  public void setScrollingByTouch(boolean enabled) {
    this.scrollingByTouch = enabled;
  }

  /**
   * Currently only onPageSelected is implemented.
   * 
   * @param onPageChangeListener
   */
  public void setOnPageChangedListener(OnPageChangeListener onPageChangeListener) {
    this.onPageChangeListener = onPageChangeListener;
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
    boolean intercept=super.onInterceptTouchEvent(ev);
    debug("onInterceptTouchEvent: "+intercept);
    this.onTouchEvent(ev);
    return false;
  }

  public void setCurrent(int current){
    this.current=current;
  }
  
  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    boolean callSuper = false;
    if (size == 0) {
      debug("Size == 0");
      return super.onTouchEvent(ev);
    }
    int action = ev.getAction();
    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        debug("Down detected");
        mLastMotionX = ev.getX();
        mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
      }
      case MotionEventCompat.ACTION_POINTER_DOWN: {
        debug("Pointer Down detected");
        callSuper = true;
        final int index = MotionEventCompat.getActionIndex(ev);
        final float x = MotionEventCompat.getX(ev, index);
        mLastMotionX = x;
        mActivePointerId = MotionEventCompat.getPointerId(ev, index);
        break;
      }
      case MotionEvent.ACTION_MOVE: {
        debug("Pointer Move detected.");
        if (!scrollingByTouch) {
          break;
        }
        if ((!isScrolling) && mActivePointerId != -1) {
          // Scroll to follow the motion event
          final int activePointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
          final float x = MotionEventCompat.getX(ev, activePointerIndex);
          fixZzIndex(mLastMotionX - x);
          mLastMotionX = x;
        } else {
          debug("Did not perform move because scrolling :" + isScrolling + " and pointerId: " + mActivePointerId);
        }
        scrollToInternal(false);
        break;
      }
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        debug("Pointer Up or Cancel detected");
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

  @Override
  public void invalidate() {
    initStackedViews(getContext(), root, current);
    super.invalidate();
  }

  @Override
  public void addView(View child) {
    if (root == this) {
      addStackedView(child);
    } else {
      super.addView(child);
    }
  }

  /**
   * Add view to root and reinitialize the view.
   * 
   * @param child
   */
  public void addStackedView(View child) {
    if (root == this) {
      super.addView(child);
    } else {
      root.addView(child);
    }
    initStackedViews(getContext(), getRoot(), current);
  }

  // INTERNAL *************************************

  private void initStackedViews(Context context, RelativeLayout relativeLayout, int initialIndex) {
    if (relativeLayout == null) {
      return;
    }
    int cCount = relativeLayout.getChildCount();
    this.views = new View[cCount];
    for (int i = 0; i < cCount; i++) {
      views[i] = relativeLayout.getChildAt(i);
    }
    size = views.length;
    setInitialViewIndex(initialIndex);
    setIsScrolling(false);
    isPrepared = false;
    root = relativeLayout;
    topPage = -1;
  }

  private void setInitialViewIndex(int n) {
    if (n != 0 && n >= size) {
      throw new IllegalArgumentException("N is greater than the number of views");
    }
    for (int i = 0; i < size; i++) {
      views[i].setVisibility(i == n ? View.VISIBLE : View.GONE);
    }
    current = n;
  }

  private void fixZzIndexLeft(float deltaX) {
    debug("Fix to left, current: " + current);
    RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
    params.leftMargin -= deltaX;
    params.rightMargin += deltaX;
    views[current].setLayoutParams(params);
    if (params.leftMargin < 0) {
      debug("Change to scrolling to right.");
      isScrollingRight = true;
      prepareScrollingToRight();
    }
  }

  private void prepareScrollingToRight() {
    if (topPage == current - 1) {
      // TODO
    }
    debug("Prepared Scrolling To Right");
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
    debug("Fix to right, current: " + current);
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
    debug("Prepared Scrolling To Left");
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
    debug("Moving and Fixing Index.");
    boolean toLeft = deltaX < 0;
    boolean toRight = deltaX > 0;
    if (toRight) {
      if (current >= size - 1) {
        debug("Return because current>size-1");
        return;
      }
    } else {
      if (toLeft && current == 0) {
        debug("Return because current==0");
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
    }
    if (isPrepared && isScrollingRight) {
      fixZzIndexRight(deltaX);
    } else {
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

  private void setIsScrolling(boolean isScrolling) {
    debug("setIsScrolling to " + isScrolling);
    this.isScrolling = isScrolling;
  }

  private synchronized void scrollTo(int index) {
    if (isScrolling) {
      return;
    }
    debug("Scrolling to from current: " + current + " to " + index + " (" + (isScrollingRight ? "Right" : "Left") + ")");
    new Thread(getScroller(index)).start();
  }

  public synchronized ScrollerRunner getScroller(int index) {
    if (scroller == null) {
      scroller = new ScrollerRunner();
    }
    scroller.index = index;
    return scroller;
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

  private static void debug(String msg) {
    Log.i(TAG, msg);
  }

  public class ScrollerRunner implements Runnable {

    public int index;

    @Override
    public synchronized void run() {
      final boolean isRestoring = current == index;
      setIsScrolling(true);
      if (isScrollingRight) {
        if (current + 1 > size - 1) {
          isPrepared = false;
          setIsScrolling(false);
          return;
        }
        debug((isRestoring ? " Restoring " : " Scrolling ") + (isRestoring ? "Right" : "Left") + " Currnet: " + current);

        final RelativeLayout.LayoutParams params = (LayoutParams)views[current + 1].getLayoutParams();

        int totalDistance = isRestoring ? (views[current].getWidth() - params.leftMargin) : (params.leftMargin); // >0

        int distance = Math.abs(totalDistance / (isRestoring ? duration / 3 : duration));

        if (distance == 0) {
          distance = 1;
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
              debug("Set Current Index to " + index);
              current = index;
              if (onPageChangeListener != null) {
                onPageChangeListener.onPageSelected(index);
              }
              RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();
              params.leftMargin = 0;
              params.rightMargin = 0;
              views[current].setLayoutParams(params);
              views[current].bringToFront();
            }
            isPrepared = false;
            setIsScrolling(false);
          }

        });
      } else {
        if (current < 1) {
          isPrepared = false;
          setIsScrolling(false);
          return;
        }

        final RelativeLayout.LayoutParams params = (LayoutParams)views[current].getLayoutParams();

        debug((isRestoring ? "  Restoring " : "  Scrolling ") + (isRestoring ? "Left" : "Right"));

        int totalDistance = isRestoring ? (params.leftMargin) : (views[current - 1].getWidth() - params.leftMargin);

        int distance = Math.abs(totalDistance / (isRestoring ? duration / 3 : duration));
        if (distance == 0) {
          distance = 1;
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
            needsMore = Math.abs(params.leftMargin) < views[current - 1].getWidth();
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
        debug("Finished scrolling.");
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
              debug("Set Current Index to " + index);
              views[current].setVisibility(View.GONE);
              current = index;
              if (onPageChangeListener != null) {
                onPageChangeListener.onPageSelected(index);
              }
              views[current].setVisibility(View.VISIBLE);
              views[current].bringToFront();
            }
            isPrepared = false;
            setIsScrolling(false);
          }

        });
      }
    }
  }
}
