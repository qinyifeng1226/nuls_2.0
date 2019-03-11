/*
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.cmd.client.processor.account;


import com.fasterxml.jackson.core.JsonProcessingException;
import io.nuls.api.provider.Result;
import io.nuls.api.provider.ServiceManager;
import io.nuls.api.provider.account.AccountService;
import io.nuls.api.provider.account.facade.AccountInfo;
import io.nuls.api.provider.account.facade.GetAccountByAddressReq;
import io.nuls.api.provider.ledger.LedgerProvider;
import io.nuls.api.provider.ledger.facade.AccountBalanceInfo;
import io.nuls.api.provider.ledger.facade.GetBalanceReq;
import io.nuls.cmd.client.CommandBuilder;
import io.nuls.cmd.client.CommandHelper;
import io.nuls.cmd.client.CommandResult;
import io.nuls.cmd.client.Config;
import io.nuls.cmd.client.processor.CommandProcessor;
import io.nuls.tools.core.annotation.Autowired;
import io.nuls.tools.core.annotation.Component;
import io.nuls.tools.data.StringUtils;
import io.nuls.tools.log.Log;
import io.nuls.tools.parse.JSONUtils;
import io.nuls.tools.parse.MapUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author: zhoulijun
 */
@Component
public class GetAccountProcessor implements CommandProcessor {

    AccountService accountService = ServiceManager.get(AccountService.class);

    LedgerProvider ledgerProvider = ServiceManager.get(LedgerProvider.class);

    @Autowired
    Config config;

    @Override
    public String getCommand() {
        return "getaccount";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription())
                .newLine("\t<address> the account address - Required");
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return "getaccount <address> --get account information";
    }

    @Override
    public boolean argsValidate(String[] args) {
        if (args.length != 2) {
            return false;
        }
        if (!CommandHelper.checkArgsIsNull(args)) {
            return false;
        }
        if (StringUtils.isBlank(args[1])) {
            return false;
        }
        return true;
    }

    @Override
    public CommandResult execute(String[] args) {
        String address = args[1];
        Result<AccountInfo> info = accountService.getAccountByAddress(new GetAccountByAddressReq(address));
        if (info.isFailed()) {
            return CommandResult.getFailed(info);
        }
        Result<AccountBalanceInfo> balance = ledgerProvider.getBalance(new GetBalanceReq(config.getAssetsId(),config.getChainId(),address));
        if (balance.isFailed()) {
            return CommandResult.getFailed(balance);
        }
        Map<String,Object> res = new HashMap<>(7);
        res.putAll(MapUtils.beanToMap(info.getData()));
        res.put("baglance",balance.getData());
        try {
            return CommandResult.getSuccess(JSONUtils.obj2PrettyJson(res));
        } catch (JsonProcessingException e) {
            Log.error("",e);
            return null;
        }
    }
}
