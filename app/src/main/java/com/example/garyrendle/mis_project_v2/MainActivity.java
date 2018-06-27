package com.example.garyrendle.mis_project_v2;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;


    //containers for face and eye rectangles
    private Mat roi;
    private Mat col;
    private Mat DRAW_IMG;
    private Mat INPUT_IMG;
    private Mat grey;
    private Mat col_port;
    private Mat grey_port;
    private Mat mirrored_output;
    private Mat output;
    private Mat init_col;
    private Mat col_temp;
    private Mat grey_temp;

    private Mat frame;

    private Display display;

    private SignFinder signFinder;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();


                    signFinder = (SignFinder) new TemplateMatcher(initAssetFile("temp1s.jpg"), 960, 1280);

                    //create here to avoid doing it during frame processing
                    col = new Mat(960,1280, CvType.CV_8UC4);
                    grey = new Mat(960,1280, CvType.CV_8U);
                    col_port = new Mat(1280,960, CvType.CV_8UC4);
                    grey_port = new Mat(1280,960, CvType.CV_8U);
                    col_temp = new Mat(1280,960, CvType.CV_8UC4);
                    grey_temp = new Mat(1280,960, CvType.CV_8U);
                    mirrored_output = new Mat(960,1280, CvType.CV_8UC4);
                    output = new Mat(960,1280, CvType.CV_8UC4);
                    init_col = new Mat(960,1280, CvType.CV_8UC4);
                    col_temp = new Mat(960,1280, CvType.CV_8UC4);
                    grey_temp = new Mat(960,1280, CvType.CV_8U);

                    DRAW_IMG = new Mat();
                    INPUT_IMG = new Mat();


                    frame = new Mat(960,1280, CvType.CV_8UC1);



                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();



        if (display != null)
            display = null;

    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {

    }



    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        long startTime = System.nanoTime();

        frame = inputFrame.gray();

        Rect sign = signFinder.findSign(frame);

        //draw a rectangle here
        Imgproc.rectangle(frame, sign.tl(), sign.br(), new Scalar(255));



        return frame;
    }






    public String initAssetFile(String filename)  {
        File file = new File(getFilesDir(), filename);
        if (!file.exists()) try {
            InputStream is = getAssets().open(filename);
            OutputStream os = new FileOutputStream(file);
            byte[] data = new byte[is.available()];
            is.read(data); os.write(data); is.close(); os.close();
            Log.d(TAG,"prepared local file: "+filename);
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "initAssetFile: file not found: " + filename);
        }
        return file.getAbsolutePath();
    }

}
