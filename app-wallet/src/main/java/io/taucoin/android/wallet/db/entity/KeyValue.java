package io.taucoin.android.wallet.db.entity;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;

import java.util.List;
import org.greenrobot.greendao.annotation.Transient;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by ly on 18-11-21
 *
 * @version 1.0
 * @description:
 */
@Entity
public class KeyValue {
    @Id
    private Long id;
    private String pubkey;
    private String privkey;
    private String address;
    private long balance;
    private String nickName;
    private String miningState;

    @Generated(hash = 2094309836)
    public KeyValue(Long id, String pubkey, String privkey, String address,
            long balance, String nickName, String miningState) {
        this.id = id;
        this.pubkey = pubkey;
        this.privkey = privkey;
        this.address = address;
        this.balance = balance;
        this.nickName = nickName;
        this.miningState = miningState;
    }

    @Generated(hash = 92014137)
    public KeyValue() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPubkey() {
        return this.pubkey;
    }

    public void setPubkey(String pubkey) {
        this.pubkey = pubkey;
    }

    public String getPrivkey() {
        return this.privkey;
    }

    public void setPrivkey(String privkey) {
        this.privkey = privkey;
    }

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getBalance() {
        return this.balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getMiningState() {
        return this.miningState;
    }

    public void setMiningState(String miningState) {
        this.miningState = miningState;
    }

}
