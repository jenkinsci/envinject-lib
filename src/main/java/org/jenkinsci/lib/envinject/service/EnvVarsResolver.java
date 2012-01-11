package org.jenkinsci.lib.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Run;
import hudson.remoting.Callable;
import org.jenkinsci.lib.envinject.EnvInjectAction;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * @author Gregory Boissinot
 */
public class EnvVarsResolver implements Serializable {

    public Map<String, String> getPollingEnvVars(AbstractProject project, Node node) throws EnvInjectException {
        Run lastBuild = project.getLastBuild();
        if (lastBuild != null) {
            EnvInjectDetector detector = new EnvInjectDetector();
            if (detector.isEnvInjectPluginActivated()) {
                EnvInjectAction envInjectAction = lastBuild.getAction(EnvInjectAction.class);
                if (envInjectAction != null) {
                    return envInjectAction.getEnvMap();
                }
            }
        }
        return getDefaultEnvVarsJob(project, node);
    }

    private Map<String, String> getDefaultEnvVarsJob(AbstractProject project, Node node) throws EnvInjectException {
        Map<String, String> result = computeEnvVarsMaster(project);
        if (node != null) {
            result.putAll(computeEnvVarsNode(project, node));
        }
        return result;
    }

    private Map<String, String> computeEnvVarsMaster(AbstractProject project) throws EnvInjectException {
        EnvVars env = new EnvVars();
        env.put("JENKINS_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey()));
        env.put("HUDSON_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey())); // Legacy compatibility
        env.put("JOB_NAME", project.getFullName());
        env.put("JENKINS_HOME", Hudson.getInstance().getRootDir().getPath());
        env.put("HUDSON_HOME", Hudson.getInstance().getRootDir().getPath());   // legacy compatibility
        return env;
    }

    private Map<String, String> computeEnvVarsNode(AbstractProject project, Node node) throws EnvInjectException {
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

