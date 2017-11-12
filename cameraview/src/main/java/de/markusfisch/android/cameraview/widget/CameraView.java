package de.markusfisch.android.cameraview.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CameraView extends FrameLayout {
	public interface OnCameraListener {
		void onConfigureParameters(Camera.Parameters parameters);
		void onCameraError();
		void onCameraStarted(Camera camera);
		void onCameraStopping(Camera camera);
	}

	public final Rect previewRect = new Rect();

	private Camera camera;
	private int viewWidth;
	private int viewHeight;
	private int frameWidth;
	private int frameHeight;
	private int frameOrientation;
	private OnCameraListener onCameraListener;

	public static int findCameraId(int facing) {
		for (int i = 0, l = Camera.getNumberOfCameras(); i < l; ++i) {
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(i, info);
			if (info.facing == facing) {
				return i;
			}
		}
		return -1;
	}

	public static int getCameraOrientation(Context context, int cameraId) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		return (info.orientation - getDeviceRotation(context) + 360) % 360;
	}

	public static int getDeviceRotation(Context context) {
		switch (((WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay()
				.getRotation()) {
			case Surface.ROTATION_90:
				return 90;
			case Surface.ROTATION_180:
				return 180;
			case Surface.ROTATION_270:
				return 270;
			case Surface.ROTATION_0:
			default:
				return 0;
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public static boolean setAutoFocus(Camera.Parameters parameters) {
		// best for taking pictures, API >= ICE_CREAM_SANDWICH
		String continuousPicture =
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
		// less aggressive than CONTINUOUS_PICTURE, API >= GINGERBREAD
		String continuousVideo =
				Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
		// last resort
		String autoFocus = Camera.Parameters.FOCUS_MODE_AUTO;

		// prefer feature detection instead of checking BUILD.VERSION
		List<String> focusModes = parameters.getSupportedFocusModes();

		if (focusModes.contains(continuousPicture)) {
			parameters.setFocusMode(continuousPicture);
		} else if (focusModes.contains(continuousVideo)) {
			parameters.setFocusMode(continuousVideo);
		} else if (focusModes.contains(autoFocus)) {
			parameters.setFocusMode(autoFocus);
		} else {
			return false;
		}

		return true;
	}

	public CameraView(Context context) {
		super(context);
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CameraView(
			Context context,
			AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void openAsync(final int cameraId) {
		new AsyncTask<Void, Void, Camera>() {
			@Override
			protected Camera doInBackground(Void... nothings) {
				if (CameraView.this.camera != null) {
					return null;
				}
				try {
					// open() may take a while so it shouldn't be
					// invoked on the main thread according to the docs
					return Camera.open(cameraId);
				} catch (RuntimeException e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(Camera camera) {
				if (camera == null) {
					if (onCameraListener != null &&
							CameraView.this.camera == null) {
						onCameraListener.onCameraError();
					}
					return;
				}
				CameraView.this.camera = camera;
				Context context = getContext();
				if (context == null) {
					close();
					return;
				}
				frameOrientation = getCameraOrientation(context, cameraId);
				if (viewWidth > 0) {
					setCameraParameters(context);
				}
			}
		}.execute();
	}

	public void close() {
		if (camera != null) {
			if (onCameraListener != null) {
				onCameraListener.onCameraStopping(camera);
			}
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
		removeAllViews();
	}

	public void setOnCameraListener(OnCameraListener listener) {
		onCameraListener = listener;
	}

	public Camera getCamera() {
		return camera;
	}

	public int getFrameWidth() {
		return frameWidth;
	}

	public int getFrameHeight() {
		return frameHeight;
	}

	public int getFrameOrientation() {
		return frameOrientation;
	}

	public Rect calculateFocusRect(float x, float y, int radius) {
		int cx = Math.round(2000f / viewWidth * x - 1000f);
		int cy = Math.round(2000f / viewHeight * y - 1000f);
		return new Rect(
				Math.max(-1000, cx - radius),
				Math.max(-1000, cy - radius),
				Math.min(1000, cx + radius),
				Math.min(1000, cy + radius));
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public boolean setFocusArea(Rect area) {
		if (camera == null || Build.VERSION.SDK_INT <
				Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return false;
		}
		try {
			Camera.Parameters parameters = camera.getParameters();
			if (parameters.getMaxNumFocusAreas() > 0) {
				if (area != null) {
					List<Camera.Area> focusAreas =
							new ArrayList<Camera.Area>();
					focusAreas.add(new Camera.Area(area, 1000));
					parameters.setFocusAreas(focusAreas);
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				} else {
					parameters.setFocusAreas(null);
					CameraView.setAutoFocus(parameters);
				}
			}
			camera.setParameters(parameters);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}

	@Override
	protected void onLayout(
			boolean changed,
			int left,
			int top,
			int right,
			int bottom) {
		if (!changed) {
			return;
		}
		viewWidth = right - left;
		viewHeight = bottom - top;
		if (camera != null && getChildCount() == 0) {
			Context context = getContext();
			if (context == null) {
				return;
			}
			setCameraParameters(context);
		}
	}

	private void setCameraParameters(Context context) {
		boolean transpose = frameOrientation == 90 || frameOrientation == 270;

		try {
			Camera.Parameters parameters = camera.getParameters();
			parameters.setRotation(frameOrientation);
			setPreviewSize(parameters, transpose);
			if (onCameraListener != null) {
				onCameraListener.onConfigureParameters(parameters);
			}
			camera.setParameters(parameters);
		} catch (RuntimeException e) {
			if (onCameraListener != null) {
				onCameraListener.onCameraError();
			}
			return;
		}

		camera.setDisplayOrientation(frameOrientation);

		int childWidth;
		int childHeight;
		if (transpose) {
			childWidth = frameHeight;
			childHeight = frameWidth;
		} else {
			childWidth = frameWidth;
			childHeight = frameHeight;
		}
		addSurfaceView(context, childWidth, childHeight);

		if (onCameraListener != null) {
			onCameraListener.onCameraStarted(camera);
		}
	}

	private void setPreviewSize(
			Camera.Parameters parameters,
			boolean transpose) {
		if (transpose) {
			frameWidth = viewHeight;
			frameHeight = viewWidth;
		} else {
			frameWidth = viewWidth;
			frameHeight = viewHeight;
		}
		Camera.Size size = findBestPreviewSize(
				// will always return at least one item
				parameters.getSupportedPreviewSizes(),
				frameWidth,
				frameHeight);
		if (size != null) {
			frameWidth = size.width;
			frameHeight = size.height;
			parameters.setPreviewSize(frameWidth, frameHeight);
		}
	}

	private static Camera.Size findBestPreviewSize(
			List<Camera.Size> sizes,
			int width,
			int height) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) width / height;
		double minDiff = Double.MAX_VALUE;
		double minDiffAspect = Double.MAX_VALUE;
		Camera.Size bestSize = null;
		Camera.Size bestSizeAspect = null;

		for (Camera.Size size : sizes) {
			double diff = (double) Math.abs(size.height - height) +
					Math.abs(size.width - width);

			if (diff < minDiff) {
				bestSize = size;
				minDiff = diff;
			}

			double ratio = (double) size.width / size.height;

			if (Math.abs(ratio - targetRatio) < ASPECT_TOLERANCE &&
					diff < minDiffAspect) {
				bestSizeAspect = size;
				minDiffAspect = diff;
			}
		}

		return bestSizeAspect != null ? bestSizeAspect : bestSize;
	}

	private void addSurfaceView(
			Context context,
			int surfaceWidth,
			int surfaceHeight) {
		SurfaceView surfaceView = new SurfaceView(context);
		SurfaceHolder holder = surfaceView.getHolder();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		holder.setKeepScreenOn(true);
		holder.addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				// wait until the surface has dimensions
			}

			@Override
			public void surfaceChanged(
					SurfaceHolder holder,
					int format,
					int width,
					int height) {
				try {
					camera.setPreviewDisplay(holder);
				} catch (IOException e) {
					return;
				}
				camera.startPreview();
			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				close();
			}
		});
		addView(surfaceView);
		setChildLayout(
				viewWidth,
				viewHeight,
				surfaceView,
				surfaceWidth,
				surfaceHeight,
				previewRect);
	}

	private static void setChildLayout(
			int width,
			int height,
			View child,
			int childWidth,
			int childHeight,
			Rect childRect) {
		int widthByHeight = width * childHeight;
		int heightByWidth = height * childWidth;
		boolean dontScaleBeyondScreen = Build.VERSION.SDK_INT <
				Build.VERSION_CODES.ICE_CREAM_SANDWICH;

		if (dontScaleBeyondScreen ?
				// center within parent view
				widthByHeight > heightByWidth :
				// scale to cover parent view
				widthByHeight < heightByWidth) {
			childWidth = childWidth * height / childHeight;
			childHeight = height;
		} else {
			childHeight = childHeight * width / childWidth;
			childWidth = width;
		}

		int l = (width - childWidth) >> 1;
		int t = dontScaleBeyondScreen ?
				(height - childHeight) >> 1 :
				0;

		childRect.set(
				l,
				t,
				l + childWidth,
				t + childHeight);

		child.layout(
				childRect.left,
				childRect.top,
				childRect.right,
				childRect.bottom);
	}
}
