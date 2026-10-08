package com.recomind.exception;

public class RecommendationException extends RuntimeException {
    public RecommendationException() { super(); }
    public RecommendationException(String message) { super(message); }
    public RecommendationException(String message, Throwable cause) { super(message, cause); }
    public RecommendationException(Throwable cause) { super(cause); }
}
