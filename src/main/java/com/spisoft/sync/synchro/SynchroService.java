package com.spisoft.sync.synchro;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.util.Pair;

import com.spisoft.sync.Configuration;
import com.spisoft.sync.Log;
import com.spisoft.sync.R;
import com.spisoft.sync.RecursiveFileObserver;
import com.spisoft.sync.account.DBAccountHelper;
import com.spisoft.sync.database.SyncedFolderDBHelper;
import com.spisoft.sync.utils.FileUtils;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.WrapperFactory;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

/**
 * Created by alexandre on 25/04/16.
 */
public class SynchroService extends Service{
    private static final String TAG = "SynchroService";
    private static final int ALARM_ID = 1001;
    private static final long REPEAT = 60*1000;
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

    public static class Result{
        public Result(int status){
            this.status = status;
        }
        public Result(int status, String path){
            this.status = status;
            this.modifiedFiles.add(path);
        }
        public Result(int status, List<String> paths){
            this.status = status;
            this.modifiedFiles.addAll(paths);
        }
        public List<String> modifiedFiles = new ArrayList();
        public int status;
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
        String path = intent.getDataString();
        if(path==null)
            path = "all";
        synchronized (lock){
            if(!toSync.contains(path))
                toSync.push(path);
        }

        if (mSyncThread == null || !mSyncThread.isAlive()) {
            showForegroundNotification("Pending");
            mSyncThread = new SyncThread();
            mSyncThread.start();

        }
        return ret;
    }

    private static final int NOTIFICATION_ID = 1;

    public void showForegroundNotification(final String contentText) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Create intent that will bring our app to the front, as if it was tapped in the app
                // launcher
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setContentTitle(getString(R.string.app_name))
                        .setContentText(contentText)
                        .setSmallIcon(R.drawable.threedots)
                        .setWhen(System.currentTimeMillis())
                        .build();
                startForeground(NOTIFICATION_ID, notification);
            }
        });
    }

    public void resetNotification(){
        showForegroundNotification("Syncing...");
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
                    if (!new File(syncedFolder).exists()) {
                        Log.d(TAG, "creating root " + new File(syncedFolder).mkdirs());

                    }
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

            showForegroundNotification("Syncing...");
            long start = System.currentTimeMillis();
            Log.d(TAG,"starting sync at "+ DateFormat.getDateTimeInstance().format(new Date(start)));
            //retrieve synced dir
            mSyncedFolderDBHelper = SyncedFolderDBHelper.getInstance(SynchroService.this);
            String syncPath = null;
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
                    if (res.status == ERROR)
                        break;
                }
                for(String path : modifiedFiles){
                    Configuration.PathObserver observer = Configuration.pathObservers.get(path);
                    Log.d(TAG, "notify observers "+path);

                    if(observer!=null)
                        observer.onPathChanged(path);
                }

            }
            if(hasAll) {
                Log.d(TAG,"sync took "+ getDurationBreakdown(System.currentTimeMillis()-start)
                );
                AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
                Intent intent = new Intent(SynchroService.this, SynchroService.class);
                PendingIntent alarmIntent = PendingIntent.getService(SynchroService.this, ALARM_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + REPEAT, alarmIntent);
                showForegroundNotification("Pending");

                 SynchroService.this.stopSelf();
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
            Result result;
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
                            if (result.status != SUCCESS)
                                return new Result(ERROR, modifiedFiles);
                        }
                    }
                    if(folderStatus == PENDING){
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
                String md5 = FileUtils.md5(file.getAbsolutePath());
                result = syncWrapper.onFile(file, md5);
                modifiedFiles.addAll(result.modifiedFiles);
                if(result.status == ERROR)
                {
                    Log.d(TAG,"file ERROR ");

                    return new Result(ERROR, modifiedFiles);
                }

            }
            return new Result(SUCCESS, modifiedFiles);

        }
    }
}
