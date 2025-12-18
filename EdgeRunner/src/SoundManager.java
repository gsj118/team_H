import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundManager {

    // 현재 재생 중인 BGM 클립
    private Clip bgmClip;

    // 볼륨 상태값 (dB)
    private float currentBGMVolume = -15.0f;
    private float currentSFXVolume = -15.0f;

    // ===== 사운드 파일 경로 =====
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

    // 사운드 파일을 로드하여 Clip으로 반환
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

    // 배경 음악 재생 (기존 BGM은 중단)
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

    // 배경 음악 정지 및 자원 해제
    public void stopBGM() {

        if (bgmClip == null) {
            return;
        }

        bgmClip.stop();
        bgmClip.close();
        bgmClip = null;
    }

    // BGM 볼륨 설정
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

    // 효과음 1회 재생
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

        // 재생 종료 시 자동으로 자원 해제
        clip.addLineListener(event -> {

            if (event.getType() ==
                    LineEvent.Type.STOP) {

                clip.close();
            }
        });

        clip.setFramePosition(0);
        clip.start();
    }

    // 효과음 볼륨 설정
    public void setSFXVolume(float volume) {

        currentSFXVolume = volume;
    }
}