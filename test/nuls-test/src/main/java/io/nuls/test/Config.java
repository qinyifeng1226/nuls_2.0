package io.nuls.test;

import io.nuls.base.api.provider.Result;
import io.nuls.base.api.provider.ServiceManager;
import io.nuls.base.api.provider.account.AccountService;
import io.nuls.base.api.provider.account.facade.ImportAccountByPrivateKeyReq;
import io.nuls.test.cases.Constants;
import io.nuls.core.basic.InitializingBean;
import io.nuls.core.core.annotation.Configuration;
import io.nuls.core.core.annotation.Value;
import io.nuls.core.exception.NulsException;
import lombok.Data;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-20 14:31
 * @Description: 功能描述
 */
@Configuration(domain = "test")
@Data
public class Config implements InitializingBean {

    AccountService accountService = ServiceManager.get(AccountService.class);

    @Value("testNodeExclude")
    String nodeExclude;

    @Value("testSeedAccountPriKey")
    String testSeedAccount;

    @Value("testNodeType")
    String nodeType;

    String testNodeList;

    int testNodeCount;

    int chainId;

    int assetsId;

    String seedAddress;

    String packetMagic;

    @Override
    public void afterPropertiesSet() throws NulsException {
        Result<String> result = accountService.importAccountByPrivateKey(new ImportAccountByPrivateKeyReq(Constants.PASSWORD,testSeedAccount,true));
        this.seedAddress = result.getData();
    }

    public boolean isMaster(){
        return "master".equals(nodeType);
    }

}
