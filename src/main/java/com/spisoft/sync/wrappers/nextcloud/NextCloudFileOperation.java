package com.spisoft.sync.wrappers.nextcloud;

import com.owncloud.android.lib.resources.files.RemoteFile;

public interface NextCloudFileOperation {
    boolean download(String remotePath, String to, long size);
    boolean upload(String fromFile, String remotePath);
    boolean mkdir(String remotePath);
    boolean delete(String remotePath);
    RemoteFile getFileInfo(String remotePath);
}
