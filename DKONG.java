import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class Hitbox extends KObject {
    public int rx, ry;
    public int rollFactor = 0;

    Hitbox(Level level) {
        super(level);
    }
}

// вроде бы движущиеся объекты (ИГРОК, БОЧКИ)
//в онтик хз что проиходит, возможно движение объектов
class Living extends Hitbox {

    public float spdx, spdy;

    public boolean grounded = false;

    Living(Level level) {
        super(level);
    }

    @Override
    public void onTick() {
        super.onTick();

        float dx = spdx;
        float dy = spdy;

        int coll = 0;

        float l1 = x - rx;
        float r1 = x + rx;
        float t1 = y - ry;
        float b1 = y + ry;

        for (Hitbox wall : level.walls) {
            float l2 = wall.x - wall.rx;
            float r2 = wall.x + wall.rx;
            float t2 = wall.y - wall.ry;
            float b2 = wall.y + wall.ry;

            float coll_dy = 0.0f;
            if (r1 >= l2 && l1 <= r2) {
                if (dy > 0 && b1 <= t2 + 1e-4f) {
                    coll_dy = b1 + dy - t2;
                    if (coll_dy <= 0) {
                        coll_dy = 0;
                    } else {
                        if (this instanceof Barrel) {
                            spdx += wall.rollFactor * 0.06f;
                        }
                        coll |= 4;
                    }
                } else if (dy < 0 && t1 >= b2 - 1e-4f) {
                    coll_dy = t1 + dy - b2;
                    if (coll_dy >= 0) {
                        coll_dy = 0;
                    } else {
                        coll |= 8;
                    }
                }
            }

            float coll_dx = 0.0f;
            if (b1 >= t2 && t1 <= b2) {
                if (dx > 0 && r1 <= l2 + 1e-4f) {
                    coll_dx = r1 + dx - l2;
                    if (coll_dx <= 0) {
                        coll_dx = 0;
                    } else {
                        coll |= 1;
                    }
                } else if (dx < 0 && l1 >= r2 - 1e-4f) {
                    coll_dx = l1 + dx - r2;
                    if (coll_dx >= 0) {
                        coll_dx = 0;
                    } else {
                        coll |= 2;
                    }
                }
            }

            dx -= coll_dx;
            dy -= coll_dy;
        }

        grounded = (coll & 4) != 0;
        if ((coll & 2) != 0) {
            spdx = 0;
        }
        if ((coll & 1) != 0) {
            spdx = 0;
        }
        if ((coll & 8) != 0) {
            spdy = 0;
        }
        if ((coll & 4) != 0) {
            spdy = 0;
        }

        x += dx;
        y += dy;
    }
}

// объект лестницы
class Stairs extends Hitbox {
    public Stairs(Level level) {
        super(level);
        this.rx = 8;
        this.ry = 32;
        this.sprite = Drawer.loadImage("/stair.png");
    }

    @Override
    public void onTick() {
        super.onTick();
    }
}

// объект игрока
class Player extends Living {
    private float jump = 0.0f;

    public int keysDown = 0;

    // конструктор для инициализации
    public Player(Level level) {
        super(level);
        this.rx = 7;
        this.ry = 7;
        this.sprite = Drawer.loadImage("/player.png");
    }

    // каждый тик
    @Override
    public void onTick() {
        super.onTick();

        // если есть соприкосновение с объектами лэвлэксит то перезапускем игру
        if (level.intersects(LevelExit.class, this).findAny().isPresent()) {
            level.resetart1();
        }

        jump -= jump * 0.28f;
        spdx = 0;
        if ((keysDown & 2) != 0) {
            spdx -= 1.8f;
        }
        if ((keysDown & 4) != 0) {
            spdx += 1.8f;
        }
        if (level.intersects(Stairs.class, this).findAny().isPresent()) {
            spdy = 0;
            if ((keysDown & 1) != 0) {
                spdy -= 1.8f;
            }
            if ((keysDown & 8) != 0) {
                spdy += 1.8f;
            }
        } else {
            spdy += 0.981f + jump;
            if ((keysDown & 1) != 0) {
                if (grounded) {
                    jump = -3.5f;
                }
            }
        }
    }
}

// объект бочки
class Barrel extends Living {
    // конструктор для инициализации
    public Barrel(Level level) {
        super(level);
        this.rx = 6;
        this.ry = 6;
        this.sprite = Drawer.loadImage("/barel.png"); // задаем картинку
    }

    @Override
    public void onTick() {
        super.onTick();

        spdy += 0.981f; // помоему задает движение

        // если есть соприкосновение  с игроком перезапускаем игру
        level.intersects(Living.class, this).forEach(e -> {
            if (e instanceof Player) {
                level.restart0();
            }
        });
    }
}

// если каснуться этого объекта игра закончится, это бочка вроде, или поверх бочки
class LevelExit extends Hitbox {
    LevelExit(Level level) {
        super(level);
    }

    @Override
    public void onTick() {
        super.onTick();
// проверяем кждый тик есть ли касание, если да перезапускаем игру
        level.intersects(Living.class, this).forEach(e -> {
            if (e instanceof Player) {
                level.restart0();
            }
        });
    }
}
// класс для создания бочек
class BarrelSpawner extends KObject {
    public int untilspawn = 60; // помоему это значит что будем создавать бочку раз в 60 тиков

    BarrelSpawner(Level level) {
        super(level);
    }

    // каждый тик
    @Override
    public void onTick() {
        super.onTick();
// каждый тик уменьшаем значение untilspawn если оно стало 0 создаем новую бочку, добавляем ее в игру
        if (--untilspawn == 0) {
            untilspawn = 60;
            Barrel barrel = new Barrel(this.level);
            barrel.x = x;
            barrel.y = y;
            level.addwithoutconcurrentmodifiactionexception(barrel);
        }
    }
}

// класс отвечающий за инициализацию всех объектов игры, перезпуска игры
class Level {
    public final List<Hitbox> walls = new ArrayList<>(); // стены
    public final List<KObject> objects = new ArrayList<>(); // стены + лестницы + спавнер бочек + объект win
    private boolean restart = false; // перезапустить ли уровень
    private final List<KObject> toadd = new ArrayList<>(); // список объектов на добавление в основные списки, сначала сюда потом в верхний список

    public Player player; // объект игрока

    public Level() {
        // инициализируем объекты лестниц, стен, бочек, win:

        // бочки наверно, хз
        Hitbox b1 = new Hitbox(this);
        Hitbox b2 = new Hitbox(this);
        Hitbox b3 = new Hitbox(this);
        // стены
        Hitbox w1 = new Hitbox(this);
        Hitbox w2 = new Hitbox(this);
        // лестницы
        Stairs l1 = new Stairs(this);
        Stairs l2 = new Stairs(this);
        Stairs l3 = new Stairs(this);
        BarrelSpawner bsp = new BarrelSpawner(this); // объект который создает бочки
        LevelExit win = new LevelExit(this); // объект если дойти до которого игра перезапустится, типа выиграл

// изначальная позиция объектов
        b1.rx = 64;
        b1.ry = 4;
        b2.rx = 64;
        b2.ry = 4;
        b3.rx = 64;
        b3.ry = 4;
        w1.rx = 8;
        w1.ry = 999;
        w2.rx = 8;
        w2.ry = 999;

// задаем картинки для стен
        BufferedImage sprite = Drawer.loadImage("/wall.png");
        b1.sprite = sprite;
        b2.sprite = sprite;
        b3.sprite = sprite;

        b1.rollFactor = 1;
        b2.rollFactor = -1;
        b3.rollFactor = -1;

        b1.x = 112;
        b1.y = 80;
        b2.x = 128;
        b2.y = 128;
        b3.x = 128;
        b3.y = 32;
        w1.x = 200;
        w1.y = 96;
        w2.x = 40;
        w2.y = 96;
        l1.x = 184;
        l1.y = 92;
        l2.x = 56;
        l2.y = 44;
        l3.x = 168;
        l3.y = -4;
        bsp.x = 184;
        bsp.y = -32;
        win.x = 168;
        win.y = -32;

        // добавляем созданные объеыт в списки
        addWall(b1);
        addWall(b2);
        addWall(b3);
        addWall(w1);
        addWall(w2);
        addObject(l1);
        addObject(l2);
        addObject(l3);
        addObject(bsp);
        addObject(win);
    }

    public void addObject(KObject entity) {
        objects.add(entity);
    }

    public void addWall(Hitbox wall) {
        objects.add(wall);
        walls.add(wall);
    }

    // метод который вызывается каждый тик
    public void onTick() {
        // вызываем метод онтик у всех объектов игры
        for (KObject ent : objects) {
            ent.onTick();
        }

// если были какие то объекты на добавление в главный список то добавляем их
        for (KObject obj : toadd) {
            addObject(obj);
        }
        // очищаем список объктов на добавление
        toadd.clear();
        // если игру нужно перезапустить - перезапускаем
        if (restart) {
            resetart1();
            restart = false;
        }
    }


    public Stream<KObject> intersects(Class<? extends Hitbox> what, Hitbox e) {
        float l1 = e.x - e.rx;
        float r1 = e.x + e.rx;
        float t1 = e.y - e.ry;
        float b1 = e.y + e.ry;

        return objects.stream().filter((box) -> {
            if (!what.isInstance(box))
                return false;
            Hitbox oe = (Hitbox) box;
            float l2 = oe.x - oe.rx;
            float r2 = oe.x + oe.rx;
            float t2 = oe.y - oe.ry;
            float b2 = oe.y + oe.ry;

            return r1 > l2 && l1 < r2 && b1 > t2 && t1 < b2;
        });
    }

    // перезапуск игры
    public void resetart1() {
        objects.removeIf(Player.class::isInstance);
        objects.removeIf(Barrel.class::isInstance);

        // создаем нового игрока передаем в него объект уровня
        player = new Player(this);
        player.x = 72;
        player.y = 116;
        addObject(player);
    }

    // указать что нужно перезапустить игру
    public void restart0() {
        restart = true;
    }

    // добавление объекта во временный список чтобы потом перенести в основной без ошибки
    public void addwithoutconcurrentmodifiactionexception(KObject entity) {
        this.toadd.add(entity);
    }
}

// класс главного окна игры
class GameFrame extends JFrame implements KeyListener {

    private final Level level;

    public GameFrame() {
        setTitle("Donkey Kong"); // указываем заголовок окна
        setDefaultCloseOperation(EXIT_ON_CLOSE); // делаем чтобы оно закрывалось на крестик

        level = new Level(); // создаем уровень (все объекты игры)

        level.resetart1(); //  перезапкскаем уровень

        Drawer rv = new Drawer(level); // создаем панель на которой все будет рисоваться
        setContentPane(rv); // определяем ее как оснвную для окна

        this.addKeyListener(this); // добавляем лисенер клавиш

        // создаем таймер, который будет каждый тик выполнять два метода внутри себя
        Timer timer = new Timer(30, ev -> {
            level.onTick();
            rv.repaint();
        });
        timer.start(); // запускаем таймер

        setSize(256, 192); // указываем размер окна
        setResizable(false); // нельзя изменять размер окна
        setVisible(true); // показать окно
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (level.player == null) return;

        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT) {
            level.player.keysDown |= 2;
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            level.player.keysDown |= 4;
        } else if (keyCode == KeyEvent.VK_UP) {
            level.player.keysDown |= 1;
        } else if (keyCode == KeyEvent.VK_DOWN) {
            level.player.keysDown |= 8;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        if (level.player == null) return;

        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT) {
            level.player.keysDown &= ~2;
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            level.player.keysDown &= ~4;
        } else if (keyCode == KeyEvent.VK_UP) {
            level.player.keysDown &= ~1;
        } else if (keyCode == KeyEvent.VK_DOWN) {
            level.player.keysDown &= ~8;
        }
    }
}

// главная панель игры на которой все рисуется
class Drawer extends JComponent {
    private final Level level;

    // конструктор чтоб передать в него "уровень" игры
    public Drawer(Level level) {
        this.level = level;
    }

    // метод для отрисовки
    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.white); // выбираем белый цвет
        g.fillRect(0, 0, getWidth(), getHeight()); // рисуем прямоугольник

        // перебираем все объекты  если у них есть картинка то рисуем ее в нужной позиции
        for (KObject entity : level.objects) {
            BufferedImage spriteImage = entity.getSprite();
            if (spriteImage != null) {
                g.drawImage(
                        spriteImage,
                        (int) (entity.x - spriteImage.getWidth(this) / 2),
                        (int) (entity.y - spriteImage.getHeight(this) / 2),
                        this
                );
            }
        }
        g.dispose();
    }

    // метод для загрузки картинки объекта
    public static BufferedImage loadImage(String name) {
        URL resource = Drawer.class.getResource(name);

        try {
            return ImageIO.read(resource);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

// абстрактный класс объекта игры (бочка
abstract class KObject {
    public final Level level; //
    public float x, y; // позиция
    public BufferedImage sprite; // картинка объекта

    // конструктор - при создании передаем объект левел
    public KObject(Level level) {
        this.level = level;
    }

    // метод дл получения картинки объекта
    public BufferedImage getSprite() {
        return sprite;
    }

// ничего не делаем на тике лол
    public void onTick() {

    }
}

// главный класс который запускает окно игры
public class DKONG {
    public static void main(String[] args) {
        EventQueue.invokeLater(GameFrame::new);
    }
}
