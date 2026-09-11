package com.example.ticketassistant.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id private String id;
    @Column(nullable = false) private String customerId;
    @Column(nullable = false) private String subject;
    @Column(nullable = false, length = 4000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Priority priority;
    private String product;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ProcessingStatus processingStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private String processingError;
    protected Ticket() { }
    public Ticket(String id, String customerId, String subject, String description, Priority priority, String product) {
        this.id=id; this.customerId=customerId; this.subject=subject; this.description=description; this.priority=priority; this.product=product; this.processingStatus=ProcessingStatus.PENDING;
    }
    @PrePersist void prePersist() { createdAt=Instant.now(); updatedAt=createdAt; }
    @PreUpdate void preUpdate() { updatedAt=Instant.now(); }
    public String getId(){return id;} public String getCustomerId(){return customerId;} public String getSubject(){return subject;} public String getDescription(){return description;} public Priority getPriority(){return priority;} public String getProduct(){return product;} public ProcessingStatus getProcessingStatus(){return processingStatus;} public String getProcessingError(){return processingError;}
    public void markProcessing(){ processingStatus=ProcessingStatus.PROCESSING; processingError=null; }
    public void markCompleted(){ processingStatus=ProcessingStatus.COMPLETED; processingError=null; }
    public void markFailed(String error){ processingStatus=ProcessingStatus.FAILED; processingError=error; }
}
