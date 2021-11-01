package org.jenkinsci.lib.envinject;


import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Exception type for the EnvInject logic.
 * 
 * @author Gregory Boissinot
 */
public class EnvInjectException extends Exception {

    public EnvInjectException(@NonNull String s) {
        super(s);
    }

    public EnvInjectException(@NonNull Throwable throwable) {
        super(throwable);
    }

    public EnvInjectException(@NonNull String s, @NonNull Throwable throwable) {
        super(s, throwable);
    }
}
