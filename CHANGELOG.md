Changelog
===

## New entries can be obtained on the [Releases](https://github.com/jenkinsci/envinject-lib/releases) tab on GitHub.

### 1.29

Release date: _Dec 28, 2017_

* [PR #15](https://github.com/jenkinsci/envinject-lib/pull/15) - 
Remove hidden string concatenation in `envinject.service.EnvInjectSavable` to reduce memory consumption.
* [PR #16](https://github.com/jenkinsci/envinject-lib/pull/16) -
Minor code cleanup.

### 1.28

Release date: _Oct 24, 2017_

* [JENKINS-47574](https://issues.jenkins-ci.org/browse/JENKINS-47574) -
Prevent `NullPointerException` in `EnvInjectAction#getEnvironment()` when it's called in XML deserialization without build reference.
* [JENKINS-47167](https://issues.jenkins-ci.org/browse/JENKINS-47167) - 
Prevent `NullPointerException` when serializing `EnvInjectAction` without build reference to the disk.

### 1.27

Release date: _Oct 07, 2017_

* [JENKINS-46479](https://issues.jenkins-ci.org/browse/JENKINS-46479) -
Prevent environment variables from being deleted on existing builds.
* [PR #12](https://github.com/jenkinsci/envinject-lib/pull/12) -
Update Jenkins core requirement to 1.625.3.
* [PR #12](https://github.com/jenkinsci/envinject-lib/pull/12) -
Fix core API usage issues reported by FindBugs.

### 1.26

Release date: _June 25, 2017_

* [JENKINS-45055](https://issues.jenkins-ci.org/browse/JENKINS-45055) -
Cleanup issues reported by FindBugs, including misformatted system log messages and `NullPointerException` risk.
* [PR #9](https://github.com/jenkinsci/envinject-lib/pull/9) -
Internal API: Restrict access to the `EnvInjectAction#envMap` cache value, getter methods should be used instead.
It prevents issues like [JENKINS-44965](https://issues.jenkins-ci.org/browse/JENKINS-44965).

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
