package com.aura.auraid.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.management.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SystemMetricsService {
    private static final Logger log = LoggerFactory.getLogger(SystemMetricsService.class);
    private final MeterRegistry meterRegistry;
    
    // Store last 24 hours of metrics with 1-minute resolution
    private static final int RETENTION_MINUTES = 24 * 60;
    private final Map<LocalDateTime, Map<String, Double>> historicalMetrics = new ConcurrentHashMap<>();
    private final Queue<LocalDateTime> timeQueue = new LinkedList<>();
    
    // Store high-resolution metrics for the last hour (1-second resolution)
    private static final int HIGH_RES_RETENTION_SECONDS = 3600;
    private final Map<LocalDateTime, Map<String, Double>> highResMetrics = new ConcurrentHashMap<>();
    private final Queue<LocalDateTime> highResTimeQueue = new LinkedList<>();
    
    // Sliding window statistics for response times
    private final ConcurrentLinkedQueue<Double> responseTimesWindow = new ConcurrentLinkedQueue<>();
    private static final int RESPONSE_TIME_WINDOW_SIZE = 1000; // Keep last 1000 response times
    
    // Timers for different types of operations
    private final Map<String, Timer> operationTimers;
    
    @Autowired
    public SystemMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize timers for different operation types
        this.operationTimers = new ConcurrentHashMap<>();
        operationTimers.put("http_request", Timer.builder("http.request.duration")
            .description("HTTP request duration")
            .register(meterRegistry));
        operationTimers.put("db_query", Timer.builder("db.query.duration")
            .description("Database query duration")
            .register(meterRegistry));
        operationTimers.put("auth", Timer.builder("auth.operation.duration")
            .description("Authentication operation duration")
            .register(meterRegistry));
    }

    @Scheduled(fixedRate = 60000) // Collect metrics every minute
    public void collectMetrics() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Double> currentMetrics = new HashMap<>();

        try {
            // Collect JVM metrics
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

            // Memory metrics with more detail
            MemoryUsage heapMemory = memoryMXBean.getHeapMemoryUsage();
            MemoryUsage nonHeapMemory = memoryMXBean.getNonHeapMemoryUsage();
            
            currentMetrics.put("heapUsed", (double) heapMemory.getUsed());
            currentMetrics.put("heapCommitted", (double) heapMemory.getCommitted());
            currentMetrics.put("heapMax", (double) heapMemory.getMax());
            currentMetrics.put("heapUtilization", (double) heapMemory.getUsed() / heapMemory.getMax() * 100);
            currentMetrics.put("nonHeapUsed", (double) nonHeapMemory.getUsed());
            currentMetrics.put("nonHeapCommitted", (double) nonHeapMemory.getCommitted());

            // Thread metrics with more detail
            currentMetrics.put("threadCount", (double) threadMXBean.getThreadCount());
            currentMetrics.put("peakThreadCount", (double) threadMXBean.getPeakThreadCount());
            currentMetrics.put("daemonThreadCount", (double) threadMXBean.getDaemonThreadCount());
            currentMetrics.put("totalStartedThreadCount", (double) threadMXBean.getTotalStartedThreadCount());
            currentMetrics.put("deadlockedThreads", (double) (threadMXBean.findDeadlockedThreads() != null ? 
                threadMXBean.findDeadlockedThreads().length : 0));

            // System metrics with more detail
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                    (com.sun.management.OperatingSystemMXBean) osMXBean;
                currentMetrics.put("systemCpuLoad", sunOsMXBean.getCpuLoad() * 100);
                currentMetrics.put("processCpuLoad", sunOsMXBean.getProcessCpuLoad() * 100);
                currentMetrics.put("freePhysicalMemory", (double) sunOsMXBean.getFreeMemorySize());
                currentMetrics.put("totalPhysicalMemory", (double) sunOsMXBean.getTotalMemorySize());
                currentMetrics.put("committedVirtualMemory", (double) sunOsMXBean.getCommittedVirtualMemorySize());
                currentMetrics.put("processCpuTime", (double) sunOsMXBean.getProcessCpuTime());
            }

            // Garbage collection metrics
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                String name = gcBean.getName().replace(" ", "_").toLowerCase();
                currentMetrics.put("gc_" + name + "_count", (double) gcBean.getCollectionCount());
                currentMetrics.put("gc_" + name + "_time", (double) gcBean.getCollectionTime());
            }

            // Response time statistics
            DoubleSummaryStatistics responseTimeStats = responseTimesWindow.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
            
            currentMetrics.put("responseTime_avg", responseTimeStats.getAverage());
            currentMetrics.put("responseTime_max", responseTimeStats.getMax());
            currentMetrics.put("responseTime_min", responseTimeStats.getMin());

            // Operation timers statistics
            operationTimers.forEach((operation, timer) -> {
                currentMetrics.put(operation + "_mean", timer.mean(TimeUnit.MILLISECONDS));
                currentMetrics.put(operation + "_max", timer.max(TimeUnit.MILLISECONDS));
                currentMetrics.put(operation + "_count", (double) timer.count());
            });

            // Store metrics
            historicalMetrics.put(now, currentMetrics);
            timeQueue.offer(now);

            // Cleanup old metrics
            while (timeQueue.size() > RETENTION_MINUTES) {
                LocalDateTime oldestTime = timeQueue.poll();
                historicalMetrics.remove(oldestTime);
            }

            // Update Micrometer metrics
            updateMicrometerMetrics(currentMetrics);

            log.debug("Metrics collected successfully at {}", now);
        } catch (Exception e) {
            log.error("Error collecting metrics: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 1000) // Collect high-resolution metrics every second
    public void collectHighResolutionMetrics() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Double> currentMetrics = new HashMap<>();

        try {
            OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
            if (osMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsMXBean = 
                    (com.sun.management.OperatingSystemMXBean) osMXBean;
                currentMetrics.put("systemCpuLoad", sunOsMXBean.getCpuLoad() * 100);
                currentMetrics.put("processCpuLoad", sunOsMXBean.getProcessCpuLoad() * 100);
            }

            // Store high-resolution metrics
            highResMetrics.put(now, currentMetrics);
            highResTimeQueue.offer(now);

            // Cleanup old high-resolution metrics
            while (highResTimeQueue.size() > HIGH_RES_RETENTION_SECONDS) {
                LocalDateTime oldestTime = highResTimeQueue.poll();
                highResMetrics.remove(oldestTime);
            }
        } catch (Exception e) {
            log.error("Error collecting high-resolution metrics: {}", e.getMessage(), e);
        }
    }

    public void recordResponseTime(double responseTimeMs) {
        responseTimesWindow.offer(responseTimeMs);
        while (responseTimesWindow.size() > RESPONSE_TIME_WINDOW_SIZE) {
            responseTimesWindow.poll();
        }
    }

    public void recordOperationTime(String operation, long durationMs) {
        Timer timer = operationTimers.get(operation);
        if (timer != null) {
            timer.record(Duration.ofMillis(durationMs));
        }
    }

    private void updateMicrometerMetrics(Map<String, Double> metrics) {
        metrics.forEach((key, value) -> {
            if (!Double.isInfinite(value) && !Double.isNaN(value)) {
                meterRegistry.gauge("system." + key, value);
            }
        });
    }

    public Map<String, Object> getLatestMetrics() {
        if (historicalMetrics.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime latest = timeQueue.peek();
        Map<String, Double> latestMetrics = historicalMetrics.get(latest);
        
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", latest);
        result.put("metrics", latestMetrics);
        return result;
    }

    public Map<String, Object> getHighResolutionMetrics(int seconds) {
        int lookback = Math.min(seconds, HIGH_RES_RETENTION_SECONDS);
        List<Map<String, Object>> history = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusSeconds(lookback);
        
        highResMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().isAfter(cutoff))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", entry.getKey());
                point.put("metrics", entry.getValue());
                history.add(point);
            });

        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("resolution", "1 second");
        result.put("dataPoints", history.size());
        return result;
    }

    public Map<String, Object> getMetricsHistory(int minutes) {
        int lookback = Math.min(minutes, RETENTION_MINUTES);
        List<Map<String, Object>> history = new ArrayList<>();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(lookback);
        
        historicalMetrics.entrySet().stream()
            .filter(entry -> entry.getKey().isAfter(cutoff))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", entry.getKey());
                point.put("metrics", entry.getValue());
                history.add(point);
            });

        Map<String, Object> result = new HashMap<>();
        result.put("history", history);
        result.put("resolution", "1 minute");
        result.put("dataPoints", history.size());
        return result;
    }

    public Map<String, Object> getMetricsSummary() {
        if (historicalMetrics.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<Double>> metricValues = new HashMap<>();
        
        // Collect all values for each metric
        historicalMetrics.values().forEach(metrics -> {
            metrics.forEach((key, value) -> {
                metricValues.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            });
        });

        // Calculate detailed statistics for each metric
        Map<String, Object> summary = new HashMap<>();
        metricValues.forEach((metric, values) -> {
            DoubleSummaryStatistics stats = values.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
            
            // Calculate percentiles
            double[] sortedValues = values.stream()
                .mapToDouble(Double::doubleValue)
                .sorted()
                .toArray();
            
            Map<String, Object> metricStats = new HashMap<>();
            metricStats.put("min", stats.getMin());
            metricStats.put("max", stats.getMax());
            metricStats.put("avg", stats.getAverage());
            metricStats.put("current", values.get(values.size() - 1));
            metricStats.put("count", stats.getCount());
            metricStats.put("stdDev", calculateStdDev(values, stats.getAverage()));
            metricStats.put("p50", calculatePercentile(sortedValues, 50));
            metricStats.put("p75", calculatePercentile(sortedValues, 75));
            metricStats.put("p90", calculatePercentile(sortedValues, 90));
            metricStats.put("p95", calculatePercentile(sortedValues, 95));
            metricStats.put("p99", calculatePercentile(sortedValues, 99));
            
            summary.put(metric, metricStats);
        });

        return summary;
    }

    private double calculateStdDev(List<Double> values, double mean) {
        return Math.sqrt(values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0));
    }

    private double calculatePercentile(double[] sortedValues, double percentile) {
        if (sortedValues.length == 0) return 0.0;
        
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.length) - 1;
        return sortedValues[Math.max(0, Math.min(sortedValues.length - 1, index))];
    }
} 