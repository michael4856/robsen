package com.robsen.robsen.user;

import com.robsen.robsen.userrole.UserRole;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity 
@Data 
public class User {
    private String firstName;
    private String middleString;
    private String lastName;
    private Integer phone;
    private String email;
    private String location;
    private UserRole role;

}
