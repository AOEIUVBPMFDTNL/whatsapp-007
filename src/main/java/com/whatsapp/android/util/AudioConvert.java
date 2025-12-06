package com.whatsapp.android.util;


import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.FrameRecorder.Exception;

/**
 * Conversion audio parameters (including the sampling rate, coding, number of bits, number of channels)
 *
 * @author eguid
 */
public class AudioConvert {
    /**
     * Universal audio format conversion parameters
     *
     * @param inputFile    - Importing audio files
     * @param outputFile   - Export audio files
     * @param audioCodec   - Audio Coding
     * @param sampleRate   - audio sample rate
     * @param audioBitrate - audio bit rate
     */
    public static void convert(String inputFile, String outputFile, int audioCodec, int sampleRate, int audioBitrate,
                               int audioChannels) {
        Frame audioSamples = null;
        // audio recording (O address, an audio channel)
        FFmpegFrameRecorder recorder = null;
        // grabber
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);

        // open the gripper
        if (start(grabber)) {
            recorder = new FFmpegFrameRecorder(outputFile, audioChannels);
            recorder.setAudioOption("crf", "0");
            recorder.setAudioCodec(audioCodec);
            recorder.setAudioBitrate(audioBitrate);
            recorder.setAudioChannels(audioChannels);
            recorder.setSampleRate(sampleRate);
            recorder.setAudioQuality(0);
            recorder.setAudioOption("aq", "10");
            // open Recorder
            if (start(recorder)) {
                try {
                    // grab audio
                    while ((audioSamples = grabber.grab()) != null) {
                        recorder.setTimestamp(grabber.getTimestamp());
                        recorder.record(audioSamples);
                    }

                } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                    System.err.println("failed to fetch.");
                } catch (Exception e) {
                    System.err.println("Record failed");
                }
                stop(grabber);
                stop(recorder);
            }
        }

    }

    private static boolean start(FrameGrabber grabber) {
        try {
            grabber.start();
            return true;
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e2) {
            try {
                System.err.println("first opening grab fails, ready to restart grabber ...");
                grabber.restart();
                return true;
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                try {
                    System.err.println("Failed to restart the crawl, closing grabber ...");
                    grabber.stop();
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                    System.err.println("Stop grabber failed!");
                }
            }

        }
        return false;
    }

    private static boolean start(FrameRecorder recorder) {
        try {
            recorder.start();
            return true;
        } catch (Exception e2) {
            try {
                System.err.println("! Recorder fails to open for the first time ready to restart the recorder ...");
                recorder.stop();
                recorder.start();
                return true;
            } catch (Exception e) {
                try {
                    System.err.println("failure to restart the recorder is stopped Recorder ...!");
                    recorder.stop();
                } catch (Exception e1) {
                    System.err.println("Close recorder failed!");
                }
            }
        }
        return false;
    }

    private static boolean stop(FrameGrabber grabber) {
        try {
            grabber.flush();
            grabber.stop();
            return true;
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            return false;
        } finally {
            try {
                grabber.stop();
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                System.err.println("closing grab failed");
            }
        }
    }

    private static boolean stop(FrameRecorder recorder) {
        try {
            recorder.stop();
            recorder.release();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                recorder.stop();
            } catch (Exception e) {

            }
        }
    }
}