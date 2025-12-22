package in.bored.api.controller;

import in.bored.api.model.AppConfig;
import in.bored.api.repo.AppConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/config")
public class AppConfigController {

    @Autowired
    private AppConfigRepository appConfigRepository;

    @GetMapping
    public Map<String, String> getAllConfig() {
        return appConfigRepository.findAll().stream()
                .collect(Collectors.toMap(AppConfig::getKey, AppConfig::getValue));
    }
}
