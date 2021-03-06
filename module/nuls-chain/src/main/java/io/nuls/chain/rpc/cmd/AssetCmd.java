package io.nuls.chain.rpc.cmd;

import io.nuls.base.basic.AddressTool;
import io.nuls.base.data.CoinData;
import io.nuls.base.data.Transaction;
import io.nuls.chain.config.NulsChainConfig;
import io.nuls.chain.info.CmErrorCode;
import io.nuls.chain.info.CmRuntimeInfo;
import io.nuls.chain.model.dto.AccountBalance;
import io.nuls.chain.model.po.Asset;
import io.nuls.chain.model.po.BlockChain;
import io.nuls.chain.model.tx.AddAssetToChainTransaction;
import io.nuls.chain.model.tx.DestroyAssetAndChainTransaction;
import io.nuls.chain.model.tx.RemoveAssetFromChainTransaction;
import io.nuls.chain.service.AssetService;
import io.nuls.chain.service.ChainService;
import io.nuls.chain.rpc.call.RpcService;
import io.nuls.chain.util.LoggerUtil;
import io.nuls.chain.util.TimeUtil;
import io.nuls.core.rpc.model.CmdAnnotation;
import io.nuls.core.rpc.model.Parameter;
import io.nuls.core.rpc.model.message.Response;
import io.nuls.core.core.annotation.Autowired;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.model.ByteUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

/**
 * @author tangyi
 * @date 2018/11/9
 * @description
 */
@Component
public class AssetCmd extends BaseChainCmd {

    @Autowired
    private AssetService assetService;
    @Autowired
    private ChainService chainService;
    @Autowired
    private RpcService rpcService;
    @Autowired
    private NulsChainConfig nulsChainConfig;

    @CmdAnnotation(cmd = "cm_asset", version = 1.0, description = "asset")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "assetId", parameterType = "int", parameterValidRange = "[1,65535]")
    public Response asset(Map params) {
        try {
            int chainId = Integer.valueOf(params.get("chainId").toString());
            int assetId = Integer.valueOf(params.get("assetId").toString());
            Asset asset = assetService.getAsset(CmRuntimeInfo.getAssetKey(chainId, assetId));
            return success(asset);
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
            return failed(e.getMessage());
        }
    }

    /**
     * 资产注册
     */
    @CmdAnnotation(cmd = "cm_assetReg", version = 1.0, description = "assetReg")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "assetId", parameterType = "int", parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "symbol", parameterType = "array")
    @Parameter(parameterName = "assetName", parameterType = "String")
    @Parameter(parameterName = "initNumber", parameterType = "String")
    @Parameter(parameterName = "decimalPlaces", parameterType = "short", parameterValidRange = "[1,128]")
    @Parameter(parameterName = "address", parameterType = "String")
    @Parameter(parameterName = "password", parameterType = "String")
    public Response assetReg(Map params) {
        try {
            /* 组装Asset (Asset object) */
            Asset asset = new Asset();
            asset.map2pojo(params);
            asset.setDepositNuls(new BigInteger(nulsChainConfig.getAssetDepositNuls()));
            asset.setAvailable(true);
            if (assetService.assetExist(asset)) {
                return failed(CmErrorCode.ERROR_ASSET_ID_EXIST);
            }
            /* 组装交易发送 (Send transaction) */
            Transaction tx = new AddAssetToChainTransaction();
            tx.setTxData(asset.parseToTransaction());
            tx.setTime(TimeUtil.getCurrentTime());
            AccountBalance accountBalance = rpcService.getCoinData(String.valueOf(params.get("address")));
            CoinData coinData = this.getRegCoinData(asset.getAddress(), CmRuntimeInfo.getMainIntChainId(),
                    CmRuntimeInfo.getMainIntAssetId(), String.valueOf(asset.getDepositNuls()), tx.size(), accountBalance, nulsChainConfig.getAssetDepositNulsLockRate());
            tx.setCoinData(coinData.serialize());

            /* 判断签名是否正确 (Determine if the signature is correct) */
            rpcService.transactionSignature( CmRuntimeInfo.getMainIntChainId(), (String) params.get("address"), (String) params.get("password"), tx);
            /* 发送到交易模块 (Send to transaction module) */
            return rpcService.newTx(tx) ? success("Sent asset transaction success") : failed("Sent asset transaction failed");
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
            return failed(e.getMessage());
        }
    }

    @CmdAnnotation(cmd = "cm_assetDisable", version = 1.0, description = "assetDisable")
    @Parameter(parameterName = "chainId", parameterType = "int", parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "assetId", parameterType = "int", parameterValidRange = "[1,65535]")
    @Parameter(parameterName = "address", parameterType = "String")
    @Parameter(parameterName = "password", parameterType = "String")
    public Response assetDisable(Map params) {
        try {
            int chainId = Integer.parseInt(params.get("chainId").toString());
            int assetId = Integer.parseInt(params.get("assetId").toString());
            byte[] address = AddressTool.getAddress(params.get("address").toString());
            /* 身份的校验，账户地址的校验 (Verification of account address) */
            Asset asset = assetService.getAsset(CmRuntimeInfo.getAssetKey(chainId, assetId));
            if (asset == null) {
                return failed(CmErrorCode.ERROR_ASSET_NOT_EXIST);
            }

            if (!ByteUtils.arrayEquals(asset.getAddress(), address)) {
                return failed(CmErrorCode.ERROR_ASSET_NOT_EXIST);
            }

            /*
              判断链下是否只有这一个资产了，如果是，则进行带链注销交易
              Judging whether there is only one asset under the chain, and if so, proceeding with the chain cancellation transaction
             */
            BlockChain dbChain = chainService.getChain(chainId);
            Transaction tx;
            if (dbChain.getSelfAssetKeyList().size() == 1
                    && dbChain.getSelfAssetKeyList().get(0).equals(CmRuntimeInfo.getAssetKey(chainId, asset.getAssetId()))) {
                /* 注销资产和链 (Destroy assets and chains) */
                tx = new DestroyAssetAndChainTransaction();
                try {
                    tx.setTxData(dbChain.parseToTransaction(asset));
                } catch (IOException e) {
                    LoggerUtil.logger().error(e);
                    return failed("parseToTransaction fail");
                }
            } else {
                /* 只注销资产 (Only destroy assets) */
                tx = new RemoveAssetFromChainTransaction();
                try {
                    tx.setTxData(asset.parseToTransaction());
                } catch (IOException e) {
                    LoggerUtil.logger().error(e);
                    return failed("parseToTransaction fail");
                }
            }
            AccountBalance accountBalance = rpcService.getCoinData(String.valueOf(params.get("address")));
            if (null == accountBalance) {
                return failed("get  rpc CoinData fail.");
            }
            CoinData coinData = this.getDisableCoinData(address, chainId, assetId, String.valueOf(asset.getDepositNuls()), tx.size(),
                    dbChain.getRegTxHash(), accountBalance, nulsChainConfig.getAssetDepositNulsLockRate());
            tx.setCoinData(coinData.serialize());

            /* 判断签名是否正确 (Determine if the signature is correct) */
            rpcService.transactionSignature(asset.getChainId(), (String) params.get("address"), (String) params.get("password"), tx);

            /* 发送到交易模块 (Send to transaction module) */
            return rpcService.newTx(tx) ? success("assetDisable success") : failed("assetDisable failed");
        } catch (Exception e) {
            LoggerUtil.logger().error(e);
            return failed("parseToTransaction fail");
        }
    }
}
