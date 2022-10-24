package jianqiang.com.hook3;

import android.app.Application;

public class app extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MinRefPlanA.unseal(this);
    }
}
