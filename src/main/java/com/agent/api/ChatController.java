/**
 * 本文件定义 {@code ChatController}，负责对外 HTTP 接口、请求模型与响应模型。
 *
 * <p>这里集中表达该模块的职责边界，具体实现细节以方法和接口契约为准。</p>
 */
package com.agent.api;

import com.agent.chat.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    public ChatController(ChatService chatService) { this.chatService = chatService; }

    /** 接收经参数校验的对话请求，并将领域结果转换为统一的 HTTP 响应。 */
    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.of(ChatResponse.from(chatService.chat(request.tenantId(), request.userId(), request.sessionId(), request.question(), request.approvalRequired())));
    }
}
