package com.smarthome.smart_home_backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "RULE_ACTIONS")
public class RuleAction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "action_seq")
    @SequenceGenerator(
            name = "action_seq",
            sequenceName = "SEQ_ACTION_ID",
            allocationSize = 1
    )
    @Column(name = "action_id")
    private Long actionId;

    @ManyToOne
    @JoinColumn(name = "rule_id", nullable = false)
    private AutomationRule rule;

    @ManyToOne
    @JoinColumn(name = "target_device_id", nullable = false)
    private Device targetDevice;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "action_value", length = 100)
    private String actionValue;

    public RuleAction() {
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public AutomationRule getRule() {
        return rule;
    }

    public void setRule(AutomationRule rule) {
        this.rule = rule;
    }

    public Device getTargetDevice() {
        return targetDevice;
    }

    public void setTargetDevice(Device targetDevice) {
        this.targetDevice = targetDevice;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getActionValue() {
        return actionValue;
    }

    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }
}