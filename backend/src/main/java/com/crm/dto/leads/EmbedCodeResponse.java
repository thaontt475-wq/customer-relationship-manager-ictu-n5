package com.crm.dto.leads;

public class EmbedCodeResponse {

    private String formId;
    private String formUrl;
    private String iframeSnippet;
    private String scriptSnippet;
    private String htmlSnippet;
    private String previewUrl;

    public EmbedCodeResponse() {
    }

    public EmbedCodeResponse(String formId, String formUrl, String iframeSnippet, String scriptSnippet, String htmlSnippet, String previewUrl) {
        this.formId = formId;
        this.formUrl = formUrl;
        this.iframeSnippet = iframeSnippet;
        this.scriptSnippet = scriptSnippet;
        this.htmlSnippet = htmlSnippet;
        this.previewUrl = previewUrl;
    }

    public String getFormId() {
        return formId;
    }

    public void setFormId(String formId) {
        this.formId = formId;
    }

    public String getFormUrl() {
        return formUrl;
    }

    public void setFormUrl(String formUrl) {
        this.formUrl = formUrl;
    }

    public String getIframeSnippet() {
        return iframeSnippet;
    }

    public void setIframeSnippet(String iframeSnippet) {
        this.iframeSnippet = iframeSnippet;
    }

    public String getScriptSnippet() {
        return scriptSnippet;
    }

    public void setScriptSnippet(String scriptSnippet) {
        this.scriptSnippet = scriptSnippet;
    }

    public String getHtmlSnippet() {
        return htmlSnippet;
    }

    public void setHtmlSnippet(String htmlSnippet) {
        this.htmlSnippet = htmlSnippet;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }
}
