package com.jinoos.flume;

import java.util.Map;

public interface DirectoryTailHeader {
  public void setHeader(String key, String value);

  public String getHeader(String key);

  public void clearHeaders();

  public Map<String, String> getheaders();
}
