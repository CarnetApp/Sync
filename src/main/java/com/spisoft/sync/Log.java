package com.spisoft.sync;

import android.os.Environment;

import com.spisoft.sync.utils.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by phoenamandre on 19/07/17.
 */

public class Log {
    static FileWriter fw =null;

    public static String getDebugLogPath(){
        return new File(Utils.context.getExternalCacheDir(),"sync.log").getAbsolutePath();
    }
    public static void d(String tag, String str){
        if(fw == null){
            try {
                fw = new FileWriter(getDebugLogPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Date today = Calendar.getInstance().getTime();
            String reportDate = df.format(today);
            fw.append(reportDate+" : "+tag+" "+str+" \n");
            fw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        android.util.Log.d(tag+(Utils.isDebugPackage()?"Debug":""), str);

    }
}
