package in.bored.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class TopicsExhaustedException extends RuntimeException {
    public TopicsExhaustedException(String message) {
        super(message);
    }
}
