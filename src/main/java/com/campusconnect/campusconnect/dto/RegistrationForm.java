package com.campusconnect.campusconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String studentName;

    @NotBlank(message = "College email is required")
    @Email(message = "Please provide a valid college email address")
    private String studentEmail;

    @NotBlank(message = "Roll number / Student ID is required")
    @Pattern(regexp = "^[A-Za-z0-9_-]{3,20}$", message = "Roll number must be 3-20 characters (letters, numbers, underscores, hyphens)")
    private String rollNumber;

    public RegistrationForm() {
    }

    public RegistrationForm(String studentName, String studentEmail, String rollNumber) {
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.rollNumber = rollNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }
}
