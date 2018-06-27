package com.example.garyrendle.mis_project_v2;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;

import static java.lang.Math.ceil;
import static java.lang.Math.round;

public class TemplateMatcher implements SignFinder {


    private static final String TAG = "MISProj:TemplateMatcher";

    private Mat templateImg = new Mat();
    private Mat init_grey = new Mat(960,1280,CvType.CV_8U);
    private Mat scaledImg = new Mat();
    private Mat result = new Mat();

    private final float maxScale = 0.5f;
    private final float minScale = 0.3f;
    private final float scaleIncrement = 0.1f;
    private final int scaleSteps = round((maxScale - minScale) / scaleIncrement) + 1;//round avoid floating point error
    private Mat[] scaledMats;
    private Mat[] resultMats;

    TemplateMatcher(String templateFileName, int input_rows, int input_cols){
        loadTemplate(templateFileName);

        // init mats in correct size and type here
        scaledMats = new Mat[scaleSteps];
        resultMats = new Mat[scaleSteps];
        for (int i = 0; i < scaleSteps; i++){
            float scale = minScale + (i * scaleIncrement);
            scaledMats[i] = new Mat((int)(input_rows * scale), (int)(input_cols * scale), CvType.CV_8U);
            resultMats[i] = new Mat(scaledMats[i].rows() - templateImg.rows() + 1, scaledMats[i].cols() - templateImg.cols() + 1, CvType.CV_32FC1);
        }

    }

    public void loadTemplate(String filename) {
        Mat templLoad = Imgcodecs.imread(filename);
        Imgproc.cvtColor(templLoad, this.templateImg, Imgproc.COLOR_RGB2GRAY);

        if (this.templateImg == null) {
            System.out.println("could not load template");
        }
        else {
            System.out.println("Template loaded succesfully");
            System.out.println("Template size: " + templateImg.width() + " x " + templateImg.height());

        }
    }

    public Rect findSign (Mat inputFrame){

        int match_method = Imgproc.TM_CCOEFF;

        //scale down until good match found or too small
//        final double threshold = 3000000;
        boolean matchFound = false;
        ArrayList<MaxLocScale> matches = new ArrayList<>();
        float scale;
        for (int i = 0; i < scaleSteps && !matchFound; i++) {


//            for (scale = minScale; scale <= maxScale && !matchFound; scale += scaleIncrement) {
            scale = minScale + (scaleIncrement * i);

            //calc scaled size
//            Size matSize = new Size(inputFrame.width() * scale, inputFrame.height() * scale);
            Imgproc.resize(inputFrame, scaledMats[i], scaledMats[i].size());

            //template matching
            Imgproc.matchTemplate(scaledMats[i], templateImg, resultMats[i], match_method);
            MaxLocScale minMaxLocResult = new MaxLocScale (Core.minMaxLoc(resultMats[i]), scale, match_method);
            matches.add(minMaxLocResult);

                //compare max to threshold
//            if (minMaxLocResult.maxVal > threshold) {
//                matchFound = true;//will break out of for loop
//            }

        }

        //if a match is found, then match is last element in array
        MaxLocScale bestMatch;
        if(matchFound) {
            bestMatch = matches.get(matches.size()-1);
        }
        else {
            //sort array
            Collections.sort(matches, (a, b) -> -Double.compare(a.maxVal, b.maxVal));

            if(match_method  == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED)
                bestMatch = matches.get(matches.size()-1);
            else
                bestMatch = matches.get(0);


        }

        //convert match location to full size coordinates
        Point match = scalePoint(bestMatch.maxLoc,1/bestMatch.scale);

        return new Rect(match, new Point(match.x + templateImg.cols()/bestMatch.scale,
                match.y + templateImg.rows()/bestMatch.scale));
    }

    private Point scalePoint(Point p, double mult) {
        return new Point(p.x * mult, p.y * mult);
    }
}
