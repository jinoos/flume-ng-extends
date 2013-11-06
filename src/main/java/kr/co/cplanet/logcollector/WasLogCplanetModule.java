package kr.co.cplanet.logcollector;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.flume.Context;
import org.apache.flume.conf.Configurable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jinoos.flume.DirectoryTailParserModulable;
import com.jinoos.flume.FileSet;

public class WasLogCplanetModule implements Configurable,
    DirectoryTailParserModulable {
  private static final Logger logger = LoggerFactory
      .getLogger(WasLogCplanetModule.class);

  private static String firstLinePattern = "^([0-9\\-: ]{19}) \\[([^\\]]+)\\] \\[([^\\]]+)\\] \\[([^\\(]+)(\\.)([^\\.]+)\\(:([0-9]+)\\)\\] - (.*)$";
  private Pattern pattern = Pattern.compile(firstLinePattern);;

  private static DateFormat dateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss");

  private static final String CONFIG_SERVICE_NAME = "service-name";
  private static final String CONFIG_INSTANCE_NAME = "instance-name";

  private static final String DEFAULT_SERVICE_NAME = "none";
  private static final String DEFAULT_INSTANCE_NAME = "none";

  private String serviceName = DEFAULT_SERVICE_NAME;
  private String instanceName = DEFAULT_INSTANCE_NAME;

  public void flush() {
  }

  public void parse(String line, FileSet fileSet) {
    Matcher match = pattern.matcher(line);

    if (match.find()) {
      fileSet
          .setHeader("log_file", fileSet.getFileObject().getName().getPath());
      fileSet.setHeader("log_file_seq", Long.toString(fileSet.getSeq()));

      Date logDate = null;

      try {
        logDate = dateFormat.parse(match.group(1));
      } catch (ParseException e) {
      }

      fileSet.setHeader("log_timestamp", "" + (logDate.getTime() / 1000));
      fileSet.setHeader("log_severity", match.group(2).trim());
      fileSet.setHeader("log_threadname", match.group(3).trim());
      fileSet.setHeader("log_classname", match.group(4));
      fileSet.setHeader("log_methodname", match.group(6));
      fileSet.setHeader("log_line_number", match.group(7));
      fileSet.setHeader("log_firstline", match.group(8).trim());
      fileSet.setHeader("service", serviceName);
      fileSet.setHeader("instance", instanceName);
    }
  }

  public boolean isFirstLine(String line) {
    return pattern.matcher(line).find();
  }

  public boolean isLastLine(String line) {
    return false;
  }

  public void configure(Context context) {
    serviceName = context.getString(CONFIG_SERVICE_NAME, DEFAULT_SERVICE_NAME);
    instanceName = context.getString(CONFIG_INSTANCE_NAME,
        DEFAULT_INSTANCE_NAME);
  }
}
