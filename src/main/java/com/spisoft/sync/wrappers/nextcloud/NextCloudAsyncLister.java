package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.FileItem;
import com.spisoft.sync.wrappers.ResultCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.Handler;
/**
 * Created by alexandre on 16/03/17.
 */

public class NextCloudAsyncLister implements AsyncLister {
    private static final String TAG = "NextCloudAsyncLister";
    private final OwnCloudClient mClient;
    private final long mAccountId;
    private final Handler mHandler;
    private final String mPath;
    private Map<Integer, AsyncListerListener> mListenerMap ;
    public NextCloudAsyncLister(OwnCloudClient client, String path, long accountId){
        mClient = client;
        mListenerMap = new HashMap<>();
        mAccountId = accountId;
        mHandler = new Handler();
        mPath = path;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void retrieveList(int requestCode, AsyncListerListener asyncListerListener) {
        ReadRemoteFolderOperation refreshOperation = new ReadRemoteFolderOperation((mPath.equals("/"))?FileUtils.PATH_SEPARATOR:mPath);
        // root folder

        refreshOperation.execute(mClient, new NextCloudListerResultListener(requestCode,asyncListerListener), mHandler);

    }

    private class NextCloudListerResultListener extends NextCloudWrapper.NextCloudResultListener {

        private final AsyncListerListener mAsyncListerListener;

        public NextCloudListerResultListener(int requestCode, AsyncListerListener asyncListerListener) {
            super(requestCode);
            mAsyncListerListener = asyncListerListener;
        }

        @Override
        public void onRemoteOperationFinish(RemoteOperation remoteOperation, RemoteOperationResult remoteOperationResult) {
            if(remoteOperationResult.isSuccess()){
                List<FileItem> files= new ArrayList<>();
                List<FileItem> folders= new ArrayList<>();
                ArrayList<Object> objects = remoteOperationResult.getData();
                for(Object obj : objects){
                    RemoteFile file = (RemoteFile) obj;
                    FileItem item = new NextCloudFileItem(file, mAccountId);
                    Log.d(TAG,"file "+file.getRemotePath());
                    if(!file.getRemotePath().equals(mPath)) {
                        if(item.isDirectory())
                            folders.add(item);
                        else
                            files.add(item);
                    }

                }
                List<FileItem> items = new ArrayList<>();
                items.addAll(folders);
                items.addAll(files);
                mAsyncListerListener.onListingResult(mRequestCode, ResultCode.RESULT_OK, items);
            }
        }
    }
}
