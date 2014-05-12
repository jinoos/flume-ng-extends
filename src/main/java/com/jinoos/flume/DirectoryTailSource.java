package com.jinoos.flume;

import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.vfs2.FileChangeEvent;
import org.apache.commons.vfs2.FileListener;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileMonitor;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.instrumentation.SourceCounter;
import org.apache.flume.source.AbstractSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * <PRE>
 * 1. ClassName : 
 * 2. FileName  : WasAppLogSource.java
 * 3. Package  : kr.co.cplanet.flumeng.source
 * 4. Comment  :
 * 5. 작성자   : jinoos
 * 6. 작성일   : 2013. 10. 16. 오후 9:22:44
 * </PRE>
 */
public class DirectoryTailSource extends AbstractSource implements
    Configurable, EventDrivenSource {
  private static final String CONFIG_SEPERATOR = ".";
  private static final String CONFIG_DIRS = "dirs";
  private static final String CONFIG_PATH = "path";
  private static final String CONFIG_FILE_PATTERN = "file-pattern";
  private static final String CONFIG_PARSER = "parser";
  private static final String CONFIG_KEEP_BUFFER_INTERVAL = "keep-buffer-interval";
  private static final String CONFIG_FILE_MONITOR_SLEEP_TIME = "file-monitor-sleep-time";

  // group(0) : DateTime YYYY-MM-DD HH:ii:ss
  // group(1) : Severity
  // group(2) : Thread Number
  // group(3) : Class
  // group(5) : Method
  // group(6) : line number
  // group(7) : Message
  private static final String DEFAULT_FILE_PATTERN = "^(.*)$";

  private static final String DEFAULT_PARSER_MODULE_CLASS = "com.jinoos.flume.SingleLineParserModule";

  private static final long DEFAULT_KEEP_BUFFER_INTERVAL = 1000;
  private static final long DEFAULT_FILE_MONITOR_SLEEP_TIME = 500;

  private static final Logger logger = LoggerFactory
      .getLogger(DirectoryTailSource.class);

  private SourceCounter sourceCounter;

  private String confDirs;

  private Map<String, DirPattern> dirMap;
  private Map<String, DirPattern> pathMap;

  private ExecutorService executorService;
  private MonitorRunnable monitorRunnable;
  private Future<?> monitorFuture;

  private long keepBufferInterval;
  private long fileMonitorSleepTime;

  DirectoryTailParserModulable parserModule;

  private DefaultFileMonitor fileMonitor;

  // private static final long eventQueueWorkerTimeoutMiliSecond = 1000;
  private static final int eventQueueWorkerSize = 10;
  private static final int maxEventQueueSize = 1000 * 1000;
  private BlockingQueue<DirectoryTailEvent> eventQueue = new LinkedBlockingQueue<DirectoryTailEvent>(
      maxEventQueueSize);
  private Future<?>[] workerFuture = new Future<?>[eventQueueWorkerSize];

  private FileSystemManager fsManager;
  private Hashtable<String, FileSet> fileSetMap;

  /**
   * <PRE>
   * 1. MethodName : configure
   * 2. ClassName  : DirectoryTailSource
   * 3. Comment   :
   * 4. 작성자    : Jinoos Lee <jinoos@gmail.com>
   * 5. 작성일    : 2013. 11. 1. 오후 12:51:20
   * </PRE>
   * 
   * @param context
   */
  public void configure(Context context) {
    logger.info("Source Configuring..");

    dirMap = new HashMap<String, DirPattern>();
    pathMap = new HashMap<String, DirPattern>();

    keepBufferInterval = context.getLong(CONFIG_KEEP_BUFFER_INTERVAL,
        DEFAULT_KEEP_BUFFER_INTERVAL);

    fileMonitorSleepTime = context.getLong(CONFIG_FILE_MONITOR_SLEEP_TIME,
        DEFAULT_FILE_MONITOR_SLEEP_TIME);

    confDirs = context.getString(CONFIG_DIRS).trim();
    Preconditions.checkState(confDirs != null,
        "Configuration must be specified directory(ies).");

    String[] confDirArr = confDirs.split(" ");

    Preconditions.checkState(confDirArr.length > 0, CONFIG_DIRS
        + " must be specified at least one.");

    for (int i = 0; i < confDirArr.length; i++) {

      String path = context.getString(CONFIG_DIRS + CONFIG_SEPERATOR
          + confDirArr[i] + CONFIG_SEPERATOR + CONFIG_PATH);
      if (path == null) {
        logger
            .warn("Configuration is empty : " + CONFIG_DIRS + CONFIG_SEPERATOR
                + confDirArr[i] + CONFIG_SEPERATOR + CONFIG_PATH);
        continue;
      }

      String patternString = context.getString(CONFIG_DIRS + CONFIG_SEPERATOR
          + confDirArr[i] + CONFIG_SEPERATOR + CONFIG_FILE_PATTERN);
      if (patternString == null) {
        patternString = DEFAULT_FILE_PATTERN;
      }

      Pattern pattern = null;
      try {
        pattern = Pattern.compile(patternString);
      } catch (PatternSyntaxException e) {
        logger.warn("Configuration has wrong file pattern, " + CONFIG_DIRS
            + "." + confDirArr[i] + "." + CONFIG_FILE_PATTERN + ":"
            + patternString);
        logger.warn("Directory will be set default file pattern, "
            + DEFAULT_FILE_PATTERN);

        pattern = Pattern.compile(patternString);
      }

      DirPattern dir = new DirPattern();
      dir.setPath(path);
      dir.setFilePattern(pattern);

      dirMap.put(confDirArr[i], dir);
      logger.debug("parsed dirs configure dir : " + confDirArr[i] + ", path : "
          + path);
    }

    String confParserModule = DEFAULT_PARSER_MODULE_CLASS;
    try {
      confParserModule = context.getString(CONFIG_PARSER,
          DEFAULT_PARSER_MODULE_CLASS);
      parserModule = (DirectoryTailParserModulable) Class.forName(
          confParserModule).newInstance();
      logger.debug("parserClass : " + confParserModule);
    } catch (ClassNotFoundException e) {
      Preconditions.checkState(false, e.getMessage() + " " + confParserModule);
      logger.error(e.getMessage(), e);
    } catch (InstantiationException e) {
      Preconditions.checkState(false, e.getMessage() + " " + confParserModule);
      logger.error(e.getMessage(), e);
    } catch (IllegalAccessException e) {
      Preconditions.checkState(false, e.getMessage() + " " + confParserModule);
      logger.error(e.getMessage(), e);
    }

    parserModule.configure(context);
  }

  /**
   * <PRE>
   * 1. MethodName : start
   * 2. ClassName  : WasAppLogSource
   * 3. Comment   :
   * 4. 작성자    : jinoos
   * 5. 작성일    : 2013. 10. 16. 오후 10:43:06
   * </PRE>
   */
  @Override
  public void start() {
    logger.info("Source Starting..");

    if (sourceCounter == null) {
      sourceCounter = new SourceCounter(getName());
    }

    fileSetMap = new Hashtable<String, FileSet>();

    try {
      fsManager = VFS.getManager();
    } catch (FileSystemException e) {
      logger.error(e.getMessage(), e);
      return;
    }

    monitorRunnable = new MonitorRunnable();

    fileMonitor = new DefaultFileMonitor(monitorRunnable);
    fileMonitor.setRecursive(false);

    FileObject fileObject;

    logger.debug("Dirlist count " + dirMap.size());
    for (Entry<String, DirPattern> entry : dirMap.entrySet()) {
      logger.debug("Scan dir " + entry.getKey());

      DirPattern dirPattern = entry.getValue();

      try {
        fileObject = fsManager.resolveFile(dirPattern.getPath());
      } catch (FileSystemException e) {
        logger.error(e.getMessage(), e);
        continue;
      }

      try {
        if (!fileObject.isReadable()) {
          logger.warn("No have readable permission, " + fileObject.getURL());
          continue;
        }

        if (FileType.FOLDER != fileObject.getType()) {
          logger.warn("Not a directory, " + fileObject.getURL());
          continue;
        }

        // 폴더를 Monitoring 대상에 추가한다.
        fileMonitor.addFile(fileObject);
        logger.debug(fileObject.getName().getPath()
            + " directory has been add in monitoring list");
        pathMap.put(fileObject.getName().getPath(), entry.getValue());
      } catch (FileSystemException e) {
        logger.warn(e.getMessage(), e);
        continue;
      } catch (Exception e) {
        logger.debug(e.getMessage(), e);
      }

    }

    executorService = Executors.newFixedThreadPool(eventQueueWorkerSize + 1);
    monitorFuture = executorService.submit(monitorRunnable);

    for (int i = 0; i < eventQueueWorkerSize; i++) {
      workerFuture[i] = executorService.submit(new WorkerRunnable(this));
    }

    sourceCounter.start();
    super.start();
  }

  /**
   * <PRE>
   * 1. MethodName : stop
   * 2. ClassName  : WasAppLogSource
   * 3. Comment   :
   * 4. 작성자    : jinoos
   * 5. 작성일    : 2013. 10. 16. 오후 10:43:03
   * </PRE>
   */
  @Override
  public void stop() {
    logger.info("Source Stopping..");
    fileMonitor.stop();
    sourceCounter.stop();

  }

  private class WorkerRunnable implements Runnable {
    private AbstractSource source;

    private WorkerRunnable(AbstractSource source) {
      this.source = source;
    }

    public void run() {
      while (true) {
        try {
          // DirectoryTailEvent event = eventQueue.poll(
          // eventQueueWorkerTimeoutMiliSecond,
          // TimeUnit.MILLISECONDS);
          DirectoryTailEvent event = eventQueue.take();

          if (event == null) {
            continue;
          }

          if (event.type == FileEventType.FILE_CHANGED) {
            fileChanged(event.event);
          } else if (event.type == FileEventType.FILE_CREATED) {
            fileCreated(event.event);
          } else if (event.type == FileEventType.FILE_DELETED) {
            fileDeleted(event.event);
          } else if (event.type == FileEventType.FLUSH) {
            if (event.fileSet != null)
              sendEvent(event.fileSet);
          }
        } catch (InterruptedException e) {
          logger.debug(e.getMessage(), e);
        } catch (FileSystemException e) {
          logger.info(e.getMessage(), e);
        }
      }
    }

    private void fileCreated(FileChangeEvent event) throws FileSystemException {
      String path = event.getFile().getName().getPath();
      String dirPath = event.getFile().getParent().getName().getPath();

      logger.debug(path + " has been created.");

      DirPattern dirPattern = null;
      dirPattern = pathMap.get(dirPath);

      if (dirPattern == null) {
        logger.warn("Occurred create event from un-indexed directory. "
            + dirPath);
        return;
      }

      // 파일명이 대상인지 검사한다.
      if (!isInFilePattern(event.getFile(), dirPattern.getFilePattern())) {
        logger.debug(path + " is not in file pattern.");
        return;
      }

      FileSet fileSet;

      fileSet = fileSetMap.get(event.getFile().getName().getPath());

      if (fileSet == null) {
        try {
          logger.info(path
              + " is not in monitoring list. It's going to be listed.");
          fileSet = new FileSet(source, event.getFile());
          fileSetMap.put(path, fileSet);
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
          return;
        }
      }
    }

    private void fileDeleted(FileChangeEvent event) throws FileSystemException {
      String path = event.getFile().getName().getPath();
      String dirPath = event.getFile().getParent().getName().getPath();

      logger.debug(path + " has been deleted.");

      DirPattern dirPattern = pathMap.get(dirPath);
      if (dirPattern == null) {
        logger.warn("Occurred delete event from un-indexed directory. "
            + dirPath);
        return;
      }

      if (!isInFilePattern(event.getFile(), dirPattern.getFilePattern())) {
        logger.debug(path + " is not in file pattern.");
        return;
      }

      FileSet fileSet = fileSetMap.get(path);

      if (fileSet != null) {
        fileSetMap.remove(path);
        logger.debug("Removed monitoring fileSet.");
      }
    }

    private void fileChanged(FileChangeEvent event) throws FileSystemException {
      String path = event.getFile().getName().getPath();
      String dirPath = event.getFile().getParent().getName().getPath();

      logger.debug(path + " has been changed.");

      DirPattern dirPattern = pathMap.get(dirPath);
      if (dirPattern == null) {
        logger.warn("Occurred change event from un-indexed directory. "
            + dirPath);
        return;
      }

      // 파일명이 대상인지 검사한다.
      if (!isInFilePattern(event.getFile(), dirPattern.getFilePattern())) {
        logger.debug("Not in file pattern, " + path);
        return;
      }

      FileSet fileSet = fileSetMap.get(event.getFile().getName().getPath());

      if (fileSet == null) {
        logger.warn(path + "is not in monitoring list.");
        try {
          fileSet = new FileSet(source, event.getFile());
          synchronized (fileSetMap) {
            fileSetMap.put(path, fileSet);
          }
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
          return;
        }
        return;
      }

      readMessage(fileSet);
    }

    // 파일이 대상 패턴에 존재하는지 검사한다.
    private boolean isInFilePattern(FileObject file, Pattern pattern) {
      String fileName = file.getName().getBaseName();
      Matcher matcher = pattern.matcher(fileName);
      if (matcher.find()) {
        return true;
      }
      return false;
    }

    // 파일을 읽고 Event를 생성한다.
    private void readMessage(FileSet fileSet) {
      try {
        String buffer;

        synchronized (fileSet) {

          while ((buffer = fileSet.readLine()) != null) {
            if (buffer.length() == 0) {
              continue;
            }

            boolean isFirstLine = parserModule.isFirstLine(buffer);
            if (isFirstLine) {
              sendEvent(fileSet);
              fileSet.appendLine(buffer);
              parserModule.parse(buffer, fileSet);

            } else {
              if (fileSet.getLineSize() == 0) {
                logger.debug("Wrong log format, " + buffer);
                continue;
              } else {
                fileSet.appendLine(buffer);
                parserModule.parse(buffer, fileSet);
              }
            }

            if (parserModule.isLastLine(buffer)) {
              sendEvent(fileSet);
            }
          }
        }
      } catch (IOException e) {
        logger.warn(e.getMessage(), e);
      }
    }

    private void sendEvent(FileSet fileSet) {
      if (fileSet.getBufferList().isEmpty())
        return;

      synchronized (fileSet) {
        StringBuffer sb = fileSet.getAllLines();
        Event event = EventBuilder.withBody(String.valueOf(sb).getBytes(),
            fileSet.getHeaders());
        source.getChannelProcessor().processEvent(event);
        sourceCounter.incrementEventReceivedCount();

        fileSet.clear();
      }
    }

  }

  private class MonitorRunnable implements Runnable, FileListener {
    public void run() {

      fileMonitor.setDelay(fileMonitorSleepTime);
      fileMonitor.start();

      while (true) {
        try {
          Thread.sleep(keepBufferInterval);
          fileMonitor.run();
        } catch (InterruptedException e) {
          logger.debug(e.getMessage(), e);
        }

        flushFileSetBuffer();
      }
    }

    // 파일생성을 감지함.
    public void fileCreated(FileChangeEvent event) throws Exception {
      DirectoryTailEvent dtEvent = new DirectoryTailEvent(event,
          FileEventType.FILE_CREATED);
      eventQueue.put(dtEvent);
    }

    // 파일 삭제를 감지함.
    public void fileDeleted(FileChangeEvent event) throws Exception {
      DirectoryTailEvent dtEvent = new DirectoryTailEvent(event,
          FileEventType.FILE_DELETED);
      eventQueue.put(dtEvent);
    }

    // 파일 변경을 감지함.
    public void fileChanged(FileChangeEvent event) throws Exception {
      DirectoryTailEvent dtEvent = new DirectoryTailEvent(event,
          FileEventType.FILE_CHANGED);
      eventQueue.put(dtEvent);
    }

    public void flush(FileSet fileSet) {
      DirectoryTailEvent dtEvent = new DirectoryTailEvent(fileSet);
      try {
        eventQueue.put(dtEvent);
      } catch (InterruptedException e) {
        logger.warn(e.getMessage(), e);
      }
    }

    private void flushFileSetBuffer() {
      synchronized (fileSetMap) {
        long cutTime = System.currentTimeMillis() - keepBufferInterval;

        for (Map.Entry<String, FileSet> entry : fileSetMap.entrySet()) {

          // If lastAppendTime is over than keepBufferInterval,
          // then, the message will be flushed even not be catched
          // new first line or last line. It's to prevent last message
          // delay delevery.
          if (entry.getValue().getBufferList().size() > 0
              && entry.getValue().getLastAppendTime() < cutTime) {
            flush(entry.getValue());
          }
        }
      }
    }
  }

  private enum FileEventType {
    FILE_CREATED, FILE_CHANGED, FILE_DELETED, FLUSH
  }

  private class DirectoryTailEvent {
    FileChangeEvent event;
    FileEventType type;
    FileSet fileSet;

    public DirectoryTailEvent(FileChangeEvent event, FileEventType type) {
      this.type = type;
      this.event = event;
      this.fileSet = null;
    }

    public DirectoryTailEvent(FileSet fileSet) {
      this.type = FileEventType.FLUSH;
      this.fileSet = fileSet;
      this.event = null;
    }
  }
}
