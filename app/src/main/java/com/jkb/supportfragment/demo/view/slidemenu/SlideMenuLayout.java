package com.jkb.supportfragment.demo.view.slidemenu;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

import com.jkb.support.utils.LogUtils;
import com.jkb.supportfragment.demo.R;

/**
 * 侧滑菜单布局
 * Created by JustKiddingBaby on 2017/6/3.
 */

public class SlideMenuLayout extends ViewGroup implements SlideMenuAction {

    private View mLeftView, mRightView, mContentView;
    //attrs
    private int mSlideMode;
    private int mSlidePadding;
    //data
    private int screenWidth;
    private int screenHeight;
    private int mSlideWidth;
    private int mContentWidth;
    private int mContentHeight;
    //slide
    private Scroller mScroller;
    private int mLastX;
    private boolean mTriggerSlideLeft;
    private boolean mTriggerSlideRight;
    private static final int TIME_SLIDE = 1000;

    public SlideMenuLayout(Context context) {
        this(context, null);
    }

    public SlideMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public SlideMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        screenWidth = getScreenWidth(context);
        screenHeight = getScreenHeight(context);
        initAttrs(attrs);//初始化属性
        mScroller = new Scroller(context);
    }

    /**
     * 初始化属性
     */
    private void initAttrs(AttributeSet attrs) {
        TypedArray ta = getContext().obtainStyledAttributes(attrs, R.styleable.SlideMenuLayout);
        mSlideMode = ta.getInteger(R.styleable.SlideMenuLayout_slideMode, SLIDE_MODE_NONE);
        mSlidePadding = (int) ta.getDimension(R.styleable.SlideMenuLayout_slidePadding, screenWidth / 4);
        ta.recycle();
    }

    @Override
    public void addView(View child) {
        if (getChildCount() > 3) {
            throw new IllegalStateException("SlideMenuLayout can host only one direct child");
        }
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (getChildCount() > 3) {
            throw new IllegalStateException("SlideMenuLayout can host only one direct child");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (getChildCount() > 3) {
            throw new IllegalStateException("SlideMenuLayout can host only one direct child");
        }
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 3) {
            throw new IllegalStateException("SlideMenuLayout can host only one direct child");
        }
        super.addView(child, index, params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //默认全屏
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthResult = 0;
        if (widthMode == MeasureSpec.EXACTLY) {
            widthResult = widthSize;
        } else {
            widthResult = screenWidth;
        }
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightResult = 0;
        if (heightMode == MeasureSpec.EXACTLY) {
            heightResult = heightSize;
        } else {
            heightResult = screenHeight;
        }
        //初始化侧滑菜单
        initSlideView(widthResult, heightResult);

        measureSlideChild(mContentView, widthMeasureSpec, heightMeasureSpec);
        measureSlideChild(mLeftView, widthMeasureSpec, heightMeasureSpec);
        measureSlideChild(mRightView, widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(mContentWidth, mContentHeight);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mLeftView != null) {
            mLeftView.layout(-mSlideWidth, 0, 0, mContentHeight);
        }
        if (mContentView != null) {
            mContentView.layout(0, 0, mContentWidth, mContentHeight);
        }
        if (mRightView != null) {
            mRightView.layout(mContentWidth, 0, mContentWidth + mSlideWidth, mContentHeight);
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = (int) event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                int currentX = (int) event.getX();
                //拿到x方向的偏移量
                int dx = currentX - mLastX;
                if (dx < 0) {//向左滑动
                    scrollLeft(dx);
                } else {//向右滑动
                    scrollRight(dx);
                }
                mLastX = currentX;
                break;
            case MotionEvent.ACTION_UP:
                if (getScrollX() < 0) {//右滑
                    inertiaScrollRight();
                } else {
                    inertiaScrollLeft();
                }
                break;
        }
        return true;
    }

    /**
     * 惯性左滑
     */
    private void inertiaScrollLeft() {
        if (mSlideMode == SLIDE_MODE_RIGHT || mSlideMode == SLIDE_MODE_LEFT_RIGHT) {
            if (getScrollX() >= mSlideWidth / 2) {
                openRightSlide();
            } else {
                if (!mTriggerSlideRight) {
                    closeRightSlide();
                }
            }
        } else if (mSlideMode == SLIDE_MODE_LEFT) {
            if (-getScrollX() >= mSlideWidth / 2) {
                closeLeftSlide();
            } else {
                if (mTriggerSlideLeft) {
                    openLeftSlide();
                }
            }
        }
    }

    /**
     * 惯性右滑
     */
    private void inertiaScrollRight() {
        if (mSlideMode == SLIDE_MODE_LEFT || mSlideMode == SLIDE_MODE_LEFT_RIGHT) {
            if (-getScrollX() >= mSlideWidth / 2) {
                openLeftSlide();
            } else {
                if (!mTriggerSlideLeft) {
                    closeLeftSlide();
                }
            }
        } else if (mSlideMode == SLIDE_MODE_RIGHT) {
            if (getScrollX() <= mSlideWidth / 2) {
                closeRightSlide();
            } else {
                if (mTriggerSlideRight) {
                    openRightSlide();
                }
            }
        }
    }

    /**
     * 向左滑动
     */
    private void scrollLeft(int dx) {
        if (mSlideMode == SLIDE_MODE_LEFT_RIGHT || mSlideMode == SLIDE_MODE_RIGHT) {
            //右滑菜单已经打开，不做操作
            if (mTriggerSlideRight || getScrollX() - dx >= mSlideWidth) {
                openRightSlide();
                return;
            }
        } else if (mSlideMode == SLIDE_MODE_LEFT) {
            //右滑菜单未打开，不做操作
            if (!mTriggerSlideLeft || getScrollX() - dx >= 0) {
                closeLeftSlide();
                return;
            }
        }
        scrollBy(-dx, 0);
    }

    /**
     * 向右滑动
     */
    private void scrollRight(int dx) {
        if (mSlideMode == SLIDE_MODE_LEFT_RIGHT || mSlideMode == SLIDE_MODE_LEFT) {
            //左滑菜单已经打开，不做操作
            if (mTriggerSlideLeft || getScrollX() - dx <= -mSlideWidth) {
                openLeftSlide();
                return;
            }
        } else if (mSlideMode == SLIDE_MODE_RIGHT) {
            //右滑菜单未打开，不做操作
            if (!mTriggerSlideRight || getScrollX() - dx <= 0) {
                closeRightSlide();
                return;
            }
        }
        scrollBy(-dx, 0);
    }

    /**
     * 缓慢滑动
     */
    private void smoothScrollTo(int destX, int destY) {
        int scrollX = getScrollX();
        int deltaX = destX - scrollX;
        float time = deltaX * 1.0f / (mSlideWidth * 1.0f / TIME_SLIDE);
        mScroller.startScroll(scrollX, 0, deltaX, destY, (int) time);
        invalidate();
    }

    /**
     * 测量子视图
     */
    private void measureSlideChild(View childView, int widthMeasureSpec, int heightMeasureSpec) {
        if (childView == null) return;
        LayoutParams lp = childView.getLayoutParams();
        int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                getPaddingLeft() + getPaddingRight(), lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                getPaddingTop() + getPaddingBottom(), lp.height);
        childView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    /**
     * 初始化SlideMenu的视图
     */
    private void initSlideView(int widthResult, int heightResult) {
        if (getChildCount() == 0) {
            throw new IllegalStateException("SlideMenuLayout must host only one direct child");
        }
        mSlideWidth = widthResult - mSlidePadding;
        mContentWidth = widthResult;
        mContentHeight = heightResult;
        switch (getChildCount()) {
            case 1:
                mSlideMode = SLIDE_MODE_NONE;
                mContentView = getChildAt(0);
                break;
            case 2:
                mLeftView = getChildAt(0);
                mContentView = getChildAt(1);
                break;
            case 3:
                mLeftView = getChildAt(0);
                mContentView = getChildAt(1);
                mRightView = getChildAt(2);
                break;
        }
        if (mLeftView != null) {
            mLeftView.getLayoutParams().width = mSlideWidth;
        }
        if (mRightView != null) {
            mRightView.getLayoutParams().width = mSlideWidth;
        }

        LayoutParams contentParams = mContentView.getLayoutParams();
        contentParams.width = widthResult;
        contentParams.height = heightResult;
    }

    /**
     * 获取手机屏幕的高度
     */
    static int getScreenHeight(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    /**
     * 获取手机屏幕的宽度
     */
    static int getScreenWidth(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    @Override
    public void setSlideMode(int slideMode) {
        mSlideMode = slideMode;
    }

    @Override
    public void setSlidePadding(int slidePadding) {
        mSlidePadding = slidePadding;
    }

    @Override
    public View getSlideLeftView() {
        return mLeftView;
    }

    @Override
    public View getSlideRightView() {
        return mRightView;
    }

    @Override
    public View getSlideContentView() {
        return mContentView;
    }

    @Override
    public void toggleLeftSlide() {
        if (mTriggerSlideLeft) {
            closeLeftSlide();
        } else {
            openLeftSlide();
        }
    }

    @Override
    public void openLeftSlide() {
        if (mSlideMode == SLIDE_MODE_RIGHT) return;
        mTriggerSlideLeft = true;
        smoothScrollTo(-mSlideWidth, 0);
    }

    @Override
    public void closeLeftSlide() {
        if (mSlideMode == SLIDE_MODE_RIGHT) return;
        mTriggerSlideLeft = false;
        smoothScrollTo(0, 0);
    }

    @Override
    public void toggleRightSlide() {
        if (mTriggerSlideRight) {
            closeRightSlide();
        } else {
            openRightSlide();
        }
    }

    @Override
    public void openRightSlide() {
        if (mSlideMode == SLIDE_MODE_LEFT) return;
        mTriggerSlideRight = true;
        smoothScrollTo(mSlideWidth, 0);
    }

    @Override
    public void closeRightSlide() {
        if (mSlideMode == SLIDE_MODE_LEFT) return;
        mTriggerSlideRight = false;
        smoothScrollTo(0, 0);
    }
}