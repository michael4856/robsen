package com.robsen.robsen.common;

public class BusinessNotFoundException extends ApplicationException{
    
    public BusinessNotFoundException(Long businessId){
        super("No business found with id: " + businessId);
    }
}
