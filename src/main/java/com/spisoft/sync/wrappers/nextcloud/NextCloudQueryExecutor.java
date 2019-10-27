package com.spisoft.sync.wrappers.nextcloud;

import java.io.InputStream;

interface NextCloudQueryExecutor {
    NextCloudQueryResponse execute(String method, String url) throws Exception;
    class  NextCloudQueryResponse{
        public InputStream stream;
        boolean success;
    }
}
