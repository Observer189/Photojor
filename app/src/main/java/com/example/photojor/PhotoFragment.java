package com.example.photojor;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.photojor.Utils.ApiService;
import com.example.photojor.Utils.Consts;
import com.example.photojor.model.Ingredient;
import com.example.photojor.model.ServIngrResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PhotoFragment extends Fragment {
    View view;
    private ImageButton btnCapture;
    private TextureView textureView;

    //Check state orientation of output image
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static{
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSessions;
    private CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;

    //Save to FILE
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    boolean isLocked;
    ServIngrResponse servResp;

    DownloadImageTask downloadImageTask;
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            cameraDevice.close();
            cameraDevice=null;
        }
    };


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view=inflater.inflate(R.layout.fragment_take_photo, container, false);

        isLocked=false;

        textureView = view.findViewById(R.id.texture_view);
        assert textureView != null;
        Log.d("!!!",textureView.toString());
        textureView.setSurfaceTextureListener(textureListener);


        Consts.retrofit = new Retrofit.Builder()
                .baseUrl(Consts.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        Consts.service = Consts.retrofit.create(ApiService.class);


        btnCapture = view.findViewById(R.id.capture_image);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {

                    case R.id.capture_image:
                        isLocked=true;
                        btnCapture.setActivated(!isLocked);
                        takePicture();
                }
            }
        };
        downloadImageTask=new DownloadImageTask();
        btnCapture.setOnClickListener(listener);
        return view;
    }








    private void uploadFile(Uri fileUri) {
        // create upload service client
        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse("image/jpg"),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        // finally, execute the request
        Call<ServIngrResponse> call = Consts.service.uploadImage(body);
        call.enqueue(new Callback<ServIngrResponse>() {
            @Override
            public void onResponse(Call<ServIngrResponse> call,
                                   Response<ServIngrResponse> response) {
                Log.wtf("Upload", "success:"+response.body().getName() );
                servResp=response.body();
                downloadImageTask=new DownloadImageTask();
                   downloadImageTask.execute(servResp.getImageUrl());
            }

            @Override
            public void onFailure(Call<ServIngrResponse> call, Throwable t) {
                Log.e("Upload error:", t.getMessage());
                isLocked=false;
                btnCapture.setActivated(!isLocked);
                Toast.makeText(getActivity(),"Не удалось соединиться с сервером",Toast.LENGTH_LONG);
            }
        });
    }

    private void takePicture() {
        if(cameraDevice == null)
            return;
        CameraManager manager = (CameraManager)getActivity().getSystemService(Context.CAMERA_SERVICE);
        try{
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;
            if(characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(ImageFormat.JPEG);

            //Capture image with custom size
            int width = 640;
            int height = 480;
            if(jpegSizes != null && jpegSizes.length > 0)
            {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }
            final ImageReader reader = ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            //Check orientation base on device
            int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
            final java.net.URI uri=new URI(Environment.getExternalStorageDirectory()+"/"+ UUID.randomUUID().toString()+".jpg");
            file = new File(uri.getPath());
            Log.d("!!!",uri.getPath());

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    try{
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        save(bytes);
                        Uri u= Uri.fromFile(file);
                        Log.d("!!!",getActivity().getContentResolver().toString());
                        uploadFile(u);
                        /*Call<ServIngrResponse> call = Consts.service.uploadImage(body);
                        call.enqueue(new Callback<ServIngrResponse>() {
                            @Override
                            public void onResponse(Call<ServIngrResponse> call, Response<ServIngrResponse> response) {
                                 Toast.makeText(CameraActivity.this,"Response: "+response.body().getName(),Toast.LENGTH_LONG).show();

                            }

                            @Override
                            public void onFailure(Call<ServIngrResponse> call, Throwable t) {

                            }
                        });*/
                    }
                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    finally {
                        {
                            if(image != null)
                                image.close();
                        }
                    }
                }
                private void save(byte[] bytes) throws IOException {
                    OutputStream outputStream = null;
                    try{
                        outputStream = new FileOutputStream(file);
                        outputStream.write(bytes);
                    }finally {
                        if(outputStream != null)
                            outputStream.close();
                    }
                }
            };

            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(getActivity(), "Saved "+file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);


        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreview() {
        try{
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert  texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(),imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if(cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(getActivity(), "Changed", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if(cameraDevice == null)
            Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);
        try{
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),null,mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private void openCamera() {
        CameraManager manager = (CameraManager)getActivity().getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            //Check realtime permission if run higher API 23
            if(ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(getActivity(),new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                },REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId,stateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_CAMERA_PERMISSION)
        {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(getActivity(), "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                getActivity().finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if(textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    @Override
    public void onPause() {
        stopBackgroundThread();
        cameraDevice.close();
        super.onPause();
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        downloadImageTask.cancel(false);
        try{
            mBackgroundThread.join();
            mBackgroundThread= null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
                Consts.fridge.add(new Ingredient(servResp.getName(),mIcon11));
                //Log.wtf("fridge ","up");
                //Intent intent=new Intent(getActivity(),ListActivity.class);
                //intent.putExtra("NewIng",Consts.gson.toJson(servResp));
                //intent.putExtra("image",Consts.gson.toJson(mIcon11));
                //Log.w("intent: ",intent.getStringExtra("image"));
                //tartActivity(intent);

            } catch (Exception e) {
                Log.e("Ошибка передачи", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }


    }
}
