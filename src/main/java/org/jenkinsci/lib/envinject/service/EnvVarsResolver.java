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

    public Map<String, String> getPollingEnvVars(Job<?, ?> job, /*can be null*/ Node node) throws EnvInjectException {

        if (job == null) {
            throw new NullPointerException("A project object must be set.");
        }

        Run lastBuild = job.getLastBuild();
        if (lastBuild != null) {
            EnvInjectDetector detector = new EnvInjectDetector();
            if (detector.isEnvInjectPluginInstalled()) {
                return getEnVars(lastBuild);
            }
        }

        if (node == null) {
            return getFallBackMasterNode(job);
        }
        if (node.getRootPath() == null) {
            return getFallBackMasterNode(job);
        }

        return getDefaultEnvVarsJob(job, node);
    }

    public Map<String, String> getEnVars(Run<?, ?> run) throws EnvInjectException {

        if (run == null) {
            throw new NullPointerException("A build object must be set.");
        }

        EnvInjectActionRetriever envInjectActionRetriever = new EnvInjectActionRetriever();
        Action envInjectAction = envInjectActionRetriever.getEnvInjectAction(run);
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

        // Retrieve node used for this build
        Node builtOn = (run instanceof AbstractBuild) ? ((AbstractBuild)run).getBuiltOn() : null;
        
        // Check if node is always on. Otherwise, gather master env vars
        if (builtOn == null) {
            return getFallBackMasterNode(run.getParent());
        }
        if (builtOn.getRootPath() == null) {
            return getFallBackMasterNode(run.getParent());
        }

        // Get envVars from the node of the last build
        return getDefaultEnvVarsJob(run.getParent(), builtOn);
    }

    private Map<String, String> getFallBackMasterNode(Job<?, ?> job) throws EnvInjectException {
        Node masterNode = getMasterNode();
        if (masterNode == null) {
            return gatherEnvVarsMaster(job);
        }
        return getDefaultEnvVarsJob(job, masterNode);
    }

    private Node getMasterNode() {
        Computer computer = Hudson.getInstance().toComputer();
        if (computer == null) {
            return null; //Master can have no executors
        }
        return computer.getNode();
    }

    public String resolveEnvVars(Run<?, ?> run, String value) throws EnvInjectException {

        if (run == null) {
            throw new NullPointerException("A build object must be set.");
        }

        if (value == null) {
            return null;
        }

        return Util.replaceMacro(value, getEnVars(run));
    }


    private Map<String, String> getDefaultEnvVarsJob(Job<?, ?> job, Node node) throws EnvInjectException {
        assert job != null;
        assert node != null;
        assert node.getRootPath() != null;
        //--- Same code for master or a slave node
        Map<String, String> result = gatherEnvVarsMaster(job);
        result.putAll(gatherEnvVarsNode(job, node));
        result.putAll(gatherEnvVarsNodeProperties(node));
        return result;
    }

    private Map<String, String> gatherEnvVarsMaster(Job<?, ?> job) throws EnvInjectException {
        assert job != null;
        EnvVars env = new EnvVars();
        env.put("JENKINS_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey()));
        env.put("HUDSON_SERVER_COOKIE", Util.getDigestOf("ServerID:" + Hudson.getInstance().getSecretKey())); // Legacy compatibility
        env.put("JOB_NAME", job.getFullName());
        env.put("JENKINS_HOME", Hudson.getInstance().getRootDir().getPath());
        env.put("HUDSON_HOME", Hudson.getInstance().getRootDir().getPath());   // legacy compatibility

        String rootUrl = Hudson.getInstance().getRootUrl();
        if (rootUrl != null) {
            env.put("JENKINS_URL", rootUrl);
            env.put("HUDSON_URL", rootUrl); // Legacy compatibility
            env.put("JOB_URL", rootUrl + job.getUrl());
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

    private Map<String, String> gatherEnvVarsNode(Job<?, ?> job, Node node) throws EnvInjectException {
        assert job != null;
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
            
            if (job instanceof AbstractProject) {
                FilePath wFilePath = ((AbstractProject)job).getSomeWorkspace();
                if (wFilePath != null) {
                    envVars.put("WORKSPACE", wFilePath.getRemote());
                }
            }

            return envVars;

        } catch (IOException ioe) {
            throw new EnvInjectException(ioe);
        } catch (InterruptedException ie) {
            throw new EnvInjectException(ie);
        }
    }

}

