package com.apitest.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.JSONArray;
import io.qameta.allure.Allure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Allure报告工具类
 */
public class AllureReportUtil {
    public static void logStep(String message) {
        Allure.step(message);
    }

    public static void attach(String name, String content) {
        Allure.addAttachment(name, content);
    }

    /**
     * 汇总 allure-results 目录下的测试结果，生成统计信息
     * @param allureResultsDir allure结果目录，默认 "allure-results"
     */
    public static AllureSummary summarize(String allureResultsDir) {
        Path dir = Paths.get(allureResultsDir == null || allureResultsDir.trim().isEmpty() ? "allure-results" : allureResultsDir);
        AllureSummary summary = new AllureSummary();
        if (!Files.exists(dir)) {
            summary.note = "allure-results 目录不存在: " + dir.toAbsolutePath();
            return summary;
        }

        List<JSONObject> results = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith("-result.json"))
                    .forEach(p -> {
                        try {
String json = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                            JSONObject obj = JSON.parseObject(json);
                            if (obj != null) results.add(obj);
                        } catch (IOException ignored) { }
                    });
        } catch (IOException e) {
            summary.note = "读取allure-results失败: " + e.getMessage();
            return summary;
        }

        // 统计
        int passed = 0, failed = 0, broken = 0, skipped = 0, unknown = 0;
        long minStart = Long.MAX_VALUE, maxStop = Long.MIN_VALUE;
        long totalDuration = 0L, maxDuration = 0L;
        List<FailureDetail> failures = new ArrayList<>();

        for (JSONObject r : results) {
            String status = optString(r, "status");
            if ("passed".equalsIgnoreCase(status)) passed++;
            else if ("failed".equalsIgnoreCase(status)) failed++;
            else if ("broken".equalsIgnoreCase(status)) broken++;
            else if ("skipped".equalsIgnoreCase(status)) skipped++;
            else unknown++;

            long start = optLong(r, "start");
            long stop = optLong(r, "stop");
            // 兼容包含 time 对象的模型
            if (start == 0 && stop == 0 && r.containsKey("time")) {
                JSONObject time = r.getJSONObject("time");
                start = optLong(time, "start");
                stop = optLong(time, "stop");
            }

            long duration = 0L;
            if (start > 0 && stop >= start) {
                duration = stop - start;
            } else {
                // 回退：部分结果没有 start/stop，尝试 steps 累计
                duration = sumStepsDuration(r.getJSONArray("steps"));
            }

            if (start > 0) minStart = Math.min(minStart, start);
            if (stop > 0) maxStop = Math.max(maxStop, stop);
            totalDuration += duration;
            maxDuration = Math.max(maxDuration, duration);

            if ("failed".equalsIgnoreCase(status) || "broken".equalsIgnoreCase(status)) {
                FailureDetail d = new FailureDetail();
                d.name = optString(r, "name");
                d.fullName = optString(r, "fullName");
                d.status = status;
                d.durationMs = duration;
                JSONObject sd = r.getJSONObject("statusDetails");
                if (sd != null) {
                    d.message = optString(sd, "message");
                    d.trace = optString(sd, "trace");
                }
                d.labels = extractLabels(r.getJSONArray("labels"));
                failures.add(d);
            }
        }

        int total = results.size();
        int executed = total - skipped; // 通过率默认不包含跳过
        double passRate = executed > 0 ? (passed * 100.0 / executed) : 0.0;
        double avgDuration = total > 0 ? (totalDuration * 1.0 / total) : 0.0;

        summary.total = total;
        summary.executed = executed;
        summary.passed = passed;
        summary.failed = failed;
        summary.broken = broken;
        summary.skipped = skipped;
        summary.unknown = unknown;
        summary.passRate = round2(passRate);
        summary.totalDurationMs = totalDuration;
        summary.avgDurationMs = Math.round(avgDuration);
        summary.maxDurationMs = maxDuration;
        summary.startEpochMs = (minStart == Long.MAX_VALUE ? 0L : minStart);
        summary.stopEpochMs = (maxStop == Long.MIN_VALUE ? 0L : maxStop);
        summary.failures = failures.stream()
                .sorted(Comparator.comparingLong((FailureDetail f) -> f.durationMs).reversed())
                .collect(Collectors.toList());
        summary.generatedAt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        return summary;
    }

    /** 将汇总以文本形式附加到 Allure 报告 */
    public static void attachSummary(AllureSummary summary) {
        attach("测试汇总", toPrettyText(summary));
    }

    /** 将失败详情以文本形式附加到 Allure 报告 */
    public static void attachFailures(AllureSummary summary, int topN) {
        if (summary == null || summary.failures == null || summary.failures.isEmpty()) {
            attach("失败详情", "无失败用例");
            return;
        }
        List<FailureDetail> list = summary.failures.stream().limit(Math.max(topN, 0)).collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            FailureDetail f = list.get(i);
            sb.append("[").append(i + 1).append("] ").append(nullToNA(f.name)).append(" (status=").append(f.status).append(")\n");
            sb.append("  duration: ").append(f.durationMs).append(" ms\n");
            if (f.labels != null && !f.labels.isEmpty()) {
                sb.append("  labels: ").append(f.labels).append("\n");
            }
            if (f.message != null && !f.message.isEmpty()) {
                sb.append("  message: ").append(f.message).append("\n");
            }
            if (f.trace != null && !f.trace.isEmpty()) {
                sb.append("  trace: \n").append(f.trace).append("\n");
            }
            sb.append('\n');
        }
        attach("失败详情(Top" + list.size() + ")", sb.toString());
    }

    /** 控制台打印汇总 */
    public static void printSummary(AllureSummary summary) {
        System.out.println(toPrettyText(summary));
    }

    /** 导出汇总为 JSON 字符串 */
    public static String toJson(AllureSummary summary) {
        return JSON.toJSONString(summary, JSONWriter.Feature.PrettyFormat);
    }

    /** 汇总转为易读文本 */
    public static String toPrettyText(AllureSummary s) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Allure 测试汇总 ===\n");
        sb.append("生成时间: ").append(nullToNA(s.generatedAt)).append("\n");
        sb.append("总用例: ").append(s.total).append(" (执行: ").append(s.executed).append(")\n");
        sb.append("通过: ").append(s.passed).append(", 失败: ").append(s.failed).append(", 异常: ").append(s.broken)
                .append(", 跳过: ").append(s.skipped).append(", 未知: ").append(s.unknown).append("\n");
        sb.append("通过率: ").append(s.passRate).append("%\n");
        sb.append("耗时: total=").append(s.totalDurationMs).append(" ms, avg=").append(s.avgDurationMs)
                .append(" ms, max=").append(s.maxDurationMs).append(" ms\n");
        if (s.startEpochMs > 0 && s.stopEpochMs > 0) {
            Duration d = Duration.ofMillis(Math.max(0, s.stopEpochMs - s.startEpochMs));
            sb.append("执行窗口: ")
                    .append(formatEpoch(s.startEpochMs)).append(" ~ ")
                    .append(formatEpoch(s.stopEpochMs)).append(" (")
                    .append(d.getSeconds()).append("s)\n");
        }
        if (s.note != null && !s.note.isEmpty()) sb.append("备注: ").append(s.note).append("\n");
        return sb.toString();
    }

    // ===== 工具与模型 =====
    private static String nullToNA(String s) { return s == null || s.trim().isEmpty() ? "N/A" : s; }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
    private static String optString(JSONObject o, String k) { return o == null ? null : Objects.toString(o.get(k), null); }
    private static long optLong(JSONObject o, String k) { return o == null ? 0L : Optional.ofNullable(o.getLong(k)).orElse(0L); }
    private static String formatEpoch(long epochMs) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(epochMs));
    }
    private static long sumStepsDuration(JSONArray steps) {
        if (steps == null || steps.isEmpty()) return 0L;
        long sum = 0L;
        for (int i = 0; i < steps.size(); i++) {
            JSONObject st = steps.getJSONObject(i);
            long start = optLong(st, "start");
            long stop = optLong(st, "stop");
            if (start > 0 && stop >= start) sum += (stop - start);
            if (st.containsKey("steps")) sum += sumStepsDuration(st.getJSONArray("steps"));
        }
        return sum;
    }
    private static Map<String, String> extractLabels(JSONArray labels) {
        if (labels == null || labels.isEmpty()) return null;
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            JSONObject l = labels.getJSONObject(i);
            String name = optString(l, "name");
            String value = optString(l, "value");
            if (name != null && value != null) map.put(name, value);
        }
        return map.isEmpty() ? null : map;
    }

    public static class AllureSummary {
        public int total;
        public int executed;
        public int passed;
        public int failed;
        public int broken;
        public int skipped;
        public int unknown;
        public double passRate;
        public long totalDurationMs;
        public long avgDurationMs;
        public long maxDurationMs;
        public long startEpochMs;
        public long stopEpochMs;
        public String generatedAt;
        public String note;
        public List<FailureDetail> failures;
    }

    public static class FailureDetail {
        public String name;
        public String fullName;
        public String status;
        public String message;
        public String trace;
        public long durationMs;
        public Map<String, String> labels;

        @Override public String toString() {
            return "FailureDetail{" +
                    "name='" + name + '\'' +
                    ", status='" + status + '\'' +
                    ", durationMs=" + durationMs +
                    '}';
        }
    }
}