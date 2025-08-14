package com.example.intranet_back_stage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// model/LeaveBalance.java : solde annuel (par employé & année)
@Entity
@Table(name = "leave_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="employee_id", nullable=false)
    private User employee;

    private int year;
    @Column(precision=5, scale=2) private BigDecimal annualEntitled;  // droits annuels (ex: 22)
    @Column(precision=5, scale=2) private BigDecimal annualUsed;      // consommé approuvé

    @Transient
    public BigDecimal getAnnualRemaining() {
        return annualEntitled.subtract(annualUsed == null ? BigDecimal.ZERO : annualUsed);
    }
}
