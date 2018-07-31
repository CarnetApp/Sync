package com.spisoft.sync.wrappers;

import android.content.Context;
import android.util.Log;

import com.spisoft.sync.wrappers.nextcloud.NextCloudWrapper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexandre on 18/11/16.
 */

public class WrapperFactory {

    public static List<Class> wrappers = new ArrayList<>();
    static {
        wrappers.add(NextCloudWrapper.class);
    }


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

    public static List<Class> getWrapperRaw() {
        return wrappers;
    }
}
