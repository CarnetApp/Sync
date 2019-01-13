package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.FileItem;
import com.spisoft.sync.wrappers.ResultCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenamandre on 14/05/17.
 */

public interface NextCloudSyncLister {


    List<RemoteFile> retrieveList(String path) throws Exception;
}
