/**
 * 本文件定义 {@code DocumentOperation}，负责文档变更、幂等入库与索引重建流程。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.ingestion;

/** 文档变更操作类型。 */
public enum DocumentOperation { UPSERT, DELETE }
