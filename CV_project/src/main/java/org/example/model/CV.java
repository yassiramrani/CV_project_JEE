package org.example.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "cvs")
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String fileName;
    private String filePath;

    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadDate;

    @OneToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(length = 4000)
    private String aiImprovements;

    @PrePersist
    protected void onCreate() {
        uploadDate = new Date();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Date getUploadDate() { return uploadDate; }
    public void setUploadDate(Date uploadDate) { this.uploadDate = uploadDate; }

    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }

    public String getAiImprovements() { return aiImprovements; }
    public void setAiImprovements(String aiImprovements) { this.aiImprovements = aiImprovements; }
}
