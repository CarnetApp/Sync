package com.spisoft.sync.wrappers.nextcloud;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.methods.GetMethod;

import java.io.IOException;


class NextCloudOCQueryExecutor implements NextCloudQueryExecutor {
    private final NextCloudWrapper mNextCloudWrapper;
    private final OwnCloudClient mClient;

    public NextCloudOCQueryExecutor(NextCloudWrapper nextCloudWrapper) {
        mNextCloudWrapper = nextCloudWrapper;
        mClient = nextCloudWrapper.getClient();
    }

    @Override
    public NextCloudQueryResponse execute(String method, String url) throws Exception {
        GetMethod met = new GetMethod(mClient.getBaseUri() + "/"+url);

        NextCloudQueryResponse response = new NextCloudQueryResponse();
        response.success = false;
        try {
            int status = mClient.executeMethod(met);
            response.stream = met.getResponseBodyAsStream();
            if( status == 200)
                response.success = true;

        } finally {
        }
        return response;
    }
}
