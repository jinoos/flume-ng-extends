flume-ng-extends
================

Source of Flume NG for tailing files in a directory

Configuration
=============
| Property Name | Default | Description |
| ------------- | :-----: | :---------- |
| Channels | - |  |
| Type | - | com.jinoos.flume.DirectoryTailSource |
| Type | - | com.jinoos.flume.DirectoryTailSource |
| dirs | - | Nick of directories, it's such as list of what directories are monitored |
| parser | com.jinoos.flume.SingleLineParserModule | Implemented class path of DirectoryTailParserModulable. |
| dirs.NICK.path | - | Directory path |
| dirs.NICK.file-pattern | - | Pattern of target file(s), ex) '^(.*)$' is for all files |

For parser, there is two module classes what implemented DirectoryTailParserModulable
* SingleLineParserModule
* MultiLineParserModule 

* Example for SingleLineParserModule
```
agent.sources = dirMon
agent.sources.dirMon.type = com.jinoos.flume.DirectoryTailSource
agent.sources.dirMon.parser = com.jinoos.flume.SingleLineParserModule
agent.sources.dirMon.dirs = tmpDir varLogDir
agent.sources.dirMon.dirs.tmpDir.path = /tmp
agent.sources.dirMon.dirs.tmpDir.file-pattern = ^(message)$       # /var/log/message
agent.sources.dirMon.dirs.varLogDir.path = /var/log
agent.sources.dirMon.dirs.varLogDir.file-pattern = ^(.*)(\.log)$  # /tmp/*.log
```

* Example for MultiLineParserModule
```
agent.sources = dirMon
agent.sources.dirMon.type = com.jinoos.flume.DirectoryTailSource
agent.sources.dirMon.parser = com.jinoos.flume.MultiLineParserModule
# Regex pattern for first line, format "yyyy-MM-dd HH:mm:ss"
agent.sources.dirMon.first-line-pattern = ^([\d]{4})-([\d]{2})-([\d]{2}) ([\d]{2}):([\d]{2}):([\d]{2})
agent.sources.dirMon.dirs = tmpDir varLogDir
agent.sources.dirMon.dirs.tmpDir.path = /tmp
agent.sources.dirMon.dirs.tmpDir.file-pattern = ^(message)$       # /var/log/message
agent.sources.dirMon.dirs.varLogDir.path = /var/log
agent.sources.dirMon.dirs.varLogDir.file-pattern = ^(.*)(\.log)$  # /tmp/*.log
```
