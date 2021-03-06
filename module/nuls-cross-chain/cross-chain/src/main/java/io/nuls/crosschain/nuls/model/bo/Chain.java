package io.nuls.crosschain.nuls.model.bo;

import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.crosschain.base.message.BroadCtxSignMessage;
import io.nuls.crosschain.nuls.model.bo.config.ConfigBean;
import io.nuls.crosschain.nuls.rpc.call.NetWorkCall;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.logback.NulsLogger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 链信息类
 * Chain information class
 *
 * @author tag
 * 2019/4/10
 **/
public class Chain {
    /**
     * 链基础配置信息
     * Chain Foundation Configuration Information
     */
    private ConfigBean config;

    /**
     * 待处理的跨链交易
     * Trans-chain transactions to be processed
     * key:交易Hash
     * value:该HASH对应的交易列表
     * */
    private Map<NulsDigestData, List<Transaction>> todoCtxMap;

    /**
     * 正在处理的交易
     * Transactions under processing
     * key:交易Hash
     * value:正在处理的交易
     * */
    private Map<NulsDigestData, Transaction> doingCtxMap;

    /**
     * 跨链交易验证结果
     * Verification results of cross-chain transactions
     * key:交易Hash
     * value：验证结果列表
     * */
    private Map<NulsDigestData, List<Boolean>> verifyCtxResultMap;

    /**
     * 跨链交易处理结果
     * Cross-Chain Transaction Processing Results
     * key:交易Hash
     * value:处理结果列表
     * */
    private Map<NulsDigestData, List<Boolean>> ctxStateMap;

    /**
     * 待广播的跨链交易Hash和签名
     * Cross-Chain Transaction Hash and Signature to be Broadcast
     * key:交易Hash
     * value:待广播的交易签名列表
     * */
    private Map<NulsDigestData, Set<BroadCtxSignMessage>> waitBroadSignMap;

    /**
     * 跨链模块基础日志类
     * */
    private NulsLogger basicLog;

    /**
     * 跨链模块消息协议处理日志
     * */
    private NulsLogger messageLog;

    /**
     * 跨链模块Rpc接口调用处理类
     * */
    private NulsLogger rpcLogger;

    /**
     * 本链是否为主网
     * */
    private boolean mainChain;

    public Chain(){
        todoCtxMap = new ConcurrentHashMap<>();
        doingCtxMap = new ConcurrentHashMap<>();
        verifyCtxResultMap = new ConcurrentHashMap<>();
        ctxStateMap = new ConcurrentHashMap<>();
        waitBroadSignMap = new ConcurrentHashMap<>();
        mainChain = false;
    }

    public int getChainId(){
        return config.getChainId();
    }

    public ConfigBean getConfig() {
        return config;
    }

    public void setConfig(ConfigBean config) {
        this.config = config;
    }

    public Map<NulsDigestData, List<Transaction>> getTodoCtxMap() {
        return todoCtxMap;
    }

    public void setTodoCtxMap(Map<NulsDigestData, List<Transaction>> todoCtxMap) {
        this.todoCtxMap = todoCtxMap;
    }

    public Map<NulsDigestData, Transaction> getDoingCtxMap() {
        return doingCtxMap;
    }

    public void setDoingCtxMap(Map<NulsDigestData, Transaction> doingCtxMap) {
        this.doingCtxMap = doingCtxMap;
    }

    public Map<NulsDigestData, List<Boolean>> getVerifyCtxResultMap() {
        return verifyCtxResultMap;
    }

    public void setVerifyCtxResultMap(Map<NulsDigestData, List<Boolean>> verifyCtxResultMap) {
        this.verifyCtxResultMap = verifyCtxResultMap;
    }

    public Map<NulsDigestData, List<Boolean>> getCtxStateMap() {
        return ctxStateMap;
    }

    public void setCtxStateMap(Map<NulsDigestData, List<Boolean>> ctxStateMap) {
        this.ctxStateMap = ctxStateMap;
    }

    public NulsLogger getBasicLog() {
        return basicLog;
    }

    public void setBasicLog(NulsLogger basicLog) {
        this.basicLog = basicLog;
    }

    public NulsLogger getMessageLog() {
        return messageLog;
    }

    public void setMessageLog(NulsLogger messageLog) {
        this.messageLog = messageLog;
    }

    public NulsLogger getRpcLogger() {
        return rpcLogger;
    }

    public void setRpcLogger(NulsLogger rpcLogger) {
        this.rpcLogger = rpcLogger;
    }

    public boolean isMainChain() {
        return mainChain;
    }

    public void setMainChain(boolean mainChain) {
        this.mainChain = mainChain;
    }

    public Map<NulsDigestData, Set<BroadCtxSignMessage>> getWaitBroadSignMap() {
        return waitBroadSignMap;
    }

    public void setWaitBroadSignMap(Map<NulsDigestData, Set<BroadCtxSignMessage>> waitBroadSignMap) {
        this.waitBroadSignMap = waitBroadSignMap;
    }

    public boolean canSendMessage(){
        try {
            int linkedNode = NetWorkCall.getAvailableNodeAmount(getChainId(), true);
            if(linkedNode >= config.getMinNodeAmount()){
                return true;
            }
        }catch (NulsException e){
            basicLog.error(e);
        }
        return false;
    }

    public void clearCache(NulsDigestData hash){
        doingCtxMap.remove(hash);
        todoCtxMap.remove(hash);
        waitBroadSignMap.remove(hash);
    }

    public boolean verifyResult(NulsDigestData hash,int threshold){
        int count = 0;
        if(verifyCtxResultMap.get(hash).size() < threshold){
            return false;
        }
        for (boolean verifyResult:verifyCtxResultMap.get(hash)) {
            if(verifyResult){
                count++;
                if(count >= threshold){
                    return true;
                }
            }

        }
        return false;
    }

    public boolean statisticsCtxState(NulsDigestData hash,int threshold){
        int count = 0;
        if(ctxStateMap.get(hash).size() < threshold){
            return false;
        }
        for (boolean ctxState:ctxStateMap.get(hash)) {
            if(ctxState){
                count++;
                if(count >= threshold){
                    return true;
                }
            }

        }
        return false;
    }
}
