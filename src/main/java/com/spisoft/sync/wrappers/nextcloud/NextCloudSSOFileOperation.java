package com.spisoft.sync.wrappers.nextcloud;

import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.aidl.ParcelFileDescriptorUtil;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.Log;
import com.spisoft.sync.utils.FileUtils;

import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class NextCloudSSOFileOperation implements NextCloudFileOperation {
    static final String TAG = "NextCloudSSOFileOperation";
    private final NextCloudWrapper mNextCloudWrapper;


    public NextCloudSSOFileOperation(NextCloudWrapper nextCloudWrapper) {
        mNextCloudWrapper = nextCloudWrapper;
    }

    @Override
    public boolean download(String remotePath, String to, long size) {
        File parent =  new File(to).getParentFile();
        parent.mkdirs();
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("GET")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        File tmp = new File(parent, ".donotsync.tmp"+System.currentTimeMillis());
        try {
            InputStream inputStream = mNextCloudWrapper.getNextcloudApi().performNetworkRequestV2(nextcloudRequest).getBody();
            FileUtils.copy(inputStream, new FileOutputStream(tmp));
            if(tmp.exists()){
                if(tmp.length() > 0 || size != -1 && size == tmp.length()) {
                    File dest = new File(to);
                    dest.delete();
                    boolean success = tmp.renameTo(dest);
                    if(!success)
                        tmp.delete();
                    return success;
                }
                tmp.delete();
            }
        } catch (Exception e) {
            if(tmp.exists())
                tmp.delete();
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean upload(String fromFile, String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);

        try {
            FileInputStream input = new FileInputStream(fromFile);
            NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                    .setMethod("PUT")
                    .setUrl("/remote.php/webdav/"+remotePath)
                    .setRequestBodyAsStream(new FileInputStream(fromFile))
                    .build();
            mNextCloudWrapper.getNextcloudApi().performNetworkRequestV2(nextcloudRequest);
            input.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("uploadbug","outing false");

        return false;
    }

    @Override
    public boolean mkdir(String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("MKCOL")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            InputStream stream = mNextCloudWrapper.getNextcloudApi().performNetworkRequestV2(nextcloudRequest).getBody();
            return true;
        } catch (Exception e) {
          //  e.printStackTrace();
            if(e.getMessage().endsWith("409")){
                File f = new File(remotePath);
                mkdir(f.getParent());
                try {
                    InputStream stream = mNextCloudWrapper.getNextcloudApi().performNetworkRequestV2(nextcloudRequest).getBody();
                    return true;
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            }
        }
        return false;
    }

    @Override
    public boolean delete(String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("DELETE")
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            mNextCloudWrapper.getNextcloudApi().performNetworkRequestV2(nextcloudRequest);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public RemoteFile getFileInfo(String remotePath) {
        remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
        Map<String, List<String>> header = new HashMap<>();
        List<String>depth = new ArrayList<>();
        depth.add("0");
        header.put("Depth", depth);
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("PROPFIND")
                .setHeader(header)
                .setUrl("/remote.php/webdav/"+remotePath)
                .build();
        try {
            List<RemoteFile> files = NextCloudSSOSyncLister.parseInputStream(mNextCloudWrapper.getNextcloudApi().performNetworkRequestV2(nextcloudRequest).getBody());
            if(files.size()>0)
                return files.get(0);
        } catch (Exception e) {
            e.printStackTrace();
        }



        return null;
    }

    @Override
    public String getEtag(String remotePath) {

        try {
            DavPropertyNameSet propSet = new DavPropertyNameSet();
            propSet.add(DavPropertyName.GETETAG);
            MyPropFindMethod propfind = new MyPropFindMethod(WebdavUtils.encodePath(remotePath), propSet, 0);

            remotePath = NextCloudSSOSyncLister.encodePath(remotePath);
            Map<String, List<String>> header = new HashMap<>();
            List<String>depth = new ArrayList<>();
            depth.add("0");
            header.put("Depth", depth);
            NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                    .setMethod("PROPFIND")
                    .setHeader(header)
                    .setUrl("/remote.php/webdav/"+remotePath)
                    .setRequestBody(propfind.getMyRequestString())
                    .build();
            Log.d("requestdebug",propfind.getMyRequestString());
           DocumentBuilderFactory factory =
           DocumentBuilderFactory.newInstance();
           DocumentBuilder builder = factory.newDocumentBuilder();
           org.w3c.dom.Document doc = builder.parse(mNextCloudWrapper.getNextcloudApi().performNetworkRequestV2(nextcloudRequest).getBody());
           NodeList items = doc.getElementsByTagName("d:response");
           for(int i = 0; i < items.getLength(); i++) {
               RemoteFile remoteFile = new RemoteFile();
               Element node = (Element) items.item(i);
               return node.getElementsByTagName("d:getetag").item(0).getTextContent().replace("\"", "");
           }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return null;
    }
}
