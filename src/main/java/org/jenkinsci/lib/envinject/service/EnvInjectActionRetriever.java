package org.jenkinsci.lib.envinject.service;

import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import org.jenkinsci.lib.envinject.EnvInjectAction;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectActionRetriever {

    public EnvInjectAction getEnvInjectAction(AbstractBuild<?, ?> build) {

        if (build == null) {
            throw new IllegalArgumentException("A build object must be set.");
        }

        EnvInjectAction envInjectAction;
        if (build instanceof MatrixRun) {
            envInjectAction = ((MatrixRun) build).getParentBuild().getAction(EnvInjectAction.class);
        } else {
            envInjectAction = build.getAction(EnvInjectAction.class);
        }
        return envInjectAction;
    }
}
