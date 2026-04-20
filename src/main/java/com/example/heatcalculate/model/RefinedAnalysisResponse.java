package com.example.heatcalculate.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 精细分析响应 DTO
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefinedAnalysisResponse {

    private String sessionId;
    private String status; // "need_input" or "complete"
    private String question;
    private CalorieResult partialResult;
    private CalorieResult result;

    public RefinedAnalysisResponse() {
    }

    public static RefinedAnalysisResponse needInput(String sessionId, String question, CalorieResult partialResult) {
        RefinedAnalysisResponse resp = new RefinedAnalysisResponse();
        resp.setSessionId(sessionId);
        resp.setStatus("need_input");
        resp.setQuestion(question);
        resp.setPartialResult(partialResult);
        return resp;
    }

    public static RefinedAnalysisResponse complete(String sessionId, CalorieResult result) {
        RefinedAnalysisResponse resp = new RefinedAnalysisResponse();
        resp.setSessionId(sessionId);
        resp.setStatus("complete");
        resp.setResult(result);
        return resp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public CalorieResult getPartialResult() {
        return partialResult;
    }

    public void setPartialResult(CalorieResult partialResult) {
        this.partialResult = partialResult;
    }

    public CalorieResult getResult() {
        return result;
    }

    public void setResult(CalorieResult result) {
        this.result = result;
    }
}
