package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.wrappers.FileItem;

/**
 * Created by alexandre on 16/03/17.
 */

public class NextCloudFileItem extends FileItem {
    private final int mAccountId;

    public NextCloudFileItem(RemoteFile remoteFile, int accountId) {
        super(remoteFile.getRemotePath(),"DIR".equals(remoteFile.getMimeType()),0, accountId);
        mAccountId = accountId;
    }
}
