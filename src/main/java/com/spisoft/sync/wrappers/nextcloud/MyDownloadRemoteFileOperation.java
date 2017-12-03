//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.spisoft.sync.wrappers.nextcloud;

import android.util.Log;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.OperationCancelledException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;


//my own class to avoid file being downloaded to /localpath/remotepath instead of /localpath
public class MyDownloadRemoteFileOperation extends RemoteOperation {
    private static final String TAG = MyDownloadRemoteFileOperation.class.getSimpleName();
    private Set<OnDatatransferProgressListener> mDataTransferListeners = new HashSet();
    private final AtomicBoolean mCancellationRequested = new AtomicBoolean(false);
    private long mModificationTimestamp = 0L;
    private String mEtag = "";
    private GetMethod mGet;
    private String mRemotePath;
    private String mLocalPath;

    public MyDownloadRemoteFileOperation(String remotePath, String localFolderPath) {
        this.mRemotePath = remotePath;
        this.mLocalPath = localFolderPath;
    }

    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        File tmpFile = new File(this.getTmpPath());

        try {
            tmpFile.getParentFile().mkdirs();
            int e = this.downloadFile(client, tmpFile);
            result = new RemoteOperationResult(this.isSuccess(e), e, this.mGet != null?this.mGet.getResponseHeaders():null);
            Log_OC.i(TAG, "Download of " + this.mRemotePath + " to " + this.getTmpPath() + ": " + result.getLogMessage());
        } catch (Exception var5) {
            result = new RemoteOperationResult(var5);
            Log_OC.e(TAG, "Download of " + this.mRemotePath + " to " + this.getTmpPath() + ": " + result.getLogMessage(), var5);
        }

        return result;
    }

    protected int logHeader(OwnCloudClient client) throws HttpException, IOException, OperationCancelledException {
        boolean status = true;
        boolean savedFile = false;
        this.mGet = new GetMethod(client.getWebdavUri() + WebdavUtils.encodePath(this.mRemotePath));
        Iterator it = null;
        FileOutputStream fos = null;

        int status1;
        try {
            status1 = client.executeMethod(this.mGet);
            if(!this.isSuccess(status1)) {
                client.exhaustResponse(this.mGet.getResponseBodyAsStream());
            } else {
                for(Header header : mGet.getResponseHeaders()){
                    Log.d(TAG, "header "+header.getName()+" "+header.getValue());
                }
            }
        } finally {
            if(fos != null) {
                fos.close();
            }


            this.mGet.releaseConnection();
        }

        return status1;
    }

    protected int downloadFile(OwnCloudClient client, File targetFile) throws HttpException, IOException, OperationCancelledException {
        boolean status = true;
        boolean savedFile = false;
        this.mGet = new GetMethod(client.getWebdavUri() + WebdavUtils.encodePath(this.mRemotePath));
        Iterator it = null;
        FileOutputStream fos = null;

        int status1;
        try {
            status1 = client.executeMethod(this.mGet);
            if(!this.isSuccess(status1)) {
                client.exhaustResponse(this.mGet.getResponseBodyAsStream());
            } else {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(this.mGet.getResponseBodyAsStream());
                fos = new FileOutputStream(targetFile);
                long transferred = 0L;
                Header contentLength = this.mGet.getResponseHeader("Content-Length");
                long totalToTransfer = contentLength != null && contentLength.getValue().length() > 0?Long.parseLong(contentLength.getValue()):0L;
                byte[] bytes = new byte[4096];
                boolean readResult = false;

                while(true) {
                    int readResult1;
                    if((readResult1 = bis.read(bytes)) == -1) {
                        if(transferred == totalToTransfer) {
                            savedFile = true;
                            Header modificationTime2 = this.mGet.getResponseHeader("Last-Modified");
                            if(modificationTime2 == null) {
                                modificationTime2 = this.mGet.getResponseHeader("last-modified");
                            }

                            if(modificationTime2 != null) {
                                Date d = WebdavUtils.parseResponseDate(modificationTime2.getValue());
                                this.mModificationTimestamp = d != null?d.getTime():0L;
                            } else {
                                Log_OC.e(TAG, "Could not read modification time from response downloading " + this.mRemotePath);
                            }

                            this.mEtag = WebdavUtils.getEtagFromResponse(this.mGet);
                            if(this.mEtag.length() == 0) {
                                Log_OC.e(TAG, "Could not read eTag from response downloading " + this.mRemotePath);
                            }
                        } else {
                            client.exhaustResponse(this.mGet.getResponseBodyAsStream());
                        }
                        break;
                    }

                    AtomicBoolean modificationTime = this.mCancellationRequested;
                    synchronized(this.mCancellationRequested) {
                        if(this.mCancellationRequested.get()) {
                            this.mGet.abort();
                            throw new OperationCancelledException();
                        }
                    }

                    fos.write(bytes, 0, readResult1);
                    transferred += (long)readResult1;
                    Set modificationTime1 = this.mDataTransferListeners;
                    synchronized(this.mDataTransferListeners) {
                        it = this.mDataTransferListeners.iterator();

                        while(it.hasNext()) {
                            ((OnDatatransferProgressListener)it.next()).onTransferProgress((long)readResult1, transferred, totalToTransfer, targetFile.getName());
                        }
                    }
                }
            }
        } finally {
            if(fos != null) {
                fos.close();
            }

            if(!savedFile && targetFile.exists()) {
                targetFile.delete();
            }

            this.mGet.releaseConnection();
        }

        return status1;
    }

    private boolean isSuccess(int status) {
        return status == 200;
    }

    private String getTmpPath() {
        return this.mLocalPath;
    }

    public void addDatatransferProgressListener(OnDatatransferProgressListener listener) {
        Set var2 = this.mDataTransferListeners;
        synchronized(this.mDataTransferListeners) {
            this.mDataTransferListeners.add(listener);
        }
    }

    public void removeDatatransferProgressListener(OnDatatransferProgressListener listener) {
        Set var2 = this.mDataTransferListeners;
        synchronized(this.mDataTransferListeners) {
            this.mDataTransferListeners.remove(listener);
        }
    }

    public void cancel() {
        this.mCancellationRequested.set(true);
    }

    public long getModificationTimestamp() {
        return this.mModificationTimestamp;
    }

    public String getEtag() {
        return this.mEtag;
    }
}
