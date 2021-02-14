package com.paz.pdsa.dsa.sign;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static android.system.OsConstants.EXIT_FAILURE;

public class Converters {


    final char ZERO = '\u200C';
    final char BIN = '\u200D';

    /**
     * represent string array  as bits array (binary , 0/1 array)
     */
    public String stringToBin(String str) {

        StringBuilder result = new StringBuilder();
        char[] chars = str.toCharArray();
        for (char aChar : chars) {
            result.append(
                    String.format("%8s", Integer.toBinaryString(aChar))   // char -> int, auto-cast
                            .replaceAll(" ", "0")                         // zero pads
            );
        }
        return result.toString();

    }

    /* convert bits array (binary 0/1 chars) to string array*/
    public String binToString(String bin) {

        ArrayList<String> list = new ArrayList<>();
        for(int i = 0 ;i<bin.length(); i+=8 )
            list.add(bin.substring(i,i+8));


        return Arrays.stream(list.toArray(new  String[0]))
                .map(binary -> Integer.parseInt(binary, 2))
                .map(integer -> Character.valueOf((char)integer.intValue()).toString())
                .collect(Collectors.joining());


    }

    /**
     * convert bits array (binary) to zero width chars array (invisible chars)
     *
     * @param bin - string in binary represent
     * @return return the binary array as a zero width array
     */
  public   String binToZeroWidth(String bin) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bin.length(); i++) {
            sb.append(bin.charAt(i) == '1' ? BIN : ZERO);
        }
        return sb.toString();
    }

    /** convert zero width chars array (invisible chars) to bits array (binary 0/1 chars)
     * @param  zeroWidth - the string as a zero width array
     * @return   the string in binary*/
   public String zeroWidthToBin(String zeroWidth) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0 ; i < zeroWidth.length();i++ ) {
            sb.append( zeroWidth.charAt(i) == ZERO? '0':"1");
        }
        return sb.toString();
    }
}
