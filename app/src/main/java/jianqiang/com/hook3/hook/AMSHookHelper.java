package jianqiang.com.hook3.hook;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import jianqiang.com.hook3.RefInvoke;

/**
 * @author weishu
 * @date 16/3/21
 */
public class AMSHookHelper {
    public static final String EXTRA_TARGET_INTENT = "extra_target_intent";
    private static final String TAG = "sanbo.AMSHookHelper";



    /**
     * Hook AMS
     * 主要完成的操作是  "把真正要启动的Activity临时替换为在AndroidManifest.xml中声明的替身Activity",进而骗过AMS
     */
    public static void hookAMN() throws ClassNotFoundException,
            NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, NoSuchFieldException {

        try {
            //获取AMN的gDefault单例gDefault，gDefault是final静态的
            Object gDefault = null;
            if (Build.VERSION.SDK_INT >= 29) {
                gDefault = RefInvoke.getStaticFieldObject("android.app.ActivityTaskManager", "IActivityTaskManagerSingleton");
            } else if (Build.VERSION.SDK_INT >= 26) {
                gDefault = RefInvoke.getStaticFieldObject("android.app.ActivityManager", "IActivityManagerSingleton");
            } else {
                gDefault = RefInvoke.getStaticFieldObject("android.app.ActivityManagerNative", "gDefault");
            }
            logi("hookAMN gDefault: " + gDefault);

            // gDefault是一个 android.util.Singleton<T>对象; 我们取出这个单例里面的mInstance字段
            Object mInstance = RefInvoke.getFieldObject("android.util.Singleton", gDefault, "mInstance");
            logi("hookAMN mInstance: " + mInstance);

            // 创建一个这个对象的代理对象MockClass1, 然后替换这个字段, 让我们的代理对象帮忙干活
            Class<?> classB2Interface = Class.forName("android.app.IActivityManager");
            Object proxy = Proxy.newProxyInstance(
                    Thread.currentThread().getContextClassLoader(),
                    new Class<?>[]{classB2Interface},
                    new IActivityInvocationHandler(mInstance));
            logi("hookAMN proxy: " + proxy);

            //把gDefault的mInstance字段，修改为proxy
            Class class1 = gDefault.getClass();
            RefInvoke.setFieldObject("android.util.Singleton", gDefault, "mInstance", proxy);
            logi("hookAMN success~~ " );

        } catch (Throwable e) {
            loge(Log.getStackTraceString(e));
        }
    }

    private static void loge(String info) {
        Log.println(Log.ERROR, TAG, info);
    }

    private static void logi(String info) {
        Log.println(Log.INFO, TAG, info);
    }

    /**
     * 由于之前我们用替身欺骗了AMS; 现在我们要换回我们真正需要启动的Activity
     * 不然就真的启动替身了, 狸猫换太子...
     * 到最终要启动Activity的时候,会交给ActivityThread 的一个内部类叫做 H 来完成
     * H 会完成这个消息转发; 最终调用它的callback
     */
    public static void hookActivityThread() throws Exception {

        try {
            // 先获取到当前的ActivityThread对象
            //  private static volatile ActivityThread sCurrentActivityThread;
//            Object currentActivityThread = RefInvoke.invokeStaticMethod("android.app.ActivityThread", "currentActivityThread");
            Object currentActivityThread = RefInvoke.getStaticFieldObject("android.app.ActivityThread", "sCurrentActivityThread");
            logi("hookActivityThread currentActivityThread: " + currentActivityThread);

            // 由于ActivityThread一个进程只有一个,我们获取这个对象的mH
            Handler mH = (Handler) RefInvoke.getFieldObject(currentActivityThread, "mH");
            logi("hookActivityThread mH: " + mH);

            //把Handler的mCallback字段，替换为new MockClass2(mH)
            if (Build.VERSION.SDK_INT >= 28) {
                RefInvoke.setFieldObject(Handler.class, mH, "mCallback", new ActivityThreadMessageHookP(mH));
            } else {
                RefInvoke.setFieldObject(Handler.class, mH, "mCallback", new ActivityThreadMessageHook(mH));
            }
            logi("hookActivityThread  success~ ");

        } catch (Throwable e) {
            loge(Log.getStackTraceString(e));
        }
    }

    /**
     * 1.处理未注册的Activity为AppCompatActivity类或者子类的情况
     * 2.hook IPackageManager,处理android 4.3以下(<= 18)启动Activity,在ApplicationPackageManager.getActivityInfo方法中未找到注册的Activity的异常
     *
     *
     * http://weishu.me/2016/03/07/understand-plugin-framework-ams-pms-hook/
     *
     * @param context          context
     */
    public static void hookPackageManager(Context context) {

        try {
            // 1.获取ActivityThread的值
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
            currentActivityThreadMethod.setAccessible(true);
            Object currentActivityThread = currentActivityThreadMethod.invoke(null);
            logi("hookPackageManager currentActivityThread: " + currentActivityThread);

            // 2.获取ActivityThread里面原始的 sPackageManager
            // static IPackageManager sPackageManager;
            Field sPackageManagerField = activityThreadClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object sPackageManager = sPackageManagerField.get(currentActivityThread);
            logi("hookPackageManager sPackageManager: " + sPackageManager);

            // 3.准备好代理对象, 用来替换原始的对象
            Class<?> iPackageManagerInterface = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(iPackageManagerInterface.getClassLoader(),
                    new Class<?>[]{iPackageManagerInterface},
                    new IPackageManagerHookHandler(sPackageManager));
            logi("hookPackageManager proxy: " + proxy);

            // 4.替换掉ActivityThread里面的 sPackageManager 字段
            sPackageManagerField.set(currentActivityThread, proxy);
            // 5.替换 ApplicationPackageManager里面的 mPM对象
            PackageManager packageManager=    context.getPackageManager();
            // PackageManager的实现类ApplicationPackageManager中的mPM
            // private final IPackageManager mPM;
            RefInvoke.setFieldObject(packageManager,"mPM",proxy);

            logi("hookPackageManager  success~ ");
        } catch ( Throwable e) {
            e.printStackTrace();
        }
    }

}
