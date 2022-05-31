package org.andresoviedo.util.android;

import android.app.Activity;
import android.view.View;
/**************************************************************************************************/
public class SystemUiHiderHoneycomb extends SystemUiHiderBase {
	/**********************************************************************************************/
	private int mShowFlags;
	/**********************************************************************************************/
	private int mHideFlags;
	/**********************************************************************************************/
	private int mTestFlags;
	/**********************************************************************************************/
	private boolean mVisible = true;

	/**********************************************************************************************/
	protected SystemUiHiderHoneycomb(Activity activity, View anchorView, int flags) {
		super(activity, anchorView, flags);

		mShowFlags = View.SYSTEM_UI_FLAG_VISIBLE;
		mHideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE;
		mTestFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE;

		if ((mFlags & FLAG_FULLSCREEN) != 0) {
			mShowFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
			mHideFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
		}

		if ((mFlags & FLAG_HIDE_NAVIGATION) != 0) {
			mShowFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
			mHideFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			mTestFlags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
		}
	}

	/**********************************************************************************************/
	@Override
	public void setup() {
		mAnchorView.setOnSystemUiVisibilityChangeListener(mSystemUiVisibilityChangeListener);
	}

	/**********************************************************************************************/
	@Override
	public void hide() {
		mAnchorView.setSystemUiVisibility(mHideFlags);
	}

	/**********************************************************************************************/
	@Override
	public void show() {
		mAnchorView.setSystemUiVisibility(mShowFlags);
	}

	/**********************************************************************************************/
	@Override
	public boolean isVisible() {
		return mVisible;
	}

	/**********************************************************************************************/
	private View.OnSystemUiVisibilityChangeListener mSystemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
		/******************************************************************************************/
		@Override
		public void onSystemUiVisibilityChange(int vis) {
			if ((vis & mTestFlags) != 0) {
				mOnVisibilityChangeListener.onVisibilityChange(false);

				mVisible = false;
			} else {
				mAnchorView.setSystemUiVisibility(mShowFlags);

				mOnVisibilityChangeListener.onVisibilityChange(true);

				mVisible = true;
			}
		}
		/******************************************************************************************/
	};
}
