package com.jinoos.flume;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.flume.Transaction;
import org.apache.flume.source.AbstractSource;

public class FileSet implements DirectoryTailHeader
{
    private AbstractSource      source;
    private FileObject          fileObject;
    private BufferedReader      bufferedReader;
    private Transaction         transaction;
    private List<String>        bufferList;
    private Map<String, String> headers;

    public FileSet(AbstractSource source, FileObject fileObject) throws IOException
    {
        this.source = source;
        this.fileObject = fileObject;
        this.bufferedReader = bufferedReader;
        this.bufferList = new ArrayList<String>();
        this.bufferedReader = new BufferedReader(new FileReader(new File(fileObject.getName().getPath())));
        while(this.bufferedReader.readLine() != null)
            ;
        bufferList = new ArrayList<String>();
        headers = new HashMap<String, String>();
        this.seq = 0L;
    }

    public void setHeader(String key, String value)
    {
        headers.put(key, value);
    }

    public String getHeader(String key)
    {
        headers.get(key);
        return null;
    }

    public void clearHeaders()
    {
        headers.clear();
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }

    public Map<String, String> getheaders()
    {
        // TODO Auto-generated method stub
        return null;
    }

    private Long seq;

    public AbstractSource getSource()
    {
        return source;
    }

    public void setSource(AbstractSource source)
    {
        this.source = source;
    }

    public List<String> getBufferList()
    {
        return bufferList;
    }

    public void setBufferList(List<String> bufferList)
    {
        this.bufferList = bufferList;
    }

    public Transaction getTransaction()
    {
        return transaction;
    }

    public void setTransaction(Transaction transaction)
    {
        this.transaction = transaction;
    }

    public FileObject getFileObject()
    {
        return fileObject;
    }

    public void setFileObject(FileObject fileObject)
    {
        this.fileObject = fileObject;
    }

    public BufferedReader getBufferedReader()
    {
        return bufferedReader;
    }

    public void setBufferedReader(BufferedReader bufferedReader)
    {
        this.bufferedReader = bufferedReader;
    }

    public Long getSeq()
    {
        return ++this.seq;
    }

    /*
     * public void setSeq(Long seq) { this.seq = seq; }
     */

}
