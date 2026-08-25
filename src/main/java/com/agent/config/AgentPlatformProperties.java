/**
 * 本文件定义 {@code AgentPlatformProperties}，负责应用属性与运行时 Bean 装配。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-platform")
public class AgentPlatformProperties {
    private final Retrieval retrieval = new Retrieval();
    private final Workflow workflow = new Workflow();
    private final Memory memory = new Memory();
    private final Llm llm = new Llm();

    public Retrieval getRetrieval() { return retrieval; }
    public Workflow getWorkflow() { return workflow; }
    public Memory getMemory() { return memory; }
    public Llm getLlm() { return llm; }

    public static class Retrieval {
        private boolean queryRewriteEnabled;
        private boolean rerankEnabled;
        private int candidateLimit = 20;
        private double minimumRrfScore = 0.016;
        private double minimumTokenOverlap = 0.15;
        public boolean isQueryRewriteEnabled() { return queryRewriteEnabled; }
        public void setQueryRewriteEnabled(boolean value) { queryRewriteEnabled = value; }
        public boolean isRerankEnabled() { return rerankEnabled; }
        public void setRerankEnabled(boolean value) { rerankEnabled = value; }
        public int getCandidateLimit() { return candidateLimit; }
        public void setCandidateLimit(int value) { candidateLimit = value; }
        public double getMinimumRrfScore() { return minimumRrfScore; }
        public void setMinimumRrfScore(double value) { minimumRrfScore = value; }
        public double getMinimumTokenOverlap() { return minimumTokenOverlap; }
        public void setMinimumTokenOverlap(double value) { minimumTokenOverlap = value; }
    }

    public static class Workflow {
        private int maxReflections = 3;
        public int getMaxReflections() { return maxReflections; }
        public void setMaxReflections(int value) { maxReflections = value; }
    }

    public static class Memory {
        private String type = "memory";
        private int maxMessages = 20;
        private int ttlSeconds = 3600;
        public String getType() { return type; }
        public void setType(String value) { type = value; }
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int value) { maxMessages = value; }
        public int getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(int value) { ttlSeconds = value; }
    }

    public static class Llm {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String modelName;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String value) { baseUrl = value; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String value) { apiKey = value; }
        public String getModelName() { return modelName; }
        public void setModelName(String value) { modelName = value; }
    }
}
