package com.spisoft.sync.wrappers.nextcloud;

import com.owncloud.android.lib.resources.files.RemoteFile;

import org.apache.jackrabbit.webdav.client.methods.DavMethodBase;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.XmlRequestEntity;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public interface NextCloudFileOperation {
    boolean download(String remotePath, String to, long size);
    boolean upload(String fromFile, String remotePath);
    boolean mkdir(String remotePath);
    boolean delete(String remotePath);
    RemoteFile getFileInfo(String remotePath);

    String getEtag(String remotePath);


    class MyPropFindMethod extends PropFindMethod {

        private String mRequestString;

        public MyPropFindMethod(String uri) throws IOException {
            super(uri);
        }

        public MyPropFindMethod(String uri, DavPropertyNameSet propNameSet, int depth) throws IOException {
            super(uri, propNameSet, depth);
        }

        public MyPropFindMethod(String uri, int propfindType, int depth) throws IOException {
            super(uri, propfindType, depth);
        }

        public MyPropFindMethod(String uri, int propfindType, DavPropertyNameSet propNameSet, int depth) throws IOException {
            super(uri, propfindType, propNameSet, depth);
        }

        public void setRequestBody(Document requestBody) throws IOException {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer;
            try {
                transformer = tf.newTransformer();
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(requestBody), new StreamResult(writer));

                mRequestString = writer.getBuffer().toString();
            }
            catch (TransformerException e)
            {
                e.printStackTrace();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            setRequestEntity(new XmlRequestEntity(requestBody));
        }

        public String getMyRequestString(){
            return mRequestString;
        }
    }
}
