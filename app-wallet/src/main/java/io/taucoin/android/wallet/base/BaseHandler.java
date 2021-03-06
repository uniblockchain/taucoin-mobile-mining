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
package io.taucoin.android.wallet.base;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

public class BaseHandler extends Handler {

    private WeakReference<HandleCallBack> mRes;

    public BaseHandler(BaseHandler.HandleCallBack handleCallBack) {
        if (null == mRes) {
            mRes = new WeakReference<>(handleCallBack);
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (null != mRes && null != msg) {
            HandleCallBack handleCallBack = mRes.get();
            if (null != handleCallBack)
                handleCallBack.handleMessage(msg);
        }
    }

    public interface HandleCallBack {
        void handleMessage(Message message);
    }
}