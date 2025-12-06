package com.whatsapp.android.config;

import lombok.extern.slf4j.Slf4j;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * @author sunnoc
 * @date 2020-07-04 10:52
 */
@Slf4j
public class ThreadLocalAuthenticator extends Authenticator {
    private ThreadLocal<PasswordAuthentication> auth = new ThreadLocal<>();

    public void setPasswordAuthentication(PasswordAuthentication passwordAuthentication) {
        auth.set(passwordAuthentication);
    }

    public void clearPasswordAuthentication() {
        auth.remove();
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        PasswordAuthentication passwordAuthentication = auth.get();
        return passwordAuthentication;
    }
}