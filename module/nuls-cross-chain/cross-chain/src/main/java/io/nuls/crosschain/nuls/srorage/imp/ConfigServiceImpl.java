package io.nuls.crosschain.nuls.srorage.imp;

import io.nuls.crosschain.nuls.model.bo.config.ConfigBean;
import io.nuls.crosschain.nuls.srorage.ConfigService;
import io.nuls.core.rockdb.model.Entry;
import io.nuls.core.rockdb.service.RocksDBService;
import io.nuls.core.core.annotation.Service;
import io.nuls.core.log.Log;
import io.nuls.core.model.ByteUtils;
import io.nuls.core.model.ObjectUtils;
import static io.nuls.crosschain.nuls.constant.NulsCrossChainConstant.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置信息存储管理类
 * Configuration Information Storage Management Class
 *
 * @author tag
 * 2018/11/8
 * */
@Service
public class ConfigServiceImpl implements ConfigService {
    @Override
    public boolean save(ConfigBean bean, int chainID) throws Exception{
        if(bean == null){
            return  false;
        }
        return RocksDBService.put(DB_NAME_CONSUME_CONGIF, ByteUtils.intToBytes(chainID), ObjectUtils.objectToBytes(bean));
    }

    @Override
    public ConfigBean get(int chainID) {
        try {
            byte[] value = RocksDBService.get(DB_NAME_CONSUME_CONGIF,ByteUtils.intToBytes(chainID));
            return ObjectUtils.bytesToObject(value);
        }catch (Exception e){
            Log.error(e);
            return null;
        }
    }

    @Override
    public boolean delete(int chainID) {
        try {
            return RocksDBService.delete(DB_NAME_CONSUME_CONGIF,ByteUtils.intToBytes(chainID));
        }catch (Exception e){
            Log.error(e);
            return  false;
        }
    }

    @Override
    public Map<Integer, ConfigBean> getList() {
        try {
            List<Entry<byte[], byte[]>> list = RocksDBService.entryList(DB_NAME_CONSUME_CONGIF);
            Map<Integer, ConfigBean> configBeanMap = new HashMap<>(INIT_CAPACITY);
            for (Entry<byte[], byte[]>entry:list) {
                int key = ByteUtils.bytesToInt(entry.getKey());
                ConfigBean value = ObjectUtils.bytesToObject(entry.getValue());
                configBeanMap.put(key,value);
            }
            return configBeanMap;
        }catch (Exception e){
            Log.error(e);
            return null;
        }
    }
}
