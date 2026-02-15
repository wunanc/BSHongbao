package com.bSHongbao.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 红包数据模型
 * Red Packet Data Model
 */
public class RedPacket {
    private final String id;
    private final String senderId;
    private final String senderName;
    private final RedPacketType type;
    private final BigDecimal totalAmount;
    private final int totalCount;
    private final long createTime;
    private final long expireTime;
    private final List<BigDecimal> amounts;
    private final Map<String, BigDecimal> claimedPlayers;
    private boolean expired;
    
    public RedPacket(String senderId, String senderName, RedPacketType type, 
                    BigDecimal totalAmount, int totalCount, long expireTimeMinutes) {
        this.id = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.senderName = senderName;
        this.type = type;
        this.totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        this.totalCount = totalCount;
        this.createTime = System.currentTimeMillis();
        this.expireTime = this.createTime + (expireTimeMinutes * 60 * 1000);
        this.amounts = new ArrayList<>();
        this.claimedPlayers = new HashMap<>();
        this.expired = false;
        
        generateAmounts();
    }
    
    /**
     * 生成红包金额分配
     */
    private void generateAmounts() {
        if (type == RedPacketType.NORMAL) {
            // 普通红包：平均分配
            BigDecimal singleAmount = totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
            for (int i = 0; i < totalCount; i++) {
                amounts.add(singleAmount);
            }
        } else {
            // 拼手气红包：随机分配
            generateLuckyAmounts();
        }
    }
    
    /**
     * 生成拼手气红包金额（二倍均值法）
     */
    private void generateLuckyAmounts() {
        Random random = new Random();
        BigDecimal remainingAmount = totalAmount;
        int remainingCount = totalCount;
        
        for (int i = 0; i < totalCount - 1; i++) {
            // 计算当前红包的最大金额（剩余金额的2倍平均值）
            BigDecimal maxAmount = remainingAmount.divide(BigDecimal.valueOf(remainingCount), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(2));
            
            // 最小金额为0.01
            BigDecimal minAmount = new BigDecimal("0.01");
            
            // 确保最大金额不会导致后续红包无法分配
            BigDecimal safeMaxAmount = remainingAmount.subtract(BigDecimal.valueOf(remainingCount - 1).multiply(minAmount));
            if (maxAmount.compareTo(safeMaxAmount) > 0) {
                maxAmount = safeMaxAmount;
            }
            
            // 生成随机金额
            BigDecimal randomAmount;
            if (maxAmount.compareTo(minAmount) <= 0) {
                randomAmount = minAmount;
            } else {
                double randomValue = random.nextDouble() * (maxAmount.doubleValue() - minAmount.doubleValue()) + minAmount.doubleValue();
                randomAmount = BigDecimal.valueOf(randomValue).setScale(2, RoundingMode.HALF_UP);
            }
            
            amounts.add(randomAmount);
            remainingAmount = remainingAmount.subtract(randomAmount);
            remainingCount--;
        }
        
        // 最后一个红包获得剩余金额
        amounts.add(remainingAmount.setScale(2, RoundingMode.HALF_UP));
        
        // 打乱顺序
        Collections.shuffle(amounts);
    }
    
    /**
     * 领取红包
     */
    public synchronized BigDecimal claim(String playerId) {
        if (expired || isExpired()) {
            return null;
        }
        
        if (claimedPlayers.containsKey(playerId)) {
            return null; // 已经领取过
        }
        
        if (claimedPlayers.size() >= amounts.size()) {
            return null; // 已经被领完
        }
        
        BigDecimal amount = amounts.get(claimedPlayers.size());
        claimedPlayers.put(playerId, amount);
        return amount;
    }
    
    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return expired || System.currentTimeMillis() > expireTime;
    }
    
    /**
     * 设置为过期
     */
    public void setExpired() {
        this.expired = true;
    }
    
    /**
     * 获取剩余金额
     */
    public BigDecimal getRemainingAmount() {
        BigDecimal claimedAmount = claimedPlayers.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalAmount.subtract(claimedAmount);
    }
    
    /**
     * 获取剩余份数
     */
    public int getRemainingCount() {
        return totalCount - claimedPlayers.size();
    }
    
    /**
     * 检查是否被领完
     */
    public boolean isFullyClaimed() {
        return claimedPlayers.size() >= totalCount;
    }
    
    // Getters
    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public RedPacketType getType() { return type; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getTotalCount() { return totalCount; }
    public long getCreateTime() { return createTime; }
    public long getExpireTime() { return expireTime; }
    public Map<String, BigDecimal> getClaimedPlayers() { return new HashMap<>(claimedPlayers); }
    
    /**
     * 红包类型枚举
     */
    public enum RedPacketType {
        NORMAL("普通红包"),
        LUCKY("拼手气红包");
        
        private final String displayName;
        
        RedPacketType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}