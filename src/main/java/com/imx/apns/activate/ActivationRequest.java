package com.imx.apns.activate;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class ActivationRequest {

    Boolean ActivationInfoComplete;
    byte[] ActivationInfoXML;
    byte[] FairPlayCertChain;
    byte[] FairPlaySignature;
    String device;

    public ActivationRequest() {
        this.ActivationInfoComplete = Boolean.TRUE;
    }

}
