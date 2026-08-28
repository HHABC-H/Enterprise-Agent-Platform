package com.agent.api;

import com.agent.memory.LongTermMemoryService;
import com.agent.memory.MemoryType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {
    private final IdentityGuard identityGuard;
    private final LongTermMemoryService memoryService;
    public MemoryController(IdentityGuard identityGuard, LongTermMemoryService memoryService) { this.identityGuard = identityGuard; this.memoryService = memoryService; }
    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody MemoryCreateRequest request) {
        if (request.type() == null) throw new IllegalArgumentException("记忆类型不能为空。");
        if (request.type() == MemoryType.PROCEDURAL) throw new IllegalArgumentException("行为模式记忆只能由受控规则生成。");
        IdentityGuard.Actor actor = identityGuard.actor();
        memoryService.saveManual(actor.tenantId(), actor.userId(), request.content(), request.type(), request.importance() == null ? 0.8 : request.importance());
        return ApiResponse.of(null);
    }
}
