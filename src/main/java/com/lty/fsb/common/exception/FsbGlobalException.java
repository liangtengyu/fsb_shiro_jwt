package com.lty.fsb.common.exception;

/**
 *  系统内部异常
 */
public class FsbGlobalException extends Exception {

    private static final long serialVersionUID = -994962710559017255L;

    public FsbGlobalException(String message) {
        super(message);
    }
}
