package com.test.plugin_manager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
import com.tencent.shadow.dynamic.host.EnterCallback;
import com.test.constants.Constant;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SimpleDes:
 * Creator: Sindi
 * Date: 2021-06-21
 * UseDes:
 */


public class SamplePluginManager extends FastPluginManager {
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Context mCurrentContext;

    public SamplePluginManager(Context context) {
        super(context);
        mCurrentContext = context;
    }

    /**
     * @return PluginManager实现的别名，用于区分不同PluginManager实现的数据存储路径
     */
    @Override
    protected String getName() {
        return "plugin-manager";
    }

//    /**
//     * @return 宿主so的ABI。插件必须和宿主使用相同的ABI。
//     */
//    @Override
//    public String getAbi() {
//        return "";
//    }

    /**
     * @return 宿主中注册的PluginProcessService实现的类名
     */
    @Override
    protected String getPluginProcessServiceName(String partKey) {//在这里支持多个插件
        if (Constant.PLUGIN_APP_NAME.equals(partKey)) {//plugin-app：插件标识名
            return "com.test.shadow.service.MainPluginProcessService";
        }if (Constant.PLUGIN_OTHER_NAME.equals(partKey)) {//plugin-other：插件标识名
            return "com.test.shadow.service.MainPluginProcessService";
        }  else {
            //如果有默认PPS，可用return代替throw
            throw new IllegalArgumentException("unexpected plugin load request意外的插件加载请求: " + partKey);
        }
    }

    @Override
    public void enter(final Context context, long fromId, Bundle bundle, final EnterCallback callback) {
        if (fromId == Constant.FROM_ID_NOOP) {
            //....
        } else if (fromId == Constant.FROM_ID_START_ACTIVITY) {
            onStartActivity(context, bundle, callback);
        } else if (fromId == Constant.FROM_ID_CALL_SERVICE) {
            //启动Service
        } else {
            throw new IllegalArgumentException("不认识的fromId==" + fromId);
        }
    }

    private void onStartActivity(final Context context, Bundle bundle, final EnterCallback callback) {
        final String pluginZipPath = bundle.getString(Constant.KEY_PLUGIN_ZIP_PATH);
        final String partKey = bundle.getString(Constant.KEY_PLUGIN_NAME);
        final String className = bundle.getString(Constant.KEY_ACTIVITY_CLASSNAME);
        if (className == null) {
            throw new NullPointerException("className == null");
        }
        final Bundle extras = bundle.getBundle(Constant.KEY_EXTRAS);
        if (callback != null) {
            final View view = LayoutInflater.from(mCurrentContext).inflate(R.layout.view_plugin_manager_loading, null);
            callback.onShowLoadingView(view);
        }
        executorService.execute(() -> {
            try {
                InstalledPlugin installedPlugin = installPlugin(pluginZipPath, null, true);
                Intent pluginIntent = new Intent();
                pluginIntent.setClassName(
                        context.getPackageName(),
                        className
                );
                if (extras != null) {
                    pluginIntent.replaceExtras(extras);
                }
                startPluginActivity(installedPlugin, partKey, pluginIntent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (callback != null) {
                callback.onCloseLoadingView();
            }
        });
    }
}