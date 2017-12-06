package com.spisoft.sync.wrappers;

import android.content.Context;

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
            NextCloudWrapper.class
    };

    public static Wrapper getWrapper(Context ct, int accountType, Integer accountID) {
        try {
            for(Class wrapperClass : wrappers){
            Method m = null;

                m = wrapperClass.getMethod("isMyAccount", Integer.class);
                m.setAccessible(true);
                boolean result = (boolean) m.invoke(wrapperClass, new Integer(accountType));
                if(result){
                    return(Wrapper) wrapperClass.getConstructor(Context.class, Integer.class).newInstance(ct, accountID);
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
}
