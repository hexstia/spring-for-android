package org.springframework.aop;

import android.util.Log;

import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

public class Advice {
    public void test(){
        Log.i("Advice","test aop module");
        GenericKeyedObjectPoolConfig  config = new GenericKeyedObjectPoolConfig();
        config.setMaxIdlePerKey(3);
        int maxIdlePerKey = config.getMaxIdlePerKey();
        Log.i("Advice","tmaxIdlePerKey :"+maxIdlePerKey);

    }
}
