package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.response.profile.QueryContactTcTokenResult;
import com.whatsapp.android.entity.response.profile.TcToken;
import com.whatsapp.android.request.AbstractRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class QueryContactTcTokenRequest extends AbstractRequest<QueryContactTcTokenResult> {

    @Override
    public String funcName() {
        return TypeConstant.TaskType.QUERY_CONTACT_TC_TOKEN;
    }



    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public QueryContactTcTokenResult execute() {
        List<TcToken> contactsTcToken = user.getGorgeousEngine().axolotlManager_.trustedContactStore.getContactsTcToken();
        return new QueryContactTcTokenResult(contactsTcToken);
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public QueryContactTcTokenResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
