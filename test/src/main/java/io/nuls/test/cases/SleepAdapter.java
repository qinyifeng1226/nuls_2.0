package io.nuls.test.cases;

import io.nuls.tools.core.annotation.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author: zhoulijun
 * @Time: 2019-03-21 10:50
 * @Description: 功能描述
 */
@Component
public abstract class SleepAdapter implements TestCaseIntf<Object,Object> {


    public abstract int sleepSec();


    @Override
    public String title() {
        return "等待"+this.sleepSec()+"秒";
    }

    @Override
    public Object doTest(Object param, int depth) throws TestFailException {
        try {
            for (int j = 1; j <= this.sleepSec(); j++) {
                System.out.print(j + " ");
                TimeUnit.SECONDS.sleep(1L);
            }
            System.out.println();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return param;
    }

    @Override
    public CaseType caseType() {
        return CaseType.Adapter;
    }
//
//    public static Class<? extends SleepAdapter> sleepSec(int sec){
//        return new SleepAdapter(){
//            @Override
//            public int sleepSec() {
//                return sec;
//            }
//        }.getClass();
//    }

    @Component
    public static class $10SEC extends SleepAdapter {

        @Override
        public int sleepSec() {
            return 10;
        }
    }

}
