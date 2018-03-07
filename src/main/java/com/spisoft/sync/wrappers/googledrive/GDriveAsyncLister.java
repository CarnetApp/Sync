package com.spisoft.sync.wrappers.googledrive;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.spisoft.sync.wrappers.AsyncLister;
import com.spisoft.sync.wrappers.FileItem;
import com.spisoft.sync.wrappers.ResultCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by phoenamandre on 04/03/18.
 */

class GDriveAsyncLister implements AsyncLister, OnCompleteListener<MetadataBuffer> {
    private final String mFolderId;
    private final GDriveWrapper mGDriveWrapper;
    private AsyncListerListener mAsyncListerListener;
    private int mRequestCode;

    public GDriveAsyncLister(Context mContext, int mAccountId, String folderId, GDriveWrapper gDriveWrapper) {
        mFolderId = folderId;
        mGDriveWrapper = gDriveWrapper;
    }

    @Override
    public void cancel() {

    }

    @Override
    public void retrieveList(int requestCode, AsyncListerListener asyncListerListener) {
        mAsyncListerListener = asyncListerListener;
        mRequestCode = requestCode;
        DriveFolder folder = DriveId.decodeFromString(mFolderId).asDriveFolder();
        mGDriveWrapper.getDriveResourceClient().listChildren(folder).addOnCompleteListener(this);
    }

    @Override
    public void onComplete(@NonNull Task<MetadataBuffer> task) {
        if(task.isSuccessful()){
            List<FileItem> items = new ArrayList<>();
            for(Metadata metadata :  task.getResult()){
                items.add(new FileItem(metadata.getDriveId().encodeToString(), metadata.getTitle(), metadata.isFolder(), metadata.getModifiedDate().getTime(),mGDriveWrapper.getAccountId() , metadata.getMimeType()));
            }
            mAsyncListerListener.onListingResult(mRequestCode, ResultCode.RESULT_OK ,items);
        }
        else
            mAsyncListerListener.onListingResult(mRequestCode, ResultCode.RESULT_FAILURE ,null);

    }
}
