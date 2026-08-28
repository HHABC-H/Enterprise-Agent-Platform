package com.agent.api;

import com.agent.memory.MemoryType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 用户主动保存长期记忆的受限请求。 */
public record MemoryCreateRequest(@NotBlank(message = "记忆内容不能为空。") @Size(max = 1000, message = "记忆内容不能超过 1000 个字符。") String content,
                                  MemoryType type,
                                  @DecimalMin(value = "0.0", message = "重要性不能小于 0。") @DecimalMax(value = "1.0", message = "重要性不能大于 1。") Double importance) {
}
