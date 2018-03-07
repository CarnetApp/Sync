package com.spisoft.sync.wrappers;

import android.net.Uri;

import com.spisoft.sync.utils.FileUtils;

import java.io.Serializable;

/**
 * Created by alexandre on 16/03/17.
 */

public class FileItem implements Serializable{
    private final boolean mIsDirectory;
    private final long mModificationDate;
    protected long mAccountId;
    protected String mPath;
    protected String mName;
    protected String mMimetype;

    public FileItem(String path, String name, boolean isDirectory, long modificationDate, long accountId, String mimetype){
        mAccountId = accountId;
        mPath = path;
        if(name == null)
            mName = FileUtils.getName(Uri.parse(path));
        else
            mName = name;
        mIsDirectory = isDirectory;
        mModificationDate = modificationDate;
        mMimetype = mimetype;
    }

    public boolean isDirectory(){
        return mIsDirectory;
    }

    public long getModificationDate(){
        return mModificationDate;
    }

    public String getName(){
        return mName;
    }

    public String getPath(){
        return mPath;
    }

    public String getMimetype() {
        return mMimetype;
    }
}
