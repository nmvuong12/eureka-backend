package com.eureka.timetabling.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " với ID " + id + " không tồn tại");
    }
}
