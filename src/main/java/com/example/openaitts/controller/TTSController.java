package com.example.openaitts.controller;

import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechModel;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class TTSController {

    @Value("${spring.ai.openai.api-key}")
     private String apiKey;

    SpeechModel speechModel;

    public TTSController(SpeechModel speechModel){
        this.speechModel=speechModel;
    }

    @PostMapping("/upload")
    public ResponseEntity<StreamingResponseBody> uploadFile(@RequestParam("file") MultipartFile file) throws Exception {
        // 업로드된 파일의 텍스트 내용 읽기
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);

        // OpenAI API 호출 준비
        var openAIAudioApi = new OpenAiAudioApi(apiKey);
        var openAIAudioSpeechModel = new OpenAiAudioSpeechModel(openAIAudioApi);

        OpenAiAudioSpeechOptions options = OpenAiAudioSpeechOptions.builder()
                .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .withSpeed(1.1f)
                .withResponseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .withModel(OpenAiAudioApi.TtsModel.TTS_1.value)
                .build();

        SpeechPrompt speechPrompt = new SpeechPrompt(content, options);

        // 리액티브 스트림을 블로킹 방식으로 변환
        Flux<SpeechResponse> responseStream = openAIAudioSpeechModel.stream(speechPrompt);

        StreamingResponseBody stream = outputStream -> {
            responseStream
                    .toStream()  // Flux를 blocking Stream으로 변환
                    .forEach(speechResponse -> {
                        try {
                            outputStream.write(speechResponse.getResult().getOutput());
                            outputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        };

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/mpeg") // MP3 응답 설정
                .body(stream);
    }
}
