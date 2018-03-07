package com.spisoft.sync.wrappers.nextcloud;

import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.wrappers.FileItem;

/**
 * Created by alexandre on 16/03/17.
 */

public class NextCloudFileItem extends FileItem {

    public NextCloudFileItem(RemoteFile remoteFile, long accountId) {
        super(remoteFile.getRemotePath(), null, "DIR".equals(remoteFile.getMimeType()),0, accountId, remoteFile.getMimeType());
    }
}
