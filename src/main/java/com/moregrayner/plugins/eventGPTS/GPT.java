package com.moregrayner.plugins.eventGPTS;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

public class GPT {

    private final String apiKey;
    private final HttpClient client;
    private static final Logger LOGGER = Logger.getLogger("GPT");

    public GPT(String apiKey) {
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
        LOGGER.info("OpenAI API 키: " + (apiKey != null && !apiKey.isEmpty() ? "성공" : "실패"));
    }

    // API 키 유효성 검사
    public boolean isApiKeyValid() {
        URI uri = URI.create("https://api.openai.com/v1/models"); // 이 부분은 GET 요청에 대한 검사를 위해 두고 있어도 됩니다.
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", "Bearer " + apiKey)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            LOGGER.warning("API 키 유효성 검사 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    // OpenAI API를 호출하여 가이드라인 요청
    public String getGuide(String guideline) {
        if (guideline == null || guideline.isEmpty()) {
            LOGGER.warning("빈 문자열이 전달됨: 발견된 아이템 이름 'null' ");
            return null;
        }

        LOGGER.info("가이드라인 요청 시작: " + guideline);

        // gpt-3.5-turbo 모델을 사용한 Chat Completion 요청
        URI uri = URI.create("https://api.openai.com/v1/chat/completions");
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", guideline);
        messages.put(message);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-3.5-turbo");
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 60000);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 응답 로그 출력
            LOGGER.warning("API 응답 코드: " + response.statusCode());
            LOGGER.warning("API 응답 본문: " + response.body());

            switch (response.statusCode()) {
                case 402 -> {
                    LOGGER.severe("API 요청 실패! (402: 결제 필요) - API 크레딧을 확인하세요.");
                    return "오류: API 크레딧이 부족합니다.";
                }
                case 404 -> {
                    LOGGER.severe("API 요청 실패! (404: 엔드포인트 없음) - API URL을 확인하세요.");
                    return "오류: API 요청 URL이 잘못되었습니다.";
                }
                case 401 -> {
                    LOGGER.severe("API 요청 실패! (401: 인증 실패) - API 키가 올바른지 확인하세요.");
                    return "오류: API 키가 올바르지 않습니다.";
                }
                case 423 -> {
                    LOGGER.severe("API 요청 실패! 오류 코드:423: - 일정 시간 후 다시 시도해 주세요.");
                    LOGGER.severe("API 키 내부에 충분한 크래딧이 없을 수 있습니다. 확인해 주세요.");
                    return "오류: 요청이 잠겨 있습니다. 잠시 후 다시 시도하세요.";
                }
            }

            if (response.statusCode() == 200) {
                JSONObject responseObject = new JSONObject(response.body());
                if (!responseObject.has("choices")) {
                    LOGGER.warning("응답 데이터가 올바르지 않음: " + response.body());
                    return null;
                }

                String translatedName = responseObject.getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content").trim();
                LOGGER.info("가이드라인: " + translatedName);
                return translatedName;
            } else {
                LOGGER.warning("API 요청 실패! 응답 코드: " + response.statusCode());
                return "오류: API 요청 실패 (코드 " + response.statusCode() + ")";
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.severe("API 요청 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "오류: API 요청 중 문제가 발생했습니다.";
        }
    }
}
