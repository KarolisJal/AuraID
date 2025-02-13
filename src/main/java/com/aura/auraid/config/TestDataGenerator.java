package com.aura.auraid.config;

import com.aura.auraid.service.AuditService;
import com.aura.auraid.metrics.CustomMetrics;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class TestDataGenerator implements CommandLineRunner {

    private final AuditService auditService;
    private final CustomMetrics customMetrics;
    private final Random random = new Random();

    // Custom request wrapper for test data
    private static class TestHttpRequest implements HttpServletRequest {
        private final String ipAddress;
        private final String userAgent;
        private final Map<String, String> headers = new HashMap<>();

        public TestHttpRequest(String ipAddress, String userAgent) {
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
            headers.put("User-Agent", userAgent);
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public String getRemoteAddr() {
            return ipAddress;
        }

        @Override
        public jakarta.servlet.http.Cookie[] getCookies() {
            return new jakarta.servlet.http.Cookie[0];
        }

        @Override
        public int getIntHeader(String name) {
            String value = getHeader(name);
            return value != null ? Integer.parseInt(value) : -1;
        }

        @Override
        public long getDateHeader(String name) {
            String value = getHeader(name);
            if (value == null) {
                return -1L;
            }
            try {
                return java.time.ZonedDateTime.parse(value)
                    .toInstant()
                    .toEpochMilli();
            } catch (Exception e) {
                return -1L;
            }
        }

        @Override
        public jakarta.servlet.ServletInputStream getInputStream() {
            return new jakarta.servlet.ServletInputStream() {
                @Override
                public boolean isFinished() { return true; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(jakarta.servlet.ReadListener readListener) { }
                @Override
                public int read() { return -1; }
            };
        }

        // Implement other required methods with default implementations
        // Only implementing methods we actually use, others return null or default values
        @Override 
        public Enumeration<String> getHeaderNames() { 
            return Collections.enumeration(headers.keySet()); 
        }
        
        @Override 
        public Enumeration<String> getHeaders(String name) { 
            return Collections.enumeration(Collections.singletonList(headers.get(name))); 
        }
        // ... other required methods with default implementations
        @Override public Object getAttribute(String name) { 
            return null; 
        }
        
        @Override 
        public Enumeration<String> getAttributeNames() { 
            return null; 
        }
        
        @Override 
        public String getCharacterEncoding() { 
            return null; 
        }
        
        @Override 
        public void setCharacterEncoding(String env) { 
        }
        
        @Override 
        public int getContentLength() { 
            return 0; 
        }
        
        @Override 
        public long getContentLengthLong() { 
            return 0; 
        }
        
        @Override 
        public String getContentType() { 
            return null; 
        }
        
        @Override
        public String getParameter(String name) { 
            return null; 
        }
        
        @Override
        public Enumeration<String> getParameterNames() { 
            return null; 
        }
        
        @Override
        public String[] getParameterValues(String name) { 
            return null; 
        }
        
        @Override
        public Map<String, String[]> getParameterMap() { 
            return null; 
        }
        
        @Override
        public String getProtocol() { 
            return null; 
        }
        
        @Override
        public String getScheme() { 
            return null; 
        }
        
        @Override
        public String getServerName() { 
            return null; 
        }
        
        @Override
        public int getServerPort() { 
            return 0; 
        }
        
        @Override
        public java.io.BufferedReader getReader() { 
            return null; 
        }
        
        @Override
        public String getRemoteHost() { 
            return null; 
        }
        
        @Override
        public void setAttribute(String name, Object o) { 
        }
        
        @Override
        public void removeAttribute(String name) { 
        }
        
        @Override
        public java.util.Locale getLocale() { 
            return null; 
        }
        
        @Override
        public Enumeration<java.util.Locale> getLocales() { 
            return null; 
        }
        
        @Override
        public boolean isSecure() { 
            return false; 
        }
        
        @Override
        public jakarta.servlet.RequestDispatcher getRequestDispatcher(String path) { 
            return null; 
        }

        
        @Override
        public int getRemotePort() { 
            return 0; 
        }
        
        @Override
        public String getLocalName() { 
            return null; 
        }
        
        @Override
        public String getLocalAddr() { 
            return null; 
        }
        
        @Override
        public int getLocalPort() { 
            return 0; 
        }
        
        @Override
        public jakarta.servlet.ServletContext getServletContext() { 
            return null; 
        }
        
        @Override
        public jakarta.servlet.AsyncContext startAsync() throws java.lang.IllegalStateException { 
            return null; 
        }
        
        @Override
        public jakarta.servlet.AsyncContext startAsync(jakarta.servlet.ServletRequest servletRequest, 
                                                     jakarta.servlet.ServletResponse servletResponse) 
                throws java.lang.IllegalStateException { 
            return null; 
        }
        
        @Override
        public boolean isAsyncStarted() { 
            return false; 
        }
        
        @Override
        public boolean isAsyncSupported() { 
            return false; 
        }
        
        @Override
        public jakarta.servlet.AsyncContext getAsyncContext() { 
            return null; 
        }
        
        @Override
        public jakarta.servlet.DispatcherType getDispatcherType() { 
            return null; 
        }
        
        @Override
        public String getRequestId() { 
            return null; 
        }
        
        @Override
        public String getProtocolRequestId() { 
            return null; 
        }
        
        @Override
        public jakarta.servlet.ServletConnection getServletConnection() { 
            return null; 
        }
        
        @Override public String getAuthType() { return null; }
        @Override public String getMethod() { return null; }
        @Override public String getPathInfo() { return null; }
        @Override public String getPathTranslated() { return null; }
        @Override public String getContextPath() { return null; }
        @Override public String getQueryString() { return null; }
        @Override public String getRemoteUser() { return null; }
        @Override public boolean isUserInRole(String role) { return false; }
        @Override public java.security.Principal getUserPrincipal() { return null; }
        @Override public String getRequestedSessionId() { return null; }
        @Override public String getRequestURI() { return null; }
        @Override public StringBuffer getRequestURL() { return null; }
        @Override public String getServletPath() { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession(boolean create) { return null; }
        @Override public jakarta.servlet.http.HttpSession getSession() { return null; }
        @Override public String changeSessionId() { return null; }
        @Override public boolean isRequestedSessionIdValid() { return false; }
        @Override public boolean isRequestedSessionIdFromCookie() { return false; }
        @Override public boolean isRequestedSessionIdFromURL() { return false; }
        @Override public boolean authenticate(jakarta.servlet.http.HttpServletResponse response) { return false; }
        @Override public void login(String username, String password) { }
        @Override public void logout() { }
        @Override public java.util.Collection<jakarta.servlet.http.Part> getParts() { return null; }
        @Override public jakarta.servlet.http.Part getPart(String name) { return null; }
        @Override public <T extends jakarta.servlet.http.HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }
        @Override public java.util.Map<String, String> getTrailerFields() { return null; }
        @Override public boolean isTrailerFieldsReady() { return false; }
        @Override public jakarta.servlet.http.PushBuilder newPushBuilder() { return null; }
    }

    private static final List<String> SAMPLE_USERNAMES = Arrays.asList(
        "john.doe", "jane.smith", "admin.user", "test.account",
        "dev.team", "support.staff", "guest.user", "system.admin"
    );

    private static final List<String> SAMPLE_IP_ADDRESSES = Arrays.asList(
        "192.168.1.100", "10.0.0.50", "172.16.0.25", "192.168.0.10",
        "10.10.10.10", "172.20.0.100", "192.168.2.200", "10.0.1.15"
    );

    private static final List<String> SAMPLE_USER_AGENTS = Arrays.asList(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 14_7_1 like Mac OS X) AppleWebKit/605.1.15"
    );

    private static final List<String> ACTIONS = Arrays.asList(
        "VIEW_DASHBOARD", "UPDATE_PROFILE", "CHANGE_SETTINGS",
        "VIEW_REPORTS", "EXPORT_DATA", "UPDATE_PREFERENCES"
    );

    private static final Map<Integer, Double> HOURLY_DISTRIBUTION = new HashMap<>() {{
        // Morning peak (9-11 AM)
        put(9, 0.12);
        put(10, 0.15);
        put(11, 0.12);
        // Afternoon peak (2-4 PM)
        put(14, 0.10);
        put(15, 0.12);
        put(16, 0.10);
        // Evening activity (7-9 PM)
        put(19, 0.08);
        put(20, 0.07);
        put(21, 0.06);
        // Low activity hours
        put(0, 0.01);
        put(1, 0.01);
        put(2, 0.01);
        put(3, 0.01);
        put(4, 0.01);
        put(5, 0.02);
        put(6, 0.03);
        put(7, 0.05);
        put(8, 0.08);
        put(12, 0.10);
        put(13, 0.09);
        put(17, 0.08);
        put(18, 0.07);
        put(22, 0.04);
        put(23, 0.02);
    }};

    @Override
    public void run(String... args) {
        System.out.println("Starting test data generation...");
        generateTestData();
        System.out.println("Test data generation completed.");
    }

    private void generateTestData() {
        LocalDateTime startTime = LocalDateTime.now().minusDays(30);
        LocalDateTime endTime = LocalDateTime.now();

        generateUserSessions(startTime, endTime);
        generateRegularActivities(startTime, endTime);
        generateSecurityEvents(startTime, endTime);
        generatePerformanceData(startTime, endTime);
    }

    private void generateUserSessions(LocalDateTime startTime, LocalDateTime endTime) {
        SAMPLE_USERNAMES.forEach(username -> {
            // Generate 3-5 sessions per day per user
            int daysCount = (int) ChronoUnit.DAYS.between(startTime, endTime);
            
            for (int day = 0; day < daysCount; day++) {
                LocalDateTime dayStart = startTime.plusDays(day);
                int sessionsToday = 3 + random.nextInt(3);
                
                for (int session = 0; session < sessionsToday; session++) {
                    // Generate session with realistic duration and activity pattern
                    generateSession(username, dayStart);
                }
            }
        });
    }

    private void generateSession(String username, LocalDateTime dayStart) {
        // Pick a realistic hour based on distribution
        int hour = pickHourBasedOnDistribution();
        LocalDateTime sessionStart = dayStart.withHour(hour)
            .withMinute(random.nextInt(60))
            .withSecond(random.nextInt(60));
        
        // Session duration: 15-120 minutes
        int sessionDuration = 15 + random.nextInt(106);
        
        // Generate 5-15 actions per session
        int actionsCount = 5 + random.nextInt(11);
        
        String ipAddress = randomFrom(SAMPLE_IP_ADDRESSES);
        String userAgent = randomFrom(SAMPLE_USER_AGENTS);
        
        for (int i = 0; i < actionsCount; i++) {
            LocalDateTime actionTime = sessionStart.plusMinutes(
                (long) (i * (sessionDuration / (double) actionsCount)));
            
            String action = generateRealisticActionSequence(i, actionsCount);
            
            logUserAction(username, action, actionTime, ipAddress, userAgent);
        }
    }

    private void generateRegularActivities(LocalDateTime startTime, LocalDateTime endTime) {
        // Generate additional random activities outside of regular sessions
        int additionalActivities = 200 + random.nextInt(300);
        
        for (int i = 0; i < additionalActivities; i++) {
            String username = randomFrom(SAMPLE_USERNAMES);
            String action = randomFrom(ACTIONS);
            LocalDateTime timestamp = randomTimeBetween(startTime, endTime);
            String ipAddress = randomFrom(SAMPLE_IP_ADDRESSES);
            String userAgent = randomFrom(SAMPLE_USER_AGENTS);
            
            logUserAction(username, action, timestamp, ipAddress, userAgent);
        }
    }

    private void generateSecurityEvents(LocalDateTime startTime, LocalDateTime endTime) {
        // Generate suspicious IP patterns
        List<String> suspiciousIPs = Arrays.asList(
            "192.168.1.200", "10.0.0.100", "172.16.0.50"
        );
        
        suspiciousIPs.forEach(ip -> {
            // Generate burst of failed attempts
            LocalDateTime burstTime = randomTimeBetween(
                endTime.minusHours(4), 
                endTime.minusHours(1)
            );
            
            for (int i = 0; i < 15 + random.nextInt(10); i++) {
                LocalDateTime attemptTime = burstTime.plusMinutes(random.nextInt(30));
                String username = randomFrom(SAMPLE_USERNAMES);
                
                logFailedLoginAttempt(username, ip, attemptTime);
            }
        });

        // Generate random failed attempts throughout the period
        for (int i = 0; i < 50; i++) {
            LocalDateTime timestamp = randomTimeBetween(startTime, endTime);
            String username = randomFrom(SAMPLE_USERNAMES);
            String ipAddress = randomFrom(SAMPLE_IP_ADDRESSES);
            
            logFailedLoginAttempt(username, ipAddress, timestamp);
        }
    }

    private void generatePerformanceData(LocalDateTime startTime, LocalDateTime endTime) {
        // Simulate periodic performance variations
        long periodInMinutes = ChronoUnit.MINUTES.between(startTime, endTime);
        
        for (long minute = 0; minute < periodInMinutes; minute += 5) {
            LocalDateTime timestamp = startTime.plusMinutes(minute);
            
            // Simulate higher load during peak hours
            int hour = timestamp.getHour();
            double loadFactor = HOURLY_DISTRIBUTION.getOrDefault(hour, 0.05);
            
            // Add some random variation
            loadFactor *= (0.8 + random.nextDouble() * 0.4); // ±20% variation
            
            // Log performance metrics
            logPerformanceMetrics(timestamp, loadFactor);
        }
    }

    private void logUserAction(String username, String action, LocalDateTime timestamp,
                             String ipAddress, String userAgent) {
        HttpServletRequest request = new TestHttpRequest(ipAddress, userAgent);

        auditService.logEvent(
            action,
            username,
            "USER",
            UUID.randomUUID().toString(),
            "Action completed successfully",
            request
        );

        if (action.equals("LOGIN")) {
            customMetrics.getLoginAttempts().increment();
            customMetrics.getLoginSuccess().increment();
        }
    }

    private void logFailedLoginAttempt(String username, String ipAddress, 
                                     LocalDateTime timestamp) {
        HttpServletRequest request = new TestHttpRequest(ipAddress, randomFrom(SAMPLE_USER_AGENTS));

        auditService.logEvent(
            "LOGIN",
            username,
            "USER",
            UUID.randomUUID().toString(),
            "Login failed: Invalid credentials",
            request
        );

        customMetrics.getLoginAttempts().increment();
        customMetrics.getLoginFailure().increment();
    }

    private void logPerformanceMetrics(LocalDateTime timestamp, double loadFactor) {
        double baseResponseTime = 100; // ms
        double actualResponseTime = baseResponseTime * (1 + random.nextDouble() * loadFactor);
        
        HttpServletRequest request = new TestHttpRequest(
            randomFrom(SAMPLE_IP_ADDRESSES),
            randomFrom(SAMPLE_USER_AGENTS)
        );
        
        auditService.logEvent(
            "PERFORMANCE",
            "system",
            "METRIC",
            UUID.randomUUID().toString(),
            String.format("Response time: %.2fms", actualResponseTime),
            request
        );
    }

    private String generateRealisticActionSequence(int actionIndex, int totalActions) {
        if (actionIndex == 0) return "LOGIN";
        if (actionIndex == totalActions - 1) return "LOGOUT";
        
        return randomFrom(ACTIONS);
    }

    private int pickHourBasedOnDistribution() {
        double rand = random.nextDouble();
        double cumulative = 0.0;
        
        for (Map.Entry<Integer, Double> entry : HOURLY_DISTRIBUTION.entrySet()) {
            cumulative += entry.getValue();
            if (rand <= cumulative) {
                return entry.getKey();
            }
        }
        
        return 12; // Default to noon if something goes wrong
    }

    private LocalDateTime randomTimeBetween(LocalDateTime start, LocalDateTime end) {
        long startEpochSecond = start.toEpochSecond(java.time.ZoneOffset.UTC);
        long endEpochSecond = end.toEpochSecond(java.time.ZoneOffset.UTC);
        long randomEpochSecond = startEpochSecond + 
            (long) (random.nextDouble() * (endEpochSecond - startEpochSecond));
        return LocalDateTime.ofEpochSecond(randomEpochSecond, 0, java.time.ZoneOffset.UTC);
    }

    private <T> T randomFrom(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
} 