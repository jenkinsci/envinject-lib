package org.jenkinsci.lib.envinject.service;

import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import org.jenkinsci.lib.envinject.EnvInjectAction;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectActionRetriever {

    public EnvInjectAction getEnvInjectAction(AbstractBuild<?, ?> build) {
        EnvInjectAction envInjectAction;
        if (build instanceof MatrixRun) {
            envInjectAction = ((MatrixRun) build).getParentBuild().getAction(EnvInjectAction.class);
        } else {
            envInjectAction = build.getAction(EnvInjectAction.class);
        }
        return envInjectAction;
    }
}
