package org.jenkinsci.lib.envinject;

import hudson.model.TaskListener;

import java.io.Serializable;
import javax.annotation.Nonnull;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectLogger implements Serializable {

    @Nonnull
    private final TaskListener listener;

    public EnvInjectLogger(@Nonnull TaskListener listener) {
        this.listener = listener;
    }

    @Nonnull
    public TaskListener getListener() {
        return listener;
    }

    public void info(@Nonnull String message) {
        listener.getLogger().println("[EnvInject] - " + message);
    }

    public void error(@Nonnull String message) {
        listener.getLogger().println("[EnvInject] - [ERROR] - " + message);
    }
}

