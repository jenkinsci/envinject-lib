package org.jenkinsci.lib.envinject.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.*;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author Gregory Boissinot
 * @deprecated The actual version of this API class is located in EnvInject API Plugin
 */
@Deprecated
@Restricted(NoExternalUse.class)
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID", justification = "Deprecated code")
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
            return getFallBackMasterNode(project);
        }
        if (node.getRootPath() == null) {
            return getFallBackMasterNode(project);
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
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new EnvInjectException(e);
            }
        }

        Node builtOn = build.getBuiltOn();
        //-- Check if node is always on. Otherwise, gather master env vars
        if (builtOn == null) {
            return getFallBackMasterNode(build.getProject());
        }
        if (builtOn.getRootPath() == null) {
            return getFallBackMasterNode(build.getProject());
        }
        //-- End check

        //Get envVars from the node of the last build
        return getDefaultEnvVarsJob(build.getProject(), builtOn);
    }

    private Map<String, String> getFallBackMasterNode(AbstractProject project) throws EnvInjectException {
        Node masterNode = getMasterNode();
        if (masterNode == null) {
            return gatherEnvVarsMaster(project);
        }
        return getDefaultEnvVarsJob(project, masterNode);
    }

    @CheckForNull
    private Node getMasterNode() {
        final Jenkins jenkins = Jenkins.getInstance();
        Computer computer = jenkins != null ? jenkins.toComputer() : null;
        if (computer == null) {
            return null; //Master can have no executors
        }
        return computer.getNode();
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
        //--- Same code for master or a slave node
        Map<String, String> result = gatherEnvVarsMaster(project);
        result.putAll(gatherEnvVarsNode(project, node));
        result.putAll(gatherEnvVarsNodeProperties(node));
        return result;
    }

    private Map<String, String> gatherEnvVarsMaster(AbstractProject project) throws EnvInjectException {
        final Jenkins jenkins = Jenkins.getActiveInstance();

        assert project != null;
        EnvVars env = new EnvVars();
        env.put("JENKINS_SERVER_COOKIE", Util.getDigestOf("ServerID:" + jenkins.getSecretKey()));
        env.put("HUDSON_SERVER_COOKIE", Util.getDigestOf("ServerID:" + jenkins.getSecretKey())); // Legacy compatibility
        env.put("JOB_NAME", project.getFullName());
        env.put("JENKINS_HOME", jenkins.getRootDir().getPath());
        env.put("HUDSON_HOME", jenkins.getRootDir().getPath());   // legacy compatibility

        String rootUrl = jenkins.getRootUrl();
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

        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = jenkins.getGlobalNodeProperties();
            if (globalNodeProperties != null) {
                for (NodeProperty nodeProperty : globalNodeProperties) {
                    if (nodeProperty != null && nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                        env.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
                    }
                }
            }
        }

        if (node != null) {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> nodeProperties = node.getNodeProperties();
            for (NodeProperty nodeProperty : nodeProperties) {
                if (nodeProperty != null && nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    EnvVars envVars = ((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars();
                    if (envVars != null) {
                        for (Map.Entry<String, String> entry : envVars.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (key != null && value != null) {
                                env.put(key, value);
                            }
                        }
                    }
                }
            }
        }

        return env;
    }

    private Map<String, String> gatherEnvVarsNode(@Nonnull AbstractProject project, @Nonnull Node node) throws EnvInjectException {
        assert project != null;
        assert node != null;

        final FilePath p = node.getRootPath();
        if (p == null) {
            throw new EnvInjectException("Cannot get Node root path for node '" + node +
                    "'. The node is offline or the path is not available");
        }

        try {
            Map<String, String> envVars = new EnvVars(p.act(new SystemEnvVarsGetter()));

            envVars.put("NODE_NAME", node.getNodeName());
            envVars.put("NODE_LABELS", Util.join(node.getAssignedLabels(), " "));
            FilePath wFilePath = project.getSomeWorkspace();
            if (wFilePath != null) {
                envVars.put("WORKSPACE", wFilePath.getRemote());
            }

            return envVars;

        } catch (IOException | InterruptedException ioe) {
            throw new EnvInjectException(ioe);
        }
    }

    private static final class SystemEnvVarsGetter extends MasterToSlaveCallable<Map<String, String>, EnvInjectException> {
        public Map<String, String> call() throws EnvInjectException {
            return EnvVars.masterEnvVars;
        }
    }

}

