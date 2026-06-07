package org.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "recruiters")
public class Recruiter extends User {

    @jakarta.persistence.Column(name = "company_name")
    private String companyName;

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}
