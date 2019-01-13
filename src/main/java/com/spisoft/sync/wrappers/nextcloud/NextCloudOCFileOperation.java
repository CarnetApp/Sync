package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;
import com.spisoft.sync.wrappers.FileItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class NextCloudOCFileOperation implements NextCloudFileOperation {

    private static final String TAG = "NextCloudSyncLister";
    private final OwnCloudClient mClient;

    public NextCloudOCFileOperation(OwnCloudClient client) {
        mClient = client;
    }


    @Override
    public boolean download(String remotePath, String to) {
        MyDownloadRemoteFileOperation readRemoteFileOperation = new MyDownloadRemoteFileOperation(remotePath, to);
        try {
            readRemoteFileOperation.logHeader(mClient);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperationCancelledException e) {
            e.printStackTrace();
        }
        RemoteOperationResult result = readRemoteFileOperation.execute(mClient);
        return result.isSuccess();
    }

    @Override
    public boolean upload(String fromFile, String remotePath) {
        UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(fromFile, remotePath, null);
        RemoteOperationResult result = uploadOperation.execute(mClient);
        return result.isSuccess();
    }

    @Override
    public boolean mkdir(String remotePath) {
        CreateRemoteFolderOperation remoteFolderOperation = new CreateRemoteFolderOperation(remotePath, true);
        return remoteFolderOperation.execute(mClient).isSuccess();
    }

    @Override
    public boolean delete(String remotePath) {
        RemoveRemoteFileOperation uploadOperation = new RemoveRemoteFileOperation(remotePath);
        RemoteOperationResult result = uploadOperation.execute(mClient);
        return result.isSuccess();
    }

    @Override
    public RemoteFile getFileInfo(String remotePath) {
        ReadRemoteFileOperation readRemoteFileOperation = new ReadRemoteFileOperation(remotePath);
        RemoteOperationResult result = readRemoteFileOperation.execute(mClient);
        if (result.isSuccess()) {
            com.spisoft.sync.Log.d(TAG, "read success ");
            return (RemoteFile) result.getData().get(0);
        }
        return null;
    }
}
