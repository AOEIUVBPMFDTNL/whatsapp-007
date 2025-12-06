package com.whatsapp.android.service;

import ProtocolTree.ProtocolTreeNode;
import axolotl.AxolotlManager;
import cn.hutool.core.codec.Base64;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.entity.User;
import com.whatsapp.android.entity.UserRecord;
import com.whatsapp.android.util.KeyLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author sunnoc
 * @date 2023-05-22 14:04
 */
@Slf4j
public class GcmPnResultService {

    public static void handlerPn(String username, String pn) {
        try {
            if (StringUtils.hasLength(username)) {
                User user = UserRecord.getRecord().get(username);
                if (user != null) {
                    GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
                    if (gorgeousEngine != null) {
                        try {
                            KeyLockUtil.lock(username);
                            gorgeousEngine.axolotlManager_.setLastPn(pn);
                        } catch (Exception ignore) {
                        } finally {
                            KeyLockUtil.unlock(username);
                        }
                        gorgeousEngine.sendPn(pn, (srcNode, result) -> {
                            //<iq from='s.whatsapp.net' type='result' id='02'><cat>2f6d77310001bcb21b7087ebca6115712d0ba72a0632767a39ca595cb6e1d90d8f9d3dc782c87d1055bba607760ead8c72322ab93820ad21f20be704343a2addcabcc2cfa0cc129675f93e2ba9cec7e8d220535158bd6a0f14872e4ef2e62653c25cd0d1c9a7cb5a07cdb6a25949681abe5c29d8775089</cat></iq>
                            ProtocolTreeNode catNode = result.getOneChildren("cat");
                            if (catNode != null) {
                                byte[] cat = catNode.GetData();
                                if (cat != null) {
                                    log.info("用户：{}，收到xmpp->cat：{}", username, Base64.encode(cat));
                                    //需要保存到数据库
                                    AxolotlManager axolotlManager_ = gorgeousEngine.axolotlManager_;
                                    try {
                                        KeyLockUtil.lock(username);
                                        axolotlManager_.setGcmCatValue(cat);
                                    } catch (Exception ignore) {
                                    } finally {
                                        KeyLockUtil.unlock(username);
                                    }
                                    gorgeousEngine.digest();
                                }
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            log.error("用户：{}，处理gcm->pn异常", username, e);
        }
    }
}