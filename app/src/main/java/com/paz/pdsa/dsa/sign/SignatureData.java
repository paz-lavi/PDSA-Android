package com.paz.pdsa.dsa.sign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder
@AllArgsConstructor
@Setter
@Getter
@ToString
@NoArgsConstructor
public class SignatureData {
    private long signature;
    private long publicKey;
    private long keyLength;
    private long timestamp;
    private String userId;
}
