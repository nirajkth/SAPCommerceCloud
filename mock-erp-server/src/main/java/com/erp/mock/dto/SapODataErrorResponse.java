package com.erp.mock.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class SapODataErrorResponse {

    @JsonProperty("error")
    private ODataErrorDetails error;

    public SapODataErrorResponse() {
    }

    public SapODataErrorResponse(String code, String message) {
        this.error = new ODataErrorDetails(code, message);
    }

    public ODataErrorDetails getError() {
        return error;
    }

    public void setError(ODataErrorDetails error) {
        this.error = error;
    }

    public static class ODataErrorDetails {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private ErrorMessage message;

        @JsonProperty("innererror")
        private Map<String, Object> innererror;

        public ODataErrorDetails() {
        }

        public ODataErrorDetails(String code, String message) {
            this.code = code;
            this.message = new ErrorMessage("en", message);
            this.innererror = Map.of(
                    "application", Map.of(
                            "component_id", "SD-SLS-API",
                            "service_namespace", "/SAP/",
                            "service_id", "API_SALES_ORDER_SRV"
                    )
            );
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public ErrorMessage getMessage() {
            return message;
        }

        public void setMessage(ErrorMessage message) {
            this.message = message;
        }

        public Map<String, Object> getInnererror() {
            return innererror;
        }

        public void setInnererror(Map<String, Object> innererror) {
            this.innererror = innererror;
        }
    }

    public static class ErrorMessage {
        @JsonProperty("lang")
        private String lang;

        @JsonProperty("value")
        private String value;

        public ErrorMessage() {
        }

        public ErrorMessage(String lang, String value) {
            this.lang = lang;
            this.value = value;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
