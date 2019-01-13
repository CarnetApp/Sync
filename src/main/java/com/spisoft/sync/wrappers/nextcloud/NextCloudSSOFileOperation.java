package com.spisoft.sync.wrappers.nextcloud;

import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.utils.FileUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NextCloudSSOFileOperation implements NextCloudFileOperation {
    private final NextCloudWrapper mNextCloudWrapper;


    public NextCloudSSOFileOperation(NextCloudWrapper nextCloudWrapper) {
        mNextCloudWrapper = nextCloudWrapper;
    }

    @Override
    public boolean download(String remotePath, String to) {
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("GET")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            InputStream inputStream = mNextCloudWrapper.getNextcloudApi().performNetworkRequest(nextcloudRequest);
            FileUtils.copy(inputStream, new FileOutputStream(to));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean upload(String fromFile, String remotePath) {
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("PUT")
                .setRequestFilePathToUpload(fromFile)
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
    public boolean mkdir(String remotePath) {
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
