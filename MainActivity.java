package com.tinkerai.bananagpt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.nio.ByteBuffer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private SurfaceView cameraPreview;
    private Button shutterButton;
    private ToggleButton autoToggle;
    private Spinner intervalSpinner;
    private CameraDevice cameraDevice;
    private ProgressBar progressBar;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        cameraPreview = findViewById(R.id.cameraPreview);
        shutterButton = findViewById(R.id.shutterButton);
        autoToggle = findViewById(R.id.autoToggle);
        intervalSpinner = findViewById(R.id.intervalSpinner);
        progressBar = findViewById(R.id.progressBar);

        // Get the SurfaceHolder
        SurfaceHolder surfaceHolder = cameraPreview.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Check permission and setup camera when surface is created
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                } else {
                    setupCameraPreview();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Handle surface change
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // Stop camera preview
            }
        });

        // Set up the shutter button
        shutterButton.setOnClickListener(v -> captureImageAndSend());
    }


    private void setupCameraPreview() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0]; // You can loop to find which camera to use.

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            // We can check the camera capabilities here.

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    startPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                }
            }, null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        Surface surface = cameraPreview.getHolder().getSurface();
        try {
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(
                    java.util.Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            // Handle failure
                        }
                    }, null
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void captureImageAndSend() {
        try {
            ImageReader reader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(reader.getSurface());

            reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image img = reader.acquireLatestImage();
                    ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    sendImageToApi(bytes);
                    img.close();
                }
            }, null);

            cameraDevice.createCaptureSession(java.util.Arrays.asList(reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.capture(captureRequestBuilder.build(), null, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // Handle failure
                }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void sendMessageToTwilio(String message, String sender) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                JSONObject json = new JSONObject();
                json.put("message", message);
                json.put("sender", "whatsapp:"+sender);

                RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json.toString());

                Request request = new Request.Builder()
                        .url("yourtwilioserverlessfunctionthatsendsawhatsappmessagehere.com/post")
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                Response response = client.newCall(request).execute();

                if (response.body() != null) {
                    response.body().close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void sendImageToApi(byte[] imageData) {
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));  // Show ProgressBar

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("image", "image.jpg",
                                RequestBody.create(imageData, MediaType.parse("image/jpeg")))
                        .build();

                Request request = new Request.Builder()
                        .url("https://yourcloudfunctionhere.com/post")
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                String responseData = response.body().string();

                // Print the API response to Logcat
                Log.d("API Response", responseData);
                // Parse JSON and show description as a Toast
                JSONObject jsonObject = new JSONObject(responseData);
                String description = jsonObject.getString("description");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, description, Toast.LENGTH_LONG).show();
                    sendMessageToTwilio(description, "+xxyyyyyyyyyy");  // Send the message to Twilio
                });

                // Close the response after use
                if (response.body() != null) {
                    response.body().close();
                }


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));  // Hide ProgressBar
            }
        }).start();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);  // Call to super
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCameraPreview();
            }
        }
    }

}
