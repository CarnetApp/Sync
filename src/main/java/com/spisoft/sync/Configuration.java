package com.spisoft.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by phoenamandre on 05/12/17.
 */

public class Configuration {
    public static OnAccountCreatedListener sOnAccountCreatedListener;
    public static Map<String, PathObserver> pathObservers = new HashMap<>();

    public static void addPathObserver(String path, PathObserver observer) {
        pathObservers.put(path, observer);
    }

    public interface OnAccountSelectedListener{
        public void onAccountSelected(int accountId, int accountType);
    }
    public interface OnAccountCreatedListener{
        public void onAccountCreated(int accountId, int accountType);
    }
    public interface PathObserver{
        public void onPathChanged(String path);
    }
    static public OnAccountSelectedListener sOnAccountSelectedListener = null;
}
