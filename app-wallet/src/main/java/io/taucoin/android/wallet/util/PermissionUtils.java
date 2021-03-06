/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.util;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationManagerCompat;

import io.taucoin.android.wallet.R;

import java.util.ArrayList;
import java.util.List;

import io.taucoin.android.wallet.MyApplication;
import io.taucoin.foundation.util.StringUtil;
import io.taucoin.foundation.util.permission.EasyPermissions;

public class PermissionUtils {
    static final int REQUEST_PERMISSIONS_CAMERA = 0x10;
    public static final int REQUEST_PERMISSIONS_STORAGE = 0x20;
    public static final int REQUEST_PERMISSIONS_NETWORK = 0x30;

    /*Check if the user has completely prohibited pop-up permission requests*/
    public static void checkUserBanPermission(FragmentActivity activity, String permission, int resMsg) {
        if (StringUtil.isEmpty(permission)) {
            return;
        }
        List<String> deniedPerms = new ArrayList<>();
        deniedPerms.add(permission);
        checkUserBanPermission(activity, deniedPerms, resMsg);
    }

    static void checkUserBanPermission(FragmentActivity activity, List<String> deniedPerms, int resMsg) {
        String message = activity.getString(resMsg);
        CharSequence positiveButton = activity.getString(R.string.common_ok);
        CharSequence negativeButton = activity.getString(R.string.common_cancel);
        EasyPermissions.checkDeniedPermissionsNeverAskAgain(activity, message, positiveButton, negativeButton, deniedPerms);
    }

    public static boolean isNotificationEnabled() {
        Context context = MyApplication.getInstance();
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
        return notificationManagerCompat.areNotificationsEnabled();
    }

    public static boolean hasPermissionToReadNetworkStats() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        Context context = MyApplication.getInstance();
        final AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    public static void requestReadNetworkStats() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Context context = MyApplication.getInstance();
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}