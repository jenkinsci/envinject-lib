package org.jenkinsci.lib.envinject;

/**
 * @author Gregory Boissinot
 */
public class EnvInjectException extends Exception {

    public EnvInjectException(String s) {
        super(s);
    }

    public EnvInjectException(Throwable throwable) {
        super(throwable);
    }

    public EnvInjectException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
