package com.example.garyrendle.mis_project_v2;


import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

//converter class for OpenCV native MinMaxLocResult class
public class MaxLocScale {
    public double maxVal;
    public Point maxLoc;
    public float scale;


    public MaxLocScale() {
        maxVal=0;
        maxLoc=new Point();
        scale=1.0f;
    }
    
    public MaxLocScale(Core.MinMaxLocResult mm, float scale, int match_method) {
    	if(match_method  == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
    		maxVal=mm.minVal;
    		maxLoc=mm.minLoc;
    	}
		else {
			maxVal=mm.maxVal;
			maxLoc=mm.maxLoc;
		}
        this.scale=scale;
    }

}
