import java.awt.Rectangle;

public class Player {

    // 플레이어 상태(능력 형태)
    public enum Form {
        BASIC,
        YELLOW,
        BLUE
    }

    // 위치 및 크기
    private double x;
    private double y;
    private double width;
    private double height;

    // 속도
    private double velX;
    private double velY;

    // 상태 플래그
    private boolean onGround;
    private boolean facingRight = true;

    // 능력 관련 상태
    private Form form = Form.BASIC;
    private boolean abilityReady;

    // 대시 상태
    private boolean dashActive;
    private double dashTimeRemaining;

    // ===== 물리/능력 상수 =====
    private static final double DASH_DURATION = 0.25;
    private static final double BOUNCE_SPEED  = 280.0;
    private static final double DASH_SPEED    = 550.0;
    private static final double BLUE_JUMP     = 450.0;

    // 충돌 판정 여유값
    private static final double COL_PAD_X = 4.0;
    private static final double COL_PAD_Y = 3.0;

    public Player(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    // 좌우 이동 속도 적용 및 바라보는 방향 갱신
    public void applyHorizontalVelocity(double vx) {
        velX = vx;
        if (vx > 0) facingRight = true;
        else if (vx < 0) facingRight = false;
    }

    // 중력 적용
    public void applyGravity(double gravity, double dt) {
        velY += gravity * dt;
    }

    // 현재 폼의 능력 사용
    public void useAbility() {
        if (!abilityReady) return;

        if (form == Form.YELLOW) {
            // 대시 능력
            dashActive = true;
            dashTimeRemaining = DASH_DURATION;
            velX = facingRight ? DASH_SPEED : -DASH_SPEED;
            velY = -BLUE_JUMP * 0.45;
        }

        if (form == Form.BLUE) {
            // 강화 점프
            velY = -BLUE_JUMP * 0.8;
        }

        // 능력 소모 후 기본 상태로 복귀
        form = Form.BASIC;
        abilityReady = false;
    }

    // 노란 능력 획득
    public void setFormYellow() {
        form = Form.YELLOW;
        abilityReady = true;
    }

    // 파란 능력 획득
    public void setFormBlue() {
        form = Form.BLUE;
        abilityReady = true;
    }

    // 이동 및 타일 충돌 처리
    public void moveAndCollide(MapLoader.MapData map, double dt, int tileSize) {

        boolean wasOnGround = onGround;

        double nx = x + velX * dt;
        double ny = y + velY * dt;

        // 축별 이동/충돌 처리
        x = moveAxis(map, nx, y, tileSize, true);
        double ry = moveAxis(map, x, ny, tileSize, false);

        onGround = false;

        // 바닥 충돌 처리
        if (ry != ny && velY > 0) {
            velY = -BOUNCE_SPEED;
            onGround = true;

            if (!wasOnGround) {
                GameCore.getSoundManager()
                        .playSFX(SoundManager.SFX_JUMP);
            }

        // 천장 충돌 처리
        } else if (ry != ny && velY < 0) {
            velY = 0;
        }

        y = ry;

        // 대시 시간 감소
        if (dashActive) {
            dashTimeRemaining -= dt;
            if (dashTimeRemaining <= 0) {
                dashActive = false;
            }
        }
    }

    // 단일 축 이동 및 벽 충돌 처리
    private double moveAxis(MapLoader.MapData map,
                            double tx, double ty,
                            int tileSize,
                            boolean horizontal) {

        double rx = horizontal ? tx : x;
        double ry = horizontal ? y  : ty;

        double left   = rx + COL_PAD_X;
        double right  = rx + width - COL_PAD_X;
        double top    = ry + COL_PAD_Y;
        double bottom = ry + height - COL_PAD_Y;

        // 모든 벽 타일과 충돌 검사
        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                if (map.tiles[y][x] != MapLoader.TileType.WALL) continue;

                int tl = x * tileSize;
                int tr = tl + tileSize;
                int tt = y * tileSize;
                int tb = tt + tileSize;

                if (right <= tl || left >= tr || bottom <= tt || top >= tb) continue;

                // 충돌 시 위치 보정
                if (horizontal) {
                    rx = velX > 0 ? tl - width - 0.01 : tr + 0.01;
                } else {
                    ry = velY > 0 ? tt - height - 0.01 : tb + 0.01;
                }
            }
        }
        return horizontal ? rx : ry;
    }

    // ===== Getter =====
    public double getX() { return x; }
    public double getY() { return y; }
    public double getVelY() { return velY; }
    public boolean isOnGround() { return onGround; }
    public Form getForm() { return form; }
    public boolean isDashing() { return dashActive; }

    // 충돌/렌더링용 사각형 반환
    public Rectangle getRect() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }
}