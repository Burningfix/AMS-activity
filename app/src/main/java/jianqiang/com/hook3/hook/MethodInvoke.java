package jianqiang.com.hook3.hook;

import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jianqiang.com.hook3.StubActivity;

class MethodInvoke implements InvocationHandler {

    private static final String TAG = "sanbo.MockClass1";

    Object mBase;

    public MethodInvoke(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            logi("------MockClass1---[invoke]----method :" + method.getName() + "\r\n\t method:" + method);
//            ------MockClass1---[invoke]----method :
//                  public abstract void android.app.IActivityManager.setRenderThread(int) throws android.os.RemoteException
//            ------MockClass2---[handleMessage]----
//               msg :{ when=-369ms what=159 obj=ClientTransaction TopResumedActivityChangeItem, hashCode,
//                              mActivityToken = android.os.BinderProxy@cfebd55 mLifecycleStateRequest null
//                              mActivityCallbacks [TopResumedActivityChangeItem{onTop=true}] target=android.app.ActivityThread$H }
            if ("startActivity".equals(method.getName())) {
                // 只拦截这个方法
                // 替换参数, 任你所为;甚至替换原始Activity启动别的Activity偷梁换柱

                // 找到参数里面的第一个Intent 对象
                Intent raw;
                int index = 0;

                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        index = i;
                        break;
                    }
                }
                raw = (Intent) args[index];

                Intent newIntent = new Intent();

                // 替身Activity的包名, 也就是我们自己的包名
                String stubPackage = raw.getComponent().getPackageName();

                // 这里我们把启动的Activity临时替换为 StubActivity
                ComponentName componentName = new ComponentName(stubPackage, StubActivity.class.getName());
                newIntent.setComponent(componentName);

                // 把我们原始要启动的TargetActivity先存起来
                newIntent.putExtra(AMSHookHelper.EXTRA_TARGET_INTENT, raw);

                // 替换掉Intent, 达到欺骗AMS的目的
                args[index] = newIntent;

                logi("hook success");
                return method.invoke(mBase, args);

            }else if("checkPermission".equals(method.getName())){
                loge("返回类型："+method.getReturnType());
                return 1;

            }
        } catch (Throwable e) {
            loge(Log.getStackTraceString(e));
        }

        return method.invoke(mBase, args);
    }


    private static void loge(String info) {
        Log.println(Log.ERROR, TAG, info);
    }

    private static void logi(String info) {
        Log.println(Log.INFO, TAG, info);
    }
}