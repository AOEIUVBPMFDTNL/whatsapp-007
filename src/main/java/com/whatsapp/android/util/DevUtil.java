package com.whatsapp.android.util;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sunnoc
 * @date 2020-07-28 11:18
 */
@Slf4j
public class DevUtil {
    /**
     * 获取终端机器码
     *
     * @return 机器码
     */
    public static String getMachineCode() {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardwareAbstractionLayer = systemInfo.getHardware();
        CentralProcessor centralProcessor = hardwareAbstractionLayer.getProcessor();
        ComputerSystem computerSystem = hardwareAbstractionLayer.getComputerSystem();
        String vendor = operatingSystem.getManufacturer();
        String processorSerialNumber = computerSystem.getSerialNumber();
        String processorIdentifier = centralProcessor.getProcessorIdentifier().getIdentifier();
        int processors = centralProcessor.getLogicalProcessorCount();
        String delimiter = "-";
        Digester md5 = new Digester(DigestAlgorithm.MD5);
        return md5.digestHex(String.format("%08x", vendor.hashCode()) + delimiter
                + String.format("%08x", processorSerialNumber.hashCode()) + delimiter
                + String.format("%08x", processorIdentifier.hashCode()) + delimiter + processors);
    }

    /**
     * 获取公网ip
     *
     * @return ip
     */
    public static String getPublicIp() {
        String ip = null;
        try {
            ip = HttpUtil.get("https://referee.immomo.com/get_ip", 5000);
            if (!Validator.isIpv4(ip)) {
                ip = null;
            }
        } catch (Exception ignored) {

        }
        if (StringUtils.isEmpty(ip)) {
            try {
                ip = HttpUtil.get("http://ipv4.gdt.qq.com/get_client_ip", 5000);
                if (!Validator.isIpv4(ip)) {
                    ip = null;
                }
            } catch (Exception ignored) {

            }
        }
        return ip;
    }

    /**
     * getTerminalIp
     */
    public static String getTerminalIp() throws IOException {
        BufferedReader br = null;
        try {
            String[] cmd = new String[]{"sh", "-c", "hostname -I"};
            Process ps = Runtime.getRuntime().exec(cmd);
            br = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            return StringUtils.trimWhitespace(br.readLine());
        } catch (Exception e) {
            log.error("获取内网ip失败", e);
        } finally {
            if (br != null) br.close();
        }
        return null;
    }

    /**
     * 获取系统cup，内存信息
     *
     * @return systemStatusInfo
     */
    public static SystemStatusInfo getSystemStatusInfo() {
        SystemInfo systemInfo = new SystemInfo();
        CentralProcessor processor = systemInfo.getHardware().getProcessor();
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // 睡眠1s
        ThreadUtil.sleep(1000);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        SystemStatusInfo systemStatusInfo = new SystemStatusInfo();
        systemStatusInfo.setCpuSize(processor.getLogicalProcessorCount());
        systemStatusInfo.setCpuUsePercent(new DecimalFormat("#.##%").format(1.0 - (idle * 1.0 / totalCpu)));
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        long totalByte = memory.getTotal();
        String totalMemory = formatByte(totalByte);
        systemStatusInfo.setSystemInfo("cpu:" + systemStatusInfo.getCpuSize() + ",memory:" + totalMemory);
        long availableByte = memory.getAvailable();
        systemStatusInfo.setMemorySize(formatByte(totalByte));
        String os = System.getProperty("os.name");
        String used = null;
        if (os.startsWith("Linux")) {
            Map<String, Map<String, String>> ramInfo = getRamInfo();
            try {
                used = ramInfo.get("mem").get("used");
            } catch (Exception ignored) {
            }
        }
        long usedByte;
        if (StringUtils.hasLength(used)) {
            usedByte = Long.parseLong(used);
        } else {
            usedByte = totalByte - availableByte;
        }
        systemStatusInfo.setMemoryUsePercent(NumberUtil.formatPercent((double) usedByte / totalByte, 1));
        return systemStatusInfo;
    }

    /**
     * 获取linux下 内存使用情况
     * mem 为内存
     * swap为交换区
     */
    public static Map<String, Map<String, String>> getRamInfo() {
        Map<String, Map<String, String>> result = new HashMap<>();
        try {
            String info = RuntimeUtil.execForStr("free -b");
            List<List<String>> lists = format(info);
            for (List<String> list : lists) {
                if (StringUtils.hasLength(list.get(0)) && list.get(0).toLowerCase().contains("mem")) {
                    Map<String, String> mem = new HashMap<>();
                    mem.put("total", list.get(1));
                    mem.put("used", list.get(2));
                    mem.put("free", list.get(3));
                    mem.put("shared", list.get(4));
                    mem.put("cache", list.get(5));
                    mem.put("available", list.get(6));
                    result.put("mem", mem);
                } else if (StringUtils.hasLength(list.get(0)) && list.get(0).toLowerCase().contains("swap")) {
                    Map<String, String> swap = new HashMap<>();
                    swap.put("total", list.get(1));
                    swap.put("used", list.get(2));
                    swap.put("free", list.get(3));
                    result.put("swap", swap);
                }
            }
        } catch (Exception e) {
            log.error("获取内存异常", e);
        }
        return result;
    }

    /**
     * 格式化linux返回
     * 返回一个二维数组
     */
    private static List<List<String>> format(String string) {
        List<List<String>> result = new ArrayList<>();
        if (StringUtils.hasLength(string)) {
            String[] lines = string.split("\n");
            for (String line : lines) {
                String[] strings = line.split(" ");
                List<String> temp = new ArrayList<>();
                for (String str : strings) {
                    if (StringUtils.hasLength(str)) {
                        temp.add(str);
                    }
                }
                result.add(temp);
            }
        }
        return result;
    }

    private static String formatByte(long byteNumber) {
        double format = 1024.0;
        double kbNumber = byteNumber / format;
        if (kbNumber < format) {
            return new DecimalFormat("#.##KB").format(kbNumber);
        }
        double mbNumber = kbNumber / format;
        if (mbNumber < format) {
            return new DecimalFormat("#.##MB").format(mbNumber);
        }
        double gbNumber = mbNumber / format;
        if (gbNumber < format) {
            return new DecimalFormat("#.##GB").format(gbNumber);
        }
        double tbNumber = gbNumber / format;
        return new DecimalFormat("#.##TB").format(tbNumber);
    }

    @Data
    public static class SystemStatusInfo {
        /**
         * cpu大小
         */
        private int cpuSize;
        /**
         * cpu使用百分比
         */
        private String cpuUsePercent;
        /**
         * 内存大小
         */
        private String memorySize;
        /**
         * 内存使用百分比
         */
        private String memoryUsePercent;
        /**
         * 系统信息
         */
        private String systemInfo;
    }
}
