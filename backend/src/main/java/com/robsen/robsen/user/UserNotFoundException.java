package com.robsen.robsen.user;

import com.robsen.robsen.common.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UserNotFoundException extends ApplicationException {

    public UserNotFoundException(Long userId) {
        super("No user found with id: " + userId);
    }
}
