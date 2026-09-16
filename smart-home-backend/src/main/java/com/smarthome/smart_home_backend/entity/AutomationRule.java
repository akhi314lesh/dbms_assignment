package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "AUTOMATION_RULES")
public class AutomationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rule_seq")
    @SequenceGenerator(
            name = "rule_seq",
            sequenceName = "SEQ_RULE_ID",
            allocationSize = 1
    )
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Column(name = "condition_operator", nullable = false, length = 20)
    private String conditionOperator;

    @Column(name = "condition_value", nullable = false, length = 100)
    private String conditionValue;

    @ManyToOne
    @JoinColumn(name = "condition_device_id", nullable = false)
    private Device conditionDevice;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    public AutomationRule() {
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getConditionOperator() {
        return conditionOperator;
    }

    public void setConditionOperator(String conditionOperator) {
        this.conditionOperator = conditionOperator;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public Device getConditionDevice() {
        return conditionDevice;
    }

    public void setConditionDevice(Device conditionDevice) {
        this.conditionDevice = conditionDevice;
    }

    public User getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(User createdByUser) {
        this.createdByUser = createdByUser;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
}