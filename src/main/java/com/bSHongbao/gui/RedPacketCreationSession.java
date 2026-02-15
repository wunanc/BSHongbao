package com.bSHongbao.gui;

import com.bSHongbao.model.RedPacket;

import java.math.BigDecimal;

/**
 * 红包创建会话
 * Red Packet Creation Session
 */
public class RedPacketCreationSession {
    private final RedPacket.RedPacketType type;
    private final long createTime;
    private Step step;
    private BigDecimal amount;
    
    public RedPacketCreationSession(RedPacket.RedPacketType type) {
        this.type = type;
        this.createTime = System.currentTimeMillis();
        this.step = Step.WAITING_FOR_AMOUNT;
    }
    
    /**
     * 会话步骤枚举
     */
    public enum Step {
        WAITING_FOR_AMOUNT,
        WAITING_FOR_COUNT,
        COMPLETED
    }
    
    // Getters and Setters
    public RedPacket.RedPacketType getType() {
        return type;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public Step getStep() {
        return step;
    }
    
    public void setStep(Step step) {
        this.step = step;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    /**
     * 检查会话是否过期（5分钟）
     */
    public boolean isExpired() {
        return System.currentTimeMillis() - createTime > 5 * 60 * 1000;
    }
}