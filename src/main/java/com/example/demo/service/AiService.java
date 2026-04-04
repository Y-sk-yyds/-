package com.example.demo.service;

import com.example.demo.mapper.OrderMapper;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;

@Service
public class AiService {

    @Autowired
    private OkHttpClient httpClient;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "deepseek-r1:8b";
    private final ObjectMapper objectMapper = new ObjectMapper();


    // 创建独立的 HttpClient，配置超时时间


    // 构建系统提示词
    private String buildSystemPrompt() {
        return """
                你是一个外卖/电商平台的智能客服助手，名叫"小食助手"。你的职责是：
                1. 回答用户关于商品、订单、配送等问题
                2. 帮助用户了解平台功能
                3. 语气友好、热情、专业
                4. 如果用户询问商品推荐，可以询问用户口味偏好
                5. 如果用户询问订单状态，引导用户提供订单号
                6. 回答要简洁明了，不要过于冗长
                7. 推荐给用户一些菜品
                
                平台信息：
                - 配送时间：30-60分钟
                - 配送费：5元起
                - 支付方式：微信支付、支付宝
                - 客服时间：9:00-22:00
                """;
    }

    // 调用 Ollama 生成回复
    public String chat(String userMessage, String username) throws IOException {
        // 构建上下文（可扩展：根据 username 获取用户信息）
        String userContext = "";
        if (username != null && !username.isEmpty()) {
            userContext = "当前用户：" + username + "。";
        }

        // 构建 prompt
        String prompt = buildSystemPrompt() + "\n\n" + userContext + "\n用户问：" + userMessage + "\n请回答：";

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_NAME);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);
        requestBody.put("options", Map.of("temperature", 0.7));

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 发送请求
        Request request = new Request.Builder()
                .url(OLLAMA_URL)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "抱歉，AI服务暂时不可用，请稍后再试。";
            }

            String responseBody = response.body().string();
            JsonNode root = objectMapper.readTree(responseBody);
            String answer = root.path("response").asText();

            // 清理回答中的思考过程（DeepSeek R1 可能包含 <｜end▁of▁thinking｜>）

            return answer;
        }
    }

    // 流式聊天方法（新增）
    public void chatStream(String userMessage, String username,
                           java.util.function.Consumer<String> onChunk,
                           java.util.function.Consumer<String> onError,
                           Runnable onComplete) {
        try {
            String userContext = (username != null && !username.isEmpty()) ? "当前用户：" + username + "。" : "";
            String prompt = buildSystemPrompt() + "\n\n" + userContext + "\n用户问：" + userMessage + "\n请回答：";

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.7);
            options.put("num_predict", 256);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_NAME);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", true);  // ← 关键：开启流式
            requestBody.put("options", options);

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            Request request = new Request.Builder()
                    .url(OLLAMA_URL)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    onError.accept("AI服务连接失败：" + e.getMessage());
                }

                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        onError.accept("AI服务响应失败");
                        return;
                    }
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.body().byteStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            try {
                                JsonNode node = objectMapper.readTree(line);
                                String chunk = node.path("response").asText();
                                if (!chunk.isEmpty()) {
                                    onChunk.accept(chunk);
                                }
                                if (node.path("done").asBoolean(false)) {
                                    onComplete.run();
                                    return;
                                }
                            } catch (Exception e) {
                                // 忽略解析错误
                            }
                        }
                    }
                    onComplete.run();
                }
            });
        } catch (Exception e) {
            onError.accept("请求失败：" + e.getMessage());
        }
    }
}