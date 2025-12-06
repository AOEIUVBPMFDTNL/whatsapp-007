package com.whatsapp.android;

import QRcode.C02700Ak;
import axolotl.AxolotlManager;
import cn.hutool.core.util.HexUtil;
import com.whatsapp.android.util.WhatsAppUtils;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author sunnoc
 * @date 2023-05-08 14:38
 */
public class DecodeId {
    public static void main(String[] args) throws CharacterCodingException {
        byte[] bytes2 = HexUtil.decodeHex("08D78901122105A9A1366A237D633BB131AACF30D58A7D4D06A945CA1F3BE3F821738CF636CB221A2088C8C3902A0345CDF71712474C60C5BC83B0CA711579DB8432BDB406BDB87953");
        System.out.println(cn.hutool.core.codec.Base64.encode(bytes2));
        // System.out.println(Base64.getUrlEncoder().encodeToString(AdjustId(0, 3)));
        /*byte[] decode = cn.hutool.core.codec.Base64.decode("L213MQABc9p4sUohosGblE/kcPhmh8SGnFNt9E3Y2GT85nwvvHrC1mbhqogy8NEQjV6PFB2ioV/3OHevlC6PoiuS/LtxbFEC080yUNUBMH6HNDWBs1fPnQu6WisnI/8criYlwWh+RPb6klesiE/LIxESSAqqn3Q=");
        System.out.println(new String(decode));*/
        // System.out.println(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(AdjustId(16884, 3))));
        System.out.println(DeAdjustId("\u0000H\u0012".getBytes(), 3));
        System.out.println(DeAdjustId(Base64.getUrlDecoder().decode("\u0000H "), 3));
        byte[] bytes = AdjustId(16878, 3);
        byte[] bytes1 = A03(16878);
        System.out.println(Base64.getUrlEncoder().encodeToString(bytes));
        System.out.println(DeAdjustId(bytes, 3));
        // System.out.println(DeAdjustId("EBiC".getBytes(), 3));
        /*String content = "Yl47S8HlKrOoMqMxT8oxn4HuBal5baxV+fvQD58Hmw8=";
        byte[] decode = Base64.decode(content);
        System.out.println(new String(decode));
        System.out.println(HexUtil.encodeHexStr(decode));*/
        /*System.out.println(bytesToInt(decode, 0));
        System.out.println(bytesToInt2(decode, 0));
        AxolotlManager axolotlManager = new AxolotlManager("/Users/sunnoc/Downloads/axolotl.db");
        byte[] serialize = axolotlManager.GetIdentityKeyPair().getPublicKey().serialize();
        byte[] data = new byte[32];
        System.arraycopy(serialize, 1, data, 0, 32);
        //IkuXR4Z7Q3o5MW9+AAt8IWmTVMuwF8FTJNo48xzS8lk=
        System.out.println(Base64.encode(data));*/
// "<iq id='2' xmlns='encrypt' type='set' to='s.whatsapp.net'><identity>Yl47S8HlKrOoMqMxT8oxn4HuBal5baxV+fvQD58Hmw8=</identity><registration>KELsnw==</registration><type>\u0005</type><list><key><id>Q*p</id><value>sdbpb5ImBlQo3PzZMqu+CscSa/bBj6HjEAdYwinPMCo=</value></key><key><id>Q*s</id><value>YeM9Hfh7YQn5d+PlqeRbLffHJ2tjhcGr8SqpEy68cWo=</value></key><key><id>Q*r</id><value>QWpDxR7Q3df6pS7tRIIC6eLWvKIACe/J/LG09xMLhF4=</value></key><key><id>Q*u</id><value>Ru0MThaCbkZJzKF0Q2Iy3aCnMDDe1bM4egNJD+U+D0g=</value></key><key><id>Q*t</id><value>a7TcBt3W6N96EtCmOV5cHt2/6c7l2GnfrBp/NBleMyo=</value></key><key><id>Q*w</id><value>dysMAEY4hZEnvvftCazTsyxvdwOraPQsOi0o5uayVgk=</value></key><key><id>Q*v</id><value>bHiwL2SdvND4DH+b26cvrD/JUvvPWem1gjqPrK1K/T4=</value></key><key><id>Q*y</id><value>4miGqYmXMB1dZ9P9UyuhQUX53gGtBPXWBVSNeSsSEAc=</value></key><key><id>Q*x</id><value>OSwPUx5JCCh6lVG50ub0fdZxZvDWyvCkdL+tHmoWpgM=</value></key><key><id>Q*{</id><value>BYiNCRC40ueqz1c1jgZoCZbr/v3YADQZE+CizLZfBh8=</value></key><key><id>Q*z</id><value>HtUJypslBPGRVPBllBkKGr8lD/HLuKCOwsZK67ddiy0=</value></key><key><id>Q*}</id><value>8+cQB9ilfIdg2ZMpkmXMXUxkSBoxIk2c/6Pvnf71ICk=</value></key><key><id>Q*|</id><value>HFRe4NyTw3rRYQ+Lgr8SmBsu7SmSgWnrgWUy4CMchAc=</value></key><key><id>Q*\u007F</id><value>B4HnF4fB+vUgXyqkKPEU6o2ECa7n6GsjkJsn0157SFY=</value></key><key><id>Q*~</id><value>OG0mH2m6B7EeJxaLK4KPM0EcrNTCRvVkHT/zXEEYv3I=</value></key><key><id>Q*m</id><value>HlL4BFJvuUcQd5F8uRZY0Osfv5OJJUTMxD017nznO08=</value></key><key><id>Q*o</id><value>x4IzfRIkl3yQC11WvebgMEeF89iNVJRNvtoN2kd8LQc=</value></key><key><id>Q*n</id><value>u+1LnJCk8dwJqulm8MI1La2ylg4+w+kkRc9chreh6zI=</value></key><key><id>USqR</id><value>NDzrZUMzrNIaHlLLSEjq+J1JUa3uJ7gcuntl1WUhPWc=</value></key><key><id>USqQ</id><value>/MxTcnmR0MbIPEHndEQaQOm1U1GAVkojuwncLGAk+2E=</value></key><key><id>USqT</id><value>K9Iok4LnjyFroTIevlh5+mYoaKEJugPX5DJy8qUCAH0=</value></key><key><id>USqS</id><value>WLliaKqR1B1RLAARDHw2Jhn05qygZ99CS23HFLp3T0Q=</value></key><key><id>USqV</id><value>Kgym+54BvZqjd69Pn18DG8neV7HXZSVutf6X8I03MCM=</value></key><key><id>USqU</id><value>wCBxa4ceUTM+0h/y/rapPL9ZVCZyvFdaAq5gMmtqtwc=</value></key><key><id>USqX</id><value>dHX34uLU+sp6E2TACVbPUC9wAeQ1O9IGniqbQgxpiSo=</value></key><key><id>USqW</id><value>b37KPxFUVQgZjqySrpmfo7DZvlr9iMQnTB6VCxUDFwY=</value></key><key><id>USqZ</id><value>rix+gvabx+eEvPxAQFXoqueclHX7iFSAdlMSR03oQVA=</value></key><key><id>USqY</id><value>JiDR15ZGOJKFFYucPflAVSwyeYHR0tHeFjcoHV4NdSE=</value></key><key><id>USqb</id><value>1DfCnCFhJiP/UwhO+2FBeIb1nRl/eWwek//CJsUf/l0=</value></key><key><id>USqa</id><value>cyK8uGnDykTg3tQzbXQzjldxzliGVMi2wz3bFcrJEVE=</value></key><key><id>USqd</id><value>QZNf8x+U84aJpv9o0UJWQi1SuOebP6aBa5V0WnhrAV8=</value></key><key><id>USqc</id><value>RKH4uwUU4Qo5Pw+b1j0x1opazqoIXe+DsF5o85MYTiE=</value></key><key><id>USqe</id><value>p/aqnmSw1d8CbulmPdoDx/XLbJ2UlAanVyGP+pz5Xx8=</value></key><key><id>USqB</id><value>E08GmisV4mS7cRDiDfEcjLaM3ojP0pvnSkTqGWpzyxY=</value></key><key><id>USqA</id><value>tCw9ZeRZ9aTBiMZKm3dWp6i5zSRrhoEhZRr/tpGqvy4=</value></key><key><id>USqD</id><value>ftwMmaMzac43ZNdFEcz8fqfk81UBUOmL8DxJlT5Q3yw=</value></key><key><id>USqC</id><value>f9kLM17daWuTwITYbswDNjbr9n2iNNPTnfByrmwCjzg=</value></key><key><id>USqF</id><value>IwQxl67BAj/j1v6BxN/8zfJJuM7J3h8j7P53QTb7szM=</value></key><key><id>USqE</id><value>KkVgblRCUOCv7tHJ9Tb+D9euxdEvzomnaOPWiDJpgW0=</value></key><key><id>USqH</id><value>IUjEEhTLdv9O1XKt44NiQ59uZHCUp1jtJtMq6GzXRHo=</value></key><key><id>USqG</id><value>Jz0EU88v6D/5PoBF6vXYpvVFjpuWsOKYRb2gxkS0IhQ=</value></key><key><id>USqJ</id><value>QBAdKoPHcLXBoCmO4TyenRQ8x1RuvNtrWNE51FbBTl8=</value></key><key><id>USqH</id><value>IUjEEhTLdv9O1XKt44NiQ59uZHCUp1jtJtMq6GzXRHo=</value></key><key><id>USqG</id><value>Jz0EU88v6D/5PoBF6vXYpvVFjpuWsOKYRb2gxkS0IhQ=</value></key><key><id>USqI</id><value>i4oYQHokg+unTmBrveoSHI/xhYCu90d9OOrofxE0KSA=</value></key><key><id>USqL</id><value>aoiZoKDT4i8mEfhscAJmRX9K581W1zzfMJVQ9CRepws=</value></key><key><id>USqK</id><value>EPWrODvzNfR/1vVBAHYr0QWiL0Nbx8MayCR3WbU3IUA=</value></key><key><id>USqN</id><value>F9kMpSc/hbATtJTqRGnMDzk/PSQfwEsoPYsN3SiAlWI=</value></key><key><id>USqM</id><value>g/wfdPEv5ItX/hX6OcKtIW9bp4fBh23Mpv/4/PAUtR4=</value></key><key><id>USqP</id><value>5VPJLbQqZBUCJUEaEQbM7hFhVVmRIJyqnTNs+VcKZ1E=</value></key><key><id>USqO</id><value>EjhrmXwwFeVk7oQLcjAbPiu21rAdgtcezIJefYqQ0AI=</value></key><key><id>USrR</id><value>sUSj9uyILPI5w91HqGHECOz3zOUUl9vh1uUDYb/GEm0=</value></key><key><id>USrQ</id><value>CPryxOaAeBGUQZ1IVLEDY7NPIgnznsy3sPvahAjFVyA=</value></key><key><id>USrA</id><value>w/EXbebZ8zr80wQKCdZJi1G0O2xMyhAoQlU6py5YEXA=</value></key><key><id>USrD</id><value>IFq0YBTdaSvedRuUl3JDihMB2HaMRSRfEEE+KPZPFRw=</value></key><key><id>USrC</id><value>D7gr24+8t3Ri9tkpOmhW/z+VS/w3OMTOJMqUJPZ4BGE=</value></key><key><id>USrF</id><value>rwwRY37NrKlRsrnJFKDeQVKibEuI+3UaKdjzipsa0Rs=</value></key><key><id>USrE</id><value>ZvybnvFkkNINxrj42jm/jPxTJF5YQBMDkRF1ZMmzoiM=</value></key><key><id>USrH</id><value>9bfCRIMvyElUMWYtz7+nXNgd90bQC+1aBimdYw+E51A=</value></key><key><id>USrG</id><value>AFNKjy8uQWctL78niNZCpd9pcViXDIBV7xi/DmZVMRs=</value></key><key><id>USrJ</id><value>GIdWfp3uuVsFdE5UADb8+YqR6GNfXWKRPsqcBMPpBQ4=</value></key><key><id>USrI</id><value>dIDknWBwoMdEOGxTtXm1bqj8yTzjRk8kTm7GJ7iT4BU=</value></key><key><id>USrL</id><value>vsasW/KbqefHXqzS0oJH8/rtX8FFu9f8+C0GLExAhxM=</value></key><key><id>USrK</id><value>5BuslULfe8+LnWZGoEjnaLUwxixIgjnFgRqHU/jaiBk=</value></key><key><id>USrN</id><value>3lmZjTAxu9JMr2OBdjfiK/PLFmQyHQQczaQaGQHciGY=</value></key><key><id>USrM</id><value>WtgamqqUZ6DKb+FfL/dwZomyw3f8QvadXGwfF8S3akk=</value></key><key><id>USrP</id><value>NtkJP1HJF6HdKDnRAaPiy3TgYjQDZ8YTTlnj666wQHI=</value></key><key><id>USrO</id><value>BlIvmT53sNog298P67+aDVJVwkr7T8ROtEjq2rIYLBc=</value></key><key><id>USqx</id><value>fhvzTDllWLrH6LRoRd0wOAUI5QdYhtIF9w57vhCDEmE=</value></key><key><id>USqw</id><value>SlzrNaaT/XLb1wLwj6dftdC0BsCtyCpq5WyjfZFglV0=</value></key><key><id>USqz</id><value>M/sb8Cxu17IYK2dbRhubioxk5iSlaAbLNW7jrKugRV0=</value></key><key><id>USqy</id><value>TRDF/o8fsQmsrD5Nk/OZbFAar46BI5K/AA21hRe3w3c=</value></key><key><id>USq1</id><value>BIwqis8aCcG/Lq+2J44qkFLzkkZjwg3c4EpnMJY71HI=</value></key><key><id>USq0</id><value>1VNMEXyD7DNpsVEIKpc/+XyIcu36vrYf85AC+jEWCB8=</value></key><key><id>USq3</id><value>VMEWTcxL8WHgJUMn6eW/i09kZiU43ZrUcW3j08ynux4=</value></key><key><id>USq2</id><value>nxJ7kkBpBjqDFBsDwS8D2fJGjg1OgTginyLBs9ENZh4=</value></key><key><id>USq5</id><value>PwXs7AXln+FsICgfFVZm536al08KtcG9TCR0ck2W9lw=</value></key><key><id>USq4</id><value>jv/ACVUJiSaSqijavmoOWyZ5ZNf2Mwob6tm0d3jBQhQ=</value></key><key><id>USq7</id><value>VpAJ+jy6bDNdItG/hcbVD6DGqfTLh1OjZtMdoEdV3GU=</value></key><key><id>USqi</id><value>XNSWU9dY4wQ6ho35q+3kAJmA9BshSwutHHJFCU+zUlA=</value></key><key><id>USql</id><value>o309ojLlqkx27lHQ8Te7kpSL5piIrjicA5HyrIR/mE4=</value></key><key><id>USqk</id><value>xOXyfS3ZMZz8hoK5db0v1Zam906zDmt4oD/kR3wvxx0=</value></key><key><id>USqn</id><value>EL72U81mqHX4QUv1/W8lBgvmoamJWfmmNaTmeSER8CI=</value></key><key><id>USqm</id><value>8TtL9guewEUMSmVEnw783wfdKBzMUpSq454MVCCoMww=</value></key><key><id>USqp</id><value>zxMOCpi4dbyugio9LIXFqOUr8jFkZluPugNKSWc2cBA=</value></key><key><id>USqo</id><value>V5UZfyel0kMUZHKRqlwKGMDqrv7o2r7fq1fxFc0mGF4=</value></key><key><id>USqr</id><value>a1BoovHOinfimSXaRWEBdFk7/zEpT3VzGaMCdqKKohI=</value></key><key><id>USqq</id><value>mGy8kLqxumO5FdKB9KNb/pZmsKqN+wxrNCYZkcW7cEE=</value></key><key><id>USqt</id><value>hMctfWMOteGX6DoxQ0QKx4TvH10JjsQln9eYAN9MmQc=</value></key><key><id>USqs</id><value>fpZIlOcpsEL09/t28Uxg/wVaZllNOwi5z5m7IF4++XI=</value></key><key><id>USqv</id><value>nQ3R7crKCz00SrRtyXMchNG+so8Ba4oXBFvMFJDquFs=</value></key><key><id>USqu</id><value>9rGbGr9x5JOkcymmxYEKePGS81MkULM39xRtVKgS8jE=</value></key><key><id>USrT</id><value>cERms4FEkp7zyyZNJwHkozeFIBdUtobBxtpqA0VNqio=</value></key><key><id>USrV</id><value>mR6P/LBixJ80jgRSOJW49iuT3tDR3P0ziGCTqXc/ui8=</value></key><key><id>USrU</id><value>+RaqT7KMs3/e45EAZDSZP7nCv0oH/eIvxNX7WJb0gHw=</value></key><key><id>USrX</id><value>XlTI5NSwew7XyjVy1JPkPZl8+Etu+DApr28ILz7c0l0=</value></key><key><id>USrW</id><value>MVAFhEyw0OZTzJS+6l8bL4RS5XeP7c92nykiqduYN1g=</value></key><key><id>USrZ</id><value>635fpLTo555iGj6fJchPsqEdMXMWq6xX/sGPZTWtuXM=</value></key><key><id>USrY</id><value>IrYOboXHWULy8reJfop6iysFUeLJPav9nR03zYnnsEs=</value></key><key><id>USrb</id><value>/OhV0kQlidkfQsUYZAuPkbw6gXli0I43mQXb8vKkHgs=</value></key><key><id>USra</id><value>HUtxCVZ7j4CQkpidnRW3jYQZU/kGX9MnOYrEnqawGiE=</value></key><key><id>USrd</id><value>egr85BKJwVDuflq7/RccJdWg11hwFpCeHMgHVsgOVAw=</value></key><key><id>USrc</id><value>ueOpqhg7M9TL75tX3aNIenZRSU1f/0AZoLdll636K0A=</value></key><key><id>USrf</id><value>i2YLXfoZ99qx0Zv7xg+FgdPgi0dIuKMCR+Caq5uzJn4=</value></key><key><id>USre</id><value>bumL9JKVQRlkTqNy/R35M+A3I1zMgzGsr24RGAXbY18=</value></key><key><id>USrx</id><value>b8yQ0HgrcHTa32kJPqj5PQe+IVka41dSsPCMB+ukLkw=</value></key><key><id>USrw</id><value>SgbEYDOl8MyL2EzQuuFVAYoO/Nz5Pv33ufc9YsG6X0o=</value></key><key><id>USrz</id><value>5N/saIVmpv9k1PeZQH/Dt5Atoxn1VZ025+7Mlzs1Ki4=</value></key><key><id>USry</id><value>mmQ+k4R4KEkzkKlDHxCkD0UQA0Vlfmbtzd26eHDV5AA=</value></key><key><id>USr1</id><value>trtebI7uRIuu06vMgEkuklOvnOcUqAxjpt1umZe52Fc=</value></key><key><id>USr0</id><value>6ucHmBWUpJR2QsuAWxoD+Zf+/he/EOdxHx/uSBSAwRk=</value></key><key><id>USr3</id><value>JDzINKdDgVH7Cbr4GZT619MQdnPjjISD5JyocwRCdDM=</value></key><key><id>USr2</id><value>aRwigu1O9fTNhSTI3RYvtbmHwvg3Ud6hz7Q6nV4YGEQ=</value></key><key><id>USr5</id><value>I8hbBqX8Hiv1SUTF+kiJUOIdSowe6gDyhq4+6POE6xo=</value></key><key><id>USr4</id><value>wq3BnQ5gtJx7If11FXraNSinlk5twWQb1GLnSOAPy0I=</value></key><key><id>USr7</id><value>Mw+6CYHjgg/6A3IzNOGj8WMkOaBS7qesj0ys563I4E0=</value></key><key><id>USr6</id><value>+GELq9EIP4OUlyfj0CnRyG1Mjse/lIp4wCMJF8718WQ=</value></key><key><id>USr9</id><value>k/Rwu3ciel79VEUaO6GVdSNuDzRES1KutARK1XpVoFM=</value></key><key><id>USr8</id><value>y7Frr+n+LeEaGkRGf+1XZ4PQr2LMcLrT9MdTIjHsnG0=</value></key><key><id>USr/</id><value>BOreO97AgKwy2QGeclwqPw+thw6AdMUHOUl1q5t7LFk=</value></key><key><id>USr+</id><value>PPt8opdl8w3PK/h2HjYS9pvm59VK2jF1NJLAc7tBVj0=</value></key><key><id>USrh</id><value>/n0da8Cyi2+DHerfqEk4ZOVu4+Kpxv5Vb5AV3IDYJyg=</value></key><key><id>USrg</id><value>HRMePfrB28vTENlDJxTdSF2nvf2CxzcpaFfKglvosBM=</value></key><key><id>USrj</id><value>9o/yyA9fYojG0+J4g3OZMNzoDFCKqIEp+WuLdsbTCgI=</value></key><key><id>USri</id><value>f1NxK6urxBuDN9lAcjlenUgS5TVQ2QYW3EQpE8aaHHY=</value></key><key><id>USrl</id><value>/ApeXWWX+qjQWKHaFqlwcCxkXLqpjRo78e2ruJXbQXQ=</value></key><key><id>USrk</id><value>zEIctv51Af+2xEJRi8f1WnL5Y3X4o2E0tbpDsmrnoA0=</value></key><key><id>USrn</id><value>j+13TwWseJPgkZjD2TA3KTnt69s5nDQttQKL2RxO6yQ=</value></key><key><id>USrm</id><value>bkkvVEwgflpC++d1/xUn5O5PxPvdT81zKPVvH6fz/0o=</value></key><key><id>USrp</id><value>hLXmVKkRmYCPfA8j3OiMtC5u9HzdxZQLAnkqWZg7PWA=</value></key><key><id>USro</id><value>m+5pu33GRdWlYvaMsWabZYvKMD/Wn4wUVJOn7WSkzig=</value></key><key><id>USrr</id><value>qWpEP8MWro0haMcDjSjoVWyc+QBlSxI1jaUaWO4+rBg=</value></key><key><id>USrq</id><value>pGSD+E7n3mTK/+Ypz4WDE2HRxXatNZDP6or4t+9yTFo=</value></key><key><id>USrt</id><value>hNDGlziFil7wlPuzRaDYZ2i0FPTBLyyDgIkyIP4dQHE=</value></key><key><id>USrs</id><value>8FqBoF3CJWyHjbou6J4twxLmJD4CNhXtFMLKfvQyiFE=</value></key><key><id>USrv</id><value>xwhdkiDcXZIEH8u70neWpo3k0Cac7ilpxxU1h+O0TEE=</value></key><key><id>USru</id><value>PKRJ/CGGn6NGrG81KhCvsTkF8WLccT1ZASnRebzFRyo=</value></key><key><id>Q+\u0001</id><value>vqpvvOfxVFgaxtx4ChX6rkYOaU3O7+pXKpebdq3KNSg=</value></key><key><id>Q+"

    }
    public static byte[] A03(int i) {
        byte[] bArr = new byte[3];
        bArr[2] = (byte) i;
        bArr[1] = (byte) (i >> 8);
        bArr[0] = (byte) (i >> 16);
        return bArr;
    }
    public static byte[] AdjustId(int input, int length) {
        byte[] result = new byte[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = (byte) (255 & input);
            input >>>= 8;
        }

        return result;
    }

    static int DeAdjustId(byte[] bytes, int length) {
        int result = 0;
        for (int i = 0; i < length; i++) {
            result = 256 * result + Byte.toUnsignedInt(bytes[i]);
        }

        return result;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }
}
