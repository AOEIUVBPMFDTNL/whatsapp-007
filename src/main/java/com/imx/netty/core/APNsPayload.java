package com.imx.netty.core;

import com.imx.apns.common.APNsState;
import com.imx.apns.gen.NonceGenerator;
import com.imx.common.Pair;
import com.imx.common.util.ByteUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Getter
public class APNsPayload implements Serializable {

    private final int id;
    private final List<Pair<Byte, byte[]>> fields;

    public APNsPayload(int id, List<Pair<Byte, byte[]>> fields) {
        this.id = id;
        this.fields = fields;
    }

    public static class Serializer {

        public static byte[] serialize(APNsPayload payload) {
            List<byte[]> payloadParts = new ArrayList<>();

            for (Pair<Byte, byte[]> field : payload.getFields()) {
                ByteBuffer fieldBuffer = ByteBuffer.allocate(1 + 2 + field.getValue().length);
                fieldBuffer.put(field.getKey());
                fieldBuffer.putShort((short) field.getValue().length);
                fieldBuffer.put(field.getValue());

                payloadParts.add(fieldBuffer.array());
            }

            int payloadLength = payloadParts.stream().mapToInt(arr -> arr.length).sum();
            ByteBuffer finalBuffer = ByteBuffer.allocate(1 + 4 + payloadLength);
            finalBuffer.put((byte) payload.getId());
            finalBuffer.putInt(payloadLength);
            for (byte[] part : payloadParts) {
                finalBuffer.put(part);
            }
            return finalBuffer.array();
        }
    }

    static class Deserializer {

        private static APNsPayload deserialize(ByteBuf in) throws IOException {
            if (in.readableBytes() < 5) {
                return null;
            }
            in.markReaderIndex();

            byte id = in.readByte();
            int len = in.readInt();

            if (in.readableBytes() < len) {
                in.resetReaderIndex();
                return null;
            }

            // Read the payload
            ByteBuf payloadBuf = in.readSlice(len);
            List<Pair<Byte, byte[]>> fields = new ArrayList<>();

            while (payloadBuf.isReadable()) {
                // 1 byte field ID + 2 bytes field length
                if (payloadBuf.readableBytes() < 3) {
                    throw new IOException("Failed to read field");
                }

                byte fid = payloadBuf.readByte();
                short flen = payloadBuf.readShort();

                if (payloadBuf.readableBytes() < flen) {
                    throw new IOException("Failed to read field value");
                }

                byte[] fval = new byte[flen];
                payloadBuf.readBytes(fval);

                fields.add(new Pair<>(fid, fval));
            }
            return new APNsPayload(id, fields);
        }

    }

    public static APNsPayload read(InputStream inputStream) throws IOException {
        byte id = (byte) inputStream.read();

        if (id == 0x0 || id == 0xD) {
            return null;
        }

        byte[] lenBytes = new byte[4];
        if (inputStream.read(lenBytes) != 4) {
            throw new IOException("Failed to read length");
        }
        int len = ByteBuffer.wrap(lenBytes).getInt();

        byte[] buf = new byte[len];
        if (inputStream.read(buf) != len) {
            throw new IOException("Failed to read buffer");
        }

        ByteBuffer currBuf = ByteBuffer.wrap(buf);
        List<Pair<Byte, byte[]>> fields = new ArrayList<>();
        while (currBuf.remaining() > 0) {
            byte fid = currBuf.get();
            byte[] flenBytes = new byte[2];
            currBuf.get(flenBytes);
            int flen = ByteBuffer.wrap(flenBytes).getShort();

            byte[] fval = new byte[flen];
            currBuf.get(fval);

            fields.add(new Pair<>(fid, fval));
        }

        return new APNsPayload(id, fields);
    }

    public static APNsPayload make(APNsState apnsState) throws Exception {
        int flags = 0b01000001;
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(apnsState.getPair().getKey()));

        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(privateKey);

        byte key = 0x0;
        byte[] nonce = NonceGenerator.generateNonce(key);
        signer.update(nonce);
        byte[] signature = ByteUtil.concat(new byte[]{0x1, 0x1}, signer.sign());

        List<Pair<Byte, byte[]>> fields = new ArrayList<>();

        fields.add(new Pair<>((byte) 0x2, new byte[]{0x01}));
        fields.add(new Pair<>((byte) 0x5, ByteBuffer.allocate(4).putInt(flags).array()));
        fields.add(new Pair<>((byte) 0xC, apnsState.getPair().getValue()));
        fields.add(new Pair<>((byte) 0xD, nonce));
        fields.add(new Pair<>((byte) 0xE, signature));

        if (apnsState.getToken() != null) {
            fields.add(new Pair<>((byte) 1, apnsState.getToken()));
        }

        return new APNsPayload(7, fields);
    }

    public static APNsPayload make() {
        return new APNsPayload(0x0C, Collections.emptyList());
    }

    public static APNsPayload make(List<String> topics, byte[] token) throws Exception {
        List<Pair<Byte, byte[]>> fields = new ArrayList<>();

        fields.add(new Pair<>((byte) 0x1, token));
        for (String topic : topics) {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            fields.add(new Pair<>((byte) 0x2, md.digest(topic.getBytes())));
        }

        return new APNsPayload(9, fields);
    }

    public static APNsPayload make(int state) {
        int magic_num = 0x7FFFFFFF;
        List<Pair<Byte, byte[]>> fields = new ArrayList<>();
        fields.add(new Pair<>((byte) 1, ByteUtil.convert(state)));
        fields.add(new Pair<>((byte) 2, ByteUtil.convert(magic_num)));

        return new APNsPayload(0x14, fields);
    }

    public static APNsPayload make(byte[] id, byte[] token) {
        List<Pair<Byte, byte[]>> fields = new ArrayList<>();
        fields.add(new Pair<>((byte) 1, token));
        fields.add(new Pair<>((byte) 4, id));
        fields.add(new Pair<>((byte) 8, new byte[]{0x0}));

        return new APNsPayload(0x0B, fields);
    }

    public Optional<byte[]> getField(byte field) {
        for (Pair<Byte, byte[]> pair : fields) {
            if (pair.getKey() == field) {
                return Optional.of(pair.getValue());
            }
        }
        return Optional.empty();
    }

    public void encode(ByteBuf out) {
        byte[] bytes = Serializer.serialize(this);
        out.writeBytes(bytes);
    }

    public static Optional<APNsPayload> decode(ByteBuf in) throws IOException {
        APNsPayload payload = Deserializer.deserialize(in);
        return Optional.ofNullable(payload);
    }
}

