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
}
