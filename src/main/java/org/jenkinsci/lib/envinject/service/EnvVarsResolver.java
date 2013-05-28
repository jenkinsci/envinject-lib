package org.jenkinsci.lib.envinject.service;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.*;
import hudson.remoting.Callable;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import hudson.util.DescribableList;
import org.jenkinsci.lib.envinject.EnvInjectException;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            } catch (NoSuchMethodException e) {
                throw new EnvInjectException(e);
            } catch (InvocationTargetException e) {
                throw new EnvInjectException(e);
            } catch (IllegalAccessException e) {
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

    private Node getMasterNode() {
        Computer computer = Hudson.getInstance().toComputer();
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

        Hudson hudson = Hudson.getInstance();
        if (hudson != null) {
            DescribableList<NodeProperty<?>, NodePropertyDescriptor> globalNodeProperties = hudson.getGlobalNodeProperties();
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
            if (nodeProperties != null) {
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
        }

        return env;
    }

    private Map<String, String> gatherEnvVarsNode(AbstractProject project, Node node) throws EnvInjectException {
        assert project != null;
        assert node != null;
        assert node.getRootPath() != null;
        try {
            Map<String, String> envVars = new EnvVars(node.getRootPath().act(new Callable<Map<String, String>, EnvInjectException>() {
                public Map<String, String> call() throws EnvInjectException {
                    return EnvVars.masterEnvVars;
                }
            }));

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

