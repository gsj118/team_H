import javax.sound.sampled.*;
import java.io.File; // ⭐️ [복구] File 기반 로딩을 위해 다시 필요합니다.
import java.io.IOException;

/**
 * SoundManager: 오디오 파일(WAV/AUFF) 재생을 담당하는 클래스
 * - 현재는 개발 환경(File 시스템) 기준으로 BGM이 재생됩니다.
 */
public class SoundManager {

    // BGM용 Clip 객체 (반복 재생용)
    private Clip bgmClip;
    
    // BGM 볼륨 값
    private float currentBGMVolume = -15.0f; 
    private float currentSFXVolume = -15.0f;

    // 사운드 파일 경로
    public static final String BGM_MAIN_MENU = "audio/menu_bgm.wav";
    public static final String BGM_STAGE = "audio/stage_bgm.wav";
    public static final String SFX_JUMP = "audio/jump.wav";
    public static final String SFX_STAR_COLLECT = "audio/star_collect.wav";
    public static final String SFX_TYPING = "audio/typing.wav";


    /**
     * ⭐️ [BGM 정상 재생을 위해 복구] 지정된 경로의 오디오 파일을 File 객체를 통해 로드합니다.
     * @param path 파일 시스템 경로 (예: audio/file.wav)
     */
    private Clip loadClip(String path) {
        try {
            File audioFile = new File(path);
            if (!audioFile.exists()) {
                System.err.println("사운드 파일을 찾을 수 없습니다: " + path);
                return null;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
            System.err.println("오디오 로드 중 오류 발생: " + path);
            return null;
        }
    }
    
    /**
     * BGM을 로드하고 반복 재생합니다.
     */
    public void playBGM(String bgmPath) {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
        }
        
        bgmClip = loadClip(bgmPath);
        if (bgmClip != null) {
            // 클립 로드 후, 저장된 볼륨 값 적용
            setBGMVolume(currentBGMVolume); 
            
            // 처음부터 재생 시작
            bgmClip.setFramePosition(0);
            // 무한 반복 재생
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    /**
     * 재생 중인 BGM을 정지합니다.
     */
    public void stopBGM() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
        }
    }

    /**
     * ⭐️ [추가된 기능] 재생 중인 BGM의 볼륨(Gain)을 조절합니다.
     * @param volume dB 값 (0.0f가 원본, 음수 값은 소리 감소)
     */
    public void setBGMVolume(float volume) {
        if (bgmClip != null) {
            try {
                FloatControl gainControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                
                float min = gainControl.getMinimum();
                float max = gainControl.getMaximum();
                
                // 입력 볼륨을 범위 내로 제한 (클램프)
                currentBGMVolume = Math.min(max, Math.max(min, volume));
                
                // 볼륨 값 적용
                gainControl.setValue(currentBGMVolume);
                
            } catch (IllegalArgumentException e) {
                System.err.println("오디오 클립이 볼륨 조절을 지원하지 않습니다.");
            }
        } else {
            // BGM이 로드되지 않은 경우, 볼륨 값만 저장
            currentBGMVolume = volume;
        }
    }

    /**
     * 효과음을 재생합니다.
     */
    public void playSFX(String sfxPath) {
        Clip clip = loadClip(sfxPath);
        if (clip != null) {
            try {
                // FloatControl을 사용하여 볼륨 조절
                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    
                    // 저장된 SFX 볼륨 값 적용
                    gainControl.setValue(currentSFXVolume);
                }

            } catch (IllegalArgumentException e) {
            	System.err.println("SFX 오디오 클립이 볼륨 조절을 지원하지 않습니다: " + sfxPath);
            }

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.setFramePosition(0);
            clip.start();
        }
    }
    
    /**
     * ⭐️ [추가] 재생될 효과음의 기본 볼륨 설정 부분
     * @param volume dB 값 (0.0f가 원본, 음수 값은 소리 감소)
     */
    public void setSFXVolume(float volume) {
        this.currentSFXVolume = volume;
    }
    
    
}