import java.awt.Rectangle;

/**
 * 플레이어 엔티티
 */
public class Player {

    public enum Form {
        BASIC,
        YELLOW,
        BLUE
    }

    private double x;
    private double y;
    private double width;
    private double height;

    private double velX;
    private double velY;

    private boolean onGround;
    private boolean facingRight = true;

    private Form form = Form.BASIC;
    private boolean abilityReady;

    private boolean dashActive;
    private double dashTimeRemaining;

    // ===== 상수 =====
    private static final double DASH_DURATION = 0.25;
    private static final double BOUNCE_SPEED  = 280.0;
    private static final double DASH_SPEED    = 550.0;
    private static final double BLUE_JUMP     = 450.0;

    private static final double COL_PAD_X = 4.0;
    private static final double COL_PAD_Y = 3.0;

    public Player(double x, double y, double w, double h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    // ===== 이동 처리 =====
    public void applyHorizontalVelocity(double vx) {
        velX = vx;
        if (vx > 0) facingRight = true;
        else if (vx < 0) facingRight = false;
    }

    public void applyGravity(double gravity, double dt) {
        velY += gravity * dt;
    }

    // ===== 능력 =====
    public void useAbility() {
        if (!abilityReady) return;

        if (form == Form.YELLOW) {
            dashActive = true;
            dashTimeRemaining = DASH_DURATION;
            velX = facingRight ? DASH_SPEED : -DASH_SPEED;
            velY = -BLUE_JUMP * 0.45;
        }

        if (form == Form.BLUE) {
            velY = -BLUE_JUMP * 0.8;
        }

        form = Form.BASIC;
        abilityReady = false;
    }

    public void setFormYellow() {
        form = Form.YELLOW;
        abilityReady = true;
    }

    public void setFormBlue() {
        form = Form.BLUE;
        abilityReady = true;
    }

    // ===== 충돌 및 이동 =====
    public void moveAndCollide(MapLoader.MapData map, double dt, int tileSize) {
        double nx = x + velX * dt;
        double ny = y + velY * dt;

        x = moveAxis(map, nx, y, tileSize, true);
        double ry = moveAxis(map, x, ny, tileSize, false);

        onGround = false;

        if (ry != ny && velY > 0) {
            velY = -BOUNCE_SPEED;
            onGround = true;
        } else if (ry != ny && velY < 0) {
            velY = 0;
        }

        y = ry;

        if (dashActive) {
            dashTimeRemaining -= dt;
            if (dashTimeRemaining <= 0) dashActive = false;
        }
    }

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

        for (int y = 0; y < map.height; y++) {
            for (int x = 0; x < map.width; x++) {
                if (map.tiles[y][x] != MapLoader.TileType.WALL) continue;

                int tl = x * tileSize;
                int tr = tl + tileSize;
                int tt = y * tileSize;
                int tb = tt + tileSize;

                if (right <= tl || left >= tr || bottom <= tt || top >= tb) continue;

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

    public Rectangle getRect() {
        return new Rectangle((int)x, (int)y, (int)width, (int)height);
    }
}