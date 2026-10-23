package com.si.rh.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

public class SecurityUtil {
    public static String getUserEmail(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String email = auth.getName();
        System.out.println(email);

        return email;
    }
}
