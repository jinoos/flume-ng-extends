package com.jinoos.flume;

import java.util.regex.Pattern;

public class DirPattern {
  private String path;
  private Pattern filePattern;

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public Pattern getFilePattern() {
    return filePattern;
  }

  public void setFilePattern(Pattern filePattern) {
    this.filePattern = filePattern;
  }
}