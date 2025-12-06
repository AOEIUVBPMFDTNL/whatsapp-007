package com.whatsapp.android.api.contact;

import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.XmppJid;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
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
 * 查询wsid
 */
public class QueryContactRequest extends AbstractRequest<SyncContactResult> {
    private SyncContactPack syncContactPack;

    public QueryContactRequest(SyncContactPack syncContactPack) {
        this.syncContactPack = syncContactPack;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.QUERY_CONTACT;
    }

    @Override
    public boolean request() {
        List<String> list = syncContactPack.getList();
        if (list == null || list.size() == 0) {
            return false;
        }
        GorgeousEngine gorgeousEngine = user.getGorgeousEngine();
        ProtocolTreeNode protocolTreeNode = gorgeousEngine.queryContact(getTaskId(), list);
        gorgeousEngine.AddTask(protocolTreeNode);
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
        if (participant != null) {
            for (ProtocolTreeNode protocolTreeNode : participant) {
                String userId = protocolTreeNode.GetAttributeValue("jid");
                if (StringUtils.hasLength(userId)) {
                    ProtocolTreeNode contact = protocolTreeNode.getOneChildren("contact");
                    String type = contact.GetAttributeValue("type");
                    ContactResult contactResult = new ContactResult();
                    boolean exist = "in".equals(type);
                    contactResult.setExist(exist);
                    byte[] bytes = contact.GetData();
                    String phone = new String(bytes);
                    phone = phone.replace("+", "");
                    contactResult.setPhone(phone);
                    if (exist) {
                        ProtocolTreeNode businessNode = protocolTreeNode.getOneChildren("business");
                        contactResult.setBusiness(businessNode != null);
                    }
                    try {
                        XmppJid xmppJid = XmppJid.of(userId);
                        if (ObjectUtil.isNotNull(xmppJid)) {
                            contactResult.setUserId(xmppJid.getUser());
                        }
                    } catch (Exception ignore) {
                    } finally {
                        list.add(contactResult);
                    }
                }
            }
        }
        syncContactResult.setList(list);
        return syncContactResult;
    }
}
