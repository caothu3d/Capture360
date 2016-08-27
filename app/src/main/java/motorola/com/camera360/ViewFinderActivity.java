package motorola.com.camera360;
// http://inducesmile.com/android/android-camera2-api-example-tutorial/

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import java.util.Arrays;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ViewFinderActivity extends AppCompatActivity {
    private static final String TAG = "Camera360";
    private View mContentView;
    private TextureView mBackTextureView;
    private String mBackCameraId;
    private Size mBackImageDimension;
    protected CameraDevice mBackCameraDevice;
    protected CaptureRequest.Builder mBackCaptureRequestBuilder;
    protected CameraCaptureSession mBackCameraCaptureSession;
    private Handler mBackBackgroundHandler;
    private HandlerThread mBackBackgroundThread;


    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private OnClickListener captureOnClickListener = new OnClickListener() {
        public void onClick(View v) {
            // do something when the button is clicked
        }
    };

    protected void updateBackPreview() {
        if(null == mBackCameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        mBackCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            mBackCameraCaptureSession.setRepeatingRequest(mBackCaptureRequestBuilder.build(), null, mBackBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createBackCameraPreview() {
        Log.e(TAG, "createBackCameraPreview");
        try {
            SurfaceTexture texture = mBackTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mBackImageDimension.getWidth(), mBackImageDimension.getHeight());
            Surface surface = new Surface(texture);
            mBackCaptureRequestBuilder = mBackCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mBackCaptureRequestBuilder.addTarget(surface);
            mBackCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "createBackCameraPreview CaptureSession::onConfigured");
                    //The camera is already closed
                    if (null == mBackCameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    mBackCameraCaptureSession = cameraCaptureSession;
                    updateBackPreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "createBackCameraPreview CaptureSession::onSurfaceTextureAvailable");
                    Toast.makeText(ViewFinderActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback backStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "backStateCallback CameraDevice.StateCallback::onOpened");
            mBackCameraDevice = camera;
            createBackCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.e(TAG, "backStateCallback CameraDevice.StateCallback::onDisconnected");
            mBackCameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "backStateCallback CameraDevice.StateCallback::onError = " + error);
            mBackCameraDevice.close();
            mBackCameraDevice = null;
        }
    };

    private void openBackCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "openBackCamera");
        try {
            mBackCameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mBackCameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mBackImageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(mBackCameraId, backStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener backTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.e(TAG, "backTextureListener::onSurfaceTextureAvailable");
            openBackCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
            Log.e(TAG, "backTextureListener::onSurfaceTextureSizeChanged");
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.e(TAG, "backTextureListener::onSurfaceTextureDestroyed");
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");

        setContentView(R.layout.activity_view_finder);

        mContentView = findViewById(R.id.fullscreen_content);

        View captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(captureOnClickListener);

        mBackTextureView = (TextureView) findViewById(R.id.backCameraView);
        mBackTextureView.setSurfaceTextureListener(backTextureListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.e(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(ViewFinderActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    protected void startBackgroundThread() {
        mBackBackgroundThread = new HandlerThread("Camera Background");
        mBackBackgroundThread.start();
        mBackBackgroundHandler = new Handler(mBackBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackBackgroundThread.quitSafely();
        try {
            mBackBackgroundThread.join();
            mBackBackgroundThread = null;
            mBackBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (mBackTextureView.isAvailable()) {
            openBackCamera();
        } else {
            mBackTextureView.setSurfaceTextureListener(backTextureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

}
