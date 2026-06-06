package org.example.dto;

import org.example.model.Role;

public class AuthResponseDto {
    private String token;
    private Role role;
    private String email;
    private String firstName;
    private String lastName;

    public AuthResponseDto(String token, Role role, String email, String firstName, String lastName) {
        this.token = token;
        this.role = role;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getToken() { return token; }
    public Role getRole() { return role; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}
