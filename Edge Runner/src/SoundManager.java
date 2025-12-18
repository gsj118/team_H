import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundManager {

    // ===============================
    // Fields
    // ===============================

    private Clip bgmClip;

    private float currentBGMVolume = -15.0f;
    private float currentSFXVolume = -15.0f;

    // ===============================
    // Sound File Paths
    // ===============================

    public static final String BGM_MAIN_MENU =
            "audio/menu_bgm.wav";

    public static final String BGM_STAGE =
            "audio/stage_bgm.wav";

    public static final String SFX_JUMP =
            "audio/jump.wav";

    public static final String SFX_STAR_COLLECT =
            "audio/star_collect.wav";

    public static final String SFX_TYPING =
            "audio/typing.wav";

    // ===============================
    // Clip Loader
    // ===============================

    private Clip loadClip(String path) {

        try {
            File audioFile = new File(path);

            if (!audioFile.exists()) {
                System.err.println("사운드 파일 없음: " + path);
                return null;
            }

            AudioInputStream stream =
                    AudioSystem.getAudioInputStream(audioFile);

            Clip clip = AudioSystem.getClip();
            clip.open(stream);

            return clip;

        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        return null;
    }

    // ===============================
    // BGM Control
    // ===============================

    public void playBGM(String bgmPath) {

        stopBGM();

        bgmClip = loadClip(bgmPath);

        if (bgmClip == null) {
            return;
        }

        setBGMVolume(currentBGMVolume);

        bgmClip.setFramePosition(0);
        bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stopBGM() {

        if (bgmClip == null) {
            return;
        }

        bgmClip.stop();
        bgmClip.close();
        bgmClip = null;
    }

    public void setBGMVolume(float volume) {

        currentBGMVolume = volume;

        if (bgmClip == null) {
            return;
        }

        try {
            FloatControl gain =
                    (FloatControl) bgmClip.getControl(
                            FloatControl.Type.MASTER_GAIN
                    );

            float clamped =
                    Math.max(
                            gain.getMinimum(),
                            Math.min(gain.getMaximum(), volume)
                    );

            gain.setValue(clamped);

        } catch (IllegalArgumentException ignored) {
        }
    }

    // ===============================
    // SFX Control
    // ===============================

    public void playSFX(String sfxPath) {

        Clip clip = loadClip(sfxPath);

        if (clip == null) {
            return;
        }

        try {
            if (clip.isControlSupported(
                    FloatControl.Type.MASTER_GAIN)) {

                FloatControl gain =
                        (FloatControl) clip.getControl(
                                FloatControl.Type.MASTER_GAIN
                        );

                gain.setValue(currentSFXVolume);
            }

        } catch (IllegalArgumentException ignored) {
        }

        clip.addLineListener(event -> {

            if (event.getType() ==
                    LineEvent.Type.STOP) {

                clip.close();
            }
        });

        clip.setFramePosition(0);
        clip.start();
    }

    public void setSFXVolume(float volume) {

        currentSFXVolume = volume;
    }
}