package io.taucoin.android.wallet.db.greendao;

import java.util.Map;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.identityscope.IdentityScopeType;
import org.greenrobot.greendao.internal.DaoConfig;

import io.taucoin.android.wallet.db.entity.MiningReward;
import io.taucoin.android.wallet.db.entity.MiningBlock;
import io.taucoin.android.wallet.db.entity.TransactionHistory;
import io.taucoin.android.wallet.db.entity.BlockInfo;
import io.taucoin.android.wallet.db.entity.KeyValue;

import io.taucoin.android.wallet.db.greendao.MiningRewardDao;
import io.taucoin.android.wallet.db.greendao.MiningBlockDao;
import io.taucoin.android.wallet.db.greendao.TransactionHistoryDao;
import io.taucoin.android.wallet.db.greendao.BlockInfoDao;
import io.taucoin.android.wallet.db.greendao.KeyValueDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see org.greenrobot.greendao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig miningRewardDaoConfig;
    private final DaoConfig miningBlockDaoConfig;
    private final DaoConfig transactionHistoryDaoConfig;
    private final DaoConfig blockInfoDaoConfig;
    private final DaoConfig keyValueDaoConfig;

    private final MiningRewardDao miningRewardDao;
    private final MiningBlockDao miningBlockDao;
    private final TransactionHistoryDao transactionHistoryDao;
    private final BlockInfoDao blockInfoDao;
    private final KeyValueDao keyValueDao;

    public DaoSession(Database db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        miningRewardDaoConfig = daoConfigMap.get(MiningRewardDao.class).clone();
        miningRewardDaoConfig.initIdentityScope(type);

        miningBlockDaoConfig = daoConfigMap.get(MiningBlockDao.class).clone();
        miningBlockDaoConfig.initIdentityScope(type);

        transactionHistoryDaoConfig = daoConfigMap.get(TransactionHistoryDao.class).clone();
        transactionHistoryDaoConfig.initIdentityScope(type);

        blockInfoDaoConfig = daoConfigMap.get(BlockInfoDao.class).clone();
        blockInfoDaoConfig.initIdentityScope(type);

        keyValueDaoConfig = daoConfigMap.get(KeyValueDao.class).clone();
        keyValueDaoConfig.initIdentityScope(type);

        miningRewardDao = new MiningRewardDao(miningRewardDaoConfig, this);
        miningBlockDao = new MiningBlockDao(miningBlockDaoConfig, this);
        transactionHistoryDao = new TransactionHistoryDao(transactionHistoryDaoConfig, this);
        blockInfoDao = new BlockInfoDao(blockInfoDaoConfig, this);
        keyValueDao = new KeyValueDao(keyValueDaoConfig, this);

        registerDao(MiningReward.class, miningRewardDao);
        registerDao(MiningBlock.class, miningBlockDao);
        registerDao(TransactionHistory.class, transactionHistoryDao);
        registerDao(BlockInfo.class, blockInfoDao);
        registerDao(KeyValue.class, keyValueDao);
    }
    
    public void clear() {
        miningRewardDaoConfig.clearIdentityScope();
        miningBlockDaoConfig.clearIdentityScope();
        transactionHistoryDaoConfig.clearIdentityScope();
        blockInfoDaoConfig.clearIdentityScope();
        keyValueDaoConfig.clearIdentityScope();
    }

    public MiningRewardDao getMiningRewardDao() {
        return miningRewardDao;
    }

    public MiningBlockDao getMiningBlockDao() {
        return miningBlockDao;
    }

    public TransactionHistoryDao getTransactionHistoryDao() {
        return transactionHistoryDao;
    }

    public BlockInfoDao getBlockInfoDao() {
        return blockInfoDao;
    }

    public KeyValueDao getKeyValueDao() {
        return keyValueDao;
    }

}
