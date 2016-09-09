package com.acp.shell;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.DexClassLoader;

public class ProxyApplication extends Application {
    private int lenDex;
    private int lenOriginDex;
    private String encryZipFileName;
    private String origZipFileName;

    protected AssetManager mAssetManager;// 资源管理器
    protected Resources mResources;// 资源
    protected Theme mTheme;// 主题

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            Log.i("shellDex", "开始解壳");
            File dexLoadFile = this.getDir("payload_odex", MODE_PRIVATE);
            encryZipFileName = dexLoadFile.getAbsolutePath() + "/encryZip.apk";
            origZipFileName = dexLoadFile.getAbsolutePath() + "/originZip.apk";
            File encryDexFile = new File(encryZipFileName);
            File origDexFile = new File(origZipFileName);

            if (!encryDexFile.exists() && !origDexFile.exists()) {
                // 在payload_odex文件夹内，创建shellClasses.dex文件
                encryDexFile.createNewFile();
                // 读取分离程序壳的classes.dex文件
                readDexFileFromApk();
            }

            if (!origDexFile.exists()) {
                // 解密出解壳后的dex文件已用于动态加载
                AESHelper helper = new AESHelper();
                boolean relsut = helper.decrypt(encryZipFileName, origZipFileName);
                Log.d("shellDex", "解密结果 = " + relsut);
            }
//			encryDexFile.delete();
            // 配置动态加载环境
            // 获取主线程对象
            Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread",
                    "currentActivityThread", new Class[]{}, new Object[]{});

            String packageName = this.getPackageName();// 当前apk的包名

            Map mPackages = (Map) RefInvoke.getFieldOjbect("android.app.ActivityThread",
                    currentActivityThread, "mPackages");
            WeakReference wr = (WeakReference) mPackages.get(packageName);
            // 创建被加壳apk的DexClassLoader对象 加载apk内的类和本地代码
            ClassLoader parentClassLoader = (ClassLoader) RefInvoke.getFieldOjbect("android.app.LoadedApk", wr.get(), "mClassLoader");
            String libPath  =  "/data/data/"+packageName+"/lib";
            DexClassLoader dLoader = new DexClassLoader(origZipFileName, dexLoadFile.getAbsolutePath(), libPath, parentClassLoader);

            // 把当前进程的DexClassLoader 设置成了被加壳apk的DexClassLoader
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mClassLoader", wr.get(), dLoader);
            try {
                Object actObj = dLoader.loadClass("com.aicai.pluginhost.activity.LaunchActivity");
            } catch (Exception e) {
                Log.i("shellDex", "activity:" + Log.getStackTraceString(e));
            }

        } catch (Exception e) {
            Log.i("shellDex", "error:" + Log.getStackTraceString(e));
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        {
            // loadResources(apkFileName);

            Log.i("shellDex", "onCreate");
            // 如果源应用配置有Appliction对象，则替换为源应用Applicaiton，以便不影响源程序逻辑。
            String appClassName = null;
            try {
                ApplicationInfo ai = this.getPackageManager().getApplicationInfo(this.getPackageName(),
                        PackageManager.GET_META_DATA);
                Bundle bundle = ai.metaData;
                if (bundle != null && bundle.containsKey("APPLICATION_CLASS_NAME")) {
                    appClassName = bundle.getString("APPLICATION_CLASS_NAME");// className
                    // 是配置在xml文件中的。
                } else {
                    Log.i("shellDex", "have no application class name");
                    return;
                }
            } catch (NameNotFoundException e) {
                Log.i("shellDex", "error:" + Log.getStackTraceString(e));
                e.printStackTrace();
            }
            // 有值的话调用该Applicaiton
            Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread",
                    "currentActivityThread", new Class[]{}, new Object[]{});
            Object mBoundApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,
                    "mBoundApplication");
            Object loadedApkInfo = RefInvoke.getFieldOjbect("android.app.ActivityThread$AppBindData", mBoundApplication,
                    "info");
            // 把当前进程的mApplication 设置成了null
            RefInvoke.setFieldOjbect("android.app.LoadedApk", "mApplication", loadedApkInfo, null);
            Object oldApplication = RefInvoke.getFieldOjbect("android.app.ActivityThread", currentActivityThread,
                    "mInitialApplication");
            // http://www.codeceo.com/article/android-context.html
            ArrayList<Application> mAllApplications = (ArrayList<Application>) RefInvoke
                    .getFieldOjbect("android.app.ActivityThread", currentActivityThread, "mAllApplications");
            mAllApplications.remove(oldApplication);// 删除oldApplication

            ApplicationInfo appinfo_In_LoadedApk = (ApplicationInfo) RefInvoke.getFieldOjbect("android.app.LoadedApk",
                    loadedApkInfo, "mApplicationInfo");
            ApplicationInfo appinfo_In_AppBindData = (ApplicationInfo) RefInvoke
                    .getFieldOjbect("android.app.ActivityThread$AppBindData", mBoundApplication, "appInfo");
            appinfo_In_LoadedApk.className = appClassName;
            appinfo_In_AppBindData.className = appClassName;
            Application app = (Application) RefInvoke.invokeMethod("android.app.LoadedApk", "makeApplication",
                    loadedApkInfo, new Class[]{boolean.class, Instrumentation.class}, new Object[]{false, null});// 执行
            // makeApplication（false,null）
            RefInvoke.setFieldOjbect("android.app.ActivityThread", "mInitialApplication", currentActivityThread, app);

            Map mProviderMap = (Map) RefInvoke.getFieldOjbect("android.app.ActivityThread",
                    currentActivityThread, "mProviderMap");
            Iterator it = mProviderMap.values().iterator();
            while (it.hasNext()) {
                try{
                    Object providerClientRecord = it.next();
                    Object localProvider = RefInvoke.getFieldOjbect("android.app.ActivityThread$ProviderClientRecord",
                            providerClientRecord, "mLocalProvider");
                    RefInvoke.setFieldOjbect("android.content.ContentProvider", "mContext", localProvider, app);
                }catch(Exception e){
                    Log.d("shellDex","替换mProviderMap出错  "+e.getStackTrace().toString());
                }

            }

            Log.i("shellDex", "app:" + app);

            app.onCreate();
        }
    }

    /**
     * 从apk包里面获取dex文件内容（byte）
     *
     * @return
     * @throws IOException
     */
    private void readDexFileFromApk() throws IOException {
        ByteArrayOutputStream dexByteArrayOutputStream = new ByteArrayOutputStream();
        ZipInputStream localZipInputStream = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(this.getApplicationInfo().sourceDir)));
        while (true) {
            ZipEntry localZipEntry = localZipInputStream.getNextEntry();
            if (localZipEntry == null) {
                localZipInputStream.close();
                break;
            }
            if (localZipEntry.getName().equals("classes.dex")) {
                byte[] arrayOfByte = new byte[1024];
                while (true) {
                    int i = localZipInputStream.read(arrayOfByte);
                    if (i == -1)
                        break;
                    dexByteArrayOutputStream.write(arrayOfByte, 0, i);
                }
            }
            localZipInputStream.closeEntry();
        }
        localZipInputStream.close();

        byte[] dexByteArr = dexByteArrayOutputStream.toByteArray();
        // 读取壳dex的长度，和被加壳dex的长度
        lenDex = dexByteArr.length;
        byte[] dexLenByArr = new byte[4];
        System.arraycopy(dexByteArr, lenDex - 4, dexLenByArr, 0, 4);
        ByteArrayInputStream bais = new ByteArrayInputStream(dexLenByArr);
        DataInputStream in = new DataInputStream(bais);
        lenOriginDex = in.readInt();
        bais.close();
        // 保存shelDex到本地
        FileOutputStream localFileOutputStream = new FileOutputStream(encryZipFileName);
        localFileOutputStream.write(dexByteArr, lenDex - 4 - lenOriginDex, lenOriginDex);
        localFileOutputStream.flush();
        localFileOutputStream.close();
        dexByteArrayOutputStream.close();
    }

//    protected void loadResources(String dexPath) {
//        try {
//            AssetManager assetManager = AssetManager.class.newInstance();
//            Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
//            addAssetPath.invoke(assetManager, dexPath);
//            mAssetManager = assetManager;
//        } catch (Exception e) {
//            Log.i("shellDex", "loadResource error:" + Log.getStackTraceString(e));
//            e.printStackTrace();
//        }
//        Resources superRes = super.getResources();
//        superRes.getDisplayMetrics();
//        superRes.getConfiguration();
//        mResources = new Resources(mAssetManager, superRes.getDisplayMetrics(), superRes.getConfiguration());
//        mTheme = mResources.newTheme();
//        mTheme.setTo(super.getTheme());
//    }
//
//    @Override
//    public AssetManager getAssets() {
//        return mAssetManager == null ? super.getAssets() : mAssetManager;
//    }
//
//    @Override
//    public Resources getResources() {
//        return mResources == null ? super.getResources() : mResources;
//    }
//
//    @Override
//    public Theme getTheme() {
//        return mTheme == null ? super.getTheme() : mTheme;
//    }
}
