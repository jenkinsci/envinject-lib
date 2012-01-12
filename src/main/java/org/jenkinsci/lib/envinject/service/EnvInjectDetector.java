package org.jenkinsci.lib.envinject.service;

import hudson.Plugin;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import org.jenkinsci.lib.envinject.EnvInjectAction;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectDetector {

    public boolean isEnvInjectActivated(AbstractBuild build) {

        if (build == null) {
            throw new IllegalArgumentException("A build object must be set.");
        }

        if (build instanceof MatrixRun) {
            return (((MatrixRun) build).getParentBuild().getAction(EnvInjectAction.class)) != null;
        } else {
            return build.getAction(EnvInjectAction.class) != null;
        }
    }

    public boolean isEnvInjectPluginActivated() {
        Plugin envInjectPlugin = Hudson.getInstance().getPlugin("envinject");
        return envInjectPlugin != null;
    }
}
