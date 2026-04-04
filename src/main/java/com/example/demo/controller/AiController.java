package com.example.demo.controller;

import com.example.demo.commen.Result;
import com.example.demo.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public Result<?> chat(@RequestAttribute(required = false) String username,
                          @RequestBody Map<String, String> request) {
        String question = request.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Result.error("请输入问题");
        }

        try {
            String answer = aiService.chat(question, username);
            Map<String, String> data = new HashMap<>();
            data.put("question", question);
            data.put("answer", answer);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("AI服务异常：" + e.getMessage());
        }
    }

    // 健康检查接口
    @GetMapping("/health")
    public Result<?> health() {
        try {
            // 简单测试 Ollama 是否可用
            aiService.chat("测试", null);
            return Result.success("AI服务正常");
        } catch (Exception e) {
            return Result.error("AI服务异常：" + e.getMessage());
        }
    }

    // 流式聊天接口（新增）
    @PostMapping("/chat/stream")
    public SseEmitter chatStream(@RequestAttribute(required = false) String username,
                                 @RequestBody Map<String, String> request) {
        String question = request.get("question");
        SseEmitter emitter = new SseEmitter(180000L); // 3分钟超时

        if (question == null || question.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().data("请输入问题"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        aiService.chatStream(question, username,
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    try {
                        emitter.send(SseEmitter.event().data(error));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                () -> emitter.complete()
        );

        return emitter;
    }
}