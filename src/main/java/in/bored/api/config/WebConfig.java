package in.bored.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.ad.media-path:/home/bored/ad/}")
    private String adMediaPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure path ends with /
        String path = adMediaPath.endsWith(File.separator) ? adMediaPath : adMediaPath + File.separator;

        // Serve files from local directory
        // Mapping to /media/** to avoid collision with API endpoints
        // Example: https://ad.boredapp.in/media/image.jpg
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:" + path);
    }
}
