package com.whatsapp.android.util.ffmpeg;


import com.whatsapp.android.util.WhatsAppUtils;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ffmpeg获取语音，视频信息
 */
public class FfmpegUtil {
    private static final Pattern FORMAT_PATTERN = Pattern.compile("^\\s*([D ])([E ])\\s+([\\w,]+)\\s+.+$");
    private static final Pattern ENCODER_DECODER_PATTERN = Pattern.compile("^\\s*([D ])([E ])([AVS]).{3}\\s+(.+)$", 2);
    private static final Pattern PROGRESS_INFO_PATTERN = Pattern.compile("\\s*(\\w+)\\s*=\\s*(\\S+)\\s*", 2);
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)", 2);
    private static final Pattern FRAME_RATE_PATTERN = Pattern.compile("([\\d.]+)\\s+(?:fps|tb\\(r\\))", 2);
    private static final Pattern BIT_RATE_PATTERN = Pattern.compile("(\\d+)\\s+kb/s", 2);
    private static final Pattern SAMPLING_RATE_PATTERN = Pattern.compile("(\\d+)\\s+Hz", 2);
    private static final Pattern CHANNELS_PATTERN = Pattern.compile("(mono|stereo)", 2);
    private static final Pattern SUCCESS_PATTERN = Pattern.compile("^\\s*video\\:\\S+\\s+audio\\:\\S+\\s+global headers\\:\\S+.*$", 2);
    private static final Pattern p1 = Pattern.compile("^\\s*Input #0, (\\w+).+$\\s*", 2);
    private static final Pattern p2 = Pattern.compile("^\\s*Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d).*$", 2);
    private static final Pattern p3 = Pattern.compile("^\\s*Stream #\\S+: ((?:Audio)|(?:Video)|(?:Data)): (.*)\\s*$", 2);


    public static void main(String[] args) {
        /*String sourceFile = "/Users/sunnoc/Downloads/南山076a6bc851a16c0165ffb174039c912f.mp4";
        String desFile = "/Users/sunnoc/Downloads/视频截图.png";
        MultimediaInfo info = WhatsAppUtils.videoCapture(sourceFile, desFile);
        if (info == null) {
            System.out.println("取媒体信息失败");
            return;
        }*/
        MultimediaInfo info = WhatsAppUtils.mp3ToOpus("/Users/sunnoc/Downloads/voice1.mp3", "/Users/sunnoc/Downloads/voice1.opus");
        System.out.println(info);

        /*String content = "Input #0, mov,mp4,m4a,3gp,3g2,mj2, from '/usr/javawork/whatsapp/南山076a6bc851a16c0165ffb174039c912f.mp4':\n" +
                "  Metadata:\n" +
                "    major_brand     : mp42\n" +
                "    minor_version   : 0\n" +
                "    compatible_brands: isommp42\n" +
                "    creation_time   : 2021-05-05T00:40:21.000000Z\n" +
                "    com.android.version: 9\n" +
                "    com.android.manufacturer: motorola\n" +
                "    com.android.model: moto g(6) play\n" +
                "  Duration: 00:00:10.17, start: 0.000000, bitrate: 614 kb/s\n" +
                "  Stream #0:0(eng): Audio: aac (LC) (mp4a / 0x6134706D), 44100 Hz, stereo, fltp, 56 kb/s (default)\n" +
                "    Metadata:\n" +
                "      creation_time   : 2021-05-05T00:40:21.000000Z\n" +
                "      handler_name    : SoundHandle\n" +
                "      vendor_id       : [0][0][0][0]\n" +
                "  Stream #0:1(eng): Video: h264 (Baseline) (avc1 / 0x31637661), yuv420p(tv, smpte170m/bt470bg/smpte170m), 480x848, 551 kb/s, SAR 1:1 DAR 30:53, 19.96 fps, 30 tbr, 90k tbn (default)\n" +
                "    Metadata:\n" +
                "      creation_time   : 2021-05-05T00:40:21.000000Z\n" +
                "      handler_name    : VideoHandle\n" +
                "      vendor_id       : [0][0][0][0]\n" +
                "Stream mapping:\n" +
                "  Stream #0:1 -> #0:0 (h264 (native) -> mjpeg (native))\n" +
                "Press [q] to stop, [?] for help\n" +
                "[swscaler @ 0x69df780] deprecated pixel format used, make sure you did set range correctly";
        String[] split = StrUtil.split(content, "\n");
        List<String> strings = Arrays.asList(split);
        MultimediaInfo info = parseMultimediaInfo(strings);*/


        /*String file = "/Users/sunnoc/Downloads/南山076a6bc851a16c0165ffb174039c912f.mp4";
        MultimediaInfo info = getVideoInfo(file);
        // 时长信息
        long duration = info.getDuration();
        System.out.println("视频时长为：" + duration / 1000 + "秒");
        // 音频信息
        MultimediaInfo.AudioInfo audio = info.getAudio();
        if (audio != null) {
            int bitRate = audio.getBitRate();  // 比特率
            int channels = audio.getChannels();  // 声道
            String decoder = audio.getDecoder();  // 解码器
            int sRate = audio.getSamplingRate();  // 采样率
            System.out.println("解码器：" + decoder + "，声道：" + channels + "，比特率：" + bitRate + "，采样率：" + sRate);
        }*/
        long duration = info.getDuration();
        System.out.println("视频时长为：" + duration + "毫秒");
        // 视频信息
        MultimediaInfo.VideoInfo video = info.getVideo();
        if (video != null) {
            int bitRate2 = video.getBitRate();
            Float fRate = video.getFrameRate();  // 帧率
            int height = video.getHeight();  // 视频高度
            int width = video.getWidth();  // 视频宽度
            System.out.println("视频帧率：" + fRate + "，比特率：" + bitRate2 + "，视频高度：" + height + "，视频宽度：" + width);
        }
    }

    /**
     * 处理媒体信息流
     *
     * @param list
     * @return
     */
    public static MultimediaInfo parseMultimediaInfo(List<String> list) {
        MultimediaInfo info = null;
        int step = 0;
        for (String line : list) {
            if (line == null) {
                break;
            }
            String type;
            String specs;
            if (step == 0) {
                Matcher m = p1.matcher(line);
                if (m.matches()) {
                    specs = m.group(1);
                    info = new MultimediaInfo();
                    info.setFormat(specs);
                    ++step;
                }
            } else {
                Matcher m;
                if (step == 1) {
                    m = p2.matcher(line);
                    if (m.matches()) {
                        long hours = Long.parseLong(m.group(1));
                        long minutes = Long.parseLong(m.group(2));
                        long seconds = Long.parseLong(m.group(3));
                        long dec = Long.parseLong(m.group(4));
                        long duration = dec * 100L + seconds * 1000L + minutes * 60L * 1000L + hours * 60L * 60L * 1000L;
                        info.setDuration(duration);
                        ++step;
                    }
                } else if (step == 2) {
                    m = p3.matcher(line);
                    if (m.matches()) {
                        type = m.group(1);
                        specs = m.group(2);
                        StringTokenizer st;
                        String token;
                        Matcher m2;
                        int i;
                        boolean parsed;
                        int bitRate;
                        if ("Video".equalsIgnoreCase(type)) {
                            MultimediaInfo.VideoInfo video = new MultimediaInfo.VideoInfo();
                            st = new StringTokenizer(specs, ",");

                            for (i = 0; st.hasMoreTokens(); ++i) {
                                token = st.nextToken().trim();
                                if (i == 0) {
                                    video.setDecoder(token);
                                } else {
                                    parsed = false;
                                    m2 = SIZE_PATTERN.matcher(token);
                                    if (!parsed && m2.find()) {
                                        bitRate = Integer.parseInt(m2.group(1));
                                        int height = Integer.parseInt(m2.group(2));
                                        //video.setSize(new VideoSize(bitRate, height));
                                        video.setWidth(bitRate);
                                        video.setHeight(height);
                                        parsed = true;
                                    }

                                    m2 = FRAME_RATE_PATTERN.matcher(token);
                                    if (!parsed && m2.find()) {
                                        try {
                                            float frameRate = Float.parseFloat(m2.group(1));
                                            video.setFrameRate(frameRate);
                                        } catch (NumberFormatException var20) {
                                        }

                                        parsed = true;
                                    }

                                    m2 = BIT_RATE_PATTERN.matcher(token);
                                    if (!parsed && m2.find()) {
                                        bitRate = Integer.parseInt(m2.group(1));
                                        video.setBitRate(bitRate);
                                    }
                                }
                            }

                            info.setVideo(video);
                        } else if ("Audio".equalsIgnoreCase(type)) {
                            MultimediaInfo.AudioInfo audio = new MultimediaInfo.AudioInfo();
                            st = new StringTokenizer(specs, ",");

                            for (i = 0; st.hasMoreTokens(); ++i) {
                                token = st.nextToken().trim();
                                if (i == 0) {
                                    audio.setDecoder(token);
                                } else {
                                    parsed = false;
                                    m2 = SAMPLING_RATE_PATTERN.matcher(token);
                                    if (!parsed && m2.find()) {
                                        bitRate = Integer.parseInt(m2.group(1));
                                        audio.setSamplingRate(bitRate);
                                        parsed = true;
                                    }

                                    m2 = CHANNELS_PATTERN.matcher(token);
                                    if (!parsed && m2.find()) {
                                        String ms = m2.group(1);
                                        if ("mono".equalsIgnoreCase(ms)) {
                                            audio.setChannels(1);
                                        } else if ("stereo".equalsIgnoreCase(ms)) {
                                            audio.setChannels(2);
                                        }

                                        parsed = true;
                                    }

                                    m2 = BIT_RATE_PATTERN.matcher(token);
                                    if (!parsed && m2.find()) {
                                        bitRate = Integer.parseInt(m2.group(1));
                                        audio.setBitRate(bitRate);
                                    }
                                }
                            }
                            info.setAudio(audio);
                        }
                    }
                }
            }
        }
        return info;
    }
}