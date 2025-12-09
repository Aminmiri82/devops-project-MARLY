package org.marly.mavigo.service.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class GoogleAccountAlreadyLinkedException extends RuntimeException {

    public GoogleAccountAlreadyLinkedException(String message) {
        super(message);
    }
}

