package net.ensah.service;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class SpeechService {

    private final OpenAiAudioTranscriptionModel audioClient;

    public SpeechService(OpenAiAudioTranscriptionModel audioClient) {
        this.audioClient = audioClient;
    }

    public String speechToText(MultipartFile file) throws IOException {
        // Créer un fichier temporaire avec la bonne extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        Path tempFile = Files.createTempFile("audio_", extension);

        try {
            // Copier le contenu du MultipartFile vers le fichier temporaire
            Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Créer la ressource à partir du fichier temporaire
            Resource audioResource = new FileSystemResource(tempFile.toFile());

            var options = OpenAiAudioTranscriptionOptions.builder()
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.JSON)
                    .build();
            var prompt = new AudioTranscriptionPrompt(audioResource, options);
            AudioTranscriptionResponse response = audioClient.call(prompt);
            return response.getResult().getOutput();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }


}
