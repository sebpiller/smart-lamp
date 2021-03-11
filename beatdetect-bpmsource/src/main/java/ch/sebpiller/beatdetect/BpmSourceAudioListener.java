package ch.sebpiller.beatdetect;

import ch.sebpiller.metronome.Tempo;
import ddf.minim.AudioInput;
import ddf.minim.AudioListener;
import ddf.minim.Minim;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.javasound.JSMinim;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;
import java.util.Queue;

/**
 * A BpmSource that connects directly an {@link ddf.minim.spi.AudioStream} (Minim) (system line-in by default)
 * and automatically find the tempo of the music actually playing.
 */
public class BpmSourceAudioListener implements Tempo, AudioListener {
    private static final Logger LOG = LoggerFactory.getLogger(BpmSourceAudioListener.class);

    private static Tempo lineIn;
    // 20 items in the buffer: that means the tempo returned is computed using the average
    // of the ~ 8..12 last seconds of measured beat.
    private final Queue<Float> mostRecentDetectedBpms = new CircularFifoQueue<>(20);
    private BeatDetect beatDetect;
    private long lastBeatNanoTime = 0;
    private float average;

    /**
     * use the system line in to catch sound.
     */
    private BpmSourceAudioListener() {
        initAudioInput(new Minim(new JSMinim(this)).getLineIn());
    }

    public BpmSourceAudioListener(AudioInput audioInput) {
        initAudioInput(audioInput);
    }

    public synchronized static Tempo getBpmFromLineIn() {
        if (lineIn == null) {
            lineIn = new BpmSourceAudioListener();
        }
        return lineIn;
    }

    private void initAudioInput(AudioInput audioInput) {
        AudioFormat format = audioInput.getFormat();
        this.beatDetect = new BeatDetect(format.getFrameSize(), format.getSampleRate());
        this.beatDetect.detectMode(BeatDetect.SOUND_ENERGY/**//*FREQ_ENERGY*/);
        this.beatDetect.setSensitivity(300);
        audioInput.addListener(this);
    }

    @Override
    public void samples(float[] samp) {
        onSamplesAcquired(samp);
    }

    @Override
    public void samples(float[] sampL, float[] sampR) {
        onSamplesAcquired(sampL);
    }

    private void onSamplesAcquired(float[] samp) {
        long now = System.nanoTime();
        this.beatDetect.detect(samp);

        if (this.beatDetect.isOnset() || this.beatDetect.isKick()) {

            if (this.lastBeatNanoTime != 0) {
                float detectedBpm = (float) (60_000_000_000d / (now - this.lastBeatNanoTime));

                // suspicious tempos are ignored
                if (detectedBpm > 80 && detectedBpm < 180) {
                    synchronized (this.mostRecentDetectedBpms) {
                        this.mostRecentDetectedBpms.add(detectedBpm);
                        LOG.info("detected bpm: {}", detectedBpm);

                        if (LOG.isTraceEnabled()) {
                            LOG.trace("  > bpms are: {}", Arrays.toString(this.mostRecentDetectedBpms.toArray()));
                        }
                        this.average = (float) this.mostRecentDetectedBpms.stream()
                                .mapToDouble((x) -> x).summaryStatistics()
                                .getAverage()
                        ;
                    }
                }
            }

            this.lastBeatNanoTime = now;
        }
    }

    @Override
    public Number get() {
            return this.average;
        }
}