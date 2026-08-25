/**
 * 本文件定义 {@code AgentPlatformApplication}，负责应用启动与组件扫描。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AgentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentPlatformApplication.class, args);
    }
}
