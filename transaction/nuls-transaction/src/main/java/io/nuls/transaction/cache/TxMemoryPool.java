package io.nuls.transaction.cache;

import io.nuls.base.data.NulsDigestData;
import io.nuls.base.data.Transaction;
import io.nuls.tools.cache.LimitHashMap;
import io.nuls.transaction.constant.TxConstant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 交易已完成交易管理模块的校验
 * @author: Charlie
 * @date: 2018/11/13
 */
public class TxMemoryPool {

    private final static TxMemoryPool INSTANCE = new TxMemoryPool();

    private Queue<Transaction> txQueue;

    private LimitHashMap<NulsDigestData, Transaction> orphanContainer;

    private TxMemoryPool() {
        this.txQueue = new LinkedBlockingDeque<>();
        this.orphanContainer = new LimitHashMap(TxConstant.ORPHAN_CONTAINER_MAX_SIZE);
    }

    public static TxMemoryPool getInstance() {
        return INSTANCE;
    }

    public boolean addInFirst(Transaction tx, boolean isOrphan) {
        try {
            if (tx == null) {
                return false;
            }
            //check Repeatability
            if (isOrphan) {
                NulsDigestData hash = tx.getHash();
                orphanContainer.put(hash, tx);
            } else {
                ((LinkedBlockingDeque) txQueue).addFirst(tx);
            }
            return true;
        } finally {
        }
    }

    public boolean add(Transaction tx, boolean isOrphan) {
        try {
            if (tx == null) {
                return false;
            }
            //check Repeatability
            if (isOrphan) {
                NulsDigestData hash = tx.getHash();
                orphanContainer.put(hash, tx);
            } else {
                txQueue.offer(tx);
            }
            return true;
        } finally {
        }
    }

    /**
     * Get a TxContainer, the first TxContainer received, removed from the memory pool after acquisition
     * <p>
     * 获取一笔交易，最先存入的交易，获取之后从内存池中移除
     *
     * @return TxContainer
     */
    public Transaction get() {
        return txQueue.poll();
    }

    public List<Transaction> getAll() {
        List<Transaction> txs = new ArrayList<>();
        Iterator<Transaction> it = txQueue.iterator();
        while (it.hasNext()) {
            txs.add(it.next());
        }
        return txs;
    }

    public List<Transaction> getAllOrphan() {
        return new ArrayList<>(orphanContainer.values());
    }

    public void remove(NulsDigestData hash) {
        orphanContainer.remove(hash);
    }

    public boolean exist(NulsDigestData hash) {
        return orphanContainer.containsKey(hash);
    }

    public void clear() {
        try {
            txQueue.clear();
            orphanContainer.clear();
        } finally {
        }
    }

    public int size() {
        return txQueue.size();
    }

    public int getPoolSize() {
        return txQueue.size();
    }

    public int getOrphanPoolSize() {
        return orphanContainer.size();
    }

    public void removeOrphan(NulsDigestData hash) {
        this.orphanContainer.remove(hash);
    }

}
