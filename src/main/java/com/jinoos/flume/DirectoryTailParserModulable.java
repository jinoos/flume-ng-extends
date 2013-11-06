package com.jinoos.flume;

import java.util.List;
import java.util.Map;

import org.apache.flume.conf.Configurable;

/**
 * <PRE>
 * 1. ClassName : 
 * 2. FileName  : DirectoyTailModulable.java
 * 3. Package  : kr.co.cplanet.logcollector.flumeng.source
 * 4. Comment  : 
 * 5. @author  : Jinoos Lee <jinoos@gmail.com>
 * 6. 작성일   : 2013. 10. 25. 오전 12:51:16
 * </PRE>
 */
public interface DirectoryTailParserModulable extends Configurable
{
    public void flush();
    public void parse(String line, FileSet header);
    public boolean isFirstLine(String line);
    public boolean isLastLine(String line);
}
