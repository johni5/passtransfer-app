package com.del.pst.session;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.common.util.Strings;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Session extends ViewModel {

    private final MutableLiveData<String> cid = new MutableLiveData<>();
    private final MutableLiveData<String> key = new MutableLiveData<>();
    private final AtomicInteger badConnectionsCounter = new AtomicInteger();

    private final MutableLiveData<byte[]> digestOfPassword = new MutableLiveData<>();
    private long lastTimeUseMillis = 0;


    public MutableLiveData<byte[]> getDigestOfPassword() {
        return digestOfPassword;
    }

    public byte[] getDigestOfPasswordValue() {
        return digestOfPassword.getValue();
    }

    public String getCid() {
        return cid.getValue();
    }

    public String getKeyValue() {
        return key.getValue();
    }

    public MutableLiveData<String> getKey() {
        return key;
    }

    public void start(String cid) {
        this.cid.setValue(cid);
        inactivate();
    }

    public void activate(String key) {
        this.key.setValue(key);
        badConnectionsCounter.set(0);
    }

    public void inactivate() {
        key.setValue(null);
        badConnectionsCounter.set(0);
    }

    public boolean isActive() {
        return !Strings.isEmptyOrWhitespace(getKeyValue()) && badConnectionsCounter.get() < 4;
    }

    public void processError() {
        if (badConnectionsCounter.incrementAndGet() > 3) {
            inactivate();
        }
    }

    public void checkTimeout(long minutes) {
        if (System.currentTimeMillis() - lastTimeUseMillis > TimeUnit.MINUTES.toMillis(minutes)) {
            inactivate();
            getDigestOfPassword().setValue(null);
        }
        lastTimeUseMillis = System.currentTimeMillis();
    }
}
