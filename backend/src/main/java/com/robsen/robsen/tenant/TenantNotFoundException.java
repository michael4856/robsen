package com.robsen.robsen.tenant;

import com.robsen.robsen.common.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class TenantNotFoundException extends ApplicationException {

    public TenantNotFoundException(Long tenantId) {
        super("No tenant found with id: " + tenantId);
    }
}