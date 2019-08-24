package com.lty.fsb.common.domain;

import java.util.HashMap;

public class FsbResponse extends HashMap<String, Object> {

    private static final long serialVersionUID = -8713837118340960775L;

    public FsbResponse message(String message) {
        this.put("message", message);
        return this;
    }

    public FsbResponse data(Object data) {
        this.put("data", data);
        return this;
    }

    @Override
    public FsbResponse put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
