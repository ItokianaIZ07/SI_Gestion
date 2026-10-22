package com.si.rh.utils;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

public class ResponseUtil {
    public static <T> ResponseEntity<T> sendResponse(HttpStatusCode code, T body){
        return ResponseEntity.status(code).body(body);
    }
}
