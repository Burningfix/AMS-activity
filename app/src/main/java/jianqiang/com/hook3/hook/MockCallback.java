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
class MockCallback implements Handler.Callback {

    Handler mBase;
    private static final String TAG = "sanbo.MockCallback";

    public MockCallback(Handler base) {
        mBase = base;
    }

    @Override
    public boolean handleMessage(Message msg) {

        logi("handleMessage msg.what:" + msg.toString());
        // support android 8
        switch (msg.what) {
            // ActivityThread里面 "LAUNCH_ACTIVITY" 这个字段的值是100
            // 本来使用反射的方式获取最好, 这里为了简便直接使用硬编码
            // ActivityThread里面 "LAUNCH_ACTIVITY" 这个字段的值是100
            // 本来使用反射的方式获取最好, 这里为了简便直接使用硬编码
            case 100:   //for API 28以下
                handleLaunchActivity(msg);
                break;
            case 159:   //for API 28
                handleActivity(msg);
                break;
        }
        mBase.handleMessage(msg);
        return true;
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
                Intent target = intent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
                intent.setComponent(target.getComponent());
            }
        }
    }

    private void handleLaunchActivity(Message msg) {
        // 这里简单起见,直接取出TargetActivity;
        Object obj = msg.obj;

        // 把替身恢复成真身
        Intent intent = (Intent) RefInvoke.getFieldObject(obj, "intent");

        logi("handleLaunchActivity before  intent:" + intent.toString());
        Intent targetIntent = intent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
        intent.setComponent(targetIntent.getComponent());
        logi("handleLaunchActivity after intent:" + intent.toString());

    }

    private void logi(String info) {
        Log.i(TAG, info);
    }
}
