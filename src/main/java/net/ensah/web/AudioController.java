package net.ensah.web;

import net.ensah.service.SpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequestMapping("/api/v1/audio")
public class AudioController {

    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);
    private final SpeechService service;

    public AudioController(SpeechService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, 
                 produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public ResponseEntity<String> index(@RequestParam("file") MultipartFile file) throws IOException {
       return new ResponseEntity<>(service.speechToText(file), HttpStatusCode.valueOf(200));
    }
}
