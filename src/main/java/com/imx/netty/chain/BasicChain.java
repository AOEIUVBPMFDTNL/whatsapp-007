package com.imx.netty.chain;

import com.imx.apns.activate.ActivationInfo;
import com.imx.apns.gen.PEMGenerator;
import com.imx.apns.gen.RSAKeyPairGenerator;
import com.imx.common.Chain;
import com.imx.common.ChainExecution;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.util.Asserts;

import java.security.KeyPair;
import java.util.Base64;
import java.util.Objects;

@Slf4j
public class BasicChain  extends Chain<ChainContext> implements ChainExecution<ChainContext> {

    @Override
    public void process(ChainContext chainContext) {
        try {
            execute(chainContext);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                log.error(e.getMessage());
            } else {
                log.error("Failed to execute basic chain.", e);
            }
            return;
        }
        if (nextChain == null) {
            return;
        }
        nextChain.process(chainContext);
    }

    @Override
    public void execute(@NonNull ChainContext chainContext) throws Exception {
        if (Objects.nonNull(chainContext.getApNsState())) {
            return;
        }
        KeyPair keyPair = RSAKeyPairGenerator.generate();
        String pem = PEMGenerator.generate(keyPair);
        String pemBase64 = Base64.getEncoder().encodeToString(pem.getBytes());
        ActivationInfo activationInfo = chainContext.getActivationInfo();
        Asserts.check(Objects.nonNull(activationInfo), "activationInfo is null.");
        activationInfo.setDeviceCertRequest(pemBase64.getBytes());

        chainContext.setKeyPair(keyPair);
    }
}
