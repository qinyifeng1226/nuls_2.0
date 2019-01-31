package io.nuls.transaction.service.impl;

import io.nuls.base.data.BlockHeader;
import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Service;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.exception.NulsRuntimeException;
import io.nuls.transaction.cache.PackablePool;
import io.nuls.transaction.constant.TxConstant;
import io.nuls.transaction.constant.TxErrorCode;
import io.nuls.transaction.db.h2.dao.TransactionH2Service;
import io.nuls.transaction.db.rocksdb.storage.ConfirmedTxStorageService;
import io.nuls.transaction.db.rocksdb.storage.CtxStorageService;
import io.nuls.transaction.db.rocksdb.storage.UnconfirmedTxStorageService;
import io.nuls.transaction.manager.ChainManager;
import io.nuls.transaction.manager.TransactionManager;
import io.nuls.transaction.model.bo.Chain;
import io.nuls.transaction.model.bo.TxRegister;
import io.nuls.transaction.model.bo.VerifyTxResult;
import io.nuls.transaction.rpc.call.ChainCall;
import io.nuls.transaction.rpc.call.LedgerCall;
import io.nuls.transaction.rpc.call.NetworkCall;
import io.nuls.transaction.rpc.call.TransactionCall;
import io.nuls.transaction.service.ConfirmedTxService;
import io.nuls.transaction.service.CtxService;
import io.nuls.transaction.utils.TransactionIndexComparator;
import io.nuls.transaction.utils.TxUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: Charlie
 * @date: 2018/11/30
 */
@Service
public class ConfirmedTxServiceImpl implements ConfirmedTxService {

    @Autowired
    private ConfirmedTxStorageService confirmedTxStorageService;

    @Autowired
    private UnconfirmedTxStorageService unconfirmedTxStorageService;

    @Autowired
    private TransactionManager transactionManager;

    @Autowired
    private ChainManager chainManager;

    @Autowired
    private TransactionIndexComparator txIndexComparator;

    @Autowired
    private CtxStorageService ctxStorageService;

    @Autowired
    private PackablePool packablePool;

    @Autowired
    private CtxService ctxService;

    @Autowired
    private TransactionH2Service transactionH2Service;

    @Override
    public Transaction getConfirmedTransaction(Chain chain, NulsDigestData hash) {
        if (null == hash) {
            return null;
        }
        return confirmedTxStorageService.getTx(chain.getChainId(), hash);
    }

    private boolean saveTx(Chain chain, Transaction tx) {
        if (null == tx) {
            throw new NulsRuntimeException(TxErrorCode.PARAMETER_ERROR);
        }
        chain.getLogger().debug("saveConfirmedTx: " + tx.getHash().getDigestHex());
        return confirmedTxStorageService.saveTx(chain.getChainId(), tx);
    }

    @Override
    public boolean saveGengsisTxList(Chain chain, List<Transaction> txhexList, String blockHeaderHex) throws NulsException {
        if (null == chain || txhexList == null || txhexList.size() == 0) {
            throw new NulsException(TxErrorCode.PARAMETER_ERROR);
        }
        LedgerCall.coinDataBatchNotify(chain);
        List<NulsDigestData> txHashList = new ArrayList<>();
        for (Transaction tx : txhexList) {
            txHashList.add(tx.getHash());
            //todo 批量验证coinData，接口和单个的区别？
            VerifyTxResult verifyTxResult = LedgerCall.verifyCoinData(chain, tx, true);
            if (!verifyTxResult.success()) {
                return false;
            }
        }
        if (saveTxList(chain, txHashList, blockHeaderHex)) {
            chain.getLogger().debug("保存创世块交易失败");
            return false;
        }
        for (Transaction tx : txhexList) {
            //保存到h2数据库
            transactionH2Service.saveTxs(TxUtil.tx2PO(tx));
        }
        return true;
    }

    /**
     * 1.保存交易
     * 2.调提交易接口
     * 3.调账本
     * 4.从未打包交易库中删除交易
     */
    @Override
    public boolean saveTxList(Chain chain, List<NulsDigestData> txHashList, String blockHeaderHex) throws NulsException {
        if (null == chain || txHashList == null || txHashList.size() == 0) {
            throw new NulsException(TxErrorCode.PARAMETER_ERROR);
        }
        List<Transaction> txList = new ArrayList<>();
        List<String> txHexList = new ArrayList<>();
        int chainId = chain.getChainId();
        List<byte[]> txHashs = new ArrayList<>();
        //组装统一验证参数数据,key为各模块统一验证器cmd
        Map<TxRegister, List<String>> moduleVerifyMap = new HashMap<>(TxConstant.INIT_CAPACITY_16);
        try {
            for (int i = 0; i < txHashList.size(); i++) {
                NulsDigestData hash = txHashList.get(i);
                txHashs.add(hash.serialize());
                Transaction tx = unconfirmedTxStorageService.getTx(chainId, hash);
                String txHex = tx.hex();
                txHexList.add(txHex);
                TxRegister txRegister = transactionManager.getTxRegister(chain, tx.getType());
                if (moduleVerifyMap.containsKey(txRegister)) {
                    moduleVerifyMap.get(txRegister).add(txHex);
                } else {
                    List<String> txHexs = new ArrayList<>();
                    txHexs.add(txHex);
                    moduleVerifyMap.put(txRegister, txHexs);
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }

        if (!saveTxs(chain, txList, true)) {
            return false;
        }
        if (!commitTxs(chain, moduleVerifyMap, blockHeaderHex, true)) {
            removeTxs(chain, txList, false);
            return false;
        }
        if (!commitLedger(chain, txHexList)) {
            rollbackTxs(chain, moduleVerifyMap, blockHeaderHex, false);
            removeTxs(chain, txList, false);
            return false;
        }
        //如果确认交易成功，则从未打包交易库中删除交易
        unconfirmedTxStorageService.removeTxList(chainId, txHashs);
        return true;
    }


    //保存交易
    public boolean saveTxs(Chain chain, List<Transaction> txList, boolean atomicity) {
        List<Transaction> savedList = new ArrayList<>();
        for (Transaction tx : txList) {
            if (saveTx(chain, tx)) {
                savedList.add(tx);
            } else {
                if(atomicity) {
                    removeTxs(chain, savedList, false);
                }
                break;
            }
        }
        return false;
    }

    //调提交易
    public boolean commitTxs(Chain chain, Map<TxRegister, List<String>> moduleVerifyMap, String blockHeaderHex, boolean atomicity) {
        //调用交易模块统一验证器 批量
        Map<TxRegister, List<String>> successed = new HashMap<>(TxConstant.INIT_CAPACITY_16);
        boolean result = true;
        for (Map.Entry<TxRegister, List<String>> entry : moduleVerifyMap.entrySet()) {
            boolean rs = TransactionCall.txProcess(chain, entry.getKey().getCommit(),
                    entry.getKey().getModuleCode(), entry.getValue(), blockHeaderHex);
            if (!rs) {
                result = false;
                break;
            }
            successed.put(entry.getKey(), entry.getValue());
        }
        if (!result && atomicity) {
            rollbackTxs(chain, successed, blockHeaderHex, false);
            return false;
        }
        return true;
    }

    //提交账本
    public boolean commitLedger(Chain chain, List<String> txHexList) {
        try {
            return LedgerCall.commitTxLedger(chain, txHexList, true);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
    }

    public boolean removeTxs(Chain chain, List<Transaction> txList, boolean atomicity) {
        List<Transaction> successedList = new ArrayList<>();
        for (Transaction tx : txList) {
            if (confirmedTxStorageService.removeTx(chain.getChainId(), tx.getHash())) {
                successedList.add(tx);
            } else {
                if(atomicity){
                    saveTxs(chain, successedList, false);
                }
            }
        }
        return false;
    }

    public boolean rollbackTxs(Chain chain, Map<TxRegister, List<String>> moduleVerifyMap, String blockHeaderHex, boolean atomicity) {
        Map<TxRegister, List<String>> successed = new HashMap<>(TxConstant.INIT_CAPACITY_16);
        boolean result = true;
        for (Map.Entry<TxRegister, List<String>> entry : moduleVerifyMap.entrySet()) {
            boolean rs = TransactionCall.txProcess(chain, entry.getKey().getRollback(),
                    entry.getKey().getModuleCode(), entry.getValue(), blockHeaderHex);
            if (!rs) {
                result = false;
                break;
            }
            successed.put(entry.getKey(), entry.getValue());
        }
        if (!result && atomicity) {
            commitTxs(chain, successed, blockHeaderHex, false);
            return false;
        }
        return true;
    }

    public boolean rollbackLedger(Chain chain, List<String> txHexList) {
        try {
            return LedgerCall.rollbackTxLedger(chain, txHexList, true);
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
    }


    @Override
    public boolean rollbackTxList(Chain chain, List<NulsDigestData> txHashList, String blockHeaderHex) throws NulsException {
        if (null == chain || txHashList == null || txHashList.size() == 0) {
            throw new NulsException(TxErrorCode.PARAMETER_ERROR);
        }
        int chainId = chain.getChainId();
        List<byte[]> txHashs = new ArrayList<>();
        List<Transaction> txList = new ArrayList<>();
        List<String> txHexList = new ArrayList<>();
        //组装统一验证参数数据,key为各模块统一验证器cmd
        Map<TxRegister, List<String>> moduleVerifyMap = new HashMap<>(TxConstant.INIT_CAPACITY_16);
        try {
            for (int i = 0; i < txHashList.size(); i++) {
                NulsDigestData hash = txHashList.get(i);
                txHashs.add(hash.serialize());
                Transaction tx = confirmedTxStorageService.getTx(chainId, hash);
                String txHex = tx.hex();
                txHexList.add(txHex);
                TxRegister txRegister = transactionManager.getTxRegister(chain, tx.getType());
                if (moduleVerifyMap.containsKey(txRegister)) {
                    moduleVerifyMap.get(txRegister).add(txHex);
                } else {
                    List<String> txHexs = new ArrayList<>();
                    txHexs.add(txHex);
                    moduleVerifyMap.put(txRegister, txHexs);
                }
            }
        } catch (Exception e) {
            chain.getLogger().error(e);
            return false;
        }


        if (!rollbackLedger(chain, txHexList)) {
            return false;
        }
        if (!rollbackTxs(chain, moduleVerifyMap, blockHeaderHex, true)) {
            commitLedger(chain, txHexList);
            return false;
        }
        if (!removeTxs(chain, txList, true)) {
            commitTxs(chain, moduleVerifyMap, blockHeaderHex, false);
            saveTxs(chain, txList, false);
            return false;
        }

        //放入未确认库, 和待打包队列
        for (Transaction tx : txList) {
            unconfirmedTxStorageService.putTx(chain.getChainId(), tx);
            savePackable(chain, tx);
        }
        return true;
    }

    /**
     * 重新放回待打包队列的最前端
     *
     * @param chain chain
     * @param tx    Transaction
     * @return boolean
     */
    private boolean savePackable(Chain chain, Transaction tx) {
        //不是系统交易则重新放回待打包队列的最前端
        if (!transactionManager.isSystemTx(chain, tx)) {
            return packablePool.addInFirst(chain, tx, false);

        }
        return true;
    }

    /**
     * 保存交易时对跨链交易进行处理, 包括跨链交易变更状态, 向链管理提交跨链交易coinData, 记录hash
     *
     * @param chain
     * @param tx
     * @return
     */
    private boolean ctxCommit(Chain chain, Transaction tx, List<NulsDigestData> ctxhashList) {
        //跨链交易变更状态
        if (null != ctxhashList) {
            ctxhashList.add(tx.getHash());
        }
        String coinDataHex = HexUtil.encode(tx.getCoinData());
        try {
            boolean rs = ChainCall.ctxChainLedgerCommit(coinDataHex);
            if (!rs) {
                throw new NulsException(TxErrorCode.CALLING_REMOTE_INTERFACE_FAILED);
            }
            return true;
        } catch (NulsException e) {
            if (null != ctxhashList) {
                ctxhashList.remove(tx.getHash());
            }
            chain.getLogger().error(e);
            return false;
        }
    }

    /**
     * 回滚交易时对跨链交易进行处理, 包括跨链交易变更状态, 通知链管理回滚coinData
     *
     * @param chain
     * @param tx
     * @return
     */
    private boolean ctxRollback(Chain chain, Transaction tx) {
        String coinDataHex = HexUtil.encode(tx.getCoinData());
        try {
            boolean rs = ChainCall.ctxChainLedgerRollback(coinDataHex);
            if (!rs) {
                throw new NulsException(TxErrorCode.CALLING_REMOTE_INTERFACE_FAILED);
            }
            return true;
        } catch (NulsException e) {
            chain.getLogger().error(e);
            return false;
        }
    }



    @Override
    public void processEffectCrossTx(Chain chain, long blockHeight) throws NulsException {
        int chainId = chain.getChainId();
        List<NulsDigestData> hashList = confirmedTxStorageService.getCrossTxEffectList(chainId, blockHeight);
        for (NulsDigestData hash : hashList) {
            Transaction tx = confirmedTxStorageService.getTx(chainId, hash);
            if (null == tx) {
                chain.getLogger().error(TxErrorCode.TX_NOT_EXIST.getMsg() + ": " + hash.toString());
                continue;
            }
            if (tx.getType() != TxConstant.TX_TYPE_CROSS_CHAIN_TRANSFER) {
                chain.getLogger().error(TxErrorCode.TX_TYPE_ERROR.getMsg() + ": " + hash.toString());
                continue;
            }
            //跨链转账交易接收者链id
            int toChainId = TxUtil.getCrossTxTosOriginChainId(tx);

            /*
                如果当前链是主网
                    1.需要对接收者链进行账目金额增加
                    2a.如果是交易收款方,则需要向发起链发送回执? todo
                    2b.如果不是交易收款方广播给收款方链
                如果当前链是交易发起链
                    1.广播给主网
             */
            if (chainId == TxConstant.NULS_CHAINID) {
                if (toChainId == chainId) {
                    //todo 已到达目标链发送回执
                } else {
                    //广播给 toChainId 链的节点
                    NetworkCall.broadcastTxHash(toChainId, tx.getHash());
                }
            } else {
                //广播给 主网 链的节点
                NetworkCall.broadcastTxHash(TxConstant.NULS_CHAINID, tx.getHash());
            }
        }
    }
}
