package com.whatsapp.android.util.ffmpeg;

/**
 * 媒体信息类
 */
public class MultimediaInfo {
    //视频格式
    private String format = null;
    //视频时长 单位（毫秒）
    private long duration = -1L;
    //音频信息
    private AudioInfo audio = null;
    //视频信息
    private VideoInfo video = null;

    public MultimediaInfo() {
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public AudioInfo getAudio() {
        return audio;
    }

    public void setAudio(AudioInfo audio) {
        this.audio = audio;
    }

    public VideoInfo getVideo() {
        return video;
    }

    public void setVideo(VideoInfo video) {
        this.video = video;
    }

    @Override
    public String toString() {
        return "MultimediaInfo{" + "format='" + format + '\'' + ", duration=" + duration + ", audio=" + audio
                + ", video=" + video + '}';
    }

    public static class AudioInfo {
        //解码器
        private String decoder;
        //采样率
        private int samplingRate = -1;
        //声道
        private int channels = -1;
        //比特率
        private int bitRate = -1;

        public AudioInfo() {
        }

        public String getDecoder() {
            return decoder;
        }

        public void setDecoder(String decoder) {
            this.decoder = decoder;
        }

        public int getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(int samplingRate) {
            this.samplingRate = samplingRate;
        }

        public int getChannels() {
            return channels;
        }

        public void setChannels(int channels) {
            this.channels = channels;
        }

        public int getBitRate() {
            return bitRate;
        }

        public void setBitRate(int bitRate) {
            this.bitRate = bitRate;
        }

        @Override
        public String toString() {
            return "AudioInfo{" + "decoder='" + decoder + '\'' + ", samplingRate=" + samplingRate + ", channels="
                    + channels + ", bitRate=" + bitRate + '}';
        }
    }

    public static class VideoInfo {
        //解码器
        private String decoder;
        //视频宽度
        private int width;
        //视频高度
        private int height;
        //比特率
        private int bitRate = -1;
        //帧率
        private float frameRate = -1.0F;

        public VideoInfo() {
        }

        public String getDecoder() {
            return decoder;
        }

        public void setDecoder(String decoder) {
            this.decoder = decoder;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getBitRate() {
            return bitRate;
        }

        public void setBitRate(int bitRate) {
            this.bitRate = bitRate;
        }

        public float getFrameRate() {
            return frameRate;
        }

        public void setFrameRate(float frameRate) {
            this.frameRate = frameRate;
        }

        @Override
        public String toString() {
            return "VideoInfo{" + "decoder='" + decoder + '\'' + ", width=" + width + ", height=" + height
                    + ", bitRate=" + bitRate + ", frameRate=" + frameRate + '}';
        }
    }
}