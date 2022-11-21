package jianqiang.com.hook3.hook;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author weishu
 * @date 16/1/7
 */
class ActivityThreadMessageHookP implements Handler.Callback {

    private static final String TAG = "sanbo.MockClass2";
    Handler mBase;

    public ActivityThreadMessageHookP(Handler base) {
        mBase = base;
    }

    @Override
    public boolean handleMessage(Message msg) {

        logi("------ActivityThreadMessageHookP---[handleMessage]-msg :" + msg.what + "\r\n\tmsg:" + msg);

        // android.app.ActivityThread$H.EXECUTE_TRANSACTION = 159
        // android 9.0反射,Accessing hidden field Landroid/app/ActivityThread$H;->EXECUTE_TRANSACTION:I (dark greylist, reflection)
        // android9.0 深灰名单（dark greylist）则debug版本在会弹出dialog提示框，在release版本会有Toast提示，均提示为"Detected problems with API compatibility"
        if (msg.what == 159) { // 直接写死,不反射了,否则在android9.0的设备上运行会弹出使用了反射的dialog提示框
            if (msg.obj == null) {
                return false;
            }
            handleActivity(msg);
        }
        return false;
    }


    private void handleActivity(Message msg) {
        try {
            // ClientTransaction-->ClientTransaction中的List<ClientTransactionItem> mActivityCallbacks-->集合中的第一个值LaunchActivityItem-->LaunchActivityItem的mIntent
            // 这里简单起见,直接取出TargetActivity;
            // final ClientTransaction transaction = (ClientTransaction) msg.obj;
            // 1.获取ClientTransaction对象
            Object clientTransactionObj = msg.obj;
            Method getLifecycleStateRequest = Class.forName("android.app.servertransaction.ClientTransaction").getDeclaredMethod("getLifecycleStateRequest");

            getLifecycleStateRequest.setAccessible(true);
            Object activityLifecycleItem = getLifecycleStateRequest.invoke(clientTransactionObj);
            if (!activityLifecycleItem.getClass().equals("android.app.servertransaction.ResumeActivityItem")) {
                return;
            }
            // 2.获取ClientTransaction类中属性mActivityCallbacks的Field
            // 3.禁止Java访问检查
            // private List<ClientTransactionItem> mActivityCallbacks;
            Field mActivityCallbacksField = clientTransactionObj.getClass().getDeclaredField("mActivityCallbacks");
            mActivityCallbacksField.setAccessible(true);
            List mActivityCallbacks = (List) mActivityCallbacksField.get(clientTransactionObj);
            if (mActivityCallbacks == null || mActivityCallbacks.size() < 1 || mActivityCallbacks.get(0) == null) {
                return;
            }

            // 5.ClientTransactionItem的Class对象
            // package android.app.servertransaction;
            // public class LaunchActivityItem extends ClientTransactionItem
            // 6.判断集合中第一个元素的值是LaunchActivityItem类型的
            if (!mActivityCallbacks.get(0).getClass().equals("android.app.servertransaction.LaunchActivityItem")) {
                return;
            }
            // 7.获取LaunchActivityItem的实例
            // public class LaunchActivityItem extends ClientTransactionItem
            // 8.ClientTransactionItem的mIntent属性的mIntent的Field
            // private Intent mIntent;
            // 9.禁止Java访问检查
            Object launchActivityItem = mActivityCallbacks.get(0);
            Field mIntentField = Class.forName("android.app.servertransaction.LaunchActivityItem").getDeclaredField("mIntent");
            mIntentField.setAccessible(true);
            // 10.获取mIntent属性的值,既桩Intent(安全的Intent)
            // 从LaunchActivityItem中获取属性mIntent的值
            Intent safeIntent = (Intent) mIntentField.get(launchActivityItem);
            if (safeIntent == null) {
                return;
            }
            Intent originIntent = null;
            // 11.获取原始的Intent
            // 12.需要判断originIntent != null
            if (Build.VERSION.SDK_INT >= 33) {
                originIntent = safeIntent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT, Intent.class);
            } else {
                originIntent = safeIntent.getParcelableExtra(AMSHookHelper.EXTRA_TARGET_INTENT);
            }
            if (originIntent == null) return;
            // 13.将原始的Intent,赋值给clientTransactionItem的mIntent属性
            safeIntent.setComponent(originIntent.getComponent());
//            // 给插件apk设置主题
//            Field mInfoFild = Class.forName("android.app.servertransaction.LaunchActivityItem").getDeclaredField("mInfo");
//            mInfoFild.setAccessible(true);
//            ActivityInfo activityInfo = (ActivityInfo) mInfoFild.get(launchActivityItem);
//            activityInfo.theme=selectSystemTheme();

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

//    private int selectSystemTheme() {
//        if (Build.VERSION.SDK_INT<24){
//            return R.style.AppCompatTheme;
//        }else{
//          return   R.style.AppCompatTheme;
//        }
//    }

    private static void loge(String info) {
        Log.println(Log.ERROR, TAG, info);
    }

    private static void logi(String info) {
        Log.println(Log.INFO, TAG, info);
    }
}
