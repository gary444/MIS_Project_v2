package com.example.garyrendle.mis_project_v2;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public interface SignFinder {

    public Rect findSign (Mat inputFrame);
}
