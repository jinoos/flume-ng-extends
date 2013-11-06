package com.jinoos.flume;

import java.util.List;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;

public class SingleLineParserModule implements Configurable, DirectoryTailParserModulable
{

    public void flush()
    {
    }

    public void parse(String line, FileSet header)
    {
    }

    public boolean isFirstLine(String line)
    {
        return false;
    }

    public boolean isLastLine(String line)
    {
        return true;
    }

    public void configure(Context context)
    {
        // TODO Auto-generated method stub
    }
}
