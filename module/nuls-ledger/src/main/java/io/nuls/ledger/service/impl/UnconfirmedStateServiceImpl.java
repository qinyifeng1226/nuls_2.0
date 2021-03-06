/*-
 * ⁣⁣
 * MIT License
 * ⁣⁣
 * Copyright (C) 2017 - 2018 nuls.io
 * ⁣⁣
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * ⁣⁣
 */
package io.nuls.ledger.service.impl;

import io.nuls.ledger.constant.ValidateEnum;
import io.nuls.ledger.model.Uncfd2CfdKey;
import io.nuls.ledger.model.ValidateResult;
import io.nuls.ledger.model.po.AccountState;
import io.nuls.ledger.model.po.AccountStateUnconfirmed;
import io.nuls.ledger.model.po.TxUnconfirmed;
import io.nuls.ledger.service.AccountStateService;
import io.nuls.ledger.service.UnconfirmedStateService;
import io.nuls.ledger.storage.Repository;
import io.nuls.ledger.storage.UnconfirmedRepository;
import io.nuls.ledger.utils.LedgerUtil;
import io.nuls.ledger.utils.LockerUtil;
import io.nuls.ledger.utils.LoggerUtil;
import io.nuls.ledger.utils.TimeUtil;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Service;

import java.util.List;
import java.util.Map;

/**
 * Created by wangkun23 on 2018/12/4.
 * 未确认账本状态实现类
 *
 * @author lanjinsheng
 */
@Service
public class UnconfirmedStateServiceImpl implements UnconfirmedStateService {
    @Autowired
    private Repository repository;
    @Autowired
    private AccountStateService accountStateService;
    @Autowired
    private UnconfirmedRepository unconfirmedRepository;


    /**
     * 计算未确认账本信息并返回
     *
     * @param accountState
     * @return
     */
    @Override
    public AccountStateUnconfirmed getUnconfirmedInfo(AccountState accountState) {
        String key = LedgerUtil.getKeyStr(accountState.getAddress(), accountState.getAssetChainId(), accountState.getAssetId());
        AccountStateUnconfirmed accountStateUnconfirmed = unconfirmedRepository.getMemAccountStateUnconfirmed(accountState.getAddressChainId(), key);
        if (null != accountStateUnconfirmed && !accountStateUnconfirmed.isOverTime()) {
            //未确认与已确认状态一样，则未确认是最后的缓存信息
            if (LedgerUtil.equalsNonces(accountState.getNonce(), accountStateUnconfirmed.getNonce())) {
                return null;
            }
        } else {
            return null;
        }
        return accountStateUnconfirmed;
    }

    /**
     * 获取账本nonce信息
     *
     * @param accountState
     * @return
     */
    @Override
    public AccountStateUnconfirmed getUnconfirmedJustNonce(AccountState accountState) {
        String key = LedgerUtil.getKeyStr(accountState.getAddress(), accountState.getAssetChainId(), accountState.getAssetId());
        AccountStateUnconfirmed accountStateUnconfirmed = unconfirmedRepository.getMemAccountStateUnconfirmed(accountState.getAddressChainId(), key);
        if (null != accountStateUnconfirmed && !accountStateUnconfirmed.isOverTime()) {
            //未确认与已确认状态一样，则未确认是最后的缓存信息
            if (LedgerUtil.equalsNonces(accountState.getNonce(), accountStateUnconfirmed.getNonce())) {
                return null;
            }
            return accountStateUnconfirmed;
        } else {
            return null;
        }
    }

    @Override
    public void mergeUnconfirmedNonce(AccountState accountState, String assetKey, Map<String,TxUnconfirmed> txsUnconfirmed, AccountStateUnconfirmed accountStateUnconfirmed) {
        //获取未确认的列表
        synchronized (LockerUtil.getUnconfirmedAccountLocker(assetKey)) {
            try {
                AccountStateUnconfirmed accountStateUnconfirmedDB = unconfirmedRepository.getMemAccountStateUnconfirmed(accountState.getAddressChainId(), assetKey);
                if (null == accountStateUnconfirmedDB || accountStateUnconfirmedDB.isOverTime()) {
                    unconfirmedRepository.saveMemAccountStateUnconfirmed(accountState.getAddressChainId(),assetKey,accountStateUnconfirmed);
                }else{
                    accountStateUnconfirmedDB.setUnconfirmedAmount(accountStateUnconfirmedDB.getUnconfirmedAmount().add(accountStateUnconfirmed.getUnconfirmedAmount()));
                }
                unconfirmedRepository.saveMemUnconfirmedTxs(accountState.getAddressChainId(), assetKey,txsUnconfirmed);
            } catch (Exception e) {
                e.printStackTrace();
                LoggerUtil.logger(accountState.getAddressChainId()).error("@@@@mergeUnconfirmedNonce exception");
            }
        }
    }

    @Override
    public boolean rollUnconfirmedTx(int addressChainId, String assetKey, String txHash) {
        //账户处理锁
        synchronized (LockerUtil.getUnconfirmedAccountLocker(assetKey)) {
            try {
                //更新未确认上一个状态
                AccountStateUnconfirmed accountStateUnconfirmed = unconfirmedRepository.getMemAccountStateUnconfirmed(addressChainId, assetKey);
                if (null != accountStateUnconfirmed) {
                    if (LedgerUtil.equalsNonces(accountStateUnconfirmed.getNonce(), LedgerUtil.getNonceDecodeByTxHash(txHash))) {
                        TxUnconfirmed preTxUnconfirmed = unconfirmedRepository.getMemUnconfirmedTx(addressChainId, assetKey, LedgerUtil.getNonceEncode(accountStateUnconfirmed.getFromNonce()));
                        TxUnconfirmed nowTxUnconfirmed = unconfirmedRepository.getMemUnconfirmedTx(addressChainId, assetKey, LedgerUtil.getNonceEncode(accountStateUnconfirmed.getNonce()));
                        if (null != preTxUnconfirmed && (null != nowTxUnconfirmed)) {
                            accountStateUnconfirmed.setNonce(preTxUnconfirmed.getNonce());
                            accountStateUnconfirmed.setFromNonce(preTxUnconfirmed.getFromNonce());
                            accountStateUnconfirmed.setUnconfirmedAmount(accountStateUnconfirmed.getUnconfirmedAmount().subtract(nowTxUnconfirmed.getAmount()));
                            accountStateUnconfirmed.setCreateTime(TimeUtil.getCurrentTime());
                        } else {
                            //不存在上一个未确认交易，刷新数据
                            unconfirmedRepository.delMemAccountStateUnconfirmed(addressChainId, assetKey);
                        }
                    }
                }
                //删除未确认过程缓存-该笔交易之后的未确认链
                TxUnconfirmed txUnconfirmed = unconfirmedRepository.getMemUnconfirmedTx(addressChainId, assetKey, LedgerUtil.getNonceEncodeByTxHash(txHash));
                unconfirmedRepository.clearMemUnconfirmedTxs(addressChainId, assetKey, txUnconfirmed);
            } catch (Exception e) {
                e.printStackTrace();
                LoggerUtil.logger(addressChainId).error("@@@@rollUnconfirmTx exception assetKey={},txHash={}", assetKey, txHash);
            }
            return true;
        }
    }

    @Override
    public boolean existTxUnconfirmedTx(int addressChainId, String assetKey, String nonce) throws Exception {
        TxUnconfirmed txUnconfirmed = unconfirmedRepository.getMemUnconfirmedTx(addressChainId, assetKey, nonce);
        if(null != txUnconfirmed) {
            unconfirmedRepository.addUncfd2Cfd(addressChainId, assetKey, txUnconfirmed.getAmount());
        }
        return txUnconfirmed != null;
    }

    @Override
    public void clearAccountUnconfirmed(int addressChainId, String accountKey) throws Exception {
        synchronized (LockerUtil.getUnconfirmedAccountLocker(accountKey)) {
            unconfirmedRepository.delMemAccountStateUnconfirmed(addressChainId, accountKey);
        }
    }

    @Override
    public void batchDeleteUnconfirmedTx(int addressChainId, List<Uncfd2CfdKey> keys) throws Exception {
        for (Uncfd2CfdKey uncfd2CfdKey : keys) {
            unconfirmedRepository.delMemUnconfirmedTx(addressChainId, uncfd2CfdKey.getAssetKey(), uncfd2CfdKey.getNonceKey());
        }
    }


    @Override
    public ValidateResult updateUnconfirmedTx(int addressChainId, byte[] txNonce, TxUnconfirmed txUnconfirmed) {
        //账户同步锁
        String keyStr = LedgerUtil.getKeyStr(txUnconfirmed.getAddress(), txUnconfirmed.getAssetChainId(), txUnconfirmed.getAssetId());
        synchronized (LockerUtil.getUnconfirmedAccountLocker(txUnconfirmed.getAddress(), txUnconfirmed.getAssetChainId(), txUnconfirmed.getAssetId())) {
            AccountState accountState = accountStateService.getAccountState(txUnconfirmed.getAddress(), addressChainId, txUnconfirmed.getAssetChainId(), txUnconfirmed.getAssetId());
            AccountStateUnconfirmed accountStateUnconfirmed = getUnconfirmedInfo(accountState);
            byte[] preNonce = null;
            if (null == accountStateUnconfirmed) {
                //新建
                preNonce = accountState.getNonce();
            } else {
                preNonce = accountStateUnconfirmed.getNonce();
            }
            if (!LedgerUtil.equalsNonces(txUnconfirmed.getFromNonce(), preNonce)) {
                return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{txUnconfirmed.getAddress(), LedgerUtil.getNonceEncode(txUnconfirmed.getFromNonce()), "account lastNonce=" + LedgerUtil.getNonceEncode(preNonce)});
            }
            if (null == accountStateUnconfirmed) {
                accountStateUnconfirmed = new AccountStateUnconfirmed(txUnconfirmed.getAddress(), addressChainId, txUnconfirmed.getAssetChainId(), txUnconfirmed.getAssetId(),
                        txUnconfirmed.getFromNonce(), txUnconfirmed.getNonce(), txUnconfirmed.getAmount());
                unconfirmedRepository.saveMemAccountStateUnconfirmed(addressChainId, keyStr, accountStateUnconfirmed);
            } else {
                accountStateUnconfirmed.setFromNonce(txUnconfirmed.getFromNonce());
                accountStateUnconfirmed.setNonce(txUnconfirmed.getNonce());
                accountStateUnconfirmed.setUnconfirmedAmount(accountStateUnconfirmed.getUnconfirmedAmount().add(txUnconfirmed.getAmount()));
            }
            try {
                TxUnconfirmed preTxUnconfirmed = unconfirmedRepository.getMemUnconfirmedTx(addressChainId, keyStr, LedgerUtil.getNonceEncode(txUnconfirmed.getFromNonce()));
                if (null != preTxUnconfirmed) {
                    preTxUnconfirmed.setNextNonce(txUnconfirmed.getNonce());
                }
                unconfirmedRepository.saveMemUnconfirmedTx(addressChainId, keyStr, LedgerUtil.getNonceEncode(txNonce), txUnconfirmed);
            } catch (Exception e) {
                e.printStackTrace();
                return ValidateResult.getResult(ValidateEnum.FAIL_CODE, new String[]{txUnconfirmed.getAddress(), LedgerUtil.getNonceEncode(txUnconfirmed.getFromNonce()), "updateUnconfirmTx exception"});
            }
            return ValidateResult.getSuccess();
        }
    }


}


