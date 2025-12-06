package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.pack.contact.SyncContactPack;
import com.whatsapp.android.entity.response.contact.ContactResult;
import com.whatsapp.android.entity.response.contact.SyncContactResult;
import com.whatsapp.android.request.AbstractRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 同步通讯录校验开通
 *
 * @author sunnoc
 * @date 2021-03-12 18:06
 */
public class SyncContactRequest extends AbstractRequest<SyncContactResult> {
    private SyncContactPack syncContactPack;

    public SyncContactRequest(SyncContactPack syncContactPack) {
        this.syncContactPack = syncContactPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.SYNC_CONTACT;
    }

    @Override
    public boolean request() {
        List<String> list = syncContactPack.getList();
        if (list == null || list.size() == 0) {
            return false;
        }
        user.getGorgeousEngine().SyncContact(list, getTaskId());
        return true;
    }

    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public long timeOut() {
        return 60;
    }

    @Override
    public SyncContactResult parseResult(ProtocolTreeNode node) {
        /**
         * <iq from='8617748754950@s.whatsapp.net' type='result' id='C67DC9FA40EA419FB855E16C0B69ED15'>
         *     <usync index='0' last='true' mode='full' context='registration' sid='sync_sid_full_fca7e916-061b-4568-96d1-e588778b206f'>
         *         <result>
         *             <business refresh='553695'/>
         *             <status refresh='561894'/>
         *             <contact version='1615359270517481' refresh='462991'/>
         *         </result>
         *         <list>
         *             <user jid='868618688676082@s.whatsapp.net'>
         *                 <contact type='out'>ODYxODY4ODY3NjA4Mg==</contact>
         *             </user>
         *             <user jid='868615228092350@s.whatsapp.net'>
         *                 <contact type='out'>ODYxNTIyODA5MjM1MA==</contact>
         *             </user>
         *         </list>
         *     </usync>
         * </iq>
         */
        SyncContactResult checkResult = checkResult(node, SyncContactResult.class);
        if (checkResult != null) {
            return checkResult;
        }

        ProtocolTreeNode syncNode = node.getOneChildren("usync");
        if (syncNode == null) {
            if (StrUtil.indexOf(node.toString(), "rate-overlimit", 0, false) != -1) {
                return new SyncContactResult(StatusResult.fail("超出频率限制"));
            }
            return new SyncContactResult(StatusResult.fail("同步通讯录失败"));
        }
        ProtocolTreeNode listNode = syncNode.getOneChildren("list");
        if (listNode == null) {
            return new SyncContactResult(StatusResult.fail("同步通讯录失败"));
        }
        SyncContactResult syncContactResult = new SyncContactResult();
        List<ContactResult> list = new ArrayList<>();
        LinkedList<ProtocolTreeNode> participant = listNode.GetChildren("user");
        List<StringUtil.JidInfo> jidInfos = new ArrayList<>();
        if (participant != null) {
            for (ProtocolTreeNode userNode : participant) {
                String jid = userNode.GetAttributeValue("jid");
                if (StringUtils.hasLength(jid)) {
                    ProtocolTreeNode contact = userNode.getOneChildren("contact");
                    String type = contact.GetAttributeValue("type");
                    ContactResult contactResult = new ContactResult();
                    contactResult.setUserId(jid);
                    boolean exist = "in".equals(type);
                    contactResult.setExist(exist);
                    byte[] bytes = contact.GetData();
                    String phone = new String(bytes);
                    phone = phone.replace("+", "");
                    contactResult.setPhone(phone);
                    if (exist) {
                        ProtocolTreeNode businessNode = userNode.getOneChildren("business");
                        contactResult.setBusiness(businessNode != null);
                        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(jid);
                        ProtocolTreeNode devicesNode = userNode.getOneChildren("devices");
                        LinkedList<ProtocolTreeNode> deviceListNode = devicesNode.GetChildren("device-list");
                        for (ProtocolTreeNode deviceNode : deviceListNode) {
                            LinkedList<ProtocolTreeNode> deviceList = deviceNode.GetChildren("device");
                            if (ObjectUtil.isNull(deviceList)) {
                                // <devices> <device-list /> </devices>
                                // 判断这种情况, 直接添加xxx.0:0@s.whatsapp.net即可
                                String userId = String.format("%s.0:0@%s", jidInfo.recipientId, "s.whatsapp.net");
                                jidInfos.add(StringUtil.ParseJid(userId));
                                break;
                            }
                            for (ProtocolTreeNode device : deviceList) {
                                String deviceId = device.GetAttributeValue("id");
                                String userId = String.format("%s.0:%s@%s", jidInfo.recipientId, deviceId, "s.whatsapp.net");
                                jidInfos.add(StringUtil.ParseJid(userId));
                            }
                        }
                    }
                    list.add(contactResult);
                }
            }
        }
        if (!jidInfos.isEmpty()) {
            if (!syncContactPack.isSkipContactStorage()) {
                user.getGorgeousEngine().axolotlManager_.contactSyncStore.batchInsertSyncContact(jidInfos);
            }
        }
        syncContactResult.setList(list);
        return syncContactResult;
    }
}
