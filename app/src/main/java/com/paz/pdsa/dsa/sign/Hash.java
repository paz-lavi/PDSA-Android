package com.paz.pdsa.dsa.sign;

import com.paz.logger.EZLog;

public class Hash {
    private final int FOLDING_MOD = 100000;
    private final EZLog ezLog = EZLog.getInstance();


    int foldFile(String fileContent) {
        ezLog.debug("foldFileV2: fileContent len \n" + fileContent.length());
        ezLog.debug("foldFileV2: fileContent \n" + fileContent);

        long sum = 0, mul = 1;
        char number;
        for (int i = 0; i < fileContent.length(); i++) {
            number = fileContent.charAt(i);
            mul = (i % 4 == 0) ? 1 : mul * 256;
            sum += number * mul;

        }

        ezLog.debug("foldFileV2: sum " + sum);
        return (int) (Math.abs(sum) % FOLDING_MOD);
    }

}
