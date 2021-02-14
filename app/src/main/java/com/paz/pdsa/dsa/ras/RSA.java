package com.paz.pdsa.dsa.ras;

import android.util.Log;

import com.paz.logger.EZLog;
import com.paz.pdsa.dsa.sign.SignatureData;

import java.math.BigInteger;

import static java.lang.Math.floor;
import static java.lang.Math.sqrt;

public class RSA {
    private final EZLog ezLog = EZLog.getInstance();


    /** check if a provided number is a prime*/
    private boolean checkIfPrime(int num) {

        if (num == 0 || num == 1) {
            return false;
        } else if (num == 2 || num == 3) {
            return true;
        } else {
            return isPrimeChecker(num);
        }
    }

    /** check if prime*/
    private boolean isPrimeChecker(int num) {
        int i, n = (int) floor(sqrt(num));
        boolean prime = true;
        for (i = 2; i <= n; i++) {
            if (num % i == 0) {
                prime = false;
                break;
            }
        }


        return prime;
    }

    /** calculate GCD of 2 numbers  GCD for both positive and negative numbers in recursion */
    private long calculateGCD(long a, long b) {
        long n1 = (a > 0) ? a : -a;
        long n2 = (b > 0) ? b : -b;
        long reminder;
        if (n1 > n2) {
            reminder = n1;
            n1 = n2;
            n2 = reminder;
        }
        while (true) {
            reminder = n1 % n2;
            if (reminder == 0) {
                return n2;
            }
            n1 = n2;
            n2 = reminder;
        }
    }


    private long decrypt(long cipher, long publicKey, long keyLength) {
        return powMod(cipher, publicKey, keyLength);

    }

    public long encrypt(long plain, long privateKey, long keyLength) {

        return powMod(plain, privateKey, keyLength);

    }

    public boolean isValidSignature(long hash, SignatureData signatureData) {
        long decrypt = decrypt(signatureData.getSignature(), signatureData.getPublicKey(),
                signatureData.getKeyLength());
         ezLog.debug( "isValidSignature: hash = " + hash + " decrypt = " + decrypt);
        return hash == decrypt;
    }


    long multiplicativeInverse(long a, long b) {
        long b0 = b, t, q;
        long x0 = 0, x1 = 1;
        if (b == 1) return 1;
        while (a > 1) {
            q = a / b;
            t = b;
            b = a % b;
            a = t;
            t = x0;
            x0 = x1 - q * x0;
            x1 = t;
        }
        if (x1 < 0) x1 += b0;
        return x1;
    }

    private long powMod(long A, long B, long C) {
        BigInteger bi1, bi2, bi3, exponent;
        bi1 = new BigInteger(String.valueOf(A));
        bi2 = new BigInteger(String.valueOf(C));
        exponent = new BigInteger(String.valueOf(B));
        bi3 = bi1.modPow(exponent, bi2);
        return bi3.longValue();

    }

    //keyS generation algorithm: https://www.di-mgt.com.au/rsa_alg.html
    public KeyPair keysGenerator(int bitSize) {
        Primes primes = new Primes();
        long e = primes.getRandomPrime(8);
        long p, q;

        do {
            p = primes.getRandomPrime(bitSize / 2);
        } while (p % e == 1);

        do {
            q = primes.getRandomPrime(bitSize / 2);
        } while (q % e == 1);

        long n = p * q;
        long phi_n = (p - 1) * (q - 1);
        long d = multiplicativeInverse(e, phi_n);

        return new KeyPair(e, d, n);

    }


}
