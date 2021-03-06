package io.nuls.poc.model.bo.config;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * 共识模块配置类
 * Consensus Module Configuration Class
 *
 * @author tag
 * 2018/11/7
 */
public class ConfigBean implements Serializable {
    /**
     * 打包间隔时间
     * Packing interval time
     */
    private long packingInterval;
    /**
     * 获得红牌保证金锁定时间
     * Lock-in time to get a red card margin
     */
    private long redPublishLockTime;
    /**
     * 注销节点保证金锁定时间
     * Log-off node margin locking time
     */
    private long stopAgentLockTime;
    /**
     * 佣金比例的最小值
     * Minimum commission ratio
     */
    private byte commissionRateMin;
    /**
     * 佣金比例的最大值
     * Maximum commission ratio
     */
    private byte commissionRateMax;
    /**
     * 创建节点的保证金最小值
     * Minimum margin for creating nodes
     */
    private BigInteger depositMin;
    /**
     * 创建节点的保证金最大值
     * Maximum margin for creating nodes
     */
    private BigInteger depositMax;
    /**
     * 节点出块委托金额最小值
     * Minimum Delegation Amount of Node Block
     */
    private BigInteger commissionMin;
    /**
     * 节点委托金额最大值
     * Maximum Node Delegation Amount
     */
    private BigInteger commissionMax;

    /**
     * 委托最小金额
     * Minimum amount entrusted
     */
    private BigInteger entrusterDepositMin;

    /**
     * 种子节点
     * Seed node
     */
    private String seedNodes;

    /**
     * 资产ID
     * assets id
     */
    private int assetsId;

    /**
     * chain id
     */
    private int chainId;

    /**
     * 节点委托金额最大值
     * Maximum Node Delegation Amount
     */
    private BigInteger inflationAmount;

    /**
     * 出块节点密码
     * */
    private String password;

    /**
     * 打包区块最大值
     * */
    private long blockMaxSize;

    /**
     * 打包一个区块获得的共识奖励
     * 每年通胀/每年出块数
     * */
    private BigInteger blockReward;

    public long getPackingInterval() {
        return packingInterval;
    }

    public void setPackingInterval(long packingInterval) {
        this.packingInterval = packingInterval;
    }


    public long getRedPublishLockTime() {
        return redPublishLockTime;
    }

    public void setRedPublishLockTime(long redPublishLockTime) {
        this.redPublishLockTime = redPublishLockTime;
    }

    public long getStopAgentLockTime() {
        return stopAgentLockTime;
    }

    public void setStopAgentLockTime(long stopAgentLockTime) {
        this.stopAgentLockTime = stopAgentLockTime;
    }

    public byte getCommissionRateMin() {
        return commissionRateMin;
    }

    public void setCommissionRateMin(byte commissionRateMin) {
        this.commissionRateMin = commissionRateMin;
    }

    public byte getCommissionRateMax() {
        return commissionRateMax;
    }

    public void setCommissionRateMax(byte commissionRateMax) {
        this.commissionRateMax = commissionRateMax;
    }

    public BigInteger getDepositMin() {
        return depositMin;
    }

    public void setDepositMin(BigInteger depositMin) {
        this.depositMin = depositMin;
    }

    public BigInteger getDepositMax() {
        return depositMax;
    }

    public void setDepositMax(BigInteger depositMax) {
        this.depositMax = depositMax;
    }

    public BigInteger getCommissionMin() {
        return commissionMin;
    }

    public void setCommissionMin(BigInteger commissionMin) {
        this.commissionMin = commissionMin;
    }

    public BigInteger getCommissionMax() {
        return commissionMax;
    }

    public void setCommissionMax(BigInteger commissionMax) {
        this.commissionMax = commissionMax;
    }

    public BigInteger getEntrusterDepositMin() {
        return entrusterDepositMin;
    }

    public void setEntrusterDepositMin(BigInteger entrusterDepositMin) {
        this.entrusterDepositMin = entrusterDepositMin;
    }

    public String getSeedNodes() {
        return seedNodes;
    }

    public void setSeedNodes(String seedNodes) {
        this.seedNodes = seedNodes;
    }

    public int getAssetsId() {
        return assetsId;
    }

    public void setAssetsId(int assetsId) {
        this.assetsId = assetsId;
    }

    public int getChainId() {
        return chainId;
    }

    public void setChainId(int chainId) {
        this.chainId = chainId;
    }

    public BigInteger getInflationAmount() {
        return inflationAmount;
    }

    public void setInflationAmount(BigInteger inflationAmount) {
        this.inflationAmount = inflationAmount;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getBlockMaxSize() {
        return blockMaxSize;
    }

    public void setBlockMaxSize(long blockMaxSize) {
        this.blockMaxSize = blockMaxSize;
    }

    public BigInteger getBlockReward() {
        return blockReward;
    }

    public void setBlockReward(BigInteger blockReward) {
        this.blockReward = blockReward;
    }
}
