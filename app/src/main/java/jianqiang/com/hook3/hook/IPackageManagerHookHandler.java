package jianqiang.com.hook3.hook;

import android.content.ComponentName;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import jianqiang.com.hook3.app;

public class IPackageManagerHookHandler implements InvocationHandler {
    private Object mBase;

    public IPackageManagerHookHandler(Object base) {
        mBase = base;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Log.i("sanbo","IPackageManagerHookHandler method:"+method);
        // public android.content.pm.ActivityInfo getActivityInfo(android.content.ComponentName className, int flags, int userId)
        if (method.getName().equals("getPackageInfo")) {
            int index = 0;
            for (Object obj : args) {

            }
            for (int i = 0; i < args.length; i++) {
                Object obj = args[i];
                if (obj instanceof ComponentName) {
                    index = i;
                    break;
                }
            }
            args[index] = app.getTarget();
        }

        // https://blog.csdn.net/skeeing/article/details/96122977
        // https://stackoverflow.com/questions/41774450/why-is-kotlin-throw-illegalargumentexception-when-using-proxy
        // * is also used to pass an array to a vararg parameter
        // method!!.invoke(worker, *(args ?: arrayOfNulls<Any>(0)))
        return method.invoke(mBase, args);

    }
}
