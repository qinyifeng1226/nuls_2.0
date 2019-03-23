/**
 * MIT License
 * <p>
 * Copyright (c) 2017-2018 nuls.io
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.contract.tx;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.base.data.BlockHeader;
import io.nuls.contract.basetest.ContractTest;
import io.nuls.contract.rpc.call.BlockCall;
import io.nuls.contract.tx.base.Base;
import io.nuls.contract.util.ContractUtil;
import io.nuls.rpc.model.ModuleE;
import io.nuls.rpc.model.message.Response;
import io.nuls.rpc.netty.processor.ResponseMessageProcessor;
import io.nuls.tools.exception.NulsException;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static io.nuls.contract.constant.ContractCmdConstant.*;

/**
 * @author: PierreLuo
 * @date: 2019-03-15
 */
public class ContractQueryTest extends Base {

    @Test
    public void getBlockHeader() throws NulsException, JsonProcessingException {
        BlockHeader blockHeader = BlockCall.getBlockHeader(chainId, 20L);
        Log.info("\nstateRoot is " + Hex.toHexString(ContractUtil.getStateRoot(blockHeader)) + ", " + blockHeader.toString());
    }

    /**
     *  账户创建的合约列表
     */
    @Test
    public void accountContracts() throws Exception {
        Map params = this.makeAccountContractsParams(sender, 1, 10);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, ACCOUNT_CONTRACTS, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(ACCOUNT_CONTRACTS));
        Assert.assertTrue(null != result);
        Log.info("accountContracts-result:{}", JSONUtils.obj2PrettyJson(cmdResp2));
    }
    private Map makeAccountContractsParams(String address, int pageNumber, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("address", address);
        params.put("pageNumber", pageNumber);
        params.put("pageSize", pageSize);
        return params;
    }


    /**
     * 预创建合约
     */
    @Test
    public void preCreateContract() throws Exception {
        InputStream in = new FileInputStream(ContractTest.class.getResource("/nrc20").getFile());
        byte[] contractCode = IOUtils.toByteArray(in);
        String remark = "create contract test - 空气币";
        String name = "KQB";
        String symbol = "KongQiBi";
        String amount = BigDecimal.TEN.pow(10).toPlainString();
        String decimals = "2";
        Map params = this.makePreCreateParams(sender, contractCode, remark, name, symbol, amount, decimals);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, PRE_CREATE, params);
        Log.info("pre_create-Response:{}", JSONUtils.obj2PrettyJson(cmdResp2));
        Assert.assertTrue(cmdResp2.isSuccess());
    }
    private Map makePreCreateParams(String sender, byte[] contractCode, String remark, Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("sender", sender);
        params.put("password", password);
        params.put("gasLimit", 200000L);
        params.put("price", 25);
        params.put("contractCode", Hex.toHexString(contractCode));
        params.put("args", args);
        params.put("remark", remark);
        return params;
    }


    /**
     * 估算创建合约的gas
     */
    @Test
    public void imputedCreateGas() throws Exception {
        InputStream in = new FileInputStream(ContractTest.class.getResource("/nrc20").getFile());
        byte[] contractCode = IOUtils.toByteArray(in);
        String name = "KQB";
        String symbol = "KongQiBi";
        String amount = BigDecimal.TEN.pow(10).toPlainString();
        String decimals = "2";
        Map params = this.makeImputedCreateGasParams(sender, contractCode, name, symbol, amount, decimals);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, IMPUTED_CREATE_GAS, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(IMPUTED_CREATE_GAS));
        Assert.assertTrue(null != result);
        Log.info("imputed_create_gas-result:{}", JSONUtils.obj2PrettyJson(result));
    }
    private Map makeImputedCreateGasParams(String sender, byte[] contractCode, Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("sender", sender);
        params.put("contractCode", Hex.toHexString(contractCode));
        params.put("args", args);
        return params;
    }

    /**
     * 验证创建合约
     */
    @Test
    public void validateCreate() throws Exception {
        InputStream in = new FileInputStream(ContractTest.class.getResource("/nrc20").getFile());
        byte[] contractCode = IOUtils.toByteArray(in);
        String name = "KQB";
        String symbol = "KongQiBi";
        String amount = BigDecimal.TEN.pow(10).toPlainString();
        String decimals = "2";
        Map params = this.makeValidateCreateParams(sender, contractCode, name, symbol, amount, decimals);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, VALIDATE_CREATE, params);
        Log.info("validate_create-Response:{}", JSONUtils.obj2PrettyJson(cmdResp2));
        Assert.assertTrue(cmdResp2.isSuccess());
    }
    private Map makeValidateCreateParams(String sender, byte[] contractCode, Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("sender", sender);
        params.put("gasLimit", 200000L);
        params.put("price", 25);
        params.put("contractCode", Hex.toHexString(contractCode));
        params.put("args", args);
        return params;
    }


    /**
     * 验证调用合约
     */
    @Test
    public void validateCall() throws Exception {
        BigInteger value = BigInteger.ZERO;
        String methodName = "transfer";
        String methodDesc = "";
        String token = BigInteger.TEN.pow(8).toString();
        Map params = this.makeValidateCallParams(sender, value, contractAddress, methodName, methodDesc, toAddress, token);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, VALIDATE_CALL, params);
        Log.info("validateCall-Response:{}", JSONUtils.obj2PrettyJson(cmdResp2));
        Assert.assertTrue(cmdResp2.isSuccess());
    }
    private Map makeValidateCallParams(String sender, BigInteger value, String contractAddress, String methodName, String methodDesc, Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("sender", sender);
        params.put("value", value);
        params.put("gasLimit", 200000L);
        params.put("price", 25);
        params.put("contractAddress", contractAddress);
        params.put("methodName", methodName);
        params.put("methodDesc", methodDesc);
        params.put("args", args);
        return params;
    }

    /**
     * 估算调用合约的gas
     */
    @Test
    public void imputedCallGas() throws Exception {
        BigInteger value = BigInteger.ZERO;
        String methodName = "transfer";
        String methodDesc = "";
        String token = BigInteger.TEN.pow(8).toString();
        Map params = this.makeImputedCallGasParams(sender, value, contractAddress, methodName, methodDesc, toAddress, token);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, IMPUTED_CALL_GAS, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(IMPUTED_CALL_GAS));
        Assert.assertTrue(null != result);
        Log.info("imputed_call_gas-result:{}", JSONUtils.obj2PrettyJson(result));
    }
    private Map makeImputedCallGasParams(String sender, BigInteger value, String contractAddress, String methodName, String methodDesc, Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("sender", sender);
        params.put("value", value);
        params.put("contractAddress", contractAddress);
        params.put("methodName", methodName);
        params.put("methodDesc", methodDesc);
        params.put("args", args);
        return params;
    }


    /**
     *  获取向合约地址转账的手续费
     */
    @Test
    public void transfer2ContractFee() throws Exception {
        BigInteger value = BigInteger.TEN.pow(8);
        String remark = "transfer 2 contract fee";
        Map params = this.makeTransferFeeParams(sender, contractAddress, value, remark);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, TRANSFER_FEE, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(TRANSFER_FEE));
        Assert.assertTrue(null != result);
        Log.info("transfer2ContractFee-result:{}", JSONUtils.obj2PrettyJson(result));
    }
    private Map makeTransferFeeParams(String address, String toAddress, BigInteger amount, String remark) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("address", address);
        params.put("toAddress", toAddress);
        params.put("amount", amount);
        params.put("remark", remark);
        return params;
    }


    /**
     *  token余额
     */
    @Test
    public void tokenBalance() throws Exception {
        Map params = this.makeTokenBalanceParams(contractAddress_nrc20, toAddress1);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, TOKEN_BALANCE, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(TOKEN_BALANCE));
        Assert.assertTrue(null != result);
        Log.info("tokenBalance-result:{}", JSONUtils.obj2PrettyJson(result));
    }
    private Map makeTokenBalanceParams(String contractAddress, String address) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("contractAddress", contractAddress);
        params.put("address", address);
        return params;
    }

    /**
     *  账户token资产
     */
    @Test
    public void tokenAssetsList() throws Exception {
        Map params = this.makeTokenAssetsListParams(sender, 1, 10);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, TOKEN_ASSETS_LIST, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(TOKEN_ASSETS_LIST));
        Assert.assertTrue(null != result);
        Log.info("tokenAssetsList-result:{}", JSONUtils.obj2PrettyJson(cmdResp2));
    }
    private Map makeTokenAssetsListParams(String address, int pageNumber, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("address", address);
        params.put("pageNumber", pageNumber);
        params.put("pageSize", pageSize);
        return params;
    }

    /**
     *  账户token交易列表
     */
    @Test
    public void tokenTransferList() throws Exception {
        Map params = this.makeTokenTransferListParams(sender, 1, 10);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, TOKEN_TRANSFER_LIST, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(TOKEN_TRANSFER_LIST));
        Assert.assertTrue(null != result);
        Log.info("tokenAssetsList-result:{}", JSONUtils.obj2PrettyJson(result));
    }
    private Map makeTokenTransferListParams(String address, int pageNumber, int pageSize) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("address", address);
        params.put("pageNumber", pageNumber);
        params.put("pageSize", pageSize);
        return params;
    }


    /**
     * 调用合约视图方法
     */
    @Test
    public void invokeView() throws Exception {
        String methodName = "balanceOf";
        String methodDesc = "";
        Map params = this.makeInvokeViewParams(contractAddress, methodName, methodDesc, toAddress);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, INVOKE_VIEW, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(INVOKE_VIEW));
        Assert.assertTrue(null != result);
        Log.info("invoke_view-result:{}", JSONUtils.obj2PrettyJson(cmdResp2));
    }
    private Map makeInvokeViewParams(String contractAddress, String methodName, String methodDesc, Object... args) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("contractAddress", contractAddress);
        params.put("methodName", methodName);
        params.put("methodDesc", methodDesc);
        params.put("args", args);
        return params;
    }

    /**
     * 获取合约构造函数
     */
    @Test
    public void constructor() throws Exception {
        InputStream in = new FileInputStream(ContractTest.class.getResource("/nrc20").getFile());
        byte[] contractCode = IOUtils.toByteArray(in);
        Map params = this.makeConstructorParams(contractCode);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, CONSTRUCTOR, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(CONSTRUCTOR));
        Assert.assertTrue(null != result);
        Log.info("constructor-result:{}", JSONUtils.obj2PrettyJson(result));
    }
    private Map makeConstructorParams(byte[] contractCode) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("contractCode", Hex.toHexString(contractCode));
        return params;
    }

    /**
     * 获取合约基本信息
     */
    @Test
    public void contractInfo() throws Exception {
        Map params = this.makeContractInfoParams(contractAddress);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, CONTRACT_INFO, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(CONTRACT_INFO));
        Assert.assertTrue(null != result);
        Log.info("contract_info-result:{}", JSONUtils.obj2PrettyJson(cmdResp2));
    }
    private Map makeContractInfoParams(String contractAddress) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("contractAddress", contractAddress);
        return params;
    }

    /**
     * 获取合约执行结果
     */
    @Test
    public void contractResult() throws Exception {
        Map params = this.makeContractResultParams(callHash);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, CONTRACT_RESULT, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(CONTRACT_RESULT));
        Assert.assertTrue(null != result);
        Log.info("contractResult-result:{}", JSONUtils.obj2PrettyJson(result));
    }
    private Map makeContractResultParams(String hash) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("hash", hash);
        return params;
    }

    /**
     * 获取合约交易详情
     */
    @Test
    public void contractTx() throws Exception {
        Map params = this.makeContractTxParams(deleteHash);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, CONTRACT_TX, params);
        Map result = (HashMap) (((HashMap) cmdResp2.getResponseData()).get(CONTRACT_TX));
        Assert.assertTrue(null != result);
        Log.info("contractTx-result:{}", JSONUtils.obj2PrettyJson(cmdResp2));
    }
    private Map makeContractTxParams(String hash) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("hash", hash);
        return params;
    }

    /**
     *  验证删除合约
     */
    @Test
    public void validateDelete() throws Exception {
        Map params = this.makeValidateDeleteParams(sender, contractAddress);
        Response cmdResp2 = ResponseMessageProcessor.requestAndResponse(ModuleE.SC.abbr, VALIDATE_DELETE, params);
        Log.info("validateDelete-Response:{}", JSONUtils.obj2PrettyJson(cmdResp2));
        Assert.assertTrue(cmdResp2.isSuccess());
    }
    private Map makeValidateDeleteParams(String sender, String contractAddress) {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("sender", sender);
        params.put("contractAddress", contractAddress);
        return params;
    }

    /**
     * 查交易
     */
    @Test
    public void getTxRecord() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("address", sender);
        params.put("assetChainId", null);
        params.put("assetId", null);
        params.put("type", null);
        params.put("pageSize", null);
        params.put("pageNumber", null);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getTxs", params);
        Map record = (Map) dpResp.getResponseData();
        Log.info("Page<TransactionPO>:{}", JSONUtils.obj2PrettyJson(record));
    }

    /**
     * 查交易模块的交易
     */
    @Test
    public void getTx() throws Exception {
        this.getTxClient(callHash);
    }
    private void getTxClient(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("txHash", hash);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getTxClient", params);
        Map record = (Map) dpResp.getResponseData();
        Log.info("{}", JSONUtils.obj2PrettyJson(record));
    }

    /**
     * 查交易模块的确认交易
     */
    @Test
    public void getConfirmTx() throws Exception {
        this.getTxCfmClient(callHash);
    }
    private void getTxCfmClient(String hash) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("chainId", chainId);
        params.put("txHash", hash);
        Response dpResp = ResponseMessageProcessor.requestAndResponse(ModuleE.TX.abbr, "tx_getConfirmedTxClient", params);
        Map record = (Map) dpResp.getResponseData();
        Log.info(JSONUtils.obj2PrettyJson(record));
    }

}
