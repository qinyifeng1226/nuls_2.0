/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.constant;

/**
 * 存储对外提供的接口命令
 *
 * @author captain
 * @version 1.0
 * @date 18-11-9 下午2:15
 */
public interface CommandConstant {

    //根据区块高度获取区块
    String GET_BLOCK_BY_HEIGHT = "getBlockByHeight";
    //根据区块hash获取区块
    String GET_BLOCK_BY_HASH = "downloadBlockByHash";
    //获取账户余额
    String GET_BALANCE = "getBalanceNonce";
    //获取账户锁定列表
    String GET_FREEZE = "getFreezeList";

    //查询交易详情
    String GET_TX = "tx_getTxClient";
    //交易验证
    String TX_VALIEDATE = "tx_verifyTx";
    //新交易确认并广播
    String TX_NEWTX = "tx_newTx";
    //查询节点详情
    String GET_AGENT = "cs_getAgentInfo";
    //获取共识种子节点配置
    String GET_CONSENSUS_CONFIG = "cs_getSeedNodeList";
    //查询智能合约详情
    String CONTRACT_INFO = "sc_contract_info";
    //查询智能合约执行结果
    String CONTRACT_RESULT = "sc_contract_result";
}
