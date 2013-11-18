package com.jinoos.flume;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class MultiLineParserModule implements Configurable,
    DirectoryTailParserModulable {
  private static final Logger logger = LoggerFactory
      .getLogger(MultiLineParserModule.class);

  private static final String CONFIG_FIRST_LINE_PATTERN = "first-line-pattern";
  private Pattern pattern;
  private String patternString;

  public void flush() {
  }

  public void parse(String line, FileSet header) {
  }

  public boolean isFirstLine(String line) {
    Matcher matcher = pattern.matcher(line);

    return matcher.find();
  }

  public boolean isLastLine(String line) {
    return false;
  }

  public void configure(Context context) {
    patternString = context.getString(CONFIG_FIRST_LINE_PATTERN);
    Preconditions.checkState(patternString != null,
        "Configuration must specify first-line-pattern.");

    try {
      pattern = Pattern.compile(patternString);
    } catch (PatternSyntaxException e) {
      Preconditions.checkState(pattern != null, e.getMessage());
      logger.error(e.getMessage(), e);
      return;
    }
  }
}
