package org.jenkinsci.lib.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.Callable;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvVarsResolver implements Serializable {

    public Map<String, String> getPollingEnvVars(AbstractProject project, /*can be null*/ Node node) throws EnvInjectException {

        if (project == null) {
            throw new NullPointerException("A project object must be set.");
        }

        Run lastBuild = project.getLastBuild();
        if (lastBuild != null) {
            EnvInjectDetector detector = new EnvInjectDetector();
            if (detector.isEnvInjectPluginInstalled()) {
                return getEnVars((AbstractBuild) lastBuild);
            }
        }

        if (node == null) {
            return Collections.emptyMap();
        }

        return getDefaultEnvVarsJob(project, node);
    }

    public Map<String, String> getEnVars(AbstractBuild build) throws EnvInjectException {

        if (build == null) {
            throw new NullPointerException("A build object must be set.");
        }

        EnvInjectActionRetriever envInjectActionRetriever = new EnvInjectActionRetriever();
        Action envInjectAction = envInjectActionRetriever.getEnvInjectAction(build);
        if (envInjectAction != null) {
            try {
                Method method = envInjectAction.getClass().getMethod("getEnvMap");
                return (Map<String, String>) method.invoke(envInjectAction);
            } catch (NoSuchMethodException e) {
                throw new EnvInjectException(e);
            } catch (InvocationTargetException e) {
                throw new EnvInjectException(e);
            } catch (IllegalAccessException e) {
                throw new EnvInjectException(e);
            }
        }

        return getDefaultEnvVarsJob(build.getProject(), build.getBuiltOn());
    }

    public String resolveEnvVars(AbstractBuild build, String value) throws EnvInjectException {

        if (build == null) {
            throw new NullPointerException("A build object must be set.");
        }

        if (value == null) {
            return null;
        }

        return Util.replaceMacro(value, getEnVars(build));
    }

    private Map<String, String> getDefaultEnvVarsJob(AbstractProject project, Node node) throws EnvInjectException {
        assert project != null;
        assert node != null;
        assert node.getRootPath() != null;
        Map<String, String> result = gatherEnvVarsMaster(project);
        result.putAll(gatherEnvVarsNode(project, node));
        result.putAll(gatherEnvVarsNodeProperties(node));

        return result;
    }

    private Map<String, String> gatherEnvVarsMaster(AbstractProject project) throws EnvInjectException {
        assert project != null;
        EnvVars env = new EnvVars();
        env.put("JENKINS_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey()));
        env.put("HUDSON_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey())); // Legacy compatibility
        env.put("JOB_NAME", project.getFullName());
        env.put("JENKINS_HOME", Hudson.getInstance().getRootDir().getPath());
        env.put("HUDSON_HOME", Hudson.getInstance().getRootDir().getPath());   // legacy compatibility

        String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl != null) {
            env.put("JENKINS_URL", rootUrl);
            env.put("HUDSON_URL", rootUrl); // Legacy compatibility
            env.put("JOB_URL", rootUrl + project.getUrl());
        }

        return env;
    }

    //Strong limitation: Restrict here to EnvironmentVariablesNodeProperty subclasses
    //in order to avoid the propagation of a Launcher object and a BuildListener object
    private Map<String, String> gatherEnvVarsNodeProperties(Node node) throws EnvInjectException {

        EnvVars env = new EnvVars();

        for (NodeProperty nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                env.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
            }
        }

        if (node != null) {
            for (NodeProperty nodeProperty : node.getNodeProperties()) {
                if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    env.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
                }
            }
        }

        return env;
    }

    private Map<String, String> gatherEnvVarsNode(AbstractProject project, Node node) throws EnvInjectException {
        assert project != null;
        assert node != null;
        assert node.getRootPath() != null;
        try {
            Map<String, String> envVars = node.getRootPath().act(new Callable<Map<String, String>, EnvInjectException>() {
                public Map<String, String> call() throws EnvInjectException {
                    return EnvVars.masterEnvVars;
                }
            });

            envVars.put("NODE_NAME", node.getNodeName());
            envVars.put("NODE_LABELS", Util.join(node.getAssignedLabels(), " "));
            FilePath wFilePath = project.getSomeWorkspace();
            if (wFilePath != null) {
                envVars.put("WORKSPACE", wFilePath.getRemote());
            }

            return envVars;

        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        }
    }

}

