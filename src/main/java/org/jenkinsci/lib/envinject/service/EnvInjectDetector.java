package org.jenkinsci.lib.envinject.service;

import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import org.jenkinsci.lib.envinject.EnvInjectAction;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectDetector {

    public boolean isEnvInjectActivated(AbstractBuild build) {

        if (build == null) {
            throw new NullPointerException("A build object must be set.");
        }

        EnvInjectActionRetriever envInjectActionRetriever = new EnvInjectActionRetriever();
        EnvInjectAction envInjectAction = envInjectActionRetriever.getEnvInjectAction(build);
        return envInjectAction != null;
    }

    public boolean isEnvInjectPluginActivated() {
        Plugin envInjectPlugin = Hudson.getInstance().getPlugin("envinject");
        return envInjectPlugin != null;
    }
}
