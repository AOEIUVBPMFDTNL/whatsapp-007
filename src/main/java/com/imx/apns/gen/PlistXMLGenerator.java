package com.imx.apns.gen;

import java.util.Map;

public class PlistXMLGenerator {

    public static String generate(Map<String, String> data) {
        StringBuilder xmlBuilder = new StringBuilder();
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xmlBuilder.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        xmlBuilder.append("<plist version=\"1.0\">\n");
        xmlBuilder.append("<dict>\n\t");

        for (Map.Entry<String, String> entry : data.entrySet()) {
            xmlBuilder.append("<key>").append(entry.getKey()).append("</key>\n\t");
            if (entry.getKey().equals("ActivationInfoXML") ||
                    entry.getKey().equals("FairPlayCertChain") ||
                    entry.getKey().equals("FairPlaySignature") ||
                    entry.getKey().equals("DeviceCertRequest")) {
                StringBuilder stringBuilder = new StringBuilder();
                indent(stringBuilder, entry.getValue());
                xmlBuilder.append("<data>").append(stringBuilder).append("\n\t</data>\n\t");
            } else if (entry.getKey().equals("ActivationInfoComplete")) {
                xmlBuilder.append("<true/>\n\t");
            } else {
                xmlBuilder.append("<string>").append(entry.getValue()).append("</string>\n\t");
            }
        }

        int lastIdx = xmlBuilder.lastIndexOf("\t");
        xmlBuilder.deleteCharAt(lastIdx);
        xmlBuilder.append("</dict>\n");
        xmlBuilder.append("</plist>\n");

        return xmlBuilder.toString();
    }

    private static void indent(StringBuilder xml, String text) {
        xml.append("\n");
        String[] var4 = text.split("\n");

        for (String line : var4) {
            xml.append("\t");
            xml.append(line);
            xml.append("\n");
        }

        int lastIdx = xml.lastIndexOf("\n");
        xml.deleteCharAt(lastIdx);
        xml.append("\t");
    }

}
