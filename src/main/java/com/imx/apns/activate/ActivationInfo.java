package com.imx.apns.activate;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
public class ActivationInfo {

    private String ActivationRandomness;
    private String ActivationState;
    private String BuildVersion;
    private byte[] DeviceCertRequest;
    private String DeviceClass;
    private String ProductType;
    private String ProductVersion;
    private String SerialNumber;
    private String UniqueDeviceID;


    public ActivationInfo() {
        ActivationRandomness = UUID.randomUUID().toString();
        UniqueDeviceID = UUID.randomUUID().toString();
    }

    @Getter
    @Setter
    public static class Builder {

        private String serialNumber;
        private String activationState;
        private String buildVersion;
        private String deviceClass;
        private String productType;
        private String productVersion;
        private byte[] deviceCertRequest;

        public Builder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder activationState(String activationState) {
            this.activationState = activationState;
            return this;
        }

        public Builder buildVersion(String buildVersion) {
            this.buildVersion = buildVersion;
            return this;
        }

        public Builder deviceClass(String deviceClass) {
            this.deviceClass = deviceClass;
            return this;
        }

        public Builder productType(String productType) {
            this.productType = productType;
            return this;
        }

        public Builder productVersion(String productVersion) {
            this.productVersion = productVersion;
            return this;
        }

        public Builder deviceCertRequest(byte[] deviceCertRequest) {
            this.deviceCertRequest = deviceCertRequest;
            return this;
        }

        public ActivationInfo build() {
            ActivationInfo activationInfo = new ActivationInfo();
            activationInfo.setSerialNumber(this.serialNumber);
            activationInfo.setActivationState(this.activationState);
            activationInfo.setBuildVersion(this.buildVersion);
            activationInfo.setDeviceClass(this.deviceClass);
            activationInfo.setProductType(this.productType);
            activationInfo.setProductVersion(this.productVersion);
            activationInfo.setDeviceCertRequest(this.deviceCertRequest);
            return activationInfo;
        }
    }
}
