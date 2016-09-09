package com.acp.shell;

/**
 * Created by alexey on 16-8-23.
 */

public class NDKUtils {
    static {
        System.loadLibrary("ZwcDecryptUtils");
    }
    //从so库中获取AES解密的秘钥
    public static native String getKeyFormC();
}
