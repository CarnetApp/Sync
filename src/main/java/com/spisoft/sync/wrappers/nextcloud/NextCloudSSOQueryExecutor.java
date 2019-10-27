package com.spisoft.sync.wrappers.nextcloud;

import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.api.NextcloudAPI;

import java.io.InputStream;

class NextCloudSSOQueryExecutor implements NextCloudQueryExecutor {
    private final NextCloudWrapper mNextCloudWrapper;

    public NextCloudSSOQueryExecutor(NextCloudWrapper nextCloudWrapper) {
        mNextCloudWrapper = nextCloudWrapper;
    }

    @Override
    public NextCloudQueryResponse execute(String method, String url) throws Exception {
        NextCloudQueryResponse response = new NextCloudQueryResponse();
        response.success = false;
        NextcloudAPI nextcloudAPI = mNextCloudWrapper.getNextcloudApi();
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod(method)
                .setUrl(url)
                .build();
        InputStream stream = nextcloudAPI.performNetworkRequest(nextcloudRequest);
        if(stream != null)
            response.success = true;
        response.stream = stream;
        return response;
    }
}
