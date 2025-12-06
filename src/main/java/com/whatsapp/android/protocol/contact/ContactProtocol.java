package com.whatsapp.android.protocol.contact;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import cn.hutool.core.util.ObjectUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.entity.response.contact.ContactInfoResult;
import lombok.experimental.UtilityClass;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@UtilityClass
public class ContactProtocol {
    /**
     * 通过jid同步联系人
     *
     * @param gorgeousEngine     gorgeousEngine
     * @param contactSyncResults 联系人信息
     * @return ProtocolTreeNode
     */
    public ProtocolTreeNode syncContactsByJid(GorgeousEngine gorgeousEngine, List<ContactInfoResult> contactSyncResults) {
        ProtocolTreeNode iq = createSyncIqNode(gorgeousEngine.GenerateIqId());
        ProtocolTreeNode usync = createUsyncNode(gorgeousEngine);

        // 添加查询节点
        usync.AddChild(createQueryNode(gorgeousEngine));

        // 添加侧边列表节点
        usync.AddChild(createSideListNode(contactSyncResults));

        // 组装并发送
        iq.AddChild(usync);
        return iq;
    }

    private ProtocolTreeNode createSyncIqNode(String id) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("xmlns", "usync"));
        iq.AddAttribute(new StanzaAttribute("id", id));
        iq.AddAttribute(new StanzaAttribute("type", "get"));
        return iq;
    }

    private ProtocolTreeNode createUsyncNode(GorgeousEngine gorgeousEngine) {
        ProtocolTreeNode usync = new ProtocolTreeNode("usync");
        if (gorgeousEngine.isIosLogin()) {
            usync.AddAttribute(new StanzaAttribute("sid", System.currentTimeMillis() / 1000 + "-" + new Random().nextInt(1000000000) + "-" + gorgeousEngine.getSyncId().incrementAndGet()));
        } else {
            usync.AddAttribute(new StanzaAttribute("sid", "ContactSyncHelper/sync_sid_sidelist_" + UUID.randomUUID()));
        }
        usync.AddAttribute(new StanzaAttribute("index", "0"));
        usync.AddAttribute(new StanzaAttribute("last", "true"));
        usync.AddAttribute(new StanzaAttribute("mode", "delta"));
        usync.AddAttribute(new StanzaAttribute("context", "interactive"));
        usync.AddAttribute(new StanzaAttribute("allow_mutation", "true"));

        return usync;
    }

    private ProtocolTreeNode createQueryNode(GorgeousEngine gorgeousEngine) {
        ProtocolTreeNode query = new ProtocolTreeNode("query");

        // 添加状态节点
        query.AddChild(new ProtocolTreeNode("status"));

        // 添加商业节点
        ProtocolTreeNode business = new ProtocolTreeNode("business");
        business.AddChild(new ProtocolTreeNode("verified_name"));

        ProtocolTreeNode profile = new ProtocolTreeNode("profile");
        if (gorgeousEngine.isIosLogin()) {
            profile.AddAttribute(new StanzaAttribute("v", "1396"));

        } else {
            profile.AddAttribute(new StanzaAttribute("v", "1908"));
        }

        business.AddChild(profile);

        query.AddChild(business);

        // 添加其他必要节点
        query.AddChild(new ProtocolTreeNode("sidelist"));

        ProtocolTreeNode devices = new ProtocolTreeNode("devices");
        devices.AddAttribute(new StanzaAttribute("version", "2"));
        query.AddChild(devices);

        query.AddChild(new ProtocolTreeNode("disappearing_mode"));
        query.AddChild(new ProtocolTreeNode("lid"));

        return query;
    }

    private ProtocolTreeNode createSideListNode(List<ContactInfoResult> contactSyncResults) {
        ProtocolTreeNode sideListNode = new ProtocolTreeNode("side_list");

        for (ContactInfoResult result : contactSyncResults) {
            ProtocolTreeNode userNode = createUserNode(result);
            sideListNode.AddChild(userNode);
        }

        return sideListNode;
    }

    private ProtocolTreeNode createUserNode(ContactInfoResult result) {
        ProtocolTreeNode user = new ProtocolTreeNode("user");
        user.AddAttribute(new StanzaAttribute("jid", result.getJid()));

        // 添加商业信息
        if (StringUtils.hasLength(result.getSerial())) {
            user.AddChild(createBusinessNode(result.getSerial()));
        }
        if (ObjectUtil.isNotNull(result.getTcToken())) {
            //添加tcToken
            user.AddChild(createTcTokenNode(result.getTcToken()));
        }
        // 添加设备信息
        if (StringUtils.hasLength(result.getDeviceHash())) {
            ProtocolTreeNode devicesNode = new ProtocolTreeNode("devices");
            devicesNode.AddAttribute(new StanzaAttribute("device_hash", result.getDeviceHash()));
            user.AddChild(devicesNode);
        }

        // 添加 lid 信息
        if (StringUtils.hasLength(result.getLid())) {
            ProtocolTreeNode lidNode = new ProtocolTreeNode("lid");
            lidNode.AddAttribute(new StanzaAttribute("jid", result.getLid()));
            user.AddChild(lidNode);
        }

        return user;
    }

    private ProtocolTreeNode createBusinessNode(String serial) {
        ProtocolTreeNode businessNode = new ProtocolTreeNode("business");
        ProtocolTreeNode verifiedNameNode = new ProtocolTreeNode("verified_name");
        verifiedNameNode.AddAttribute(new StanzaAttribute("serial", serial));
        businessNode.AddChild(verifiedNameNode);
        return businessNode;
    }

    private ProtocolTreeNode createTcTokenNode(byte[] tcToken) {
        ProtocolTreeNode businessNode = new ProtocolTreeNode("tctoken");
        businessNode.SetData(tcToken);
        return businessNode;
    }

}
