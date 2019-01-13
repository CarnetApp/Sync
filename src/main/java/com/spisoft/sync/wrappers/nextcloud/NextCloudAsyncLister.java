package com.spisoft.sync.wrappers.nextcloud;

import android.os.AsyncTask;
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
    private final long mAccountId;
    private final Handler mHandler;
    private final String mPath;
    private final NextCloudWrapper mWrapper;
    private Map<Integer, AsyncListerListener> mListenerMap ;
    public NextCloudAsyncLister(NextCloudWrapper wrapper, String path, long accountId){
        mWrapper = wrapper;
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

        new ListerTask(requestCode, asyncListerListener,  mPath.equals("/")?FileUtils.PATH_SEPARATOR:mPath).execute();

    }

    public class ListerTask extends AsyncTask<Void, Void, List<FileItem>>{
        private final AsyncListerListener mAsyncListerListener;
        private final String mRemotePath;
        private final int mRequestCode;

        public ListerTask(int requestCode, AsyncListerListener asyncListerListener, String remotePath) {
            mRequestCode = requestCode;
            mAsyncListerListener = asyncListerListener;
            mRemotePath = remotePath;
        }

        @Override
        protected List<FileItem> doInBackground(Void... voids) {
            try {
                List<RemoteFile> list = mWrapper.getSyncLister().retrieveList(mRemotePath);
                List<FileItem> files= new ArrayList<>();
                List<FileItem> folders= new ArrayList<>();
                for(RemoteFile file : list){
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
                return items;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(List<FileItem> result) {
            if(result!=null)
                mAsyncListerListener.onListingResult(mRequestCode, ResultCode.RESULT_OK, result);
            else
                mAsyncListerListener.onListingResult(mRequestCode, ResultCode.RESULT_FAILURE, result);
        }
    }
}
