package com.spisoft.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by phoenamandre on 05/12/17.
 */

public class Configuration {
    public static OnAccountCreatedListener sOnAccountCreatedListener;
    public static Map<String, PathObserver> pathObservers = new HashMap<>();
    public static boolean dontDisplayNotification;
    public static List<SyncStatusListener> syncStatusListener = new ArrayList<>();
    public static int icon;

    public static void addPathObserver(String path, PathObserver observer) {
        pathObservers.put(path, observer);
    }

    public interface OnAccountSelectedListener{
        public void onAccountSelected(int accountId, int accountType);
    }
    public interface OnAccountCreatedListener{
        public void onAccountCreated(int accountId, int accountType);
    }
    public interface SyncStatusListener{
        public void onSyncStatusChanged(boolean isSyncing);
    }

    public static void addSyncStatusListener(SyncStatusListener listener) {
        syncStatusListener.add(listener);
    }

    public static void removeSyncStatusListener(SyncStatusListener listener) {
        syncStatusListener.remove(listener);
    }

    public interface PathObserver{
        public void onPathChanged(String path);
    }
    static public OnAccountSelectedListener sOnAccountSelectedListener = null;
}
