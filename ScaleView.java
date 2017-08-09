package com.capton.sl;
/**
 * Created by capton on 2017/4/18.
 */

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;


/**
 * @描述 实现可以自由移动缩放的图片控件
 * @项目名称 App_imooc
 * @包名 com.android.imooc.imagescale
 * @类名 ScaleView
 * @author chenlin
 * @date 2013年6月10日 下午10:11:11
 * @version 1.0
 */

public class ScaleView extends ImageView implements OnGlobalLayoutListener, OnScaleGestureListener, OnTouchListener {

    /** 表示是否只有一次加载 */
    private boolean isOnce = false;
    /** 初始时的缩放值 */
    private float mInitScale;
    /** 双击时 的缩放值 */
    private float mClickScale;
    /** 最大的缩放值 */
    private float mMaxScale;
    /** 图片缩放矩阵 */
    private Matrix mMatrix;
    /** 图片缩放手势 */
    private ScaleGestureDetector mScaleGesture;

    // ----------------------------自由移动--------------------------------
    /** 可移动最短距离限制，大于这个值时就可移动 */
    private int mTouchSlop;
    /** 是否可以拖动 */
    private boolean isCanDrag;

    // ----------------------------双击放大--------------------------------
    private GestureDetector mGesture;
    // 是否自动缩放
    private boolean isAutoScale;

    public ScaleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleView(Context context) {
        this(context, null);
    }

    public ScaleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // 必须设置才能触发
        this.setOnTouchListener(this);

        mMatrix = new Matrix();
        // 设置缩放模式
        super.setScaleType(ScaleType.MATRIX);

        mScaleGesture = new ScaleGestureDetector(context, this);
        mGesture = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {

                // 如果正在缩放时，不能放大
                if (isAutoScale) {
                    return true;
                }

                float px = e.getX();
                float py = e.getY();
                // 只有小于最大缩放比例才能放大
                float scale = getScale();
                if (scale < mClickScale) {
                    // mMatrix.postScale(mClickScale/scale, mClickScale/scale,
                    // px, py);
                    postDelayed(new ScaleRunnale(px, py, mClickScale), 16);
                    isAutoScale = true;
                } else {
                    // mMatrix.postScale(mInitScale/scale, mInitScale/scale, px,
                    // py);
                    postDelayed(new ScaleRunnale(px, py, mInitScale), 16);
                    isAutoScale = true;
                }
                // setImageMatrix(mMatrix);
                return true;
            }
        });

        /**
         * 是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件。如果小于这个距离就不触发移动控件，如viewpager
         * 就是用这个距离来判断用户是否翻页。
         */
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    private class ScaleRunnale implements Runnable {
        // 放大值
        private static final float BIGGER = 1.08f;
        // 缩小值
        private static final float SMALLER = 0.96f;
        private float x;
        private float y;
        private float mTargetScale;
        private float mTempScale;

        public ScaleRunnale(float x, float y, float mTargetScale) {
            super();
            this.x = x;
            this.y = y;
            this.mTargetScale = mTargetScale;

            if (getScale() < mTargetScale) {
                mTempScale = BIGGER;
            } else if (getScale() > mTargetScale) {
                mTempScale = SMALLER;
            }
        }

        @Override
        public void run() {
            // 先进行缩放
            mMatrix.postScale(mTempScale, mTempScale, x, y);
            checkSideAndCenterWhenScale();
            setImageMatrix(mMatrix);

            float currentScale = getScale();

            // 如果想放大，并且当前的缩放值小于目标值
            if ((mTempScale > 1.0f && currentScale < mTargetScale)
                    || (mTempScale < 1.0f && currentScale > mTargetScale)) {
                // 递归执行run方法
                postDelayed(this, 16);
            } else {
                float scale = mTargetScale / currentScale;
                mMatrix.postScale(scale, scale, x, y);
                checkSideAndCenterWhenScale();
                setImageMatrix(mMatrix);

                isAutoScale = false;
            }
        }

    }

    @Override
    public void onGlobalLayout() {
        // 如果还没有加载图片
        if (!isOnce) {

            // 获得控件的宽高
            int width = getWidth();
            int height = getHeight();

            Drawable drawable = getDrawable();
            if (drawable == null) {
                return;
            }
            // 获得图片的宽高
            int bitmapWidth = drawable.getIntrinsicWidth();
            int bitmapHeight = drawable.getIntrinsicHeight();

            // 设定比例值
            float scale = 0.0f;

            // 如果图片的宽度>控件的宽度，缩小
            if (bitmapWidth > width && bitmapHeight < height) {
                scale = width * 1.0f / bitmapWidth;
            }
            // 如果图片的高度>控件的高度，缩小
            if (bitmapHeight > height && bitmapWidth < width) {
                scale = height * 1.0f / bitmapHeight;
            }
            // 如果图片的宽高度>控件的宽高度，缩小 或者 如果图片的宽高度<控件的宽高度，放大
            if ((bitmapWidth > width && bitmapHeight > height) || (bitmapWidth < width && bitmapHeight < height)) {
                float f1 = width * 1.0f / bitmapWidth;
                float f2 = height * 1.0f / bitmapHeight;
                scale = Math.min(f1, f2);
            }

            // 初始化缩放值
            mInitScale = scale;
            mClickScale = mInitScale * 2;
            mMaxScale = mInitScale * 4;

            // 得到移动的距离
            int dx = width / 2 - bitmapWidth / 2;
            int dy = height / 2 - bitmapHeight / 2;

            // 平移
            mMatrix.postTranslate(dx, dy);

            // 在控件的中心缩放
            mMatrix.postScale(scale, scale, width / 2, height / 2);

            // 设置矩阵
            setImageMatrix(mMatrix);

            // 关于matrix，就是个3*3的矩阵
            /**
             * xscale xskew xtrans yskew yscale ytrans 0 0 0
             */

            isOnce = true;
        }
    }

    /**
     * 注册全局事件
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /**
     * 移除全局事件
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    /**
     * 获得缩放值
     *
     * @return
     */
    public float getScale() {
        /**
         * xscale xskew xtrans yskew yscale ytrans 0 0 0
         */
        float[] values = new float[9];
        mMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        // 如果没有图片，返回
        if (getDrawable() == null) {
            return true;
        }
        // 缩放因子，>0表示正在放大，<0表示正在缩小
        float intentScale = detector.getScaleFactor();
        float scale = getScale();

        // 进行缩放范围的控制
        // 判断，如果<最大缩放值，表示可以放大，如果》最小缩放，说明可以缩小
        if ((scale < mMaxScale && intentScale > 1.0f) || (scale > mInitScale && intentScale < 1.0f)) {

            // scale 变小时， intentScale变小
            if (scale * intentScale < mInitScale) {
                // intentScale * scale = mInitScale ;
                intentScale = mInitScale / scale;
            }

            // scale 变大时， intentScale变大
            if (scale * intentScale > mMaxScale) {
                // intentScale * scale = mMaxScale ;
                intentScale = mMaxScale / scale;
            }

            // 以控件为中心缩放
            // mMatrix.postScale(intentScale, intentScale, getWidth()/2,
            // getHeight()/2);
            // 以手势为中心缩放
            mMatrix.postScale(intentScale, intentScale, detector.getFocusX(), detector.getFocusY());

            // 检测边界与中心点
            checkSideAndCenterWhenScale();

            setImageMatrix(mMatrix);
        }

        return true;
    }

    /**
     * 获得图片缩放后的矩阵
     *
     * @return
     */
    public RectF getMatrixRectF() {
        Matrix matrix = mMatrix;
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            // 初始化矩阵
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            // 移动s
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    private void checkSideAndCenterWhenScale() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0f;
        float deltaY = 0f;
        int width = getWidth();
        int height = getHeight();

        // 情况1， 如果图片的宽度大于控件的宽度
        if (rectF.width() >= width) {
            if (rectF.left > 0) {
                deltaX = -rectF.left;// 如果图片没有左边对齐，就往左边移动
            }
            if (rectF.right < width) {
                deltaX = width - rectF.right;// 如果图片没有右边对齐，就往右边移动
            }
        }
        // 情况2， 如果图片的宽度大于控件的宽度
        if (rectF.height() >= height) {
            if (rectF.top > 0) {
                deltaY = -rectF.top;//
            }
            if (rectF.bottom < height) {
                deltaY = height - rectF.bottom;// 往底部移动
            }
        }

        // 情况3,如图图片在控件内，则让其居中
        if (rectF.width() < width) {
            // deltaX = width/2-rectF.left - rectF.width()/2;
            // 或
            deltaX = width / 2f - rectF.right + rectF.width() / 2f;
        }

        if (rectF.height() < height) {
            deltaY = height / 2f - rectF.bottom + rectF.height() / 2f;
        }

        mMatrix.postTranslate(deltaX, deltaY);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        // TODO Auto-generated method stub

    }

    private float mLastX;
    private float mLastY;
    /** 上次手指的数量 */
    private int mLastPointerCount;

    /** 判断是否检测了x,y轴 */
    private boolean isCheckX;
    private boolean isCheckY;

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        // 把事件传递给双击手势
        if (mGesture.onTouchEvent(event)) {
            return true;
        }
        // 把事件传递给缩放手势
        mScaleGesture.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        x /= pointerCount;
        y /= pointerCount;

        // 说明手指改变
        if (mLastPointerCount != pointerCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointerCount = pointerCount;

        RectF rectF = getMatrixRectF();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rectF.width() > getWidth()) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (rectF.width() > getWidth()) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                /**
                 * 如果能移动
                 */
                if (isCanDrag) {
                    //RectF rectF = getMatrixRectF();
                    if (getDrawable() == null) {
                        return true;
                    }

                    isCheckX = isCheckY = true;

                    // 如果图片在控件内，不允许移动
                    if (rectF.width() < getWidth()) {
                        isCheckX = false;
                        dx = 0f;
                    }
                    if (rectF.height() < getHeight()) {
                        isCheckY = false;
                        dy = 0f;
                    }

                    mMatrix.postTranslate(dx, dy);

                    // 移动事检测边界
                    checkSideAndCenterWhenTransate();

                    setImageMatrix(mMatrix);
                }

                mLastX = x;
                mLastY = y;

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 清楚手指
                mLastPointerCount = 0;

                break;
        }

        return true;
    }

    private void checkSideAndCenterWhenTransate() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0f;
        float deltaY = 0f;
        int width = getWidth();
        int height = getHeight();

        if (rectF.top > 0 && isCheckY) {
            deltaY = -rectF.top;// 往上边移动
        }
        if (rectF.bottom < height && isCheckY) {
            deltaY = height - rectF.bottom;// 往底部移动
        }

        if (rectF.left > 0 && isCheckX) {
            deltaX = -rectF.left;// 往左边移动
        }
        if (rectF.right < width && isCheckX) {
            deltaX = width - rectF.right;// 往右边移动
        }
        // 移动
        mMatrix.postTranslate(deltaX, deltaY);
    }

    private boolean isMoveAction(float dx, float dy) {
        // 求得两点的距离
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
    }

}
