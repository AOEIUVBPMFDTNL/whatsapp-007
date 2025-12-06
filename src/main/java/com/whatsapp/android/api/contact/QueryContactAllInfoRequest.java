package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import Util.StringUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
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
 * 查询联系人校验开通
 *
 * @author sunnoc
 * @date 2021-03-12 18:06
 */
public class QueryContactAllInfoRequest extends AbstractRequest<SyncContactResult> {
    private final SyncContactPack syncContactPack;

    public QueryContactAllInfoRequest(SyncContactPack syncContactPack) {
        this.syncContactPack = syncContactPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.QUERY_CONTACT_ALL_INFO;
    }

    @Override
    public boolean request() {
        List<String> list = syncContactPack.getList();
        if (list == null || list.isEmpty()) {
            return false;
        }
        ProtocolTreeNode protocolTreeNode = user.getGorgeousEngine().queryContactNodeAllInfo(getTaskId(), list);
        user.getGorgeousEngine().AddTask(protocolTreeNode);
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
        // 检查基本结果
        SyncContactResult checkResult = checkResult(node, SyncContactResult.class);
        if (checkResult != null) {
            return checkResult;
        }

        // 获取同步节点
        ProtocolTreeNode syncNode = node.getOneChildren("usync");
        if (syncNode == null) {
            return handleSyncNodeError(node);
        }

        // 获取列表节点
        ProtocolTreeNode listNode = syncNode.getOneChildren("list");
        if (listNode == null) {
            return new SyncContactResult(StatusResult.fail("同步通讯录失败"));
        }

        // 解析联系人列表
        return parseContactList(listNode);
    }

    private SyncContactResult handleSyncNodeError(ProtocolTreeNode node) {
        if (StrUtil.indexOf(node.toString(), "rate-overlimit", 0, false) != -1) {
            return new SyncContactResult(StatusResult.fail("超出频率限制"));
        }
        return new SyncContactResult(StatusResult.fail("同步通讯录失败"));
    }

    private SyncContactResult parseContactList(ProtocolTreeNode listNode) {
        SyncContactResult result = new SyncContactResult();
        List<ContactResult> contactList = new ArrayList<>();
        List<StringUtil.JidInfo> jidInfos = new ArrayList<>();

        LinkedList<ProtocolTreeNode> userNodes = listNode.GetChildren("user");
        if (userNodes == null) {
            result.setList(contactList);
            return result;
        }

        for (ProtocolTreeNode userNode : userNodes) {
            ContactResult contact = parseUserNode(userNode, jidInfos);
            if (contact != null) {
                contactList.add(contact);
            }
        }

        result.setList(contactList);
        return result;
    }

    private ContactResult parseUserNode(ProtocolTreeNode userNode, List<StringUtil.JidInfo> jidInfos) {
        String jid = userNode.GetAttributeValue("jid");
        if (!StringUtils.hasLength(jid)) {
            return null;
        }

        ProtocolTreeNode contactNode = userNode.getOneChildren("contact");
        if (contactNode == null) {
            return null;
        }

        ContactResult contact = createBasicContact(jid, contactNode);

        // 只有存在的用户才需要处理详细信息
        if (contact.isExist()) {
            enrichContactInfo(userNode, contact, jidInfos);
        }

        return contact;
    }

    private ContactResult createBasicContact(String jid, ProtocolTreeNode contactNode) {
        String type = contactNode.GetAttributeValue("type");
        boolean exist = "in".equals(type);

        ContactResult contact = new ContactResult();
        contact.setUserId(jid);
        contact.setExist(exist);

        // 设置电话号码
        byte[] phoneBytes = contactNode.GetData();
        if (phoneBytes != null) {
            String phone = new String(phoneBytes).replace("+", "");
            contact.setPhone(phone);
        }

        return contact;
    }

    private void enrichContactInfo(ProtocolTreeNode userNode, ContactResult contact, List<StringUtil.JidInfo> jidInfos) {
        // 设置商业账号标识
        ProtocolTreeNode businessNode = userNode.getOneChildren("business");
        contact.setBusiness(businessNode != null);

        // 处理头像信息
        processPictureInfo(userNode, contact);

        // 处理状态信息
        processStatusInfo(userNode, contact);

        // 处理设备信息
        processDeviceInfo(userNode, contact, jidInfos);
    }

    private void processPictureInfo(ProtocolTreeNode userNode, ContactResult contact) {
        ProtocolTreeNode pictureNode = userNode.getOneChildren("picture");
        if (pictureNode == null) {
            return;
        }

        String directPath = pictureNode.GetAttributeValue("direct_path");
        if (!StringUtils.hasLength(directPath)) {
            return;
        }

        String url = "https://pps.whatsapp.net" + directPath;
        contact.setPicture(url);

        String pictureId = pictureNode.GetAttributeValue("id");
        long modifyTime = Convert.toLong(pictureId, 0L);
        if (modifyTime != 0) {
            contact.setModifyPictureTime(DateUtil.date(modifyTime * 1000).toString());
        }
    }

    private void processStatusInfo(ProtocolTreeNode userNode, ContactResult contact) {
        ProtocolTreeNode statusNode = userNode.getOneChildren("status");
        if (statusNode == null) {
            return;
        }

        String timestamp = statusNode.GetAttributeValue("t");
        long modifyTime = Convert.toLong(timestamp, 0L);
        byte[] describeBytes = statusNode.GetData();

        if (modifyTime != 0 && describeBytes != null) {
            contact.setDescribe(new String(describeBytes));
            contact.setModifyDescribeTime(DateUtil.date(modifyTime * 1000).toString());
        }
    }

    private void processDeviceInfo(ProtocolTreeNode userNode, ContactResult contact, List<StringUtil.JidInfo> jidInfos) {
        ProtocolTreeNode devicesNode = userNode.getOneChildren("devices");
        if (devicesNode == null) {
            return;
        }

        StringUtil.JidInfo jidInfo = StringUtil.ParseJid(contact.getUserId());
        LinkedList<ProtocolTreeNode> deviceListNodes = devicesNode.GetChildren("device-list");

        for (ProtocolTreeNode deviceListNode : deviceListNodes) {
            LinkedList<ProtocolTreeNode> deviceNodes = deviceListNode.GetChildren("device");

            if (deviceNodes == null || deviceNodes.isEmpty()) {
                // 处理空设备列表的情况
                String userId = String.format("%s.0:0@s.whatsapp.net", jidInfo.recipientId);
                jidInfos.add(StringUtil.ParseJid(userId));
                break;
            }

            // 处理每个设备
            for (ProtocolTreeNode deviceNode : deviceNodes) {
                String deviceId = deviceNode.GetAttributeValue("id");
                String userId = String.format("%s.0:%s@s.whatsapp.net", jidInfo.recipientId, deviceId);
                jidInfos.add(StringUtil.ParseJid(userId));
            }
        }
    }
}
