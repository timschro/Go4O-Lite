/**
 * Released by SPORTident under the CC BY 3.0 license.
 */
package de.sportident;

public class CRCCalculator {

    private CRCCalculator() {
    }

    private static final int POLY = 0x8005;
    private static final int BITF = 0x8000;

    static public int crc(byte[] buffer) {
        int count = buffer.length;
        int i, j;
        int tmp, val_; // 16 Bit
        int ptr = 0;

        tmp = (short) (buffer[ptr++] << 8 | (buffer[ptr++] & 0xFF));

        if (count > 2) {
            for (i = count / 2; i > 0; i--) {
                if (i > 1) {
                    val_ = (int) (buffer[ptr++] << 8 | (buffer[ptr++] & 0xFF));
                } else {
                    if (count % 2 == 1) {
                        val_ = buffer[count - 1] << 8;
                    } else {
                        val_ = 0;
                    }
                }

                for (j = 0; j < 16; j++) {
                    if ((tmp & BITF) != 0) {
                        tmp <<= 1;
                        if ((val_ & BITF) != 0) {
                            tmp++;
                        }
                        tmp ^= POLY;
                    } else {
                        tmp <<= 1;
                        if ((val_ & BITF) != 0) {
                            tmp++;
                        }
                    }
                    val_ <<= 1;
                }
            }
        }
        return (tmp & 0xFFFF);
    }
}
