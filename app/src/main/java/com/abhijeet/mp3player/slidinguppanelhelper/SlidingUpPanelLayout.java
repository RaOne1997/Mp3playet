package com.abhijeet.mp3player.slidinguppanelhelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;

import com.abhijeet.mp3player.R;

public class SlidingUpPanelLayout extends ViewGroup {

	private static final String TAG = SlidingUpPanelLayout.class.getSimpleName();


	private static final int DEFAULT_PANEL_HEIGHT = 68; // dp;


	private static final float DEFAULT_ANCHOR_POINT = 1.0f; // In relative %


	private static PanelState DEFAULT_SLIDE_STATE = PanelState.COLLAPSED;


	private static final int DEFAULT_SHADOW_HEIGHT = 4; // dp;


	private static final int DEFAULT_FADE_COLOR = 0x99000000;


	private static final int DEFAULT_MIN_FLING_VELOCITY = 400;
	private static final boolean DEFAULT_OVERLAY_FLAG = false;

	private static final boolean DEFAULT_CLIP_PANEL_FLAG = true;

	private static final int[] DEFAULT_ATTRS = new int[] { android.R.attr.gravity };


	private int mMinFlingVelocity = DEFAULT_MIN_FLING_VELOCITY;


	private int mCoveredFadeColor = DEFAULT_FADE_COLOR;


	private static final int DEFAULT_PARALAX_OFFSET = 0;


	private final Paint mCoveredFadePaint = new Paint();


	private final Drawable mShadowDrawable;


	private int mPanelHeight = -1;

	private int mShadowHeight = -1;


	private int mParallaxOffset = -1;


	private boolean mIsSlidingUp;


	private boolean mOverlayContent = DEFAULT_OVERLAY_FLAG;


	private boolean mClipPanel = DEFAULT_CLIP_PANEL_FLAG;


	private View mDragView;


	private int mDragViewResId = -1;

	private View mSlideableView;


	private View mMainView;


	public enum PanelState {
		EXPANDED, COLLAPSED, ANCHORED, HIDDEN, DRAGGING
	}

	private PanelState mSlideState = DEFAULT_SLIDE_STATE;

	private PanelState mLastNotDraggingSlideState = null;

	private float mSlideOffset;


	private int mSlideRange;


	private boolean mIsUnableToDrag;


	private boolean mIsTouchEnabled;


	private boolean mIsUsingDragViewTouchEvents;

	private float mInitialMotionX;
	private float mInitialMotionY;
	private float mAnchorPoint = 1.f;

	private PanelSlideListener mPanelSlideListener;

	private final ViewDragHelper mDragHelper;


	private boolean mFirstLayout = true;

	private final Rect mTmpRect = new Rect();


	public interface PanelSlideListener {

		public void onPanelSlide(View panel, float slideOffset);

		public void onPanelCollapsed(View panel);

		public void onPanelExpanded(View panel);

		public void onPanelAnchored(View panel);

		public void onPanelHidden(View panel);
	}


	public static class SimplePanelSlideListener implements PanelSlideListener {
		@Override
		public void onPanelSlide(View panel, float slideOffset) {
		}

		@Override
		public void onPanelCollapsed(View panel) {
		}

		@Override
		public void onPanelExpanded(View panel) {
		}

		@Override
		public void onPanelAnchored(View panel) {
		}

		@Override
		public void onPanelHidden(View panel) {
		}
	}

	public SlidingUpPanelLayout(Context context) {
		this(context, null);
	}

	public SlidingUpPanelLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SlidingUpPanelLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		if (isInEditMode()) {
			mShadowDrawable = null;
			mDragHelper = null;
			return;
		}

		if (attrs != null) {
			TypedArray defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS);

			if (defAttrs != null) {
				int gravity = defAttrs.getInt(0, Gravity.NO_GRAVITY);
				setGravity(gravity);
			}

			defAttrs.recycle();

			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpPanelLayout);

			if (ta != null) {
				mPanelHeight = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_panelHeight, -1);
				mShadowHeight = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_shadowHeight, -1);
				mParallaxOffset = ta.getDimensionPixelSize(R.styleable.SlidingUpPanelLayout_parallaxOffset, -1);

				mMinFlingVelocity = ta.getInt(R.styleable.SlidingUpPanelLayout_flingVelocity, DEFAULT_MIN_FLING_VELOCITY);
				mCoveredFadeColor = ta.getColor(R.styleable.SlidingUpPanelLayout_fadeColor, DEFAULT_FADE_COLOR);

				mDragViewResId = ta.getResourceId(R.styleable.SlidingUpPanelLayout_dragView, -1);

				mOverlayContent = ta.getBoolean(R.styleable.SlidingUpPanelLayout_overlay, DEFAULT_OVERLAY_FLAG);
				mClipPanel = ta.getBoolean(R.styleable.SlidingUpPanelLayout_clipPanel, DEFAULT_CLIP_PANEL_FLAG);

				mAnchorPoint = ta.getFloat(R.styleable.SlidingUpPanelLayout_anchorPoint, DEFAULT_ANCHOR_POINT);

				mSlideState = PanelState.values()[ta.getInt(R.styleable.SlidingUpPanelLayout_initialState, DEFAULT_SLIDE_STATE.ordinal())];
			}

			ta.recycle();
		}

		final float density = context.getResources().getDisplayMetrics().density;
		if (mPanelHeight == -1) {
			mPanelHeight = (int) (DEFAULT_PANEL_HEIGHT * density + 0.5f);
		}
		if (mShadowHeight == -1) {
			mShadowHeight = (int) (DEFAULT_SHADOW_HEIGHT * density + 0.5f);
		}
		if (mParallaxOffset == -1) {
			mParallaxOffset = (int) (DEFAULT_PARALAX_OFFSET * density);
		}

		if (mShadowHeight > 0) {
			if (mIsSlidingUp) {
				mShadowDrawable = getResources().getDrawable(R.drawable.above_shadow);
			} else {
				mShadowDrawable = getResources().getDrawable(R.drawable.below_shadow);
			}

		} else {
			mShadowDrawable = null;
		}

		setWillNotDraw(false);

		mDragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
		mDragHelper.setMinVelocity(mMinFlingVelocity * density);

		mIsTouchEnabled = true;
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		if (mDragViewResId != -1) {
			setDragView(findViewById(mDragViewResId));
		}
	}

	public void setGravity(int gravity) {
		if (gravity != Gravity.TOP && gravity != Gravity.BOTTOM) {
			throw new IllegalArgumentException("gravity must be set to either top or bottom");
		}
		mIsSlidingUp = gravity == Gravity.BOTTOM;
		if (!mFirstLayout) {
			requestLayout();
		}
	}

	public void setCoveredFadeColor(int color) {
		mCoveredFadeColor = color;
		invalidate();
	}

	public int getCoveredFadeColor() {
		return mCoveredFadeColor;
	}

	public void setTouchEnabled(boolean enabled) {
		mIsTouchEnabled = enabled;
	}

	public boolean isTouchEnabled() {
		return mIsTouchEnabled && mSlideableView != null && mSlideState != PanelState.HIDDEN;
	}

	public void setPanelHeight(int val) {
		if (getPanelHeight() == val) {
			return;
		}

		mPanelHeight = val;
		if (!mFirstLayout) {
			requestLayout();
		}

		if (getPanelState() == PanelState.COLLAPSED) {
			smoothToBottom();
			invalidate();
			return;
		}
	}

	protected void smoothToBottom() {
		smoothSlideTo(0, 0);
	}

	public int getShadowHeight() {
		return mShadowHeight;
	}

	public void setShadowHeight(int val) {
		mShadowHeight = val;
		if (!mFirstLayout) {
			invalidate();
		}
	}


	public int getPanelHeight() {
		return mPanelHeight;
	}

	public int getCurrentParalaxOffset() {
		int offset = (int) (mParallaxOffset * Math.max(mSlideOffset, 0));
		return mIsSlidingUp ? -offset : offset;
	}

	public void setParalaxOffset(int val) {
		mParallaxOffset = val;
		if (!mFirstLayout) {
			requestLayout();
		}
	}

	public int getMinFlingVelocity() {
		return mMinFlingVelocity;
	}

	public void setMinFlingVelocity(int val) {
		mMinFlingVelocity = val;
	}

	public void setPanelSlideListener(PanelSlideListener listener) {
		mPanelSlideListener = listener;
	}

	public void setDragView(View dragView) {
		if (mDragView != null) {
			mDragView.setOnClickListener(null);
		}
		mDragView = dragView;
		if (mDragView != null) {
			mDragView.setClickable(true);
			mDragView.setFocusable(false);
			mDragView.setFocusableInTouchMode(false);
			mDragView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!isEnabled() || !isTouchEnabled())
						return;
					if (mSlideState != PanelState.EXPANDED && mSlideState != PanelState.ANCHORED) {
						if (mAnchorPoint < 1.0f) {
							setPanelState(PanelState.ANCHORED);
						} else {
							setPanelState(PanelState.EXPANDED);
						}
					} else {
						setPanelState(PanelState.COLLAPSED);
					}
				}
			});
			;
		}
	}

	public void setDragView(int dragViewResId) {
		mDragViewResId = dragViewResId;
		setDragView(findViewById(dragViewResId));
	}

	public void setAnchorPoint(float anchorPoint) {
		if (anchorPoint > 0 && anchorPoint <= 1) {
			mAnchorPoint = anchorPoint;
		}
	}


	public float getAnchorPoint() {
		return mAnchorPoint;
	}


	public void setOverlayed(boolean overlayed) {
		mOverlayContent = overlayed;
	}


	public boolean isOverlayed() {
		return mOverlayContent;
	}


	public void setClipPanel(boolean clip) {
		mClipPanel = clip;
	}


	public boolean isClipPanel() {
		return mClipPanel;
	}

	void dispatchOnPanelSlide(View panel) {
		if (mPanelSlideListener != null) {
			mPanelSlideListener.onPanelSlide(panel, mSlideOffset);
		}
	}

	void dispatchOnPanelExpanded(View panel) {
		if (mPanelSlideListener != null) {
			mPanelSlideListener.onPanelExpanded(panel);
		}
		sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
	}

	void dispatchOnPanelCollapsed(View panel) {
		if (mPanelSlideListener != null) {
			mPanelSlideListener.onPanelCollapsed(panel);
		}
		sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
	}

	void dispatchOnPanelAnchored(View panel) {
		if (mPanelSlideListener != null) {
			mPanelSlideListener.onPanelAnchored(panel);
		}
		sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
	}

	void dispatchOnPanelHidden(View panel) {
		if (mPanelSlideListener != null) {
			mPanelSlideListener.onPanelHidden(panel);
		}
		sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
	}

	void updateObscuredViewVisibility() {
		if (getChildCount() == 0) {
			return;
		}
		final int leftBound = getPaddingLeft();
		final int rightBound = getWidth() - getPaddingRight();
		final int topBound = getPaddingTop();
		final int bottomBound = getHeight() - getPaddingBottom();
		final int left;
		final int right;
		final int top;
		final int bottom;
		if (mSlideableView != null && hasOpaqueBackground(mSlideableView)) {
			left = mSlideableView.getLeft();
			right = mSlideableView.getRight();
			top = mSlideableView.getTop();
			bottom = mSlideableView.getBottom();
		} else {
			left = right = top = bottom = 0;
		}
		View child = getChildAt(0);
		final int clampedChildLeft = Math.max(leftBound, child.getLeft());
		final int clampedChildTop = Math.max(topBound, child.getTop());
		final int clampedChildRight = Math.min(rightBound, child.getRight());
		final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
		final int vis;
		if (clampedChildLeft >= left && clampedChildTop >= top && clampedChildRight <= right && clampedChildBottom <= bottom) {
			vis = INVISIBLE;
		} else {
			vis = VISIBLE;
		}
		child.setVisibility(vis);
	}

	void setAllChildrenVisible() {
		for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() == INVISIBLE) {
				child.setVisibility(VISIBLE);
			}
		}
	}

	private static boolean hasOpaqueBackground(View v) {
		final Drawable bg = v.getBackground();
		return bg != null && bg.getOpacity() == PixelFormat.OPAQUE;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mFirstLayout = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mFirstLayout = true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		final int heightSize = MeasureSpec.getSize(heightMeasureSpec);



		final int childCount = getChildCount();

		if (childCount != 2) {
			throw new IllegalStateException("Sliding up panel layout must have exactly 2 children!");
		}

		mMainView = getChildAt(0);
		mSlideableView = getChildAt(1);
		if (mDragView == null) {
			setDragView(mSlideableView);
		}


		if (mSlideableView.getVisibility() != VISIBLE) {
			mSlideState = PanelState.HIDDEN;
		}

		int layoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
		int layoutWidth = widthSize - getPaddingLeft() - getPaddingRight();


		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();


			if (child.getVisibility() == GONE && i == 0) {
				continue;
			}

			int height = layoutHeight;
			int width = layoutWidth;
			if (child == mMainView) {
				if (!mOverlayContent && mSlideState != PanelState.HIDDEN) {
					height -= mPanelHeight;
				}

				width -= lp.leftMargin + lp.rightMargin;
			} else if (child == mSlideableView) {

				height -= lp.topMargin;
			}

			int childWidthSpec;
			if (lp.width == LayoutParams.WRAP_CONTENT) {
				childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST);
			} else if (lp.width == LayoutParams.MATCH_PARENT) {
				childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
			} else {
				childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
			}

			int childHeightSpec;
			if (lp.height == LayoutParams.WRAP_CONTENT) {
				childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST);
			} else if (lp.height == LayoutParams.MATCH_PARENT) {
				childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
			} else {
				childHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
			}

			child.measure(childWidthSpec, childHeightSpec);

			if (child == mSlideableView) {
				mSlideRange = mSlideableView.getMeasuredHeight() - mPanelHeight;
			}
		}

		setMeasuredDimension(widthSize, heightSize);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int paddingLeft = getPaddingLeft();
		final int paddingTop = getPaddingTop();

		final int childCount = getChildCount();

		if (mFirstLayout) {
			switch (mSlideState) {
			case EXPANDED:
				mSlideOffset = 1.0f;
				break;
			case ANCHORED:
				mSlideOffset = mAnchorPoint;
				break;
			case HIDDEN:
				int newTop = computePanelTopPosition(0.0f) + (mIsSlidingUp ? +mPanelHeight : -mPanelHeight);
				mSlideOffset = computeSlideOffset(newTop);
				break;
			default:
				mSlideOffset = 0.f;
				break;
			}
		}

		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();

			// Always layout the sliding view on the first layout
			if (child.getVisibility() == GONE && (i == 0 || mFirstLayout)) {
				continue;
			}

			final int childHeight = child.getMeasuredHeight();
			int childTop = paddingTop;

			if (child == mSlideableView) {
				childTop = computePanelTopPosition(mSlideOffset);
			}

			if (!mIsSlidingUp) {
				if (child == mMainView && !mOverlayContent) {
					childTop = computePanelTopPosition(mSlideOffset) + mSlideableView.getMeasuredHeight();
				}
			}
			final int childBottom = childTop + childHeight;
			final int childLeft = paddingLeft + lp.leftMargin;
			final int childRight = childLeft + child.getMeasuredWidth();

			child.layout(childLeft, childTop, childRight, childBottom);
		}

		if (mFirstLayout) {
			updateObscuredViewVisibility();
		}
		applyParallaxForCurrentSlideOffset();

		mFirstLayout = false;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		if (h != oldh) {
			mFirstLayout = true;
		}
	}

	public void setEnableDragViewTouchEvents(boolean enabled) {
		mIsUsingDragViewTouchEvents = enabled;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		final int action = MotionEventCompat.getActionMasked(ev);

		if (!isEnabled() || !isTouchEnabled() || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
			mDragHelper.cancel();
			return super.onInterceptTouchEvent(ev);
		}

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			mDragHelper.cancel();
			return false;
		}

		final float x = ev.getX();
		final float y = ev.getY();

		switch (action) {
		case MotionEvent.ACTION_DOWN: {
			mIsUnableToDrag = false;
			mInitialMotionX = x;
			mInitialMotionY = y;
			break;
		}

		case MotionEvent.ACTION_MOVE: {
			final float adx = Math.abs(x - mInitialMotionX);
			final float ady = Math.abs(y - mInitialMotionY);
			final int dragSlop = mDragHelper.getTouchSlop();

			if (mIsUsingDragViewTouchEvents && adx > dragSlop && ady < dragSlop) {
				return super.onInterceptTouchEvent(ev);
			}

			if ((ady > dragSlop && adx > ady) || !isDragViewUnder((int) mInitialMotionX, (int) mInitialMotionY)) {
				mDragHelper.cancel();
				mIsUnableToDrag = true;
				return false;
			}
			break;
		}
		}

		return mDragHelper.shouldInterceptTouchEvent(ev);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!isEnabled() || !isTouchEnabled()) {
			return super.onTouchEvent(ev);
		}
		mDragHelper.processTouchEvent(ev);
		return true;
	}

	private boolean isDragViewUnder(int x, int y) {
		if (mDragView == null)
			return false;
		int[] viewLocation = new int[2];
		mDragView.getLocationOnScreen(viewLocation);
		int[] parentLocation = new int[2];
		this.getLocationOnScreen(parentLocation);
		int screenX = parentLocation[0] + x;
		int screenY = parentLocation[1] + y;
		return screenX >= viewLocation[0] && screenX < viewLocation[0] + mDragView.getWidth() && screenY >= viewLocation[1]
				&& screenY < viewLocation[1] + mDragView.getHeight();
	}


	private int computePanelTopPosition(float slideOffset) {
		int slidingViewHeight = mSlideableView != null ? mSlideableView.getMeasuredHeight() : 0;
		int slidePixelOffset = (int) (slideOffset * mSlideRange);
		return mIsSlidingUp ? getMeasuredHeight() - getPaddingBottom() - mPanelHeight - slidePixelOffset : getPaddingTop() - slidingViewHeight
				+ mPanelHeight + slidePixelOffset;
	}


	private float computeSlideOffset(int topPosition) {
		final int topBoundCollapsed = computePanelTopPosition(0);


		return (mIsSlidingUp ? (float) (topBoundCollapsed - topPosition) / mSlideRange : (float) (topPosition - topBoundCollapsed) / mSlideRange);
	}

	public PanelState getPanelState() {
		return mSlideState;
	}

	public void setPanelState(PanelState state) {
		if (state == null || state == PanelState.DRAGGING) {
			throw new IllegalArgumentException("Panel state cannot be null or DRAGGING.");
		}
		if (!isEnabled() || (!mFirstLayout && mSlideableView == null) || state == mSlideState || mSlideState == PanelState.DRAGGING)
			return;

		if (mFirstLayout) {
			mSlideState = state;
		} else {
			if (mSlideState == PanelState.HIDDEN) {
				mSlideableView.setVisibility(View.VISIBLE);
				requestLayout();
			}
			switch (state) {
			case ANCHORED:
				smoothSlideTo(mAnchorPoint, 0);
				break;
			case COLLAPSED:
				smoothSlideTo(0, 0);
				break;
			case EXPANDED:
				smoothSlideTo(1.0f, 0);
				break;
			case HIDDEN:
				int newTop = computePanelTopPosition(0.0f) + (mIsSlidingUp ? +mPanelHeight : -mPanelHeight);
				smoothSlideTo(computeSlideOffset(newTop), 0);
				break;
			}
		}
	}


	@SuppressLint("NewApi")
	private void applyParallaxForCurrentSlideOffset() {
		if (mParallaxOffset > 0) {
			int mainViewOffset = getCurrentParalaxOffset();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				mMainView.setTranslationY(mainViewOffset);
			} else {

			}
		}
	}

	private void onPanelDragged(int newTop) {
		mLastNotDraggingSlideState = mSlideState;
		mSlideState = PanelState.DRAGGING;

		mSlideOffset = computeSlideOffset(newTop);
		applyParallaxForCurrentSlideOffset();

		dispatchOnPanelSlide(mSlideableView);
		LayoutParams lp = (LayoutParams) mMainView.getLayoutParams();
		int defaultHeight = getHeight() - getPaddingBottom() - getPaddingTop() - mPanelHeight;

		if (mSlideOffset <= 0 && !mOverlayContent) {

			lp.height = mIsSlidingUp ? (newTop - getPaddingBottom())
					: (getHeight() - getPaddingBottom() - mSlideableView.getMeasuredHeight() - newTop);
			mMainView.requestLayout();
		} else if (lp.height != defaultHeight && !mOverlayContent) {
			lp.height = defaultHeight;
			mMainView.requestLayout();
		}
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		boolean result;
		final int save = canvas.save(Canvas.CLIP_SAVE_FLAG);

		if (mSlideableView != child) {

			canvas.getClipBounds(mTmpRect);
			if (!mOverlayContent) {
				if (mIsSlidingUp) {
					mTmpRect.bottom = Math.min(mTmpRect.bottom, mSlideableView.getTop());
				} else {
					mTmpRect.top = Math.max(mTmpRect.top, mSlideableView.getBottom());
				}
			}
			if (mClipPanel) {
				canvas.clipRect(mTmpRect);
			}

			result = super.drawChild(canvas, child, drawingTime);

			if (mCoveredFadeColor != 0 && mSlideOffset > 0) {
				final int baseAlpha = (mCoveredFadeColor & 0xff000000) >>> 24;
				final int imag = (int) (baseAlpha * mSlideOffset);
				final int color = imag << 24 | (mCoveredFadeColor & 0xffffff);
				mCoveredFadePaint.setColor(color);
				canvas.drawRect(mTmpRect, mCoveredFadePaint);
			}
		} else {
			result = super.drawChild(canvas, child, drawingTime);
		}

		canvas.restoreToCount(save);

		return result;
	}

	boolean smoothSlideTo(float slideOffset, int velocity) {
		if (!isEnabled()) {

			return false;
		}

		int panelTop = computePanelTopPosition(slideOffset);
		if (mDragHelper.smoothSlideViewTo(mSlideableView, mSlideableView.getLeft(), panelTop)) {
			setAllChildrenVisible();
			ViewCompat.postInvalidateOnAnimation(this);
			return true;
		}
		return false;
	}

	@Override
	public void computeScroll() {
		if (mDragHelper != null && mDragHelper.continueSettling(true)) {
			if (!isEnabled()) {
				mDragHelper.abort();
				return;
			}

			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	@Override
	public void draw(Canvas c) {
		super.draw(c);

		if (mShadowDrawable != null) {
			final int right = mSlideableView.getRight();
			final int top;
			final int bottom;
			if (mIsSlidingUp) {
				top = mSlideableView.getTop() - mShadowHeight;
				bottom = mSlideableView.getTop();
			} else {
				top = mSlideableView.getBottom();
				bottom = mSlideableView.getBottom() + mShadowHeight;
			}
			final int left = mSlideableView.getLeft();
			mShadowDrawable.setBounds(left, top, right, bottom);
			mShadowDrawable.draw(c);
		}
	}

	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (v instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) v;
			final int scrollX = v.getScrollX();
			final int scrollY = v.getScrollY();
			final int count = group.getChildCount();

			for (int i = count - 1; i >= 0; i--) {
				final View child = group.getChildAt(i);
				if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() && y + scrollY >= child.getTop()
						&& y + scrollY < child.getBottom() && canScroll(child, true, dx, x + scrollX - child.getLeft(), y + scrollY - child.getTop())) {
					return true;
				}
			}
		}
		return checkV && ViewCompat.canScrollHorizontally(v, -dx);
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof MarginLayoutParams ? new LayoutParams((MarginLayoutParams) p) : new LayoutParams(p);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof LayoutParams && super.checkLayoutParams(p);
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();

		SavedState ss = new SavedState(superState);
		if (mSlideState != PanelState.DRAGGING) {
			ss.mSlideState = mSlideState;
		} else {
			ss.mSlideState = mLastNotDraggingSlideState;
		}
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		mSlideState = ss.mSlideState;
	}

	private class DragHelperCallback extends ViewDragHelper.Callback {

		@Override
		public boolean tryCaptureView(View child, int pointerId) {
			if (mIsUnableToDrag) {
				return false;
			}

			return child == mSlideableView;
		}

		@Override
		public void onViewDragStateChanged(int state) {
			if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
				mSlideOffset = computeSlideOffset(mSlideableView.getTop());
				applyParallaxForCurrentSlideOffset();

				if (mSlideOffset == 1) {
					if (mSlideState != PanelState.EXPANDED) {
						updateObscuredViewVisibility();
						mSlideState = PanelState.EXPANDED;
						dispatchOnPanelExpanded(mSlideableView);
					}
				} else if (mSlideOffset == 0) {
					if (mSlideState != PanelState.COLLAPSED) {
						mSlideState = PanelState.COLLAPSED;
						dispatchOnPanelCollapsed(mSlideableView);
					}
				} else if (mSlideOffset < 0) {
					mSlideState = PanelState.HIDDEN;
					mSlideableView.setVisibility(View.INVISIBLE);
					dispatchOnPanelHidden(mSlideableView);
				} else if (mSlideState != PanelState.ANCHORED) {
					updateObscuredViewVisibility();
					mSlideState = PanelState.ANCHORED;
					dispatchOnPanelAnchored(mSlideableView);
				}
			}
		}

		@Override
		public void onViewCaptured(View capturedChild, int activePointerId) {
			setAllChildrenVisible();
		}

		@Override
		public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
			onPanelDragged(top);
			invalidate();
		}

		@Override
		public void onViewReleased(View releasedChild, float xvel, float yvel) {
			int target = 0;


			float direction = mIsSlidingUp ? -yvel : yvel;

			if (direction > 0) {

				target = computePanelTopPosition(1.0f);
			} else if (direction < 0) {
								target = computePanelTopPosition(0.0f);
			} else if (mAnchorPoint != 1 && mSlideOffset >= (1.f + mAnchorPoint) / 2) {

				target = computePanelTopPosition(1.0f);
			} else if (mAnchorPoint == 1 && mSlideOffset >= 0.5f) {

				target = computePanelTopPosition(1.0f);
			} else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint) {
				target = computePanelTopPosition(mAnchorPoint);
			} else if (mAnchorPoint != 1 && mSlideOffset >= mAnchorPoint / 2) {
				target = computePanelTopPosition(mAnchorPoint);
			} else {

				target = computePanelTopPosition(0.0f);
			}

			mDragHelper.settleCapturedViewAt(releasedChild.getLeft(), target);
			invalidate();
		}

		@Override
		public int getViewVerticalDragRange(View child) {
			return mSlideRange;
		}

		@Override
		public int clampViewPositionVertical(View child, int top, int dy) {
			final int collapsedTop = computePanelTopPosition(0.f);
			final int expandedTop = computePanelTopPosition(1.0f);
			if (mIsSlidingUp) {
				return Math.min(Math.max(top, expandedTop), collapsedTop);
			} else {
				return Math.min(Math.max(top, collapsedTop), expandedTop);
			}
		}
	}

	public static class LayoutParams extends MarginLayoutParams {
		private static final int[] ATTRS = new int[] { android.R.attr.layout_weight };

		public LayoutParams() {
			super(MATCH_PARENT, MATCH_PARENT);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(LayoutParams source) {
			super(source);
		}

		public LayoutParams(Context c, AttributeSet attrs) {
			super(c, attrs);

			final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
			a.recycle();
		}

	}

	static class SavedState extends BaseSavedState {
		PanelState mSlideState;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			try {
				mSlideState = Enum.valueOf(PanelState.class, in.readString());
			} catch (IllegalArgumentException e) {
				mSlideState = PanelState.COLLAPSED;
			}
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeString(mSlideState.toString());
		}

		public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
