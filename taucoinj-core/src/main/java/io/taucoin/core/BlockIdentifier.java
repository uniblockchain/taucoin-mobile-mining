package io.taucoin.core;

import io.taucoin.util.ByteUtil;
import io.taucoin.util.RLP;
import io.taucoin.util.RLPList;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.Arrays;

import static io.taucoin.util.ByteUtil.byteArrayToLong;

/**
 * Block identifier holds block hash and number <br>
 * This tuple is used in some places of the core,
 * like by {@link io.taucoin.net.tau.message.TauMessageCodes#NEW_BLOCK_HASHES} message wrapper
 *
 * @author Mikhail Kalinin
 * @since 04.09.2015
 */
public class BlockIdentifier {

    /**
     * Block hash
     */
    private byte[] hash;

    /**
     * Block number
     */
    private long number;

    public BlockIdentifier(RLPList rlp) {
        this.hash = rlp.get(0).getRLPData();
        this.number = byteArrayToLong(rlp.get(1).getRLPData());
    }

    public BlockIdentifier(byte[] hash, long number) {
        this.hash = hash;
        this.number = number;
    }

    public byte[] getHash() {
        return hash;
    }

    public long getNumber() {
        return number;
    }

    public byte[] getEncoded() {
        byte[] hash = RLP.encodeElement(this.hash);
        byte[] number = RLP.encodeBigInteger(BigInteger.valueOf(this.number));

        return RLP.encodeList(hash, number);
    }

    @Override
    public String toString() {
        return "BlockIdentifier{" +
                "hash=" + Hex.toHexString(hash) +
                ", number=" + number +
                '}';
    }
}
