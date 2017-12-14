package com.spisoft.sync.wrappers.nextcloud;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.spisoft.sync.Log;
import com.spisoft.sync.synchro.SyncWrapper;
import com.spisoft.sync.synchro.SynchroService;
import com.spisoft.sync.utils.FileUtils;

import java.io.File;
import java.io.IOException;
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

    /* private static final String MD5_CUSTOM_KEY = "md5_key";
     public static final int RESOLVE_CONNECTION_REQUEST_CODE = 1001;
     private GoogleDriveAccountHelper.GoogleAccount mGoogleAccount;
     private Map<String,Metadata> metadataList = new HashMap<>();
     private Map<String,Object> metadataFullList = new HashMap<>(); // even when folder, do not start with / or end with /
     private GoogleApiClient mGoogleApiClient;
     private Activity mActiviy;
     private Map<String, Metadata> metadataDownloadList = new HashMap<>();
 */
    public NextCloudSyncWrapper(Context ct, int accountID, NextCloudWrapper wrapper) {
        super(ct, accountID);
        mWrapper = wrapper;
        mRemoteFiles = new HashMap();
        metadataDownloadList = new HashMap();
      /*  mGoogleAccount = GoogleDriveAccountHelper.getInstance(ct).getGoogleAccount(accountID);

        mGoogleApiClient = new GoogleApiClient.Builder(ct)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

*/
       // mGoogleApiClient = mGoogleAccount.googleApiClient;
    }


    public int connect(){
      /*  ConnectionResult connectionResult = mGoogleApiClient.blockingConnect();
        if(!connectionResult.isSuccess())
            return ERROR;
        Status result = Drive.DriveApi.requestSync(mGoogleApiClient).await(); //SYNC file status
*/
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
        Log.d("filedebug","path1 "+localPath);
        String remotePath = localPath.substring(mRootPath.length());
        Log.d("filedebug","path2 "+remotePath);

        if (remotePath.startsWith("/"))
            remotePath = remotePath.substring(1);
        Log.d("filedebug","path3 "+remotePath);


        Log.d("filedebug","path4 "+remotePath);

        remotePath = mRemoteRootPath + (mRemoteRootPath.endsWith("/")?"":"/")+remotePath;
        Log.d("filedebug","path5 "+remotePath);

        if (remotePath.startsWith("/"))
            remotePath = remotePath.substring(1);
        if(remotePath.endsWith("/"))
            remotePath = remotePath.substring(0, remotePath.length()-1);
        Log.d("filedebug","path6 "+remotePath);

        return remotePath;
    }


    @Override
    public SynchroService.Result onFile(File file, String md5) {
        String remotePath = getRemotePathFromLocal(file.getAbsolutePath());
        Log.d(TAG, "OnFile "+remotePath);

        RemoteFile remoteFile = metadataDownloadList.get(remotePath);
        NextCloudFileHelper.DBNextCloudFile dbNextCloudFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remotePath);

        if(dbNextCloudFile == null) {
            Log.d(TAG, "db null");

            dbNextCloudFile = new NextCloudFileHelper.DBNextCloudFile(remotePath);
            dbNextCloudFile.accountID = mAccountID;

        }
        if(remoteFile!=null) { //file exists !
            metadataDownloadList.remove(remotePath);//won't need to download
            Log.d(TAG, "Distant File exists "+remotePath);


            if(!md5.equals(dbNextCloudFile.md5)) { //file was modified locally
                if(!remoteFile.getEtag().equals(dbNextCloudFile.currentlyDownloadedOnlineEtag)) {//modified externally
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
                            return new SynchroService.Result(newFile.delete()?STATUS_SUCCESS:STATUS_FAILURE, modPath);
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

                    return new SynchroService.Result(STATUS_SUCCESS);
                }
            }
        }else {
            //remote file doesn't exist.
            //check if was deleted
            if(dbNextCloudFile.currentlyDownloadedOnlineEtag !=null && !dbNextCloudFile.currentlyDownloadedOnlineEtag.isEmpty()){
                //was on server, checking if last version has been deleted
                if(md5.equals(dbNextCloudFile.md5)) {
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

            CreateRemoteFolderOperation remoteFolderOperation = new CreateRemoteFolderOperation(remotePath, true);
            if(remoteFolderOperation.execute(mWrapper.getClient()).isSuccess()){
                Log.d(TAG, "CreateRemoteFolderOperation success ");

                //record it
                ReadRemoteFolderOperation readRemoteFolderOperation = new ReadRemoteFolderOperation(remotePath);
                RemoteOperationResult remoteOperationResult = readRemoteFolderOperation.execute(mWrapper.getClient());
                Log.d(TAG, "CreateRemoteFolderOperation success ");

                if(remoteOperationResult.isSuccess()){
                    RemoteFile remoteFile = (RemoteFile) remoteOperationResult.getData().get(0);
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
            UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(file.getAbsolutePath(), remotePath, null);
            if (uploadOperation.execute(mWrapper.getClient()).isSuccess()) {
                //record it
                Log.d(TAG, "upload success ");

                ReadRemoteFileOperation readRemoteFileOperation = new ReadRemoteFileOperation(remotePath);
                RemoteOperationResult result = readRemoteFileOperation.execute(mWrapper.getClient());
                if (result.isSuccess()) {
                    Log.d(TAG, "read success ");

                    RemoteFile remoteFile = (RemoteFile) result.getData().get(0);
                    nextCloudFile.currentlyDownloadedOnlineEtag = remoteFile.getEtag();
                    nextCloudFile.md5 = md5;
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
        //download
        for(String file : metadataDownloadList.keySet()){
            RemoteFile remoteFile = metadataDownloadList.get(file);
            //these files are on server but not local
            NextCloudFileHelper.DBNextCloudFile driveFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, file);
            if(driveFile==null) {
                driveFile = new NextCloudFileHelper.DBNextCloudFile(file);
                driveFile.accountID = mAccountID;
            }
            if(remoteFile.getEtag() == driveFile.currentlyDownloadedOnlineEtag){
                //was deleted locally
                RemoveRemoteFileOperation uploadOperation = new RemoveRemoteFileOperation(remoteFile.getRemotePath());
                RemoteOperationResult result = uploadOperation.execute(mWrapper.getClient());
                if(!result.isSuccess()){
                    return new SynchroService.Result(STATUS_FAILURE, modified);
                }

            }else{
                Log.d(TAG, "Distant File "+file+" not on local");
                //downloading
                int res = downloadFileAndRecord(remoteFile, getLocalPathFromRemote(file), driveFile);
                if(res == STATUS_FAILURE)
                    return new SynchroService.Result(STATUS_FAILURE, modified);
                modified.add(getLocalPathFromRemote(file));
            }
        }
        return new SynchroService.Result(STATUS_SUCCESS,modified);
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
            SynchroService.sService.showForegroundNotification("Downloading "+ Uri.parse(remoteFile.getRemotePath()).getLastPathSegment());
            MyDownloadRemoteFileOperation readRemoteFileOperation = new MyDownloadRemoteFileOperation(remoteFile.getRemotePath(), localFile);
            try {
                readRemoteFileOperation.logHeader(mWrapper.getClient());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (OperationCancelledException e) {
                e.printStackTrace();
            }
            RemoteOperationResult result = readRemoteFileOperation.execute(mWrapper.getClient());
            Log.d(TAG,"download ?");
            SynchroService.sService.resetNotification();

            if (result.isSuccess()) {
                Log.d(TAG,"success ?");

                //record in DB
                driveFile.currentlyDownloadedOnlineEtag = remoteFile.getEtag();
                driveFile.onlineEtag = remoteFile.getEtag();
                driveFile.remoteMimeType = remoteFile.getMimeType();
                driveFile.md5 = FileUtils.md5(localFile);
                NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(driveFile);
                return STATUS_SUCCESS;
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

    private int recursiveLoadFolder(String remotePath) {

        NextCloudSyncLister nextCloudSyncLister = mWrapper.getSyncLister();
        List<RemoteFile> remoteFileList = nextCloudSyncLister.retrieveList(remotePath);
        Log.d(TAG,"retrieveList remotePath "+remotePath);
        NextCloudFileHelper.DBNextCloudFile nextCloudFile = NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID, remotePath);
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
                //check dire etag with db, if last visit = OK and ETAG hasn't changed, fill with DB and break

                if(nextCloudFile!=null&&etag.equals(nextCloudFile.currentlyDownloadedOnlineEtag) && nextCloudFile.visitStatus == NextCloudFileHelper.DBNextCloudFile.VisitStatus.STATUS_OK) {
                    Log.d(TAG, "hasn't changed");
                    List<NextCloudFileHelper.DBNextCloudFile> list = NextCloudFileHelper.getInstance(mContext).getChildrenTree(mAccountID, remotePath);
                    remoteFileList.clear();
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
                    break;
                }else{
                    Log.d(TAG, "has changed");
                }

                continue;
            }
            mRemoteFiles.put(remoteFilePath, remoteFile);
            metadataDownloadList.put(remoteFilePath, remoteFile);
            NextCloudFileHelper.DBNextCloudFile nextCloudFileChild = new NextCloudFileHelper.DBNextCloudFile();
            nextCloudFileChild.accountID = mAccountID;
            nextCloudFileChild.relativePath = remoteFilePath;
            nextCloudFileChild.remoteMimeType = remoteFile.getMimeType();
            Log.d(TAG, "put etag for "+remoteFilePath+" : "+remoteFile.getEtag());

            nextCloudFileChild.onlineEtag = remoteFile.getEtag() ;//needed to fill in RemoteFile when loading from DB
            NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(nextCloudFileChild);
            Log.d(TAG, "check etag "+NextCloudFileHelper.getInstance(mContext).getDBDriveFile(mAccountID,remoteFilePath).onlineEtag);
            ;
            Log.d(TAG, remoteFile.getRemotePath());
            if("DIR".equals(remoteFile.getMimeType())){
                if(recursiveLoadFolder(remoteFilePath) == STATUS_FAILURE) {
                    markVisitFailed(nextCloudFile);
                    return STATUS_FAILURE;
                }

            }
        }
        if(nextCloudFile == null) {
            nextCloudFile = new NextCloudFileHelper.DBNextCloudFile(remotePath);
            nextCloudFile.accountID = mAccountID;
        }
        // write folder to db with visit = success
        nextCloudFile.currentlyDownloadedOnlineEtag = etag;
        nextCloudFile.visitStatus = NextCloudFileHelper.DBNextCloudFile.VisitStatus.STATUS_OK;
        NextCloudFileHelper.getInstance(mContext).addOrUpdateDBDriveFile(nextCloudFile);
        return STATUS_SUCCESS;
    }



    public void resolve(int requestCode) {
    }
}
