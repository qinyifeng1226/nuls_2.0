package io.nuls.test.cases.transcation;

import io.nuls.api.provider.Result;
import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.account.AccountService;
import io.nuls.api.provider.account.facade.AccountInfo;
import io.nuls.api.provider.account.facade.GetAccountByAddressReq;
import io.nuls.test.cases.Constants;
import io.nuls.test.cases.TestCaseIntf;
import io.nuls.test.cases.TestFailException;
import io.nuls.tools.core.annotation.Component;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-21 14:56
 * @Description: 功能描述
 *
 */
@Component
public class CheckAliasCase implements TestCaseIntf<String,String> {

    AccountService accountService = ServiceManager.get(AccountService.class);

    @Override
    public String title() {
        return "别名是否设置成功";
    }

    @Override
    public String doTest(String address, int depth) throws TestFailException {
        AccountInfo accountInfo = accountService.getAccountByAddress(new GetAccountByAddressReq(address)).getData();
        if(!Constants.getAlias(address).equals(accountInfo.getAlias())){
            throw new TestFailException("账户别名不符合预期");
        }
        return address;
    }
}
