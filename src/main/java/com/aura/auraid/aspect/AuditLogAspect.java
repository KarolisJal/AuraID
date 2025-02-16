package com.aura.auraid.aspect;

import com.aura.auraid.security.SecurityUtils;
import com.aura.auraid.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditService auditService;

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String username = SecurityUtils.getCurrentUsername();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes())
                .getRequest();

        Object result = joinPoint.proceed();

        auditService.logEvent(
            methodName,
            username,
            className,
            getEntityId(joinPoint),
            username == null ? "Method executed by unauthenticated user" : "Method executed successfully",
            request
        );

        return result;
    }

    private String getEntityId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        
        for (int i = 0; i < parameterNames.length; i++) {
            if (parameterNames[i].equals("id") || parameterNames[i].equals("username")) {
                return args[i] != null ? args[i].toString() : "N/A";
            }
        }
        return "N/A";
    }
} 