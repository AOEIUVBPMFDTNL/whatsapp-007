package com.imx.netty.chain;

import com.imx.apns.common.AppleBag;
import com.imx.common.Chain;
import com.imx.common.ChainExecution;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppleBagChain extends Chain<ChainContext> implements ChainExecution<ChainContext> {

    @Override
    public void process(ChainContext chainContext) {
        try {
            execute(chainContext);
        } catch (Exception e) {
            log.error("Failed to execute appleBag chain.", e);
            return;
        }
        if (nextChain == null) {
            return;
        }
        nextChain.process(chainContext);
    }

    @Override
    public void execute(@NonNull ChainContext chainContext) throws Exception {
        String host = AppleBag.getAPNsServerHost();
        chainContext.setCourierHost(host);
    }
}
