package com.spisoft.sync.wrappers.nextcloud;

import android.net.Uri;
import android.util.Log;

import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NextCloudSSOFileOperation implements NextCloudFileOperation {
    static final String TAG = "NextCloudSSOFileOperation";
    private final NextCloudWrapper mNextCloudWrapper;


    public NextCloudSSOFileOperation(NextCloudWrapper nextCloudWrapper) {
        mNextCloudWrapper = nextCloudWrapper;
    }

    @Override
    public boolean download(String remotePath, String to) {
        File parent =  new File(to).getParentFile();
        parent.mkdirs();
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("GET")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        File tmp = new File(parent, ".donotsync.tmp"+System.currentTimeMillis());
        try {
            InputStream inputStream = mNextCloudWrapper.getNextcloudApi().performNetworkRequest(nextcloudRequest);
            FileUtils.copy(inputStream, new FileOutputStream(tmp));
            if(tmp.exists()){
                if(tmp.length() > 0) {
                    File dest = new File(to);
                    dest.delete();
                    boolean success = tmp.renameTo(dest);
                    if(!success)
                        tmp.delete();
                    return success;
                }
                tmp.delete();
            }
        } catch (Exception e) {
            if(tmp.exists())
                tmp.delete();
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean upload(String fromFile, String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("PUT")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            mNextCloudWrapper.getNextcloudApi().performNetworkRequest(nextcloudRequest, new FileInputStream(fromFile));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean mkdir(String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("MKCOL")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            mNextCloudWrapper.getNextcloudApi().performNetworkRequest(nextcloudRequest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean delete(String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("DELETE")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            mNextCloudWrapper.getNextcloudApi().performNetworkRequest(nextcloudRequest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public RemoteFile getFileInfo(String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        Map<String, List<String>> header = new HashMap<>();
        List<String>depth = new ArrayList<>();
        depth.add("0");
        header.put("Depth", depth);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("PROPFIND")
                .setHeader(header)
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            List<RemoteFile> files = NextCloudSSOSyncLister.parseInputStream(mNextCloudWrapper.getNextcloudApi().performNetworkRequest(nextcloudRequest));
            if(files.size()>0)
                return files.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }



        return null;
    }
}
