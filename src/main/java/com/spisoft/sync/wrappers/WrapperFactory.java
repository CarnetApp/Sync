package com.spisoft.sync.wrappers;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.spisoft.sync.wrappers.googledrive.GDriveDatabase;
import com.spisoft.sync.wrappers.googledrive.GDriveWrapper;
import com.spisoft.sync.wrappers.nextcloud.NextCloudWrapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexandre on 18/11/16.
 */

public class WrapperFactory {

    public static Class[] wrappers = new Class[]{
            NextCloudWrapper.class,
            GDriveWrapper.class
    };

    public static Wrapper getWrapper(Context ct, int accountType, Integer accountID) {
        try {
            for(Class wrapperClass : wrappers){
                Wrapper wrapper = (Wrapper) wrapperClass.getConstructor(Context.class).newInstance(ct);
                if(wrapper.isMyAccount(accountType)){
                    wrapper.init(ct, accountID);
                    return wrapper;
                }
        }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<Wrapper> getWrapperList(Context context){
        List<Wrapper> wrapperList = new ArrayList<>();
        try {
            for(Class wrapperClass : wrappers){
                Wrapper wrapper = (Wrapper) wrapperClass.getConstructor(Context.class).newInstance(context);
                wrapperList.add(wrapper);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return wrapperList;
    }
}
