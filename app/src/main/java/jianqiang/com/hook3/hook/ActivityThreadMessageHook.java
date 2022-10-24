package jianqiang.com.hook3.hook;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;

import jianqiang.com.hook3.RefInvoke;

/**
 * @author weishu
 * @date 16/1/7
 */
class ActivityThreadMessageHook implements Handler.Callback {

    private static final String TAG = "sanbo.MockClass2";
    Handler mBase;

    public ActivityThreadMessageHook(Handler base) {
        mBase = base;
    }

    @Override
    public boolean handleMessage(Message msg) {

        try {
            logi("------MockClass2---[handleMessage]----msg :" +msg.what+"\r\n\tmsg:"+msg);
            switch (msg.what) {
                // ActivityThread里面 "LAUNCH_ACTIVITY" 这个字段的值是100
                // 本来使用反射的方式获取最好, 这里为了简便直接使用硬编码
                case 100:   //for API 28以下版本
                    handleLaunchActivity(msg);
                    break;
                case 159:   //for API 28
                    handleActivity(msg);
                    break;

            }
        } catch (Throwable e) {
            loge(Log.getStackTraceString(e));
        }

        mBase.handleMessage(msg);
        return true;
    }

    private void handleLaunchActivity(Message msg) {
        // 这里简单起见,直接取出TargetActivity;
        Object obj = msg.obj;

        // 把替身恢复成真身
        Intent intent = (Intent) RefInvoke.getFieldObject(obj, "intent");
        logi("handleLaunchActivity before intent:" + intent);

        Intent targetIntent = intent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
        intent.setComponent(targetIntent.getComponent());
        logi("handleLaunchActivity end intent:" + intent);

    }

    private void handleActivity(Message msg) {
        // 这里简单起见,直接取出TargetActivity;
        Object obj = msg.obj;

        List<Object> mActivityCallbacks = (List<Object>) RefInvoke.getFieldObject(obj, "mActivityCallbacks");
        logi("handleActivity msg:" + msg.toString());
        logi("handleActivity mActivityCallbacks:" + mActivityCallbacks.toString());
        if (mActivityCallbacks.size() > 0) {
            String className = "android.app.servertransaction.LaunchActivityItem";
            if (mActivityCallbacks.get(0).getClass().getCanonicalName().equals(className)) {
                Object object = mActivityCallbacks.get(0);
                Intent intent = (Intent) RefInvoke.getFieldObject(object, "mIntent");
                Intent targetIntent = intent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
                intent.setComponent(targetIntent.getComponent());
            }
        }
    }

    private static void loge(String info) {
        Log.println(Log.ERROR, TAG, info);
    }

    private static void logi(String info) {
        Log.println(Log.INFO, TAG, info);
    }
}
