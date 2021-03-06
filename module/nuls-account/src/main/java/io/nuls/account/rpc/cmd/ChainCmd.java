package io.nuls.account.rpc.cmd;

import io.nuls.account.service.ChainService;
import io.nuls.core.rpc.cmd.BaseCmd;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.basic.Result;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;

import java.util.Map;

/**
 * 管理链的运行和停止接口
 * Management chain run and stop interface
 * @author: qinyifeng
 * @date: 2018/12/13
 */
@Component
public class ChainCmd extends BaseCmd {

    @Autowired
    private ChainService chainService;

    /**
     * 停止一条子链
     * stop a subchain
     */
    @CmdAnnotation(cmd = "tx_stopChain", version = 1.0, description = "stop a subchain")
    public Response stopChain(Map<String, Object> params) {
        Result result = chainService.stopChain(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 运行一条子链
     * run a subchain
     */
    @CmdAnnotation(cmd = "tx_runChain", version = 1.0, description = "run a subchain")
    public Response runChain(Map<String, Object> params) {
        Result result = chainService.runChain(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

    /**
     * 启动主链
     * run the main chain
     */
    @CmdAnnotation(cmd = "tx_runMainChain", version = 1.0, description = "run the main chain")
    public Response runMainChain(Map<String, Object> params) {
        Result result = chainService.runMainChain(params);
        if (result.isFailed()) {
            return failed(result.getErrorCode());
        }
        return success(result.getData());
    }

}
