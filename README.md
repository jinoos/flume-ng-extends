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
agent.sources.dirs = tmpDir varLogDir
agent.sources.dirs.tmpDir.path = /tmp
agent.sources.dirs.tmpDir.file-pattern = ^(message)$       # /var/log/message
agent.sources.dirs.varLogDir.path = /var/log
agent.sources.dirs.varLogDir.file-pattern = ^(.*)(\.log)$  # /tmp/*.log
```
