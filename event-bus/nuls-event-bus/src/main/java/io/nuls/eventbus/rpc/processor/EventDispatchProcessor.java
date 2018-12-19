package io.nuls.eventbus.rpc.processor;

import io.nuls.eventbus.constant.EbConstants;
import io.nuls.eventbus.model.Subscriber;
import io.nuls.eventbus.runtime.EventBusRuntime;

import io.nuls.tools.log.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author naveen
 */
public class EventDispatchProcessor implements Runnable {

    @Override
    public void run() {
        try{
            Log.info("Processing the published event starts..");
            Object[] objects = EventBusRuntime.firstObjArrInEventDispatchQueue();
            if(null != objects){
                Object data = objects[0];
                Set<Subscriber> subscribers =(Set<Subscriber>) objects[1];
                for (Subscriber subscriber : subscribers){
                    Map<String,Object> params = new HashMap<>(1);
                    params.put("data",data);
                    EventBusRuntime.SEND_AND_RETRY_QUEUE.offer(new Object[]{subscriber,params});
                    EbConstants.SEND_RETRY_THREAD_POOL.execute(new SendRetryProcessor());
                }
                Log.info("Processing the published event Ends..");
            }
        }catch (Exception e){
            Log.error(e);
        }
    }
}
