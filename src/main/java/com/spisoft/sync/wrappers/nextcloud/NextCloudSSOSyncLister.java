package com.spisoft.sync.wrappers.nextcloud;

import android.net.Uri;

import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.api.NextcloudAPI;
import com.owncloud.android.lib.resources.files.RemoteFile;
import com.spisoft.sync.Log;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by phoenamandre on 14/05/17.
 */

public class NextCloudSSOSyncLister implements NextCloudSyncLister{

    private static final String TAG = "NextCloudSSOSyncLister";
    private final NextCloudWrapper mNextCloudWrapper;


    public NextCloudSSOSyncLister(NextCloudWrapper nextCloudWrapper) {
        mNextCloudWrapper = nextCloudWrapper;
    }

    public static List<RemoteFile> parseInputStream(InputStream stream) throws Exception{
        List<RemoteFile> files = new ArrayList<>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(stream);
        NodeList items = doc.getElementsByTagName("d:response");
        for(int i = 0; i < items.getLength(); i++){
            RemoteFile remoteFile = new RemoteFile();
            Element node = (Element) items.item(i);
            String path = node.getElementsByTagName("d:href").item(0).getTextContent();
            Log.d(TAG, "href "+path);
            path = path.substring(path.indexOf("/remote.php/webdav/")+"/remote.php/webdav/".length());
            Log.d(TAG, "corrected "+path);
            remoteFile.setRemotePath(Uri.decode(path));

            String modTimeStr = node.getElementsByTagName("d:getlastmodified").item(0).getTextContent();
            if(!modTimeStr.isEmpty())
                remoteFile.setModifiedTimestamp(simpleDateFormat.parse(modTimeStr).getTime());
            remoteFile.setEtag(node.getElementsByTagName("d:getetag").item(0).getTextContent().replace("\"", ""));
            if(node.getElementsByTagName("d:getcontenttype").getLength()>0) {
                remoteFile.setMimeType(node.getElementsByTagName("d:getcontenttype").item(0).getTextContent());
            }
            NodeList contentLength = node.getElementsByTagName("d:getcontentlength");
            if(contentLength.getLength()>0 && !contentLength.item(0).getTextContent().isEmpty())
                remoteFile.setSize(Long.parseLong(contentLength.item(0).getTextContent()));
            NodeList resourceType = node.getElementsByTagName("d:resourcetype");
            if(resourceType.getLength()>0 && ((Element)resourceType.item(0)).getElementsByTagName("d:collection").getLength()>0)
                remoteFile.setMimeType("DIR");
            files.add(remoteFile);
        }
        return files;
    }

    public static String encodePath(String remotePath){
        String encoded = "";
        for(String part:remotePath.split("/")){
            encoded+="/"+Uri.encode(part);
        }
        if(encoded.startsWith("/"))
            encoded = encoded.substring(1);
        return encoded;
    }

    /**
     * throws exception when error
     * @param path
     */
    public List<RemoteFile> retrieveList(String path) throws Exception {
        path = encodePath(path);
        Log.d(TAG, "retrieveList "+path);
        NextcloudAPI nextcloudAPI = mNextCloudWrapper.getNextcloudApi();
        NextcloudRequest nextcloudRequest = new NextcloudRequest.Builder()
                .setMethod("PROPFIND")
                .setUrl("/remote.php/webdav/"+path)
                .build();
        InputStream inputStream = nextcloudAPI.performNetworkRequestV2(nextcloudRequest).getBody();
        return parseInputStream(inputStream);
    }
}
