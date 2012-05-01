package org.jenkinsci.lib.envinject.service;

import hudson.Plugin;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Hudson;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectDetector {

    public boolean isEnvInjectActivated(AbstractBuild build) {

        if (build == null) {
            throw new NullPointerException("A build object must be set.");
        }

        EnvInjectActionRetriever envInjectActionRetriever = new EnvInjectActionRetriever();
        Action envInjectAction = envInjectActionRetriever.getEnvInjectAction(build);
        return envInjectAction != null;
    }

    public boolean isEnvInjectPluginInstalled() {
        Plugin envInjectPlugin = Hudson.getInstance().getPlugin("envinject");
        return envInjectPlugin != null;
    }
}
