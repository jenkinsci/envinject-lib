Changelog
===

### 1.25

Release date: _May 02, 2017_

* [JENKINS-43845](https://issues.jenkins-ci.org/browse/JENKINS-43845) -
Deprecate obsolete utility methods in the library.
Methods have been moved to the [EnvInject API Plugin](https://plugins.jenkins.io/envinject-api).
* [JENKINS-43535](https://issues.jenkins-ci.org/browse/JENKINS-43535) - 
Make `EnvInjectAction` API compatible with non-`AbstractProject` job types like Jenkins Pipeline

#### Developer notes

* Starting from this version, the library should not be directly used by plugins
  * Use dependency on the [EnvInject API Plugin](https://plugins.jenkins.io/envinject-api) instead.
  * Remove explicit dependency on the EnvInject Library.
  * Replace all usages of the 
`org.jenkinsci.lib.envinject.service` package by the new methods offered by the plugin. 

# 1.24

Release date: _Jul 01, 2016_

* [JENKINS-36184](https://issues.jenkins-ci.org/browse/JENKINS-36184) - 
Remove implicit dependency on the [Matrix Project Plugin](https://plugins.jenkins.io/matrix-project), 
which has been detached from the core.
It was causing regressions in Jenkins installations without this plugin.
 
### Changes before 1.24

See the commit history