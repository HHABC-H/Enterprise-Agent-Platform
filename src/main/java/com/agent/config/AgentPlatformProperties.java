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
    private final Evaluation evaluation = new Evaluation();
    private final Auth auth = new Auth();
    private final WebSearch webSearch = new WebSearch();

    public Retrieval getRetrieval() { return retrieval; }
    public Workflow getWorkflow() { return workflow; }
    public Memory getMemory() { return memory; }
    public Llm getLlm() { return llm; }
    public Evaluation getEvaluation() { return evaluation; }
    public Auth getAuth() { return auth; }
    public WebSearch getWebSearch() { return webSearch; }

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
        private int ttlSeconds = 86400;
        private int maxTokens = 15000;
        private int retainedRecentRounds = 5;
        private final LongTerm longTerm = new LongTerm();
        public String getType() { return type; }
        public void setType(String value) { type = value; }
        public int getMaxMessages() { return maxMessages; }
        public void setMaxMessages(int value) { maxMessages = value; }
        public int getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(int value) { ttlSeconds = value; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int value) { maxTokens = value; }
        public int getRetainedRecentRounds() { return retainedRecentRounds; }
        public void setRetainedRecentRounds(int value) { retainedRecentRounds = value; }
        public LongTerm getLongTerm() { return longTerm; }
    }

    public static class LongTerm {
        private int topK = 5;
        private double similarityThreshold = 0.7;
        private int recentDays = 7;
        private int mediumDays = 30;
        private double recentFactor = 1.0;
        private double mediumFactor = 0.7;
        private double oldFactor = 0.3;
        private final Embedding embedding = new Embedding();
        public int getTopK() { return topK; }
        public void setTopK(int value) { topK = value; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double value) { similarityThreshold = value; }
        public int getRecentDays() { return recentDays; }
        public void setRecentDays(int value) { recentDays = value; }
        public int getMediumDays() { return mediumDays; }
        public void setMediumDays(int value) { mediumDays = value; }
        public double getRecentFactor() { return recentFactor; }
        public void setRecentFactor(double value) { recentFactor = value; }
        public double getMediumFactor() { return mediumFactor; }
        public void setMediumFactor(double value) { mediumFactor = value; }
        public double getOldFactor() { return oldFactor; }
        public void setOldFactor(double value) { oldFactor = value; }
        public Embedding getEmbedding() { return embedding; }
    }

    public static class Embedding {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String modelName = "text-embedding-v4";
        private int dimensions = 1024;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String value) { baseUrl = value; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String value) { apiKey = value; }
        public String getModelName() { return modelName; }
        public void setModelName(String value) { modelName = value; }
        public int getDimensions() { return dimensions; }
        public void setDimensions(int value) { dimensions = value; }
    }

    public static class WebSearch {
        private boolean enabled;
        private String baseUrl = "https://api.firecrawl.dev";
        private String apiKey;
        private int limit = 5;
        private int timeoutSeconds = 10;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String value) { baseUrl = value; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String value) { apiKey = value; }
        public int getLimit() { return limit; }
        public void setLimit(int value) { limit = value; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int value) { timeoutSeconds = value; }
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

    /** 评测执行与可选 Ragas 服务的受控配置。 */
    public static class Evaluation {
        private int concurrency = 2;
        private int timeoutSeconds = 30;
        private final Ragas ragas = new Ragas();
        public int getConcurrency() { return concurrency; }
        public void setConcurrency(int value) { concurrency = value; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int value) { timeoutSeconds = value; }
        public Ragas getRagas() { return ragas; }
    }

    public static class Ragas {
        private boolean enabled;
        private String baseUrl;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean value) { enabled = value; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String value) { baseUrl = value; }
    }

    /** 本项目本地签发 JWT 的配置。 */
    public static class Auth {
        private String jwtSecret = "local-development-jwt-secret-must-be-replaced";
        private long jwtTtlSeconds = 86400;
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String value) { jwtSecret = value; }
        public long getJwtTtlSeconds() { return jwtTtlSeconds; }
        public void setJwtTtlSeconds(long value) { jwtTtlSeconds = value; }
    }
}
