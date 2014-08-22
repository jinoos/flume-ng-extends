flume-ng-extends
================

Source of Flume NG for tailing files in a directory

Configuration
=============
| Property Name | Default | Description |
| ------------- | :-----: | :---------- |
| Channels | - |  |
| Type | - | com.jinoos.flume.DirectoryTailSource |
| dirs | - | Nick of directories, it's such as list of what directories are monitored |
| dirs.NICK.path | - | Directory path |
| dirs.NICK.file-pattern | - | Pattern of target file(s), ex) '^(.*)$' is for all files |

* Example
```
agent.sources = dirMon
agent.sources.dirMon.type = com.jinoos.flume.DirectoryTailSource
agent.sources.dirMon.dirs = tmpDir varLogDir
agent.sources.dirMon.dirs.tmpDir.path = /tmp
agent.sources.dirMon.dirs.tmpDir.file-pattern = ^(message)$       # /var/log/message
agent.sources.dirMon.dirs.varLogDir.path = /var/log
agent.sources.dirMon.dirs.varLogDir.file-pattern = ^(.*)(\.log)$  # /tmp/*.log
```
