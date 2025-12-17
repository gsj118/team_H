import javax.sound.sampled.*;
import java.io.File;

public class SoundManager {

    // 파일 경로(프로젝트에 맞게 수정 가능)
    public static final String BGM_MAIN_MENU = "audio/menu_bgm.wav";
    public static final String BGM_STAGE     = "audio/stage_bgm.wav";

    private Clip bgmClip;

    public void playBGM(String path) {
        stopBGM();

        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(path));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            bgmClip = clip;
        } catch (Exception e) {
            // UnsupportedAudioFileException 포함해서 여기로 들어옴
            System.out.println("오디오 로드 중 오류 발생: " + path);
            e.printStackTrace();
        }
    }

    public void stopBGM() {
        if (bgmClip != null) {
            try {
                bgmClip.stop();
                bgmClip.close();
            } catch (Exception ignored) {}
            bgmClip = null;
        }
    }
}
