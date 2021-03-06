package strategybots.games.legacy;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;

public class HyperMNK implements Serializable {
    
    public enum Gravity { NONE, POSITIVE, NEGATIVE }
    
    private static final String TITLE = "HyperMNK";
    
    private static final int MAX_WIDTH = 1600, MAX_HEIGHT = 900;
    
    private static final Color BCKG_COLOR = new Color(225, 225, 255),
                                  HIGHLIGHT_COLOR = new Color(123, 239, 178);
    
    private static final int BCKG_CONTRAST = 16;
    
    private static int PIECE_SIZE;
    
    private final Color[] DEFAULT_COLORS = new Color[] {
            new Color(87, 95, 207),
            new Color(255, 94, 87),
            new Color(5, 196, 107),
            new Color(255, 211, 42),
    };
    
    private int[] board;
    
    protected JFrame window;
    protected JButton[] buttons;
    
    private int[] dimensions;
    
    private Gravity[] gravity;
    private int gravityIndex = -1;
    
    private int target;
    
    protected List<HyperMNKPlayer> players = new ArrayList<>();
    protected List<Color> playerColors = new ArrayList<>();
    
    private Optional<HyperMNKPlayer> winner = Optional.empty();
    
    private HyperMNKPlayer currentPlayer;
    private volatile int currentPlayerId = 1;
    private volatile boolean turnTaken;
    
    private HyperMNK() {}
    
    public static HyperMNKBuilder create() { return new HyperMNKBuilder(); }
    
    public void placePiece(int... position) {
        
        if(position.length == dimensions.length - 1 && gravityIndex != -1)
            position = getPosition(getShorthandIndex(position));
        
        int index = getIndex(position);
        
        checkPlaceErrors(position, index);
        
        drawCircle(index);
        board[index] = currentPlayerId;
        
        System.out.println(currentPlayer.getName() + " placed a piece at "
                + formatArray(position, "(", ", ", ")") + ".");
        
        endIfWon(currentPlayerId, position);
        turnTaken = true;
    }
    
    public int getPiece(int... position) {
        return board[getIndex(position)];
    }
    
    public int[] getDimensions() { return dimensions.clone(); }
    
    public Gravity[] getGravity() { return gravity.clone(); }
    
    public int getTarget() { return target; }
    
    public int getNumPlayers() { return players.size(); }
    
    public int getDisplayWidth() { return window.getWidth(); }
    
    public int getDisplayHeight() { return window.getHeight(); }
    
    public void setDisplaySize(int width, int height) { window.setSize(width, height); }
    
    public Optional<HyperMNKPlayer> getWinner() { return winner; }
    
    public boolean inProgress() { return !winner.isPresent(); }
    
    private void start() {
        
        launchWindow();
        
        for(int i = 1; i <= players.size(); i++) players.get(i - 1).init(this, i);
        
        while(!winner.isPresent()) {
            
            currentPlayer = players.get(currentPlayerId - 1);
            turnTaken = false;
            
            System.out.println("It's " + currentPlayer.getName() + "'s turn.");
            window.setTitle(TITLE + " - " + currentPlayer.getName() + "'s Turn");
            
            currentPlayer.takeTurn(this, currentPlayerId);
            
            if(!turnTaken)
                throw new IllegalStateException(currentPlayer.getName()
                        + " did not place a piece.");
            
            currentPlayerId = currentPlayerId % players.size() + 1;
        }
    }
    
    private void checkPlaceErrors(int[] position, int index) {
        
        if(turnTaken)
            throw new IllegalStateException(currentPlayer.getName()
                    + " has placed multiple pieces on the same turn.");
        
        if(board[index] != 0)
            throw new IllegalArgumentException(currentPlayer.getName()
                    + " attempted to move into an occupied position "
                    + formatArray(position, "(", ", ", ")") + ".");
        
        int[] pos = position.clone();
        
        for(int i = 0; i < dimensions.length; i++) {
            
            if(gravity[i] == Gravity.NONE) continue;
            
            if(gravity[i] == Gravity.POSITIVE) pos[i]++;
            else if(gravity[i] == Gravity.NEGATIVE) pos[i]--;
            
            if(insideBounds(pos) && getPiece(pos) == 0)
                throw new IllegalArgumentException(currentPlayer.getName()
                        + " attempted to place a piece without support "
                        + formatArray(position, "(", ", ", ")") + ".");
            
            if(gravity[i] == Gravity.POSITIVE) pos[i]--;
            else if(gravity[i] == Gravity.NEGATIVE) pos[i]++;
        }
    }
    
    private void drawCircle(int index) {
        
        BufferedImage circle = new BufferedImage(PIECE_SIZE,
                PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = circle.createGraphics();
        g.setColor(playerColors.get(currentPlayerId - 1));
        g.fillOval(0, 0, PIECE_SIZE, PIECE_SIZE);
        buttons[index].setIcon(new ImageIcon(circle));
    }
    
    private void endIfWon(int player, int... position) {
        
        Optional<Set<Integer>> winningStreak = detectWin(currentPlayerId, position);
        
        if(winningStreak.isPresent()) {
            
            winner = Optional.of(currentPlayer);
            
            System.out.println(currentPlayer.getName() + " has won!");
            window.setTitle(TITLE + " - " + currentPlayer.getName() + " Wins!");
            
            for (HyperMNKPlayer p : players)
            {
            	p.endGame(this, currentPlayerId);
            }
            
            for(int index : winningStreak.get()) {
                buttons[index].setBackground(HIGHLIGHT_COLOR);
            }
            
            for(JButton button : buttons) {
                for(ActionListener l : button.getActionListeners()) {
                    button.removeActionListener(l);
                }
            }
        }
    }
    
    private Optional<Set<Integer>> detectWin(int player, int... position) {
        
        Set<Integer> winningStreak = new HashSet<>();
        
        int[] pos = new int[dimensions.length];
        int[] m = new int[dimensions.length];
        int[] x = new int[dimensions.length];
        
        int n = (int) Math.pow(3, dimensions.length);
        
        x[0] = 1;
        for(int i = 1; i < dimensions.length; i++)
            x[i] = x[i - 1] * 3;
        
        loop: for(int i = 0; i < n; i++) {
            
            boolean leadingOne = false;
            
            for(int j = 0; j < m.length; j++) {
                
                m[j] = (i / x[j]) % 3 - 1;
                
                if(!leadingOne) {
                    if(m[j] == -1) continue loop;
                    if(m[j] == 1) leadingOne = true;
                }
            }
            if(!leadingOne) continue;
            
            int streak = -1;
            for(int sign : new int[] {-1, 1}) {
                
                for(int j = 0; j < dimensions.length; j++)
                    pos[j] = position[j];
                
                while(insideBounds(pos) && getPiece(pos) == player) {
                    
                    winningStreak.add(getIndex(pos));
                    
                    if(++streak == target)
                        return Optional.of(winningStreak);
                    
                    for(int j = 0; j < dimensions.length; j++)
                        pos[j] += m[j] * sign;
                }
            }
            winningStreak.clear();
        }
        return Optional.empty();
    }
    
    private int getIndex(int... position) {
        
        if(position.length != dimensions.length)
            throw new IllegalArgumentException(currentPlayer.getName()
                    + " gave an invalid number of dimensions. Expected: "
                    + dimensions.length + ", Given: " + position.length);
        
        int index = 0, multiplier = 1;
        
        for(int i = 0; i < position.length; i++) {
            
            if(position[i] < 0 || position[i] >= dimensions[i])
                throw new IndexOutOfBoundsException(currentPlayer.getName()
                        + " gave dimension " + i + " out of bounds. " + "Expected: 0 to "
                        + (dimensions[i] - 1) + ", Given: " + position[i]);
            
            index += position[i] * multiplier;
            multiplier *= dimensions[i];
        }
        return index;
    }
    
    private int getShorthandIndex(int... position) {
        
        int[] fullPosition = new int[dimensions.length];
        
        for(int i = 0; i < gravityIndex; i++)
            fullPosition[i] = position[i];
        
        for(int i = gravityIndex; i < dimensions.length - 1; i++)
            fullPosition[i + 1] = position[i];
        
        switch(gravity[gravityIndex]) {
            
            case POSITIVE:
                for(int i = dimensions[gravityIndex] - 1; i >= 0; i--) {
                    fullPosition[gravityIndex] = i;
                    int index = getIndex(fullPosition);
                    if(board[index] == 0) return index;
                }
                break;
                
            case NEGATIVE:
                for(int i = 0; i < dimensions[gravityIndex]; i++) {
                    fullPosition[gravityIndex] = i;
                    int index = getIndex(fullPosition);
                    if(board[index] == 0) return index;
                }
                break;
                
            case NONE: break;
        }
        fullPosition[gravityIndex] = -1;
        throw new IllegalArgumentException(currentPlayer.getName()
                + "  attempted to place a piece in a full column "
                + formatArray(fullPosition, "(", ", ", ")") + ".");
    }
    
    private int[] getPosition(int index) {
        
        int[] pos = new int[dimensions.length];
        
        for(int i = 0; i < dimensions.length; i++) {
            pos[i] = index % dimensions[i];
            index /= dimensions[i];
        }
        return pos;
    }
    
    private int[] getPosition(int x, int y) {
        
        int[] pos = new int[dimensions.length];
        
        for(int i = 0; i < dimensions.length; i++) {
            if(i % 2 == 0) {
                pos[i] = x % dimensions[i];
                x /= dimensions[i];
            } else {
                pos[i] = y % dimensions[i];
                y /= dimensions[i];
            }
        }
        return pos;
    }
    
    private boolean insideBounds(int... position) {
        
        for(int i = 0; i < position.length; i++) {
            if(position[i] < 0 || position[i] >= dimensions[i]) {
                return false;
            }
        }
        return true;
    }
    
    private void launchWindow() {
        
        int[] size = getSize();
        createFrame(size);
        
        for(int y = size[1] - 1; y >= 0; y--) {
            
            for(int x = 0; x < size[0]; x++) {
                
                int[] pos = getPosition(x, y);
                JButton button = getButton(getColor(pos));
                buttons[getIndex(pos)] = button;
                window.add(button);
            }
        }
        window.setVisible(true);
    }
    
    private int[] getSize() {
        
        int[] size = {1, 1};
        
        for(int i = 0; i < dimensions.length; i++) {
            if(i % 2 == 0) size[0] *= dimensions[i];
            else size[1] *= dimensions[i];
        }
        return size;
    }
    
    private void createFrame(int[] size) {
        
        window = new JFrame(TITLE);
        
        if(MAX_WIDTH / size[0] < MAX_HEIGHT / size[1]) {
            window.setSize(MAX_WIDTH, MAX_WIDTH * size[1] / size[0]);
            
        } else {
            window.setSize(MAX_HEIGHT * size[0] / size[1], MAX_HEIGHT);
        }
        
        window.setLayout(new GridLayout(size[1], size[0]));
        window.setResizable(true);
        window.setLocationByPlatform(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
    
    private JButton getButton(Color color) {
        
        JButton button = new JButton();
        button.setBackground(color);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setFont(new Font("Arial", Font.BOLD, 20));
        return button;
    }
    
    private Color getColor(int[] pos) {
        
        int brightness = 255 - 2 * BCKG_CONTRAST, delta = BCKG_CONTRAST;
        
        for(int i = (dimensions.length - 1) / 2; i >= 0; i--) {
            
            int vCoord = 2 * i + 1 < dimensions.length ? pos[2 * i + 1] : 0;
            brightness += ((pos[2 * i] + vCoord) % 2 == 0) ? delta : -delta;
            delta >>= 1;
        }
        
        return new Color(
                BCKG_COLOR.getRed() * brightness / 255,
                BCKG_COLOR.getGreen() * brightness / 255,
                BCKG_COLOR.getBlue() * brightness / 255);
    }
    
    private static String formatArray(int[] array, String prefix,
            String delimiter, String suffix) {
        
        String str = prefix;
        for(int i = 0; i < array.length; i++) {
            str += array[i];
            str += i != array.length - 1 ? delimiter : suffix;
        }
        return str;
    }
    
    public interface HyperMNKPlayer {
        
        default void init(HyperMNK game, int playerId) {}
        
        void takeTurn(HyperMNK game, int playerId);
        
        default void endGame(HyperMNK game, int winner) {}
        
        default String getName() { return "Bot"; }
    }
    
    public static final class HyperMNKController implements HyperMNKPlayer, Serializable {
        
        private String name = "Controller";
        
        public HyperMNKController() {}
        
        public HyperMNKController(String name) { this.name = name; }
        
        @Override
        public void init(HyperMNK game, int playerId) {
            
            for(int i = 0; i < game.buttons.length; i++) {
                
                int[] position = game.getPosition(i);
                
                game.buttons[i].addActionListener(l -> {
                    
                    if(game.currentPlayerId == playerId) {
                    
                        try {
                            game.placePiece(position);
                        
                        } catch(Exception e) {
                            System.err.println("Invalid move by controller.");
                        }
                    }
                });
            }
        }
        
        @Override
        public void takeTurn(HyperMNK game, int playerId) {
            while(!game.turnTaken) {}
        }
        
        @Override
        public String getName() { return name; }
    }
    
    public static final class HyperMNKBuilder {
        
        private HyperMNK game = new HyperMNK();
        
        private HyperMNKBuilder() {}
        
        public HyperMNKBuilder dimensions(int... dimensions) {
            game.dimensions = dimensions.clone();
            return this;
        }
        
        public HyperMNKBuilder target(int target) {
            game.target = target;
            return this;
        }
        
        public HyperMNKBuilder gravity(Gravity... gravity) {
            game.gravity = gravity.clone();
            return this;
        }
        
        public HyperMNKBuilder gravity(Gravity gravity, int dimension) {
            
            if(game.dimensions == null)
                throw new IllegalArgumentException("Dimensions must be set before gravity.");
            
            if(dimension < 0 || dimension >= game.dimensions.length)
                throw new IndexOutOfBoundsException("Index " + dimension + " out of bounds. "
                        + "Expected: 0 to " + (game.dimensions.length - 1));
            
            generateGravity();
            game.gravity[dimension] = gravity;
            return this;
        }
        
        public HyperMNKBuilder player(HyperMNKPlayer player) {
            
            game.players.add(player);
            
            if(game.players.size() <= game.DEFAULT_COLORS.length) {
                game.playerColors.add(game.DEFAULT_COLORS[game.players.size() - 1]);
                
            } else {
                game.playerColors.add(new Color((int) (255 * Math.random()),
                        (int) (255 * Math.random()), (int) (255 * Math.random())));
            }
            return this;
        }
        
        public HyperMNKBuilder player(HyperMNKPlayer player, Color color) {
            game.players.add(player);
            game.playerColors.add(color);
            return this;
        }
        
        public HyperMNK start() {
            
            checkErrors();
            generateGravity();
            
            int product = 1;
            for(int d : game.dimensions) product *= d;
            game.board = new int[product];
            game.buttons = new JButton[product];
            
            PIECE_SIZE = 3 * Math.min(MAX_WIDTH / game.getSize()[0],
                    MAX_HEIGHT / game.getSize()[1]) / 4;
            
            new Thread("HyperMNK") {
                @Override public void run() {
                    game.start();
                }
            }.start();
            
            return game;
        }
        
        private void checkErrors() {
            
            if(game.dimensions == null || game.dimensions.length == 0)
                throw new IllegalStateException("No dimensions are specified.");
            
            if(game.target <= 0)
                throw new IllegalStateException("Target must be a positive integer.");
            
            if(game.players.isEmpty())
                throw new IllegalStateException("No players are specified.");
            
            for(int i = 0; i < game.dimensions.length; i++) {
                if(game.dimensions[i] <= 0)
                    throw new IllegalStateException("All dimensions must be positive integers. "
                            + "Dimension " + i + " = " + game.dimensions[i] + ".");
            }
            
            if(game.gravity != null && game.gravity.length != game.dimensions.length)
                throw new IllegalStateException("The number of gravity specifications "
                        + "does not match the number of dimensions.");
        }
        
        private void generateGravity() {
            
            if(game.gravity == null) {
                game.gravity = new Gravity[game.dimensions.length];
                for(int i = 0; i < game.dimensions.length; i++)
                    game.gravity[i] = Gravity.NONE;
            }
                
            for(int i = 0; i < game.gravity.length; i++) {
                if(game.gravity[i] != Gravity.NONE && game.gravityIndex != -1) break;
                else if(game.gravity[i] != Gravity.NONE) game.gravityIndex = i;
            }
        }
    }
}
