package com.whatsapp.android.controller;

import com.whatsapp.android.constant.Constant;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author sunnoc
 * @date 2021-05-26 11:03
 */
@RestController
@RequestMapping("/api")
public class KeyController {
    @GetMapping(value = "getApiKey")
    public String getTerminalInfo(@RequestParam("secretKey") String secretKey) {
        if (StringUtils.isEmpty(secretKey) || !secretKey.equals(Constant.GET_SECRET_KEY)) {
            return "";
        }
        return Constant.KEY;
    }
}
