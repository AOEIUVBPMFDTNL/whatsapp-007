package com.whatsapp.android.service.impl.wam;

import Env.DeviceEnv;
import ProtocolTree.ProtocolTreeNode;
import ProtocolTree.StanzaAttribute;
import Wam.Wam;
import Wam.proto.WamEntryPointConversion;
import Wam.proto.WamEvent;
import Wam.proto.WamRecord;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.whatsapp.android.GorgeousEngine;
import com.whatsapp.android.entity.MccMnc;
import com.whatsapp.android.util.WamEventUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@UtilityClass
public class WamReportService {
    public ProtocolTreeNode reportPkMsgEvent(GorgeousEngine gorgeousEngine, String entryPointConversionSource) {
        ProtocolTreeNode iq = new ProtocolTreeNode("iq");
        iq.AddAttribute(new StanzaAttribute("id", gorgeousEngine.GenerateIqId()));
        iq.AddAttribute(new StanzaAttribute("xmlns", "w:stats"));
        iq.AddAttribute(new StanzaAttribute("type", "set"));
        iq.AddAttribute(new StanzaAttribute("to", "s.whatsapp.net"));
        long time = DateUtil.currentSeconds();
        ProtocolTreeNode add = new ProtocolTreeNode("add");
        add.AddAttribute(new StanzaAttribute("t", String.valueOf(time)));
        if (gorgeousEngine.isIosLogin()) {
            add.SetData(generateIOSBasicWamRecord(gorgeousEngine, time, entryPointConversionSource));
        } else {
            add.SetData(generateAndroidBasicWamRecord(gorgeousEngine, time, entryPointConversionSource));
        }
        iq.AddChild(add);
        return iq;
    }

    private byte[] generateAndroidBasicWamRecord(GorgeousEngine gorgeousEngine, long time, String entryPointConversionSource) {
        String username = gorgeousEngine.getUsername();
        DeviceEnv.AndroidEnv.Builder envBuilder_ = gorgeousEngine.getEnvBuilder_();
        WamRecord.Builder builder = WamRecord.newBuilder();
        if (gorgeousEngine.isBusinessVersion()) {
            builder.setPlatform(13);
        } else {
            builder.setPlatform(2);
        }
        DeviceEnv.UserAgent userAgent = envBuilder_.getUserAgent();
        builder.setDeviceName(userAgent.getManufacturer() + "-" + userAgent.getDevice());
        builder.setOsVersion(userAgent.getOsVersion());
        DeviceEnv.AppVersion appVersion = userAgent.getAppVersion();
        String version = String.format("%s.%s.%s.%s", appVersion.getPrimary(), appVersion.getSecondary(), appVersion.getTertiary(), appVersion.getQuaternary());
        builder.setAppVersion(version);
        builder.setAppIsBetaRelease(0);
        builder.setNetworkIsWifi(1);
        builder.setNetworkRadioType(1);
        builder.setModel(userAgent.getManufacturer());
        builder.setManufacturer(userAgent.getManufacturer());
        builder.setDevice(userAgent.getDevice());
        builder.setMemClass(256);
        builder.setTotalMemory(3765);
        builder.setAppBuild(4);
        builder.setAppDistribution(2);
        builder.setYearClass2016(2015);
        if (StrUtil.isNotEmpty(gorgeousEngine.getLocation())) {
            builder.setLocation(gorgeousEngine.getLocation());
        }
        if (StrUtil.isNotEmpty(gorgeousEngine.getAbKey2())) {
            builder.setAbKey2(gorgeousEngine.getAbKey2());
        }
        if (StrUtil.isNotEmpty(gorgeousEngine.getExpoKey())) {
            builder.setExpoKey(gorgeousEngine.getExpoKey());
        }
        builder.setOcVersion(1);
        builder.setIsMdOptIn(1);
        builder.setIsGooglePlayInstall(1);
        builder.setScreenDiagonal(497);
        builder.setIsCompanion(0);
        builder.setServiceImprovementOptOut(0);
        builder.setDeviceClassification(0);
        builder.setNumberOfInactiveAccounts(1);
        builder.setTimestamp(DateUtil.currentSeconds());
        /*MccMnc mccMnc = gorgeousEngine.getMccMnc();
        if (ObjectUtil.isNotNull(mccMnc)) {
            builder.setMcc(Long.parseLong(mccMnc.getMcc()));
            builder.setMnc(Long.parseLong(mccMnc.getMnc()));
        } else {
            builder.setMcc(Long.parseLong(userAgent.getMcc()));
            builder.setMnc(Long.parseLong(userAgent.getMnc()));
        }*/
        if (StringUtils.hasLength(entryPointConversionSource)) {
            WamEntryPointConversion.Builder wamEntryPointConversionBuild = WamEntryPointConversion.newBuilder();
            wamEntryPointConversionBuild.setEntryPointConverstionType(0);
            wamEntryPointConversionBuild.setEntryPointConversionSource(entryPointConversionSource);
            wamEntryPointConversionBuild.setFirstMessageTimeStamp(time);
            WamEvent.Builder wamEvent = WamEvent.newBuilder();
            wamEvent.setWamEntryPointConversion(wamEntryPointConversionBuild);
            builder.addEvents(wamEvent);
        } else {
            int eventNum = RandomUtil.randomInt(1, 5);
            int sessionId = (int) UUID.randomUUID().getLeastSignificantBits();
            long timeStamp = System.currentTimeMillis() - RandomUtil.randomInt(3600000, 10800000);
            for (int i = 0; i < eventNum; i++) {
                timeStamp = WamEventUtil.navigationMainChat(builder, timeStamp, sessionId, gorgeousEngine.getUnifiedSession());
            }
        }
        byte[] bytes = generateWStats(envBuilder_, builder);
        String encode = cn.hutool.core.codec.Base64.encode(bytes);
        log.debug("用户: {}, 生成pkmsg wstats: {}", username, encode);
        return bytes;
    }

    private byte[] generateIOSBasicWamRecord(GorgeousEngine gorgeousEngine, long time, String entryPointConversionSource) {
        String username = gorgeousEngine.getUsername();
        DeviceEnv.AndroidEnv.Builder envBuilder_ = gorgeousEngine.getEnvBuilder_();
        WamRecord.Builder builder = WamRecord.newBuilder();
        DeviceEnv.UserAgent userAgent = envBuilder_.getUserAgent();
        if (gorgeousEngine.isBusinessVersion()) {
            builder.setPlatform(15);
        } else {
            builder.setPlatform(1);
        }
        MccMnc mccMnc = gorgeousEngine.getMccMnc();
        /*if (ObjectUtil.isNotNull(mccMnc)) {
            builder.setMcc(Long.parseLong(mccMnc.getMcc()));
            builder.setMnc(Long.parseLong(mccMnc.getMnc()));
        } else {
            builder.setMcc(Long.parseLong(userAgent.getMcc()));
            builder.setMnc(Long.parseLong(userAgent.getMnc()));
        }*/
        builder.setDeviceName(userAgent.getDevice());
        String osVersion = userAgent.getOsVersion();
        builder.setOsVersion(osVersion);
        DeviceEnv.AppVersion appVersion = userAgent.getAppVersion();
        String version = String.format("%s.%s.%s.%s", appVersion.getPrimary(), appVersion.getSecondary(), appVersion.getTertiary(), appVersion.getQuaternary());
        builder.setAppVersion(version);
        builder.setAppIsBetaRelease(0);
        builder.setNetworkIsWifi(1);
        builder.setIphoneProcess(1);
        builder.setAppBuild(4);
        builder.setAppDistribution(2);
        if (StrUtil.isNotEmpty(gorgeousEngine.getLocation())) {
            builder.setLocation(gorgeousEngine.getLocation());
        }
        builder.setOcVersion(0);
        builder.setIsMdOptIn(0);
        builder.setIphoneOsBuildNumber(userAgent.getOsBuildNumber());
        builder.setIphoneSdkVersion(StrUtil.count(osVersion, ".") == 2 ? osVersion.substring(0, osVersion.lastIndexOf(".")) : osVersion);
        builder.setIsCompanion(0);
        builder.setServiceImprovementOptOut(0);
        builder.setDeviceClassification(0);
        builder.setTimestamp(DateUtil.currentSeconds());
        if (StringUtils.hasLength(entryPointConversionSource)) {
            WamEntryPointConversion.Builder wamEntryPointConversionBuild = WamEntryPointConversion.newBuilder();
            wamEntryPointConversionBuild.setEntryPointConverstionType(0);
            wamEntryPointConversionBuild.setEntryPointConversionSource(entryPointConversionSource);
            wamEntryPointConversionBuild.setFirstMessageTimeStamp(time);
            WamEvent.Builder wamEvent = WamEvent.newBuilder();
            wamEvent.setWamEntryPointConversion(wamEntryPointConversionBuild);
            builder.addEvents(wamEvent);
        } else {
            int eventNum = RandomUtil.randomInt(1, 5);
            int sessionId = (int) UUID.randomUUID().getLeastSignificantBits();
            long timeStamp = System.currentTimeMillis() - RandomUtil.randomInt(3600000, 10800000);
            for (int i = 0; i < eventNum; i++) {
                timeStamp = WamEventUtil.navigationMainChat(builder, timeStamp, sessionId, gorgeousEngine.getUnifiedSession());
            }
        }


        byte[] bytes = generateWStats(envBuilder_, builder);
        String encode = cn.hutool.core.codec.Base64.encode(bytes);
        log.debug("用户: {}, 生成pkmsg wstats: {}", username, encode);
        return bytes;
    }

    /**
     * 生成w:stats
     */
    private byte[] generateWStats(DeviceEnv.AndroidEnv.Builder envBuilder_, WamRecord.Builder record) {
        int connectionLc = envBuilder_.getConnectionLc();
        byte[] seqId = {(byte) connectionLc, (byte) (connectionLc >> 8), (byte) (connectionLc >> 16)};
        byte[] header = {0x57, 0x41, 0x4d, 0x05, 0x01};
        byte[] serialize = Wam.serialize(record.build());
        byte[] wstats = new byte[seqId.length + header.length + serialize.length];
        System.arraycopy(header, 0, wstats, 0, header.length);
        System.arraycopy(seqId, 0, wstats, header.length, seqId.length);
        System.arraycopy(serialize, 0, wstats, seqId.length + header.length, serialize.length);
        return wstats;
    }
}
