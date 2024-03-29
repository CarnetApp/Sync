package com.spisoft.sync.synchro;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.util.Pair;

import com.spisoft.sync.Configuration;
import com.spisoft.sync.Log;
import com.spisoft.sync.R;
import com.spisoft.sync.RecursiveFileObserver;
import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.database.SyncedFolderDBHelper;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.WrapperFactory;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * Created by alexandre on 25/04/16.
 */
public class SynchroService extends Service{
    private static final String TAG = "SynchroService";
    private static final int ALARM_ID = 1001;
    private static final String TAGOBS = "ObserverTag";
    private Thread mSyncThread;
    private SyncWrapper mGoogleDriveSyncWrapper;
    private int SUCCESS = SyncWrapper.STATUS_SUCCESS;
    private int ERROR =  SyncWrapper.STATUS_FAILURE;
    private int PENDING =  SyncWrapper.STATUS_PENDING;
    private SyncedFolderDBHelper mSyncedFolderDBHelper;
    private Map<String, FileObserver> mObserverList;

    private Stack<String> toSync = new Stack<>();
    private Object lock = new Object();
    private Handler mHandler;
    public static SynchroService sService;
    public static boolean isSyncing = false;
    private String mChannelId = "";
    private String mWarningChannelId = "";

    public static void startIfNeeded(@NotNull Context context) {
        int next = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(context).getString("sync_frequency", "60"));
        if(next == -1)
            return;
        context.startService(new Intent(context, SynchroService.class));
    }

    public static class Result{
        public final String errorMessage;
        public List<String> modifiedFiles = new ArrayList();
        public int status;
        public Result(int status){
            this.status = status;
            this.errorMessage = "";
        }
        public Result(int status, String path){
            this.status = status;
            this.modifiedFiles.add(path);
            this.errorMessage = "";
        }

        public Result(int status, int errorCode, String errorMessage){
            this.status = status;
            this.errorMessage = errorMessage;
        }

        public Result(int status, int errorCode, String errorMessage, List<String> paths){
            this.status = status;
            this.errorMessage = errorMessage;
            this.modifiedFiles.addAll(paths);
        }

        public Result(int status, List<String> paths){
            this.status = status;
            this.modifiedFiles.addAll(paths);
            this.errorMessage = "";
        }

    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //first get all distant files
    //get All local files with needed info


    //first case : new local file

    /*

        iterate throw local files
        give each one of them to wrapper
        wrapper compare to db : not in db, not sync ? compare with online, not sync : sync


     */
    @Override
    public void onCreate(){
        super.onCreate();
        mObserverList = new HashMap<>();
        sService = this;
        mHandler = new android.os.Handler();
    }
    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {
        int ret = super.onStartCommand(intent,flags, startId);
        String path;
        if(intent == null || (path = intent.getDataString())==null)
            path = "all";
        synchronized (lock){
            if(!toSync.contains(path))
                toSync.push(path);
        }

        if (mSyncThread == null || !mSyncThread.isAlive()) {
            showForegroundNotification(this.getString(R.string.waiting_next_sync));
            mSyncThread = new SyncThread();
            mSyncThread.start();

        }
        return ret;
    }

    private static final int NOTIFICATION_ID = 1;

    public void showForegroundNotification(final String contentText) {
        if(Configuration.dontDisplayNotification)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Create intent that will bring our app to the front, as if it was tapped in the app
                // launcher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&mChannelId.isEmpty()) {
                    mChannelId = createNotificationChannel("sync2", "Sync Service");
                }
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(SynchroService.this, mChannelId);
                Notification notification = notificationBuilder.setOngoing(true)
                        .setSmallIcon(Configuration.icon)
                        .setContentText(contentText)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setCategory(Notification.CATEGORY_SERVICE)
                        .build();
                startForeground(NOTIFICATION_ID, notification);
            }
        });
    }

    public void sendWarningNotification(final String text){
        // launcher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O&&mWarningChannelId.isEmpty()) {
            mWarningChannelId = createNotificationChannel("warning_sync", getString(R.string.warning_notification));
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(SynchroService.this, mWarningChannelId);
        Notification notification = notificationBuilder.setOngoing(false)
                .setSmallIcon(Configuration.icon)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_ERROR)
                .build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(new Random().nextInt(1000)+11000, notification);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_LOW);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    public void resetNotification(){
        showForegroundNotification(this.getString(R.string.syncing));
    }

    public static void onSyncFrequencyChange(Context context){
        if(isSyncing)
            return;
        if(sService != null)
            sService.stopSelf();
        SynchroService.startIfNeeded(context);
    }

    public void cancelNextLaunch(){
        Intent intent = new Intent(SynchroService.this, SynchroService.class);
        PendingIntent alarmIntent = PendingIntent.getService(SynchroService.this, ALARM_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(alarmIntent);
    }



    public void planNextLaunch(){
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(SynchroService.this, SynchroService.class);
        int next = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(SynchroService.this).getString("sync_frequency", "60"));
        if(next == -1) {
            SynchroService.this.stopSelf();
            return;
        }
        PendingIntent alarmIntent = PendingIntent.getService(SynchroService.this, ALARM_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.M)
            alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + next*60*1000, alarmIntent);
        showForegroundNotification(SynchroService.this.getString(R.string.waiting_next_sync));
        if(next>=15)
            SynchroService.this.stopSelf();
    }

    private class MyFileObserver extends RecursiveFileObserver {

        public MyFileObserver(String path) {
            super(path);
        }

        @Override
        public void onEvent(int i, String s) {
            Log.d(TAGOBS, "modifications "+s);

        }
    }
    /*

    Pour l'instant :
    on arrive String folder modified
    à partir de là, we obtain every accounts syncing this folder
        we give the wrapper currently modified synced folder as well as the root it cames from
     */
    private class SyncThread extends Thread {
        private Result syncFolder(String syncedFolder, String rootFolder) {
            int status = ERROR;
            List<String> modifiedFiles = new ArrayList<>();
            List<DBAccountHelper.Account> syncedAccounts = mSyncedFolderDBHelper.getRemoteAccountForSyncedFolder(rootFolder);
            {
                for (DBAccountHelper.Account syncedAccount : syncedAccounts) {
                    Log.d(TAG, "account type "+syncedAccount.accountType);
                    Wrapper wrapper = WrapperFactory.getWrapper(SynchroService.this, syncedAccount.accountType, syncedAccount.accountID);
                    SyncWrapper syncWrapper = wrapper.getSyncWrapper(SynchroService.this);
                    syncWrapper.setLocalRootFolder(rootFolder);
                    syncWrapper.setCurrentlySyncedDir(syncedFolder);
                    if ((status = syncWrapper.connect())!= SyncWrapper.STATUS_SUCCESS)
                        return new Result(status, modifiedFiles);
                    if ((status = syncWrapper.loadRootFolder()) != SyncWrapper.STATUS_SUCCESS)
                        return new Result(status, modifiedFiles);
                    Log.d(TAG, "populating");
                    if ((status = syncWrapper.loadDistantFiles()) != SyncWrapper.STATUS_SUCCESS) {
                        Log.d(TAG, "failure");

                        return new Result(status, modifiedFiles);
                    }
                    Log.d(TAG, "success");
                    if (!new File(syncedFolder).exists())
                        continue;
                    Result res = recursiveSync(new File(syncedFolder), syncWrapper, true);
                    modifiedFiles.addAll(res.modifiedFiles);
                    if (res.status == SUCCESS) {
                        res = syncWrapper.endOfSync();
                        modifiedFiles.addAll(res.modifiedFiles);

                        //put parents
                        for(String path : res.modifiedFiles){
                            File parent = new File(path).getParentFile();
                            while(!modifiedFiles.contains(parent.getAbsolutePath())&&!parent.getAbsolutePath().equals(syncedFolder)){
                                Log.d(TAG, "adding parent "+parent.getAbsolutePath());
                                modifiedFiles.add(parent.getAbsolutePath());
                                parent = parent.getParentFile();
                            }
                        }
                        if (res.status == SUCCESS) {
                            status = SUCCESS;

                        }
                    }
                }
            }
            return new Result(status, modifiedFiles);
        }
        public void run(){
            isSyncing = true;
            for (Configuration.SyncStatusListener listener : Configuration.syncStatusListener){
                listener.onSyncStatusChanged(isSyncing);
            }
            int error = SUCCESS;
            String errorMessage = "";
            showForegroundNotification(SynchroService.this.getString(R.string.syncing));
            long start = System.currentTimeMillis();
            Log.d(TAG,"starting sync at "+ DateFormat.getDateTimeInstance().format(new Date(start)));
            //retrieve synced dir
            mSyncedFolderDBHelper = SyncedFolderDBHelper.getInstance(SynchroService.this);
            String syncPath = null;
            int modifiedSize = 0;
            boolean hasAll = false;
            while(true) {
                synchronized (lock){
                    try {
                        syncPath = toSync.pop();
                    }catch (EmptyStackException e){
                        syncPath = null;
                    }
                }
                if(syncPath == null)
                    break;
                if(syncPath.equals("all"))
                    hasAll = true;

                List<Pair<String, String>> syncedFolders = mSyncedFolderDBHelper.getLocalSyncedFolders(syncPath);
                List<String> modifiedFiles = new ArrayList<>();
                for (Pair<String, String> syncedFolder : syncedFolders) {
                    Log.d(TAG, "syncing folder "+syncedFolder.first);
                    Result res = syncFolder(syncedFolder.first, syncedFolder.second);
                    modifiedFiles.addAll(res.modifiedFiles);
                    if (res.status == ERROR) {
                        error = ERROR;
                        errorMessage = res.errorMessage;
                        break;
                    }
                }
                modifiedSize += modifiedFiles.size();
                for(String path : modifiedFiles){
                    List<Configuration.PathObserver> observers = Configuration.getPathObservers(path);
                    Log.d(TAG, "notify observers "+path);

                    if(observers!=null) {
                        ArrayList<String> modifiedInPath = new ArrayList<>();
                        for(String inPath : modifiedFiles){
                            if(inPath.startsWith(path))
                                modifiedInPath.add(inPath);
                        }

                        for(Configuration.PathObserver observer : observers)
                            observer.onPathChanged(path, modifiedInPath);
                    }
                }

            }
            isSyncing = false;
            for (Configuration.SyncStatusListener listener : Configuration.syncStatusListener){
                listener.onSyncStatusChanged(isSyncing);
                if(error == ERROR)
                    listener.onSyncFailure(errorMessage);
                else
                    listener.onSyncSuccess();
            }

            if(hasAll) {
                Log.d(TAG,"sync took "+ getDurationBreakdown(System.currentTimeMillis()-start)+ " modified "+ modifiedSize, Log.LEVEL_ALWAYS_LOG);
                planNextLaunch();

            }
            // sendBroadcast(new Intent(NoteListFragment.ACTION_RELOAD));

        }


        public String getDurationBreakdown(long millis) {
            if(millis < 0) {
                throw new IllegalArgumentException("Duration must be greater than zero!");
            }

            long days = TimeUnit.MILLISECONDS.toDays(millis);
            millis -= TimeUnit.DAYS.toMillis(days);
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            millis -= TimeUnit.HOURS.toMillis(hours);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            millis -= TimeUnit.MINUTES.toMillis(minutes);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

            StringBuilder sb = new StringBuilder(64);
            sb.append(days);
            sb.append(" Days ");
            sb.append(hours);
            sb.append(" Hours ");
            sb.append(minutes);
            sb.append(" Minutes ");
            sb.append(seconds);
            sb.append(" Seconds");

            return sb.toString();
        }

        private Result recursiveSync(File file, SyncWrapper syncWrapper, boolean isRoot) {
            List<String>modifiedFiles = new ArrayList<>();
            boolean hasFailedOnce = false;
            String errorMessage = "";
            Result result;
            if(file.getName().startsWith(".donotsync"))
                return new Result(SUCCESS, modifiedFiles);
            if(file.isDirectory()){
                int folderStatus = 0;
                if(true) {
                    boolean hasAddedFolder = false;
                    Log.d(TAG,"folder detected "+file.getAbsolutePath());
                    if(!isRoot) {
                        result = syncWrapper.onFolder(file, false);
                        folderStatus = result.status;
                        modifiedFiles.addAll(result.modifiedFiles);
                        if(result.modifiedFiles.size()>0)
                            hasAddedFolder = true;
                        if (folderStatus == ERROR)
                            return new Result(ERROR, modifiedFiles);
                    }
                    File[] files = file.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            Log.d(TAG, "for file "+f.getAbsolutePath());
                            result = recursiveSync(f, syncWrapper, false);
                            modifiedFiles.addAll(result.modifiedFiles);
                            if(result.modifiedFiles.size()>0 && !hasAddedFolder){
                                Log.d(TAG, "adding folder "+file.getAbsolutePath());

                                modifiedFiles.add(file.getAbsolutePath());
                                hasAddedFolder = true;
                            }
                            if (result.status != SUCCESS) {
                                hasFailedOnce = true;
                                errorMessage += result.errorMessage;
                                if(!errorMessage.endsWith("\n")) errorMessage += "\n";
                            }
                        }
                    }
                    if(folderStatus == PENDING && !hasFailedOnce){
                        files = file.listFiles();
                        if (files != null || files.length == 0) {
                            result = syncWrapper.onFolder(file, true);
                            modifiedFiles.addAll(result.modifiedFiles);
                            if (result.status == ERROR)
                                return new Result(ERROR, modifiedFiles);
                        }
                    }
                }
            }
            else {
                Log.d(TAG,"file detected "+file.getAbsolutePath());
                //sync
                result = syncWrapper.onFile(file);
                modifiedFiles.addAll(result.modifiedFiles);
                if(result.status == ERROR)
                {
                    hasFailedOnce = true;
                    errorMessage += result.errorMessage;
                    if(!errorMessage.endsWith("\n")) errorMessage += "\n";
                }

            }
            if(hasFailedOnce)
                return new Result(ERROR, -1, errorMessage, modifiedFiles);
            else
                return new Result(SUCCESS, modifiedFiles);

        }
    }
}
