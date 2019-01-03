package com.project.stephencao.imagemagnifier.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.*;

public class ZoomImageView extends android.support.v7.widget.AppCompatImageView
        implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {
    // 控制初始化比例 只执行一次
    private boolean mOnce;

    // 初始化时scale的值
    private float mInitScale;

    //双击放大时到达的值
    private float mMidScale;

    // 放大的极限值
    private float mMaxScale;

    // 捕获 用户多点触控时 缩放的比例
    private ScaleGestureDetector mScaleGestureDetector;

    private Matrix mScaleMatrix;

    //------------------自由移动--------------------
    //记录上次多点触控的数量
    private int mLastPointerCount;

    private float mLastX;

    private float mLastY;

    private int mTouchSlop;

    private boolean mIsCanDrag;

    private boolean shouldCheckLeftAndRight;

    private boolean shouldCheckTopAndBottom;

    //---------------双击放大缩小-------------------
    private GestureDetector mGestureDetector;

    private boolean isAutoScaling;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaleMatrix = new Matrix();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isAutoScaling) {
                    return true;
                }
                float x = e.getX();
                float y = e.getY();
                if (getScale() < mMidScale) {
//                    mScaleMatrix.postScale(mMidScale / getScale(), mMidScale / getScale(), x, y);
                    postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
                    isAutoScaling = true;
                } else {
//                    mScaleMatrix.postScale(mInitScale / getScale(), mInitScale / getScale(), x, y);
                    postDelayed(new AutoScaleRunnable(mInitScale, x, y), 16);
                    isAutoScaling = true;
                }
                return true;
            }
        });
    }

    //自动放大缩小
    private class AutoScaleRunnable implements Runnable {
        // 缩放目标值
        private float mTargetScale;
        //缩放中心
        private float x;
        private float y;
        private final float BIGGER = 1.07f;
        private final float SMALLER = 0.93f;

        private float tempScale;

        public AutoScaleRunnable(float mTargetScale, float x, float y) {
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;

            if (getScale() < mTargetScale) {
                tempScale = BIGGER;
            }
            if (getScale() > mTargetScale) {
                tempScale = SMALLER;
            }
        }

        @Override
        public void run() {
            // 进行缩放
            mScaleMatrix.postScale(tempScale, tempScale, x, y);
            checkBoarderAndCentreWhenScaling();
            setImageMatrix(mScaleMatrix);
            float currentScale = getScale();
            if ((tempScale > 1.0f && currentScale < mTargetScale) || (tempScale < 1.0f && currentScale > mTargetScale)) {
                postDelayed(this, 16);
            } else {
                float scale = mTargetScale / currentScale;
                mScaleMatrix.postScale(scale, scale, x, y);
                checkBoarderAndCentreWhenScaling();
                setImageMatrix(mScaleMatrix);
                isAutoScaling = false;
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        if (!mOnce) {
            // 得到控件的宽高
            int width = getWidth();
            int height = getHeight();

            // 得到图片以及宽和高
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return;
            }
            // 图片的宽高
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();
            float scale = 1.0f;
            if (intrinsicWidth > width && intrinsicHeight <= height) {
                scale = width * 1.0f / intrinsicWidth;
            }
            if (intrinsicHeight > height && intrinsicWidth <= width) {
                scale = height * 1.0f / intrinsicHeight;
            }
            if ((intrinsicHeight > height && intrinsicWidth > width) || (intrinsicHeight < height && intrinsicWidth < width)) {
                scale = (float) Math.min(height * 1.0 / intrinsicHeight, width * 1.0 / intrinsicWidth);
            }
            // 初始化图片比例
            mInitScale = scale;
            mMaxScale = mInitScale * 4;
            mMidScale = mInitScale * 2;

            // 将图片移动到当前控件中心
            int dx = width / 2 - intrinsicWidth / 2;
            int dy = height / 2 - intrinsicHeight / 2;

            mScaleMatrix.postTranslate(dx, dy);
            mScaleMatrix.postScale(mInitScale, mInitScale, width * 1.0f / 2, height * 1.0f / 2);
            setImageMatrix(mScaleMatrix);
            mOnce = true;
        }
    }

    // 获取当前图片缩放值
    public float getScale() {
        float[] values = new float[9];
        mScaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    //获取image view 加载完成的图片
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();
        float scaleFactor = detector.getScaleFactor();
        if (getDrawable() == null) {
            return true;
        }
        if ((scale < mMaxScale && scaleFactor > 1.0f) || (scale > mInitScale && scaleFactor < 1.0f)) {
            if (scale * scaleFactor < mInitScale) {
                scaleFactor = mInitScale / scale; //重置scale factor
            }
            if (scale * scaleFactor > mMaxScale) {
                scaleFactor = mMaxScale / scale; //重置scale factor
            }

            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            checkBoarderAndCentreWhenScaling();
            setImageMatrix(mScaleMatrix);
        }

        return true;
    }

    //得到图片的四个点坐标
    private RectF getMatrixRectF() {
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            // 矩阵的坐标变换
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    //缩放时 进行边界和位置的控制
    private void checkBoarderAndCentreWhenScaling() {
        RectF matrixRectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (matrixRectF.width() >= width) {
            // 右边出界
            if (matrixRectF.left > 0) {
                deltaX = -matrixRectF.left;
            }
            // 左边出界
            if (matrixRectF.right < width) {
                deltaX = width - matrixRectF.right;
            }
        }
        if (matrixRectF.height() >= height) {
            // 下边出界
            if (matrixRectF.top > 0) {
                deltaY = -matrixRectF.top;
            }
            // 上边出界
            if (matrixRectF.bottom < height) {
                deltaY = height - matrixRectF.bottom;
            }
        }
        //如果宽度或高度小于屏幕宽高时，使其居中
        if (matrixRectF.width() < width) {
            deltaX = width * 1.0f / 2 - matrixRectF.right + matrixRectF.width() / 2.0f;
        }
        if (matrixRectF.height() < height) {
            deltaY = height * 1.0f / 2 - matrixRectF.bottom + matrixRectF.height() / 2.0f;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }
        // 把event 传给scale gesture detector处理
        mScaleGestureDetector.onTouchEvent(event);

        // 记录多点触控的中心点
        float x = 0;
        float y = 0;
        // 多点触控的数量
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;

        if (mLastPointerCount != pointerCount) {
            mIsCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;
        RectF matrixRectF = getMatrixRectF();
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                if (getParent() instanceof ViewPager) {
                    if ((matrixRectF.width() > getWidth() + 0.01) || (matrixRectF.height() > getHeight() + 0.01)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                float dx = x - mLastX;
                float dy = y - mLastY;
                if (!mIsCanDrag) {
                    mIsCanDrag = isCanDrag(dx, dy);
                }
                if (mIsCanDrag) {
                    if (getDrawable() != null) {
                        shouldCheckLeftAndRight = shouldCheckTopAndBottom = true;
                        // 如果图片宽度小于控件宽度，不能左右移动
                        if (matrixRectF.width() < getWidth()) {
                            shouldCheckLeftAndRight = false;
                            dx = 0;
                        }
                        if (matrixRectF.height() < getHeight()) {
                            shouldCheckTopAndBottom = false;
                            dy = 0;
                        }
                        mScaleMatrix.postTranslate(dx, dy);
                        checkBoarderWhenTranslating();
                        setImageMatrix(mScaleMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            }
            // 拦截viewpager的滑动事件
            case MotionEvent.ACTION_DOWN: {
                if (getParent() instanceof ViewPager) {
                    if ((matrixRectF.width() > getWidth() + 0.01) || (matrixRectF.height() > getHeight() + 0.01)) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                mLastPointerCount = 0;
                break;
            }
        }
        return true;
    }

    /**
     * 移动时边界检查
     */
    private void checkBoarderWhenTranslating() {

        RectF matrixRectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;
        int width = getWidth();
        int height = getHeight();
        //下边出界
        if (matrixRectF.top > 0 && shouldCheckTopAndBottom) {
            deltaY = -matrixRectF.top;
        }
        if (matrixRectF.bottom < height && shouldCheckTopAndBottom) {
            deltaY = height - matrixRectF.bottom;
        }
        if (matrixRectF.left > 0 && shouldCheckLeftAndRight) {
            deltaX = -matrixRectF.left;
        }
        if (matrixRectF.right < width && shouldCheckLeftAndRight) {
            deltaX = width - matrixRectF.right;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);
    }

    // 判断是否移动了足够大的距离
    private boolean isCanDrag(float dx, float dy) {
        return Math.sqrt((dx * dx) + (dy * dy)) >= mTouchSlop;
    }
}
