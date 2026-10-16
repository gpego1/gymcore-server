package com.gymcore.student.entity;

import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.UUID;
import com.gymcore.shared.entity.TenantEntity;


@Entity
@Table(name = "students")
public class Student extends TenantEntity {
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(nullable = false, length = 16)
    private String phone;
    @Column(nullable = false)
    private LocalDate birthDate;
    @Column(nullable = false)
    private boolean active;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
