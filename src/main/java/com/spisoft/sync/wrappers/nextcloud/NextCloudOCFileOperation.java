package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.ReadRemoteFileOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation;
import com.owncloud.android.lib.resources.files.UploadRemoteFileOperation;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.xml.Namespace;

import java.io.File;
import java.io.IOException;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class NextCloudOCFileOperation implements NextCloudFileOperation {

    private static final String TAG = "NextCloudOCFileOperation";
    private final OwnCloudClient mClient;
    private final NextCloudWrapper mNextCloudWrapper;

    public NextCloudOCFileOperation(NextCloudWrapper wrapper) {
        mNextCloudWrapper = wrapper;
        mClient = wrapper.getClient();
    }


    @Override
    public boolean download(String remotePath, String to, long size) {
        Log.d(TAG, "download " +remotePath+" to "+to);
        File dest = new File(to);
        File parent = dest.getParentFile();
        parent.mkdirs();
        File tmp = new File(parent, ".donotsync.tmp"+System.currentTimeMillis());
        MyDownloadRemoteFileOperation readRemoteFileOperation = new MyDownloadRemoteFileOperation(remotePath, tmp.getAbsolutePath());
        try {
            readRemoteFileOperation.logHeader(mClient);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperationCancelledException e) {
            e.printStackTrace();
        }
        RemoteOperationResult result = readRemoteFileOperation.execute(mClient);
        if(tmp.exists()) {
            Log.d(TAG, "tmp.exists");
            if (result.isSuccess()) {
                Log.d(TAG, "result success "+(size != -1 && size == tmp.length()));
                if(tmp.length()>0 || size != -1 && size == tmp.length()){
                    dest.delete();
                    boolean success = tmp.renameTo(dest);
                    Log.d(TAG, "renaming... "+success);
                    if(!success)
                        tmp.delete();
                    return success;
                }
            }
            tmp.delete();
        }
        return false;
    }

    @Override
    public boolean upload(String fromFile, String remotePath) {
        Long timeStampLong = new File(fromFile).lastModified()/1000;
        String timeStamp = timeStampLong.toString();

        UploadRemoteFileOperation uploadOperation = new UploadRemoteFileOperation(fromFile, remotePath, null, timeStamp);
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

    @Override
    public String getEtag(String remotePath) {
        PropFindMethod propfind = null;
        RemoteOperationResult result = null;

        try {
            DavPropertyNameSet propSet = new DavPropertyNameSet();
            propSet.add(DavPropertyName.GETETAG);

            propfind = new PropFindMethod(mClient.getWebdavUri() + WebdavUtils.encodePath(remotePath), propSet, 0);
            int status = mClient.executeMethod(propfind, 40000, 5000);
            boolean isSuccess = status == 207 || status == 200;
            if (isSuccess) {
                MultiStatus resp = propfind.getResponseBodyAsMultiStatus();
                MultiStatusResponse ms = resp.getResponses()[0];
                if (ms.getStatus().length != 0) {

                    int msstatus = ms.getStatus()[0].getStatusCode();
                    if (msstatus == 404) {
                        msstatus = ms.getStatus()[1].getStatusCode();
                    }

                    DavPropertySet propSetRes = ms.getProperties(msstatus);
                    DavProperty prop = propSetRes.get(DavPropertyName.GETETAG);
                    if (prop != null) {
                        String  eTag = (String)prop.getValue();
                        eTag = WebdavUtils.parseEtag(eTag);
                        return eTag;
                    }

                }
            }
               /* WebdavEntry we = new WebdavEntry(resp.getResponses()[0], mClient.getWebdavUri().getPath());
                RemoteFile remoteFile = new RemoteFile(we);
                ArrayList<Object> files = new ArrayList();
                files.add(remoteFile);
                result = new RemoteOperationResult(true, propfind);
                result.setData(files);*/
            } catch (DavException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
