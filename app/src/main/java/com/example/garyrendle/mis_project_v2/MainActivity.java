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

    private CascadeClassifier face_cascade;
    private int maxAbsoluteFaceSize;
    private int minAbsoluteFaceSize;
    private CascadeClassifier eye_cascade;
    private CascadeClassifier road_sign_cascade;

    private OrientationEventListener mOrientationListener;
    public enum Orientation {
        PORTRAIT_UP,
        PORTRAIT_DOWN,
        LANDSCAPE_L,
        LANDSCAPE_R
    }
    Orientation phone_orientation;

    //change to see face and eye frames
    boolean DRAW_FRAMES = false;

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
    private Mat init_grey;
    private Mat col_temp;
    private Mat grey_temp;
    private MatOfRect eye_rects_m;
    private List<Rect> eye_rects = new ArrayList<Rect>();
    private ArrayList<Rect> rects = new ArrayList<>();
    private MatOfRect rects_m;

    private Mat scaledImg;
    private Mat result;
    private Mat returnResult;
    private Mat templateImg;

    private Display display;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                    road_sign_cascade = new CascadeClassifier(initAssetFile("Speedlimit_HAAR_ 17Stages.xml"));

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
                    init_grey = new Mat(960,1280, CvType.CV_8U);
                    col_temp = new Mat(960,1280, CvType.CV_8UC4);
                    grey_temp = new Mat(960,1280, CvType.CV_8U);

                    DRAW_IMG = new Mat();
                    INPUT_IMG = new Mat();

                    scaledImg = new Mat();
                    result = new Mat();
                    returnResult = new Mat();
                    templateImg = new Mat();


                    rects_m = new MatOfRect();
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

        mOrientationListener = new OrientationEventListener(this,
                SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                updateOrientation(orientation);
            }
        };
        if (mOrientationListener.canDetectOrientation() == true) {
            Log.d(TAG, "onCreate: can detect orientation");
            mOrientationListener.enable();
        } else {
            Log.d(TAG, "onCreate: cannot detect orientation");
            mOrientationListener.disable();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();


        rects_m.release();
        eye_rects_m.release();
        if (eye_rects != null)
            eye_rects = null;

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
        eye_rects = new ArrayList<Rect>();

    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();

        mOrientationListener.disable();
    }

    public void onCameraViewStarted(int width, int height) {
        minAbsoluteFaceSize = (int) (height * 0.2);
        maxAbsoluteFaceSize = (int) (height);
    }

    public void onCameraViewStopped() {

    }

    public void loadTemplate() {
        Mat templLoad = Imgcodecs.imread("resources/temp1s.jpg");
        Imgproc.cvtColor(templLoad, this.templateImg, Imgproc.COLOR_BGR2GRAY);
        if (this.templateImg == null) {
            System.out.println("could not load template");
        }
        else {
            System.out.println("Template loaded succesfully");

        }
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {


        init_grey = inputFrame.gray();

        int match_method = Imgproc.TM_CCOEFF;

        //scale down input aautomatically?

        //scale down until good match found or too small
        final double threshold = 3000000;
        double peak;
        boolean matchFound = false;
        ArrayList<MaxLocScale> matches = new ArrayList<>();
        float scale = 1.f;
        float scaleStep = 0.1f;
        for (scale = 1.f; scale >= 0.5 && matchFound == false; scale -= scaleStep) {

            //calc scaled size
            Size matSize = new Size(init_grey.width() * scale, init_grey.height() * scale);
            //resize
            Imgproc.resize(init_grey, scaledImg, matSize);
            //create result matrix
            result.create(scaledImg.rows() - templateImg.rows() + 1, scaledImg.cols() - templateImg.cols() + 1, CvType.CV_8U);

            //template matching
            Imgproc.matchTemplate(scaledImg, templateImg, result, match_method);
            MaxLocScale minMaxLocResult = new MaxLocScale (Core.minMaxLoc(result, new Mat()), scale, match_method);
            matches.add(minMaxLocResult);

            //compare max to threshold
            if (minMaxLocResult.maxVal > threshold) {
                matchFound = true;//will break out of for loop
            }

            //output response for max scale - to visualize only
            if (scale == 1.0) {
                Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
                //multiply result for viewing
                Core.multiply(result, new Scalar(255), returnResult);
            }

        }

        //if a match is found, then match is last element in array
        MaxLocScale bestMatch;
        if(matchFound) {
            bestMatch = matches.get(matches.size()-1);
        }
        else {
            //sort array
            Collections.sort(matches, (a, b) -> a.maxVal < b.maxVal ? 1 : a.maxVal == b.maxVal ? 0 : -1);
            //best match is first element in array
            bestMatch = matches.get(0);
        }

        //convert match location to full size coordinates
        Point match = scalePoint(bestMatch.maxLoc,1/bestMatch.scale);

        //update text fields
//        match_output.setText(String.format("Match Rating = %13.3f%s     Time per frame: %d ", bestMatch.maxVal/1000000, matchFound ? "*" : " ", (System.nanoTime() - startTime)/1000000));
//        scale_output.setText(String.format("Scale = %5.2f", bestMatch.scale));

        //draw a rectangle here

        return init_grey;
    }

    private Point scalePoint(Point p, double mult) {
        return new Point(p.x * mult, p.y * mult);
    }

//    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
//
//        init_col = inputFrame.rgba();
//        init_grey = inputFrame.gray();
//
//        /
//
//        if (face_cascade != null){
//
//            //set references to correctly sized in/out matrices
//            if (phone_orientation == Orientation.PORTRAIT_UP
//                    || phone_orientation == Orientation.PORTRAIT_DOWN){
//                DRAW_IMG = col_port;
//                INPUT_IMG = grey_port;
//            }
//            else {
//                DRAW_IMG = col;
//                INPUT_IMG = grey;
//            }
//
//            face_cascade.detectMultiScale(INPUT_IMG, rects_m, 1.2, 4, 0,
//                    new Size(minAbsoluteFaceSize, minAbsoluteFaceSize), new Size(maxAbsoluteFaceSize,maxAbsoluteFaceSize));
//
//
//            if (!rects_m.empty()){
//                rects.addAll(rects_m.toList());
//
//                //check against overlaps to avoid painting multiple noses on the same person
//                for (int i = rects.size()-1; i >= 0; i--){
//                    int rectToCheck = i-1;
//                    while (rectToCheck >= 0){
//                        if (doRectsIntersect(rects.get(i), rects.get(rectToCheck))){
//                            //intersection found, if there is a large overlap,
//                            // eliminate one of the rectangles (smallest)
//                            double a1 = rects.get(i).area();
//                            double a2 = rects.get(rectToCheck).area();
//                            if (intersectionArea(rects.get(i), rects.get(rectToCheck)) >
//                                    (Math.min(a1,a2))*0.5){
//                                if (a1 > a2){
//                                    rects.remove(rectToCheck);
//                                    i--;
//                                }
//                                else{
//                                    rects.remove(i);
//                                    break;
//                                }
//
//                            }
//                        }
//                        rectToCheck--;
//                    }
//                }
//
//                //process faces to find eyes
//                for (Rect r : rects){
//
//                    if (DRAW_FRAMES) Imgproc.rectangle(DRAW_IMG, r.tl(), r.br(), new Scalar(255,255,255), 3);
//                    roi = new Mat(INPUT_IMG, r);
//
//                    //find eyes
//                    eye_rects_m = new MatOfRect();
//                    eye_cascade.detectMultiScale(roi, eye_rects_m, 1.1, 20, 0,
//                            new Size(minAbsoluteFaceSize/8, minAbsoluteFaceSize/8), new Size(roi.width()/2, roi.height()/2));
//
//                    if (!eye_rects_m.empty()){
//                        eye_rects = eye_rects_m.toList();
//                        for (Rect er : eye_rects){
//                            //draw eyes
//                            if (DRAW_FRAMES) Imgproc.rectangle(DRAW_IMG, addPoints(er.tl(), r.tl()), addPoints(er.br(), r.tl()), new Scalar(0,100,255), 3);
//
//                        }
//                    }
//
//                    //derive nose centre from 2 eye positions, if at least 2 exist
//                    if (eye_rects.size() >= 2){
//
//                        //find 2 largest eyes
//                        int[] eye_idxs = findLargestEyeIndexes(eye_rects);
//                        Point nose_centre = deriveNoseCentre(r, eye_rects.get(eye_idxs[0]), eye_rects.get(eye_idxs[1]));
//                        //draw a nose
//                        Imgproc.circle(DRAW_IMG, nose_centre, r.height / 8, new Scalar(255,0,0), -1);
//
//                    }
//                    else {
//                        //use centre of rectangle
//                        Point cntr = new Point(r.x + r.width/2, r.y + r.height/2);
//                        Imgproc.circle(DRAW_IMG, cntr, r.height / 8, new Scalar(255,0,0), -1);
//
//                    }
//                }
//            }
//        }
//
//        rects.clear();
//
//        //orientation correction
//        if (phone_orientation == Orientation.LANDSCAPE_R){
//            Core.flip(col, output, 0);
//        }
//        else if (phone_orientation == Orientation.LANDSCAPE_L){
//            output = col;
//        }
//        else if (phone_orientation == Orientation.PORTRAIT_UP){
//
//            Core.flip(col_port, col_temp, 0);
//            Core.transpose(col_temp, output);
//        }
//        else if (phone_orientation == Orientation.PORTRAIT_DOWN){
//            Core.transpose(col_port, output);
//        }
//        else {//default
//            output = col;
//        }
//
////        flip around vertical for mirror image in front camera
//        Core.flip(output, mirrored_output, 1);
//        return mirrored_output;
//    }

    private int[] findLargestEyeIndexes(List<Rect> eyes){

        int largest[] = {0,0};
        double area[] = {0.0, 0.0};
        for (int i = 0; i < eyes.size(); i++){
            if (eyes.get(i).area() > area[0]) {
                area[0] = eyes.get(i).area();
                largest[0] = i;
            }
        }
        for (int i = 0; i < eyes.size(); i++){
            if (eyes.get(i).area() > area[1]
                    && i != largest[0]) {
                area[1] = eyes.get(i).area();
                largest[1] = i;
            }
        }
        return largest;
    }

    private Point deriveNoseCentre(Rect face, Rect eye1, Rect eye2){


        Point eye1_corner = pointClosestToCenter(eye1,face);
        Point eye2_corner = pointClosestToCenter(eye2,face);
        Point mid_point = midPoint(eye1_corner, eye2_corner);

//        double dx = (eye2_corner.x - eye1_corner.x);
//        double dy = (eye2_corner.y - eye1_corner.y);
//        double inverse_grad;
//        if (dx != 0.0 && dy != 0.0){
//            double eye_line_grad = dy/dx;
//            inverse_grad = -1 / eye_line_grad;
//        }
//        else {
//            if (dx == 0.0)
//                inverse_grad = 0;
//            else
//                inverse_grad = Double.MAX_VALUE;
//        }
//        //travel by 'nose offset' along inverse eyeline and place nose there
//        double nose_offset = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));
//        double angle = Math.atan2(inverse_grad,1) + Math.PI;
//        double nose_dy = Math.sin(angle) * nose_offset;
//        double nose_dx = Math.cos(angle) * nose_offset;
//        Point nosePoint = new Point(mid_point.x + nose_dx, mid_point.y + nose_dy);

        //place nose
        Point nosePoint = new Point(mid_point.x, mid_point.y + face.height/8);
        //correct to face co-ordinates
        return addPoints(nosePoint, face.tl());
    }

    private Point pointClosestToCenter(Rect eye, Rect face){

        ArrayList<Point> points = new ArrayList<>();
        points.add(eye.tl());
        points.add(eye.br());
        points.add(new Point(eye.x, eye.y+eye.height));
        points.add(new Point(eye.x+eye.width, eye.y));

        int cntrx = face.width / 2;
        int cntry = face.height / 2;

        int closest = -1;
        double min_distance = Double.MAX_VALUE;
        for (int i = 0; i < points.size(); i++){
            double dx = Math.abs(points.get(i).x - cntrx);
            double dy = Math.abs(points.get(i).y - cntry);
            double distance = Math.sqrt(Math.pow(dx,2) + Math.pow(dy,2));
            if (distance < min_distance){
                min_distance = distance;
                closest = i;
            }


        }
        return points.get(closest);
    }

    private Point addPoints(Point p1, Point p2){
        return new Point(p1.x + p2.x, p1.y + p2.y);
    }
    private Point midPoint(Point p1, Point p2){
        return new Point((p1.x + p2.x)/2, (p1.y + p2.y)/2);
    }
    private boolean doRectsIntersect(Rect r1, Rect r2){

        if (r1.br().y < r2.tl().y || r2.br().y < r1.tl().y){
            return false;
        }
        if (r1.br().x < r2.tl().x || r2.br().x < r1.tl().x){
            return false;
        }
        return true;
    }
    private double intersectionArea(Rect r1, Rect r2){
//        https://math.stackexchange.com/questions/99565/simplest-way-to-calculate-the-intersect-area-of-two-rectangles
        double x_overlap = Math.max(0, Math.min(r1.x+r1.width, r2.x+r2.width) - Math.max(r1.x, r2.x));
        double y_overlap = Math.max(0, Math.min(r1.y+r1.height, r2.y+r1.height) - Math.max(r1.y, r2.y));
        return x_overlap * y_overlap;
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

    //quantise orientation to 4 directions
    private void updateOrientation(int angle){
        if (angle < 45){
            phone_orientation = Orientation.PORTRAIT_UP;
        }
        else if (angle < 135) {
            phone_orientation = Orientation.LANDSCAPE_R;
        }
        else if (angle < 225) {
            phone_orientation = Orientation.PORTRAIT_DOWN;
        }
        else if (angle < 315) {
            phone_orientation = Orientation.LANDSCAPE_L;
        }
        else {
            phone_orientation = Orientation.PORTRAIT_UP;
        }
    }
}
