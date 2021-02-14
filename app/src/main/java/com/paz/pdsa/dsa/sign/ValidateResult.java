package com.paz.pdsa.dsa.sign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ValidateResult {
    private boolean valid;
    private SignatureData data;
}
