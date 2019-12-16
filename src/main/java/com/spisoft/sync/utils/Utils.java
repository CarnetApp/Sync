package com.spisoft.sync.utils;

import android.content.Context;

/**
 * Created by phoenamandre on 03/06/17.
 */

public class Utils {

    public static Context context;
    public static boolean isDebugPackage(){
        return context.getPackageName().endsWith(".debug");
    }

    public static String getCorrespondingNote(String path){
        int index = path.lastIndexOf(".sqd");
        if(index >=0){
            return path.substring(0, index);
        }
        return null;
    }
}
