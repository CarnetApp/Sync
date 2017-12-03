package com.spisoft.sync.wrappers;

import java.util.List;

/**
 * Created by alexandre on 16/03/17.
 */

public interface AsyncLister {

    public interface AsyncListerListener{
        void onListingResult(int requestCode, int resultCode, List<FileItem> list);
    }

    public void cancel();

    public void retrieveList(int requestCode, AsyncListerListener asyncListerListener);

}
