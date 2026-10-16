package com.gymcore.exception;

public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;
    public BusinessException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
    public int getStatus() { return status; }
    public String getCode() { return code; }
    public static BusinessException missing() { return new BusinessException(404, "NOT_FOUND", "Recurso não encontrado no contexto da academia."); }
    public static BusinessException conflict(String code, String message) { return new BusinessException(409, code, message); }
    public static BusinessException rule(String code, String message) { return new BusinessException(422, code, message); }
    public static BusinessException invalid(String code, String message) { return new BusinessException(400, code, message); }
}
