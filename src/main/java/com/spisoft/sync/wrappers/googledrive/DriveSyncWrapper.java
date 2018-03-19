package com.spisoft.sync.wrappers.googledrive;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.spisoft.sync.synchro.SyncWrapper;
import com.spisoft.sync.synchro.SynchroService;
import com.spisoft.sync.utils.FileLocker;
import com.spisoft.sync.utils.FileUtils;
import com.spisoft.sync.wrappers.Wrapper;
import com.spisoft.sync.wrappers.nextcloud.NextCloudSyncedFoldersDBHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alexandre on 25/04/16.
 */
public class DriveSyncWrapper extends SyncWrapper implements ResultCallback<DriveApi.MetadataBufferResult>, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "DriveWrapper";
    private static final String MD5_CUSTOM_KEY = "md5_key";
    public static final int RESOLVE_CONNECTION_REQUEST_CODE = 1001;
    private GoogleDriveAccountHelper.GoogleAccount mGoogleAccount;
    private Map<String,Metadata> metadataList = new HashMap<>();
    private Map<String,Object> metadataFullList = new HashMap<>(); // even when folder, do not start with / or end with /
    private GoogleApiClient mGoogleApiClient;
    private Activity mActiviy;
    private Map<String, Metadata> metadataDownloadList = new HashMap<>();
    private String mRootPath;
    private String mRemoteRootPath;

    public DriveSyncWrapper(Context ct, Integer accountID) {
        super(ct, accountID);
        mGoogleAccount = GoogleDriveAccountHelper.getInstance(ct).getGoogleAccount(accountID);

        mGoogleApiClient = new GoogleApiClient.Builder(ct)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


       // mGoogleApiClient = mGoogleAccount.googleApiClient;
    }


    public int connect(){
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if(!connectionResult.isSuccess())
            return ERROR;
        Status result = Drive.DriveApi.requestSync(mGoogleApiClient).await(); //SYNC file status

        return STATUS_SUCCESS;
    }














    OnConnectiongListener mOnConnectiongListener = null;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DriveSyncWrapper.RESOLVE_CONNECTION_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    mGoogleApiClient.connect();
                }

                break;
        }
    }

    public interface OnConnectiongListener{
        void onConnected();
        void onConnectionFailed();

    }

    public void authorize(Activity activity, OnConnectiongListener listener){
        mOnConnectiongListener = listener;
        mActiviy = activity;
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed( ConnectionResult connectionResult) {
        if(mActiviy==null)
            return;
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(mActiviy, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {

            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), mActiviy, 0).show();
        }
    }

    @Override
    public void onConnected( Bundle bundle) {
        if(mOnConnectiongListener != null)
            mOnConnectiongListener.onConnected();
    }

    public void resolve(int requestCode) {
    }







    @Override
    public void setLocalRootFolder(String rootPath){
        mRootPath = rootPath;
        //load distant path
        mRemoteRootPath = DriveSyncedFoldersDBHelper.getInstance(mContext).getRemoteSyncedPathForLocal(mAccountID,mRootPath);
    }

    public String getRelativePathFromAbsolute(String localPath) {
        String remotePath = localPath.substring(mRootPath.length());
        if (remotePath.startsWith("/"))
            remotePath = remotePath.substring(1);
        return remotePath;
    }

    @Override
    public SynchroService.Result onFile(File file, String md5){
        String relativePath = getRelativePathFromAbsolute(file.getAbsolutePath());
        metadataDownloadList.remove(relativePath); //won't need to download
        DBDriveFileHelper.DBDriveFile driveFile =DBDriveFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, relativePath);
        if(driveFile==null)
            driveFile = new DBDriveFileHelper.DBDriveFile(relativePath,null);
        if(metadataList.containsKey(relativePath)){ //exists
            Log.d(TAG, "Distant File exists "+relativePath);

            if(!md5.equals(driveFile.md5)){ //file was modified locally
                Metadata distant = metadataList.get(relativePath);
                if(distant.getModifiedDate().getTime()!=driveFile.lastOnlineModifiedDate){//modified externally
                    Log.d(TAG, "Distant File has been modified, trying avoid conflict ");
                    Log.d(TAG, "distant File has been modified on "+distant.getModifiedDate().getTime());
                    Log.d(TAG, "whereas last time "+driveFile.lastOnlineModifiedDate);
                    //get file, get md5, compare
                    File newFile = new File(FileUtils.stripExtensionFromName(file.getAbsolutePath())+System.currentTimeMillis()+"."+FileUtils.getExtension(file.getAbsolutePath()));
                    file.renameTo(newFile);
                    int result = dowloadFile(relativePath,file.getAbsolutePath());
                    if(result==ERROR)
                        return new SynchroService.Result(ERROR);
                    String md5Download = FileUtils.md5(file.getAbsolutePath());
                    Log.d(TAG, "distant File md5Download "+md5Download);
                    Log.d(TAG, "whereas local "+md5);

                    if(md5Download.equals(md5)){//file was the same, updating
                        Log.d(TAG, "distant file was the same, updating");
                        driveFile.lastOnlineModifiedDate = distant.getModifiedDate().getTime();
                        driveFile.md5 = md5;
                        //delete tmp file
                        newFile.delete();
                        DBDriveFileHelper.getInstance(mContext).addOrReplaceDBDriveFile(driveFile);

                    }else{ //conflict
                        //moving local file to another location
                        Log.d(TAG, "conflit file, keeping both");
                    }

                }else{//not modified on server : upload
                    Log.d(TAG, "File wasn't up to date on server, uploading local file"+relativePath);
                    int result = uploadAndSave(driveFile, relativePath, md5, file);
                    if(result==ERROR)
                        return new SynchroService.Result(ERROR);
                }

            }else{//Not modified locally

                //check online state
                Metadata distant = metadataList.get(relativePath);
                if((distant.getModifiedDate().getTime()-driveFile.lastOnlineModifiedDate)!=0) {//modified externally
                    Log.d(TAG, "old date "+driveFile.lastOnlineModifiedDate);
                    Log.d(TAG, "new date "+distant.getModifiedDate().getTime());
                    Log.d(TAG, "bool "+((distant.getModifiedDate().getTime()-driveFile.lastOnlineModifiedDate)!=0));
                    Log.d(TAG, "sub date "+(distant.getModifiedDate().getTime()-driveFile.lastOnlineModifiedDate));
                    int result = dowloadFile(relativePath,file.getAbsolutePath());
                    if(result==ERROR)
                        return new SynchroService.Result(ERROR);;
                    String md5Download = FileUtils.md5(file.getAbsolutePath());
                    driveFile.md5 = md5Download;
                    driveFile.lastOnlineModifiedDate = distant.getModifiedDate().getTime();

                    DBDriveFileHelper.getInstance(mContext).addOrReplaceDBDriveFile(driveFile);
                    new SynchroService.Result(STATUS_SUCCESS, file.getAbsolutePath());
                }
                else {
                    //nothing to do here

                }

            }

        }else{ //NOT on server
            Log.d(TAG, "Distant File doesn't exist "+relativePath);

            if(/**/true){
                //check whether we had LAST version online
                if(md5.equals(driveFile.md5)){
                    Log.d(TAG, "File was up to date on server, deleting local file"+relativePath);
                    if(file.delete());
                        DBDriveFileHelper.getInstance(mContext).delete(driveFile);
                    new SynchroService.Result(STATUS_SUCCESS, file.getAbsolutePath());
                }
                else{
                    Log.d(TAG, "File wasn't up to date on server, uploading local file"+relativePath);
                    int result = uploadAndSave(driveFile, relativePath, md5, file);
                    if(result==ERROR)
                        return new SynchroService.Result(ERROR);
                }
            }

        }
        return new SynchroService.Result(STATUS_SUCCESS);
    }

    @Override
    public SynchroService.Result endOfSync() {
        //download
        List<String> modifiedFiles = new ArrayList<>();
        for(String file : metadataDownloadList.keySet()){
            Metadata metadata = metadataDownloadList.get(file);
            DBDriveFileHelper.DBDriveFile driveFile =DBDriveFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, file);
            if(driveFile==null)
                driveFile = new DBDriveFileHelper.DBDriveFile(file,null);
            if(metadata.getModifiedDate().getTime() == driveFile.lastOnlineModifiedDate){
                Log.d(TAG, "File was deleted locally, deleting distant File");
                Status result = metadata.getDriveId().asDriveFile().trash(mGoogleApiClient).await();
                if(!result.isSuccess())
                    return new SynchroService.Result(ERROR, modifiedFiles);
                DBDriveFileHelper.getInstance(mContext).delete(driveFile);

            }else{
                Log.d(TAG, "Distant File "+file+" not on local");
                String dest = mRootPath+"/"+file;
                Log.d(TAG, "Download distant File "+file+" to "+dest);
                Log.d(TAG, "Distant File "+file+" not on local");
                int result = dowloadFile(file, dest);
                if(result==ERROR)
                    return new SynchroService.Result(ERROR, modifiedFiles);
                modifiedFiles.add(dest);
                String md5Download = FileUtils.md5(dest);
                driveFile.md5 = md5Download;
                Log.d(TAG, "Distant down debug time "+ metadata.getModifiedDate().getTime());
                driveFile.lastOnlineModifiedDate = metadata.getModifiedDate().getTime();
                DBDriveFileHelper.getInstance(mContext).addOrReplaceDBDriveFile(driveFile);
            }


        }
        return new SynchroService.Result(STATUS_SUCCESS, modifiedFiles);
    }

    public int uploadAndSave(DBDriveFileHelper.DBDriveFile driveFile, String relativePath,String md5, File file){
        if(metadataList.containsKey(relativePath))
            Log.d(TAG, "last upload date : "+metadataList.get(relativePath).getModifiedDate().getTime());
        int result = upload(file, md5,relativePath);
        if(result==ERROR)
            return result;
        long date = metadataList.get(relativePath).getModifiedDate().getTime();
        Log.d(TAG, "new upload date : "+date);
        driveFile.accountID=mAccountID;
        driveFile.md5 = md5;
        driveFile.lastOnlineModifiedDate = date;
        driveFile.relativePath = relativePath;
        DBDriveFileHelper.getInstance(mContext).addOrReplaceDBDriveFile(driveFile);
        return STATUS_SUCCESS;
    }

    private int dowloadFile(String relativeSourcePath, String absoluteDestinationPath) {
        synchronized (FileLocker.getLockOnPath(absoluteDestinationPath)) {
            File file = new File(absoluteDestinationPath);
            file.getParentFile().mkdirs();
            file.delete();

            DriveApi.DriveContentsResult fileResult = metadataList.get(relativeSourcePath).getDriveId().asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
            if (!fileResult.getStatus().isSuccess())
                return ERROR;
            FileLock lock = null;
            try {
                file.createNewFile();
                FileOutputStream outputStream = new FileOutputStream(file);
                lock = outputStream.getChannel().lock();
                FileUtils.copy(fileResult.getDriveContents().getInputStream(), outputStream);

                return STATUS_SUCCESS;
            } catch (IOException e) {
                if (lock != null)
                    try {
                        lock.release();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                return ERROR;
            }
        }
    }

    public int loadRootFolder(){

        if(mGoogleAccount==null){
            mGoogleAccount = new GoogleDriveAccountHelper.GoogleAccount();

        }
        if(mGoogleAccount.rootFolder==null){ //get folder if exists
            DriveFolder folder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
            DriveApi.MetadataBufferResult result = folder.listChildren(mGoogleApiClient).await();
            if (!result.getStatus().isSuccess()) {
                return ERROR;
            }
            for(Metadata metadata : result.getMetadataBuffer()){
                if(metadata.isFolder()){
                    if(metadata.getTitle().equals(mRemoteRootPath)&&!metadata.isTrashed()&&!metadata.isExplicitlyTrashed()&&metadata.isEditable())
                        mGoogleAccount.rootFolder = metadata.getDriveId().encodeToString();

                }

            }
            if(mGoogleAccount.rootFolder==null){ //create folder if doesn't exist
                MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                        .setTitle(mRemoteRootPath).build();
                DriveFolder.DriveFolderResult result2 = folder.createFolder(mGoogleApiClient, changeSet).await();
                if (!result2.getStatus().isSuccess()) {
                    return ERROR;
                }
                DriveResource.MetadataResult resultMetadata = result2.getDriveFolder().getMetadata(mGoogleApiClient).await();
                if (!resultMetadata.getStatus().isSuccess()) {
                    return ERROR;
                }
                mGoogleAccount.rootFolder = resultMetadata.getMetadata().getDriveId().encodeToString();
                Log.d(TAG, "new root folder "+ mGoogleAccount.rootFolder);

            }
            metadataFullList.put("",DriveId.decodeFromString(mGoogleAccount.rootFolder).asDriveFolder());
            Log.d(TAG, "setting root folder "+ mGoogleAccount.rootFolder);

        }

        return STATUS_SUCCESS;
    }

    @Override
    public void setCurrentlySyncedDir(String rootPath) {

    }

    @Override
    public SynchroService.Result onFolder(File file, boolean secondPathWithFolderEmpty) {
        return null;
    }


    public int upload(File file, String md5, String relativePath) {
        synchronized (FileLocker.getLockOnPath(file.getAbsolutePath())) {
            List<String> folderToCreate = new ArrayList<>();

            boolean leave = false;
            File relativeFile = new File(relativePath).getParentFile();
            String highestPath = "";
            while (!leave && relativeFile != null) {

                String path = relativeFile.getAbsolutePath();
                if (path.startsWith("/"))
                    path = path.substring(1);
                if (metadataFullList.containsKey(path) || relativeFile == null || path.equals("")) {
                    highestPath = path;
                    break;
                } else folderToCreate.add(path);
                relativeFile = relativeFile.getParentFile();

            }
            Object cur = metadataFullList.get(highestPath);
            Log.d(TAG, "highestPath " + highestPath);
            if (cur instanceof Metadata && ((Metadata) cur).isFolder() || cur instanceof DriveFolder) {
                DriveFolder currentDriveFolder = null;
                if (cur instanceof Metadata)
                    currentDriveFolder = ((Metadata) cur).getDriveId().asDriveFolder();
                else
                    currentDriveFolder = (DriveFolder) cur;
                Log.d(TAG, "currentDriveFolder " + currentDriveFolder.getDriveId());
                for (int i = folderToCreate.size() - 1; i >= 0; i--) {
                    Log.d(TAG, "creating folder " + folderToCreate.get(i));
                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(Uri.parse(folderToCreate.get(i)).getLastPathSegment()).build();
                    DriveFolder.DriveFolderResult result = currentDriveFolder.createFolder(mGoogleApiClient, changeSet).await();
                    if (!result.getStatus().isSuccess()) {
                        return ERROR;
                    }
                    currentDriveFolder = result.getDriveFolder();
                    metadataFullList.put(folderToCreate.get(i), currentDriveFolder);
                }

                FileLock lock = null;
                try {
                    RandomAccessFile random = new RandomAccessFile(file, "rw");
                    lock = random.getChannel().lock();
                    OutputStream out;
                    DriveApi.DriveContentsResult resultContent;
                    boolean create = false;
                    if (metadataList.containsKey(relativePath)) {
                        resultContent = metadataList.get(relativePath).getDriveId().asDriveFile().open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
                    } else {
                        resultContent = Drive.DriveApi.newDriveContents(mGoogleApiClient).await();
                        create = true;
                    }

                    if (!resultContent.getStatus().isSuccess()) {
                        return ERROR;
                    }
                    out = resultContent.getDriveContents().getOutputStream();
                    FileUtils.copy(random, out);

                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                            .setTitle(Uri.parse(relativePath).getLastPathSegment())
                            .setCustomProperty(new CustomPropertyKey(MD5_CUSTOM_KEY, CustomPropertyKey.PUBLIC), md5).build();
                    DriveFile toPut = null;
                    if (create) {
                        DriveFolder.DriveFileResult result = currentDriveFolder.createFile(mGoogleApiClient, changeSet, resultContent.getDriveContents()).await();
                        if (!result.getStatus().isSuccess()) {
                            return ERROR;
                        }
                        toPut = result.getDriveFile();
                    } else {
                        Status result2 = resultContent.getDriveContents().commit(mGoogleApiClient, changeSet).await();
                        if (!result2.getStatus().isSuccess()) {
                            return ERROR;
                        }
                        toPut = metadataList.get(relativePath).getDriveId().asDriveFile();

                    }

                    metadataFullList.put(relativePath, toPut);
                    DriveResource.MetadataResult resultMetadata = toPut.getMetadata(mGoogleApiClient).await();
                    if (!resultMetadata.getStatus().isSuccess()) {
                        return ERROR;
                    }
                    Log.d(TAG, "upload success");
                    metadataList.put(relativePath, resultMetadata.getMetadata());
                    //setLast
                    if (lock != null)
                        try {
                            lock.release();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                } catch (FileNotFoundException e) {
                    if (lock != null)
                        try {
                            lock.release();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    return ERROR;
                } catch (IOException e) {
                    e.printStackTrace();
                    if (lock != null)
                        try {
                            lock.release();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    return ERROR;
                }

            } else
                return ERROR;
            return STATUS_SUCCESS;
        }
    }


    @Override
    public int loadDistantFiles() {

        return recursiveLoadFile(DriveId.decodeFromString(mGoogleAccount.rootFolder),"");
    }


    private int recursiveLoadFile(DriveId folderID, String relativePath){
        DriveFolder folder = folderID.asDriveFolder();
        DriveApi.MetadataBufferResult result = folder.listChildren(mGoogleApiClient).await();
        if (!result.getStatus().isSuccess()) {
            return ERROR;
        }
        for(Metadata metadata : result.getMetadataBuffer()){
            if(metadata.isTrashed()||metadata.isExplicitlyTrashed())
                continue;
            Log.d(TAG,"found file "+metadata.getTitle() +" t "+metadata.isTrashable());
            if(metadata.isFolder()){
                int status = recursiveLoadFile(metadata.getDriveId(),relativePath+metadata.getTitle()+"/");
                if(status!=STATUS_SUCCESS)
                    return status;
            }
            else {
                Log.d(TAG,"adding file "+relativePath+metadata.getTitle());
                metadataList.put(relativePath+metadata.getTitle(),metadata);
                metadataDownloadList.put(relativePath+metadata.getTitle(),metadata);

            }
            metadataFullList.put(relativePath+metadata.getTitle(),metadata);
            Log.d(TAG,"adding file or folder "+relativePath+metadata.getTitle());
        }

        return STATUS_SUCCESS;
    }

    @Override
    public void onResult( DriveApi.MetadataBufferResult result) {

    }

}
