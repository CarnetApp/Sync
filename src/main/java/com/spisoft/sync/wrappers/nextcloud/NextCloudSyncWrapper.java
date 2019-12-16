package com.spisoft.sync.wrappers.nextcloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.network.NetworkUtils;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.Log;
import com.spisoft.sync.R;
import com.spisoft.sync.synchro.SyncWrapper;
import com.spisoft.sync.synchro.SynchroService;
import com.spisoft.sync.utils.FileLocker;
import com.spisoft.sync.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alexandre on 25/04/16.
 */
public class NextCloudSyncWrapper extends SyncWrapper {
    private static final String TAG = "NextCloudWrapper";
    private final NextCloudWrapper mWrapper;
    private String mRootPath;
    private String mRemoteRootPath;
    private Map <String, RemoteFile> mRemoteFiles;
    //link between relative path and remoteFile
    private Map <String, RemoteFile> metadataDownloadList;
    private String mCurrentlyLocalSyncedDir;

    public static X509Certificate cert = null;

    public NextCloudSyncWrapper(Context ct, int accountID, NextCloudWrapper wrapper) {
        super(ct, accountID);
        mWrapper = wrapper;
        mRemoteFiles = new HashMap();
        metadataDownloadList = new HashMap();

    }


    public int connect(){

        return STATUS_SUCCESS;
    }

    @Override
    public int loadRootFolder() {
        return STATUS_SUCCESS;
    }

    @Override
    public void setLocalRootFolder(String rootPath){
        mRootPath = rootPath;
        //load distant path
        mRemoteRootPath = NextCloudSyncedFoldersDBHelper.getInstance(mContext).getRemoteSyncedPathForLocal(mAccountID,mRootPath);
    }
    @Override
    public void setCurrentlySyncedDir(String syncedDir){
        mCurrentlyLocalSyncedDir = syncedDir;
    }
    @Override
    public SynchroService.Result onFolder(File file, boolean secondPathWithFolderEmpty) {
        String remotePath = getRemotePathFromLocal(file.getAbsolutePath());

        Log.d(TAG, "onFolder "+remotePath);

        if(metadataDownloadList.containsKey(remotePath)){
            Log.d(TAG, "already contains folder "+remotePath);
            metadataDownloadList.remove(remotePath);
            return new SynchroService.Result(STATUS_SUCCESS);
        }
        else {
            NextCloudFileHelper.DBNextCloudFile dbNextCloudFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remotePath);
            if(dbNextCloudFile!=null){
                //folder was there before, delaying folder creation
                Log.d(TAG, "folder was there, should delete");

                if(!secondPathWithFolderEmpty)
                    return new SynchroService.Result(STATUS_PENDING);
                else {//folder empty, delete
                    Log.d(TAG, "deleting folder");
                    boolean delete = file.delete();
                    Log.d(TAG, "folder deleted "+delete);
                    if(delete) {
                        NextCloudFileHelper.getInstance(mContext).delete(dbNextCloudFile);
                        return new SynchroService.Result(STATUS_SUCCESS,file.getAbsolutePath());
                    }
                    else return new  SynchroService.Result(STATUS_FAILURE);
                }

            }else {
                Log.d(TAG, "creating folder " + remotePath);

                dbNextCloudFile = new NextCloudFileHelper.DBNextCloudFile(remotePath);
                dbNextCloudFile.md5 = "";//init md5 even when folder
                dbNextCloudFile.accountID = mAccountID;
                return new SynchroService.Result(uploadFileAndRecord(file, remotePath, null, dbNextCloudFile));
            }
        }
    }

    public void authorize(Activity activity){
       /* mActiviy = activity;
        mGoogleApiClient.connect();
*/

    }

    public String getRemotePathFromLocal(String localPath){
        String remotePath = localPath.substring(mRootPath.length());
        if (remotePath.startsWith("/"))
            remotePath = remotePath.substring(1);
        remotePath = mRemoteRootPath + (mRemoteRootPath.endsWith("/")?"":"/")+remotePath;
        if (remotePath.startsWith("/"))
            remotePath = remotePath.substring(1);
        if(remotePath.endsWith("/"))
            remotePath = remotePath.substring(0, remotePath.length()-1);

        return remotePath;
    }


    @Override
    public SynchroService.Result onFile(File file) {
        String md5 = null;
        String remotePath = getRemotePathFromLocal(file.getAbsolutePath());
        Log.d(TAG, "OnFile "+remotePath);

        RemoteFile remoteFile = metadataDownloadList.get(remotePath);
        NextCloudFileHelper.DBNextCloudFile dbNextCloudFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remotePath);

        if(dbNextCloudFile == null) {
            Log.d(TAG, "db null");
            dbNextCloudFile = new NextCloudFileHelper.DBNextCloudFile(remotePath);
            dbNextCloudFile.accountID = mAccountID;

        }
        else if (dbNextCloudFile.lastMod == -1 && dbNextCloudFile.md5 != null && ! dbNextCloudFile.md5.isEmpty()){
            //last round wasn't using last modification date, but md5. So we will need the md5 sum for the process
            md5 = FileUtils.md5(file.getAbsolutePath());
        }
        if(remoteFile!=null) { //file exists !
            metadataDownloadList.remove(remotePath);//won't need to download
            Log.d(TAG, "Distant File exists "+remotePath);

            // md5 != null => we need to use md5
            if(md5 != null && !md5.equals(dbNextCloudFile.md5) || md5 == null && dbNextCloudFile.lastMod != file.lastModified()) { //file was modified locally
                if(!remoteFile.getEtag().equals(dbNextCloudFile.currentlyDownloadedOnlineEtag)) {//modified externally
                    try {
                        if (md5 == null)
                            md5 = FileUtils.md5(file.getAbsolutePath()); //we will need the md5 to avoid the conflict
                    } catch(Exception e){
                        return new SynchroService.Result(STATUS_FAILURE, -1, e.toString());
                    }
                    // try to avoid conflict
                    Log.d(TAG, "conflict (dl: "+dbNextCloudFile.currentlyDownloadedOnlineEtag+", online: "+remoteFile.getEtag()+")");
                    File newFile = new File(FileUtils.stripExtensionFromName(file.getAbsolutePath())+System.currentTimeMillis()+"."+FileUtils.getExtension(file.getAbsolutePath()));
                    file.renameTo(newFile);
                    int success = downloadFileAndRecord(remoteFile, file.getAbsolutePath(), dbNextCloudFile);
                    if(success != STATUS_SUCCESS)
                        return new SynchroService.Result(success);
                    String downloadedMD5 = FileUtils.md5(file.getAbsolutePath());
                    Log.d(TAG,"dl "+dbNextCloudFile.md5+" calc "+downloadedMD5);

                    if(downloadedMD5.equals(md5)){
                        return new SynchroService.Result(newFile.delete()?STATUS_SUCCESS:STATUS_FAILURE, file.getAbsolutePath());
                    }else{
                        NextCloudFileHelper.DBNextCloudFile newDbNextCloudFile = new NextCloudFileHelper.DBNextCloudFile(getRemotePathFromLocal(newFile.getAbsolutePath()));
                        newDbNextCloudFile.accountID = mAccountID;
                        if(uploadFileAndRecord(newFile, newDbNextCloudFile.relativePath,md5, newDbNextCloudFile)==STATUS_SUCCESS){
                            List<String> modPath = new ArrayList<>();
                            modPath.add(file.getAbsolutePath());
                            modPath.add(newFile.getAbsolutePath());
                            return new SynchroService.Result(STATUS_SUCCESS, modPath);
                        }
                    }
                }else{
                    //upload
                    Log.d(TAG, "file was modified locally");

                    return new SynchroService.Result(uploadFileAndRecord(file, remotePath, md5,dbNextCloudFile));
                }
            }else{
                Log.d(TAG, "file wasn't modified locally "+remoteFile.getRemotePath());
                //check online state
                if(!remoteFile.getEtag().equals(dbNextCloudFile.currentlyDownloadedOnlineEtag)) {//modified externally
                    Log.d(TAG, "remote file was modified, downloading... ");
                    //download
                    return new SynchroService.Result(downloadFileAndRecord(remoteFile, file.getAbsolutePath(), dbNextCloudFile),file.getAbsolutePath());

                }
                else {
                    Log.d(TAG, "file wasn't modified remotely");
                    if(md5 != null){// we need to update lastMod parameter to avoid using md5 on next sync
                        dbNextCloudFile.lastMod = file.lastModified();
                        NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(dbNextCloudFile);
                        Log.d(TAG, "saving last modified to avoid using md5");
                    }

                    return new SynchroService.Result(STATUS_SUCCESS);
                }
            }
        }else {
            //remote file doesn't exist.
            //check if was deleted
            if(dbNextCloudFile.currentlyDownloadedOnlineEtag !=null && !dbNextCloudFile.currentlyDownloadedOnlineEtag.isEmpty()){
                //was on server, checking if last version has been deleted
                if(md5 != null && md5.equals(dbNextCloudFile.md5) || md5 == null && dbNextCloudFile.lastMod == file.lastModified()) {
                    //last version was on server, deleting local file
                    if (!file.delete())
                        return new SynchroService.Result(STATUS_FAILURE);
                    else{
                        NextCloudFileHelper.getInstance(mContext).delete(dbNextCloudFile);
                        return new SynchroService.Result(STATUS_SUCCESS, file.getAbsolutePath());
                    }
                }
                else{
                    //uploading new version
                    return new SynchroService.Result(uploadFileAndRecord(file, remotePath, md5,dbNextCloudFile));
                }
            }else{
                return new SynchroService.Result(uploadFileAndRecord(file, remotePath, md5,dbNextCloudFile));
            }
        }
        return new SynchroService.Result(STATUS_SUCCESS);
    }

    private int uploadFileAndRecord(File file, String remotePath, String md5, NextCloudFileHelper.DBNextCloudFile nextCloudFile) {
        Log.d(TAG, "uploading "+file.getAbsolutePath());
        if(file.isDirectory()) {
            Log.d(TAG, "uploading directory ");

            boolean success = mWrapper.getFileOperation().mkdir(remotePath);
            if(success){
                Log.d(TAG, "CreateRemoteFolderOperation success ");

                //record it
                RemoteFile remoteFile = mWrapper.getFileOperation().getFileInfo(remotePath);
                Log.d(TAG, "CreateRemoteFolderOperation success ");

                if(remoteFile != null){
                    Log.d(TAG, "CreateRemoteFolderOperation etag  "+remoteFile.getEtag());
                    mRemoteFiles.put(remotePath, remoteFile);
                    nextCloudFile.currentlyDownloadedOnlineEtag = remoteFile.getEtag();
                    nextCloudFile.remoteMimeType = remoteFile.getMimeType();
                    NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(nextCloudFile);
                    return STATUS_SUCCESS;
                }
                

            }
        }
        else {
            File tmp = null;
            synchronized (FileLocker.getLockOnPath(file.getAbsolutePath())) {
                Log.d(TAG, "upload file ");
                SynchroService.sService.showForegroundNotification(mContext.getString(R.string.uploading) + " " + Uri.parse(remotePath).getLastPathSegment());
                tmp = new File(mContext.getExternalCacheDir(), ".tmp.upload.note");
                tmp.delete();
                try {
                    FileUtils.copy(new FileInputStream(file), new FileOutputStream(new File(mContext.getExternalCacheDir(), ".tmp.upload.note")));
                } catch (Exception e){
                    tmp = null;
                }
            }
            if(tmp == null)
                return STATUS_FAILURE;
            boolean isSuccess = mWrapper.getFileOperation().upload(tmp.getAbsolutePath(), remotePath);
            tmp.delete();
            SynchroService.sService.resetNotification();
            if (isSuccess) {
                //record it
                Log.d(TAG, "upload success ");
                RemoteFile remoteFile = mWrapper.getFileOperation().getFileInfo(remotePath);
                if (remoteFile != null) {
                    Log.d(TAG, "read success ");
                    nextCloudFile.currentlyDownloadedOnlineEtag = remoteFile.getEtag();
                    nextCloudFile.md5 = md5;
                    nextCloudFile.lastMod = file.lastModified();
                    nextCloudFile.remoteMimeType = remoteFile.getMimeType();
                    NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(nextCloudFile);
                    return STATUS_SUCCESS;
                }

            }

        }
        return STATUS_FAILURE;

    }

    @Override
    public SynchroService.Result endOfSync() {
        List<String> modified = new ArrayList<>();
        boolean hasFailedOnce = false;
        List<RemoteFile> folderToEventuallyDelete = new ArrayList<>();
        //download
        for(String file : metadataDownloadList.keySet()){
            Log.d(TAG, "remote "+file);
            RemoteFile remoteFile = metadataDownloadList.get(file);
            //these files are on server but not local
            NextCloudFileHelper.DBNextCloudFile driveFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, file);
            if(driveFile==null) {
                driveFile = new NextCloudFileHelper.DBNextCloudFile(file);
                driveFile.accountID = mAccountID;
            }
            if(remoteFile.getEtag().equals(driveFile.currentlyDownloadedOnlineEtag)){
                if(remoteFile.getMimeType().equals("DIR")) {
                    folderToEventuallyDelete.add(remoteFile);
                    //delete folder only if no files are downloaded after.
                }
                else {
                    Log.d(TAG, "was deleted locally");
                    //was deleted locally
                    boolean success = mWrapper.getFileOperation().delete(remoteFile.getRemotePath());
                    if (!success) {
                        hasFailedOnce = true;
                    } else {
                        NextCloudFileHelper.getInstance(mContext).delete(driveFile);
                    }
                }

            }else{
                Log.d(TAG, "Distant File "+file+" not on local");
                //downloading
                int res = downloadFileAndRecord(remoteFile, getLocalPathFromRemote(file), driveFile);
                if(res == STATUS_FAILURE)
                    hasFailedOnce = true;
                else
                    modified.add(getLocalPathFromRemote(file));
            }
        }
        if(!hasFailedOnce) // to avoid case file not download therefore folder not created
        for (RemoteFile folder : folderToEventuallyDelete){
            if(!new File(getLocalPathFromRemote(folder.getRemotePath())).exists()){
                String remoteFilePath = folder.getRemotePath();
                if(remoteFilePath.startsWith("/"))
                    remoteFilePath = remoteFilePath.substring(1);
                if(remoteFilePath.endsWith("/"))
                    remoteFilePath = remoteFilePath.substring(0, remoteFilePath.length()-1);
                NextCloudFileHelper.DBNextCloudFile driveFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remoteFilePath);
                Log.d(TAG, "folder " + folder.getRemotePath() + " was deleted locally");
                //was deleted locally
                boolean success = mWrapper.getFileOperation().delete(folder.getRemotePath());
                if (!success) {
                    return new SynchroService.Result(STATUS_FAILURE, modified);
                } else {
                    NextCloudFileHelper.getInstance(mContext).delete(driveFile);
                }
            }

        }
        return new SynchroService.Result(hasFailedOnce?STATUS_FAILURE:STATUS_SUCCESS,modified);
    }

    private int downloadFileAndRecord(RemoteFile remoteFile, String localFile, NextCloudFileHelper.DBNextCloudFile driveFile) {
        Log.d(TAG,"download to "+localFile);
        Log.d(TAG,"download from "+localFile);

        if(remoteFile.getMimeType().equals("DIR")) {
            if (!new File(localFile).mkdirs() && !new File(localFile).exists())
                return STATUS_FAILURE;
            driveFile.currentlyDownloadedOnlineEtag = remoteFile.getEtag();
            driveFile.remoteMimeType = remoteFile.getMimeType();
            NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(driveFile);
            return STATUS_SUCCESS;
        } else {
            synchronized (FileLocker.getLockOnPath(localFile)) {
                SynchroService.sService.showForegroundNotification(mContext.getString(R.string.downloading) + " " + Uri.parse(remoteFile.getRemotePath()).getLastPathSegment());
                boolean success = mWrapper.getFileOperation().download(remoteFile.getRemotePath(), localFile, remoteFile.getSize());

                Log.d(TAG, "download ?");
                SynchroService.sService.resetNotification();

                if (success) {
                    Log.d(TAG, "success ?");

                    //record in DB
                    driveFile.currentlyDownloadedOnlineEtag = remoteFile.getEtag();
                    driveFile.onlineEtag = remoteFile.getEtag();
                    driveFile.remoteMimeType = remoteFile.getMimeType();
                    driveFile.md5 = FileUtils.md5(localFile);
                    driveFile.lastMod = new File(localFile).lastModified();
                    NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(driveFile);
                    return STATUS_SUCCESS;
                }
            }
            return STATUS_FAILURE;
        }
    }

    private String getLocalPathFromRemote(String remotePath) {
        int toCut = mRemoteRootPath.length();
        if(mRemoteRootPath.startsWith("/")&& !remotePath.startsWith("/"))
            toCut--;
        String local = remotePath.substring(toCut);
        if(local.startsWith("/"))
            local = local.substring(1);
        if(local.endsWith("/"))
            local = local.substring(0, local.length()-1);
        local = mRootPath + (mRootPath.endsWith("/")?"":"/")+local;
        return local;
    }


    @Override
    public int loadDistantFiles() {
        Log.d(TAG,"syncing "+getRemotePathFromLocal(mCurrentlyLocalSyncedDir));
        Log.d(TAG,"root "+mRemoteRootPath);
        return recursiveLoadFolder(getRemotePathFromLocal(mCurrentlyLocalSyncedDir));
    }

    private void markVisitFailed(NextCloudFileHelper.DBNextCloudFile nextCloudFile){
        if(nextCloudFile != null){
            nextCloudFile.visitStatus = NextCloudFileHelper.DBNextCloudFile.VisitStatus.STATUS_FAILURE;
            NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(nextCloudFile);
        }
    }

    private void fillWithDBRemoteFiles(String remotePath){
        List<NextCloudFileHelper.DBNextCloudFile> list = NextCloudFileHelper.getInstance(mContext).getChildrenTree(mAccountID, remotePath);
        for(NextCloudFileHelper.DBNextCloudFile nextCloudFile1  : list){
            if(nextCloudFile1.onlineEtag == null){
                throw new RuntimeException("Invalid DB etag for "+nextCloudFile1.relativePath);
            }
            RemoteFile remoteFile1 = new RemoteFile("/"+nextCloudFile1.relativePath);//RemoteFile path needs to start with a /
            remoteFile1.setEtag(nextCloudFile1.onlineEtag);

            remoteFile1.setMimeType(nextCloudFile1.remoteMimeType);
            mRemoteFiles.put( nextCloudFile1.relativePath, remoteFile1);
            metadataDownloadList.put( nextCloudFile1.relativePath, remoteFile1);

        }
    }

    private int recursiveLoadFolder(String remotePath) {
        NextCloudSyncLister nextCloudSyncLister = mWrapper.getSyncLister();
        NextCloudFileHelper.DBNextCloudFile nextCloudFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remotePath);
        if(nextCloudFile == null) {
            nextCloudFile = new NextCloudFileHelper.DBNextCloudFile(remotePath);
            nextCloudFile.accountID = mAccountID;
        }
        else if(remotePath.equals(getRemotePathFromLocal(mCurrentlyLocalSyncedDir))) {

            String remoteEtag = mWrapper.getFileOperation().getEtag(remotePath);
            if (remoteEtag != null && remoteEtag.equals(nextCloudFile.onlineEtag) && nextCloudFile.visitStatus == NextCloudFileHelper.DBNextCloudFile.VisitStatus.STATUS_OK) {
                Log.d(TAG, "root dir hasn't changed " + remoteEtag);
                fillWithDBRemoteFiles(remotePath);
                return STATUS_SUCCESS;
            }
        }

        List<RemoteFile> remoteFileList = null;
        try {
            remoteFileList = nextCloudSyncLister.retrieveList(remotePath);
        } catch (final Exception e) {
            for(StackTraceElement ee : e.getStackTrace()){
                Log.d(TAG, ee.toString());
            }
            if(e instanceof  com.nextcloud.android.sso.exceptions.NextcloudHttpRequestFailedException || e instanceof  java.lang.NullPointerException){
                //create folder
                mWrapper.getFileOperation().mkdir(remotePath);
                try {
                    remoteFileList = nextCloudSyncLister.retrieveList(remotePath);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    markVisitFailed(nextCloudFile);
                    return STATUS_FAILURE;
                }
            } else {

                if(e instanceof  com.owncloud.android.lib.common.network.CertificateCombinedException && !PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("refuse_certificate", false)){
                    cert = ((CertificateCombinedException) e).getServerCertificate();
                    try {
                        cert.checkValidity();
                    } catch (CertificateExpiredException e1) {
                        Intent i = new Intent(mContext, CertificateActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(i);
                    } catch (CertificateNotYetValidException e1) {
                        Intent i = new Intent(mContext, CertificateActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(i);
                    }
                    try {
                        NetworkUtils.addCertToKnownServersStore(NextCloudSyncWrapper.cert, mContext);
                        mContext.startService(new Intent(mContext, SynchroService.class));
                    } catch (KeyStoreException e1) {
                        e1.printStackTrace();
                    } catch (NoSuchAlgorithmException e1) {
                        e1.printStackTrace();
                    } catch (CertificateException e1) {
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

                markVisitFailed(nextCloudFile);
                return STATUS_FAILURE;
            }
        }
        Log.d(TAG,"retrieveList remotePath "+remotePath);

        if(remoteFileList == null) {
            Log.d(TAG,"remoteFileList is null");
            markVisitFailed(nextCloudFile);
            return STATUS_FAILURE;
        }
        String etag = "";
        for(RemoteFile remoteFile : remoteFileList){
            Log.d(TAG, "loading remote : path "+remoteFile.getRemotePath());

            //first we add them
            String remoteFilePath = remoteFile.getRemotePath();
            if(remoteFilePath.startsWith("/"))
                remoteFilePath = remoteFilePath.substring(1);
            if(remoteFilePath.endsWith("/"))
                remoteFilePath = remoteFilePath.substring(0, remoteFilePath.length()-1);
            if(remoteFilePath.equals(remotePath)) {
                etag = remoteFile.getEtag();
                //First item is usually current directory
                //check dir etag with db, if last visit = OK and ETAG hasn't changed, fill with DB and break

                if(nextCloudFile!=null&&etag.equals(nextCloudFile.onlineEtag) && nextCloudFile.visitStatus == NextCloudFileHelper.DBNextCloudFile.VisitStatus.STATUS_OK) {
                    Log.d(TAG, "hasn't changed "+etag);
                    fillWithDBRemoteFiles(remotePath);
                    break;
                }else{
                    Log.d(TAG, "has changed");
                }

                continue;
            }
            mRemoteFiles.put(remoteFilePath, remoteFile);
            metadataDownloadList.put(remoteFilePath, remoteFile);;
            Log.d(TAG, remoteFile.getRemotePath());
            if("DIR".equals(remoteFile.getMimeType())){
                NextCloudFileHelper.DBNextCloudFile nextCloudChildFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remoteFilePath);

                if(nextCloudChildFile!=null&&remoteFile.getEtag().equals(nextCloudChildFile.onlineEtag) && nextCloudChildFile.visitStatus == NextCloudFileHelper.DBNextCloudFile.VisitStatus.STATUS_OK) {
                    Log.d(TAG, "child dir hasn't changed "+remoteFile.getEtag());
                    fillWithDBRemoteFiles(remoteFilePath);
                }else if(recursiveLoadFolder(remoteFilePath) == STATUS_FAILURE) {
                    markVisitFailed(nextCloudFile);
                    return STATUS_FAILURE;
                }
            }
            // saving item to database
            // online etag must be updated AFTER visiting the folder otherwise it will be marked as visited OK even if it wasn't
            NextCloudFileHelper.DBNextCloudFile nextCloudFileChild = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remoteFilePath);
            if(nextCloudFileChild == null) {
                nextCloudFileChild = new NextCloudFileHelper.DBNextCloudFile(remoteFilePath);
            }
            nextCloudFileChild.accountID = mAccountID;
            nextCloudFileChild.relativePath = remoteFilePath;
            nextCloudFileChild.remoteMimeType = remoteFile.getMimeType();
            nextCloudFileChild.onlineEtag = remoteFile.getEtag() ;//needed to fill in RemoteFile when loading from DB
            NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(nextCloudFileChild);
        }
        if(nextCloudFile == null) {
            nextCloudFile = new NextCloudFileHelper.DBNextCloudFile(remotePath);
            nextCloudFile.accountID = mAccountID;
        }
        // write folder to db with visit = success
        nextCloudFile.onlineEtag = etag;
        nextCloudFile.visitStatus = NextCloudFileHelper.DBNextCloudFile.VisitStatus.STATUS_OK;
        NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(nextCloudFile);
        return STATUS_SUCCESS;
    }



    public void resolve(int requestCode) {
    }
}
