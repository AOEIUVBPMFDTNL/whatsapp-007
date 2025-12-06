package com.whatsapp.android.api.profile;

import ProtocolTree.ProtocolTreeNode;
import com.whatsapp.android.constant.TypeConstant;
import com.whatsapp.android.entity.StatusResult;
import com.whatsapp.android.entity.response.profile.TcToken;
import com.whatsapp.android.request.AbstractRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImportContactTcTokenRequest extends AbstractRequest<StatusResult> {
    private final List<TcToken> tcTokens;

    public ImportContactTcTokenRequest(List<TcToken> tcTokens) {
        this.tcTokens = tcTokens;
    }

    @Override
    public String funcName() {
        return TypeConstant.TaskType.IMPORT_CONTACT_TC_TOKEN;
    }


    @Override
    public boolean showLogs() {
        return false;
    }

    @Override
    public StatusResult execute() {
        user.getGorgeousEngine().axolotlManager_.trustedContactStore.batchInsertToken(tcTokens);
        return StatusResult.ok();
    }

    @Override
    public boolean request() {
        return false;
    }

    @Override
    public StatusResult parseResult(ProtocolTreeNode node) {
        return null;
    }
}
