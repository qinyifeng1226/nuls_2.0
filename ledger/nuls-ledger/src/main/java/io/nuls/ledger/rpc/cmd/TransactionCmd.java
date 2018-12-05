package io.nuls.ledger.rpc.cmd;

import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.data.Transaction;
import io.nuls.ledger.service.TransactionService;
import io.nuls.rpc.cmd.BaseCmd;
import io.nuls.rpc.model.CmdAnnotation;
import io.nuls.rpc.model.message.Response;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.crypto.HexUtil;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.exception.NulsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Created by wangkun23 on 2018/11/20.
 */
@Component
public class TransactionCmd extends BaseCmd {

    final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TransactionService transactionService;

    /**
     * save pendingState transaction
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "lg_tx",
            version = 1.0, scope = "private", minEvent = 0, minPeriod = 0,
            description = "test lg_tx 1.0")
    public Object saveUnConfirmTx(Map params) {
        String txHex = (String) params.get("value");
        if (StringUtils.isNotBlank(txHex)) {
            return failed("txHex not blank");
        }
        byte[] txStream = HexUtil.decode(txHex);
        Transaction tx = new Transaction();
        try {
            tx.parse(new NulsByteBuffer(txStream));
        } catch (NulsException e) {
            logger.error("transaction parse error", e);
        }
        transactionService.txProcess(tx);
        return success();
    }

    /**
     * delete pendingState transaction
     *
     * @param params
     * @return
     */
    @CmdAnnotation(cmd = "lg_deleteUnConfirmTx",
            version = 1.0, scope = "private", minEvent = 0, minPeriod = 0,
            description = "test getHeight 1.0")
    public Response deleteTransaction(List params) {
        return success("hash");
    }
}
