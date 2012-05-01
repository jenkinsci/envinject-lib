package org.jenkinsci.lib.envinject.service;

import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import org.jenkinsci.lib.envinject.EnvInjectAction;

import java.util.List;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectActionRetriever {

    //Returns the abstract class Action due to a class loading issue
    //with EnvInjectAction subclasses. Subclasses cannot be casted from
    //all point of Jenkins (classes are not loaded in some points)
    public Action getEnvInjectAction(AbstractBuild<?, ?> build) {

        if (build == null) {
            throw new NullPointerException("A build object must be set.");
        }

        List<Action> actions;
        if (build instanceof MatrixRun) {
            actions = ((MatrixRun) build).getParentBuild().getActions();
        } else {
            actions = build.getActions();
        }

        for (Action action : actions) {
            if (EnvInjectAction.URL_NAME.equals(action.getUrlName())) {
                return action;
            }
        }
        return null;
    }
}
