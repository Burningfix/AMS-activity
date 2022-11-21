package jianqiang.com.hook3;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;

public class app extends Application {
    private static Context mContext=null;
    private static ComponentName mComponentName=null;
    @Override
    public void onCreate() {
        super.onCreate();
        MinRefPlanA.unseal(this);
        mContext=this;
        mComponentName= new ComponentName(getPackageName(),StubActivity.class.getName());
    }

    public static ComponentName getTarget(){
        return mComponentName;
    }
}
