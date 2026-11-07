package com.erp.mock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SapODataResponseWrapper<T> {

    @JsonProperty("d")
    private T d;

    public SapODataResponseWrapper() {
    }

    public SapODataResponseWrapper(T d) {
        this.d = d;
    }

    public T getD() {
        return d;
    }

    public void setD(T d) {
        this.d = d;
    }
}
