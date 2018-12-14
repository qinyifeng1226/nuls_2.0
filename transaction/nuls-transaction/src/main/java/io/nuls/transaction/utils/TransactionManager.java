/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package io.nuls.transaction.utils;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.TransactionFeeCalculator;
import io.nuls.base.data.*;
import io.nuls.base.signture.SignatureUtil;
import io.nuls.tools.basic.Result;
import io.nuls.tools.data.BigIntegerUtils;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.model.bo.TxRegister;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 交易管理类，存储管理交易注册的基本信息
 * @author: Charlie
 * @date: 2018/11/22
 */
public class TransactionManager {
    /**
     * 交易注册信息
     */
    private static final Map<Integer, TxRegister> TX_REGISTER_MAP = new HashMap<>();

    private static final TransactionManager INSTANCE = new TransactionManager();

    public static TransactionManager getInstance(){
        return INSTANCE;
    }


    private TransactionManager() {
        //TODO 注册跨链交易
        TxRegister txRegister = new TxRegister();
        txRegister.setModuleCode(TxConstant.MODULE_CODE);
        txRegister.setModuleValidator(TxConstant.TX_MODULE_VALIDATOR);
        txRegister.setTxType(TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER);
        txRegister.setValidator(TxConstant.CROSS_TRANSFER_VALIDATOR);
        txRegister.setCommit(TxConstant.CROSS_TRANSFER_COMMIT);
        txRegister.setRollback(TxConstant.CROSS_TRANSFER_ROLLBACK);
        txRegister.setSystemTx(true);
        txRegister.setUnlockTx(false);
        txRegister.setVerifySignature(true);
        register(txRegister);
    }

    /**
     * 注册交易
     * @param txRegister 注册交易请求数据封装
     * @return boolean
     */
    public boolean register(TxRegister txRegister){
        boolean rs = false;
        if(!TX_REGISTER_MAP.containsKey(txRegister.getTxType())){
            TX_REGISTER_MAP.put(txRegister.getTxType(), txRegister);
            rs = true;
        }
        return rs;
    }

    /**
     * 获取交易的注册对象
     * @param type
     * @return
     */
    public TxRegister getTxRegister(int type){
        return TX_REGISTER_MAP.get(type);
    }

    /**
     * 根据交易类型返回交易类型是否存在
     * @param type
     * @return
     */
    public boolean contain(int type){
        return TX_REGISTER_MAP.containsKey(type);
    }

    /**
     * 返回系统交易类型
      */
    public List<Integer> getSysTypes(){
        List<Integer> list = new ArrayList<>();
        for(Map.Entry<Integer, TxRegister> map : TX_REGISTER_MAP.entrySet()){
            if(map.getValue().getSystemTx()){
                list.add(map.getKey());
            }
        }
        return list;
    }

    /**
     * 判断交易是系统交易
     * @param tx
     * @return
     */
    public boolean isSystemTx(Transaction tx){
        TxRegister txRegister =TX_REGISTER_MAP.get(tx.getType());
        return txRegister.getSystemTx();
    }

    /**
     * 验证交易
     * @param chainId
     * @param tx
     * @return
     */
    public boolean verify(int chainId, Transaction tx) throws NulsException{

        baseTxValidate(chainId, tx);
        TxRegister txRegister = this.getTxRegister(tx.getType());
        txRegister.getValidator();
        //todo 调验证器
        return false;
    }

    /**
     *  交易基础验证
     *  基础字段
     *  交易size
     *  交易类型
     *  交易签名
     *  * from的地址必须全部是发起链(本链or相同链）地址
     *  from里面的资产是否存在
     *  to里面的地址必须是相同链的地址
     *  交易手续费
     * @param chainId
     * @param tx
     * @return Result
     */
    private boolean baseTxValidate(int chainId, Transaction tx) throws NulsException {

        if (null == tx) {
            throw new NulsException(TxErrorCode.TX_NOT_EXIST);
        }
        if (tx.getHash() == null || tx.getHash().size() == 0 || tx.getHash().size() > TxConstant.TX_HASH_DIGEST_BYTE_MAX_LEN) {
            throw new NulsException(TxErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        if(!contain(tx.getType())) {
            throw new NulsException(TxErrorCode.TX_NOT_EFFECTIVE);
        }
        if (tx.getTime() == 0L) {
            throw new NulsException(TxErrorCode.TX_DATA_VALIDATION_ERROR);
        }
        if(tx.size() > TxConstant.TX_MAX_SIZE){
            throw new NulsException(TxErrorCode.TX_SIZE_TOO_LARGE);
        }
        //todo 确认验证签名正确性
        if(!SignatureUtil.validateTransactionSignture(tx)){
            throw new NulsException(TxErrorCode.SIGNATURE_ERROR);
        }
        //如果有coinData, 则进行验证
        if(null != tx.getCoinData() && tx.getCoinData().length > 0) {
            //coinData基础验证以及手续费 (from中所有的nuls资产-to中所有nuls资产)
            CoinData coinData = TxUtil.getCoinData(tx);
            if (!validateCoinFromBase(chainId, tx.getType(),coinData.getFrom())) {
                return false;
            }
            if (!validateCoinToBase(coinData.getTo())) {
                return false;
            }
            if (!validateFee(chainId, tx.getType(), tx.size(), coinData)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 验证交易的付款方数据
     * 1.from中地址对应的链id是否是发起链id
     * 2.验证资产是否存在
     * @param chainId
     * @param listFrom
     * @return Result
     */
    public boolean validateCoinFromBase(int chainId, int type, List<CoinFrom> listFrom) throws NulsException {
        //coinBase交易没有from
        if(type == TxConstant.TX_TYPE_COINBASE) {
            throw new NulsException(TxErrorCode.SUCCESS);
        }
        if(null == listFrom || listFrom.size() == 0){
            throw new NulsException(TxErrorCode.COINFROM_NOT_FOUND);
        }
        for(CoinFrom coinFrom : listFrom){
            byte[] addrBytes = coinFrom.getAddress();
            String address =  AddressTool.getStringAddressByBytes(addrBytes);
            //from中地址对应的链id是否是发起链id
            if(!AddressTool.validAddress(chainId, address)){
                throw new NulsException(TxErrorCode.CROSS_TX_PAYER_CHAINID_MISMATCH);
            }
            //验证资产是否存在
            if(!TxUtil.assetExist(coinFrom.getAssetsChainId(), coinFrom.getAssetsId())){
                throw new NulsException(TxErrorCode.ASSET_NOT_EXIST);
            }
        }
        return true;
    }

    /**
     * 验证交易的收款方数据(coinTo是不是属于同一条链)
     * 1.收款方所有地址是不是属于同一条链
     * @param listTo
     * @return Result
     */
    public boolean validateCoinToBase(List<CoinTo> listTo) throws NulsException {
        if (null == listTo || listTo.size() == 0) {
            throw new NulsException(TxErrorCode.COINTO_NOT_FOUND);
        }
        //验证收款方是不是属于同一条链
        Integer addressChainId = null;
        for(CoinTo coinTo : listTo){
            int chainId = AddressTool.getChainIdByAddress(coinTo.getAddress());
            if(null == addressChainId){
                addressChainId = chainId;
                continue;
            }else if(addressChainId != chainId){
                throw new NulsException(TxErrorCode.CROSS_TX_PAYER_CHAINID_MISMATCH);
            }
        }
        return true;
    }

    /**
     * 验证交易手续费是否正确
     * @param chainId 链id
     * @param type tx type
     * @param txSize tx size
     * @param coinData
     * @return Result
     */
    private boolean validateFee(int chainId, int type, int txSize, CoinData coinData) throws NulsException {
        if(type == TxConstant.TX_TYPE_REDPUNISH){
            //红牌惩罚没有手续费
            return true;
        }
        BigInteger feeFrom = BigInteger.ZERO;
        for(CoinFrom coinFrom : coinData.getFrom()){
            feeFrom = feeFrom.add(accrueFee(type, chainId, coinFrom));
        }
        BigInteger feeTo = BigInteger.ZERO;
        for(CoinTo coinTo : coinData.getTo()){
            feeFrom = feeFrom.add(accrueFee(type, chainId, coinTo));
        }
        //交易中实际的手续费
        BigInteger fee = feeFrom.subtract(feeTo);
        if(BigIntegerUtils.isEqualOrLessThan(fee, BigInteger.ZERO)){
            Result.getFailed(TxErrorCode.INSUFFICIENT_FEE);
        }
        //根据交易大小重新计算手续费，用来验证实际手续费
        BigInteger targetFee;
        if(type == TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER){
            targetFee = TransactionFeeCalculator.getCrossTxFee(txSize);
        }else{
            targetFee = TransactionFeeCalculator.getNormalTxFee(txSize);
        }
        if(BigIntegerUtils.isLessThan(fee, targetFee)) {
            Result.getFailed(TxErrorCode.INSUFFICIENT_FEE);
        }
        return true;
    }

    /**
     * 累积计算当前coinfrom中可用于计算手续费的资产
     * @param type tx type
     * @param chainId chain id
     * @param coin coinfrom
     * @return BigInteger
     */
    private BigInteger accrueFee(int type, int chainId, Coin coin){
        BigInteger fee = BigInteger.ZERO;
        if(type == TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER){
            //为跨链交易时，只算nuls
            if(TxUtil.isNulsAsset(coin)){
                fee = fee.add(coin.getAmount());
            }
        }else{
            //不为跨链交易时，只算发起链的主资产
            if(TxUtil.isTheChainMainAsset(chainId, coin)){
                fee = fee.add(coin.getAmount());
            }
        }
        return fee;
    }

}
