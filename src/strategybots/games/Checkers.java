package strategybots.games;

import java.util.Optional;

import strategybots.games.util.TileGame;
import strategybots.games.util.Board.Pattern;
import strategybots.graphics.Colour;

/**
 * <b>Checkers implementation.</b><br>
 * <br>
 * Rules: <a href="https://en.wikipedia.org/wiki/English_draughts">Wikipedia</a><br>
 * <br>
 * Bot players can be made by implementing 'Player<Checkers>'.<br>
 * Human players can be made by instantiating 'CheckersController'.
 * 
 * @author Alec Dorrington
 */
public class Checkers extends TileGame {
    
    /** Title of the window. */
    private static final String TITLE = "Checkers";
    
    /** Textures used for basic game pieces. */
    private static final String[] MAN_TEXTURES = new String[] {
            "res/draughts/red_draught.png", "res/draughts/white_draught.png"};
    
    /** Textures used for crowned game pieces. */
    private static final String[] KING_TEXTURES = new String[] {
            "res/draughts/red_crowned.png", "res/draughts/white_crowned.png"};
    
    /** The display name of the colour of each player. */
    private static final String[] COLOUR_NAMES = new String[] {
            "Red", "White"};
    
    /** Background tile colours. */
    private static final Colour BOARD_COLOUR1 = Colour.rgb(123, 237, 159);
    private static final Colour BOARD_COLOUR2 = Colour.rgb(248, 239, 186);
    
    /** The number of rows on each end to be filled with pieces. */
    private final float pieceRows;
    
    /** The piece that was/is being moved on this turn. */
    private volatile Optional<Piece> moved = Optional.empty();
    
    /**
     * Asynchronously runs a new Checkers instance.
     * @param width the width of the game board.
     * @param height the height of the game board.
     * @param pieceRows the number of rows in which initial pieces should be placed.
     * @param player1 the first (red) player to participate.
     * @param player2 the second (white) player to participate.
     */
    public Checkers(int width, int height, int pieceRows,
            Player<Checkers> player1, Player<Checkers> player2) {
        super(width, height, TITLE, player1, player2);
        this.pieceRows = pieceRows;
    }
    
    /**
     * Moves your piece at the given position to a new position.<br>
     * Must be called at least once per turn, as per the rules of the game.<br>
     * @param x_from the current x position of the piece.
     * @param y_from the current y position of the piece.
     * @param x_to the new x position of the piece.
     * @param y_to the new y position of the piece.
     * @return whether the move was valid and successful.
     */
    public boolean movePiece(int x_from, int y_from, int x_to, int y_to) {
        
        //Ensure game is running and turn hasn't already been taken.
        if(!isRunning() || turnTaken()) return false;
        
        //Ensure positions are in bounds.
        if(!inBounds(x_from, y_from) || !inBounds(x_to, y_to)) return false;
        
        //Ensure there is a piece at the from location.
        if(!getPiece(x_from, y_from).isPresent()) return false;
        
        //Ensure piece is owned by the current player.
        if(getPiece(x_from, y_from).get().getOwnerId() != getCurrentPlayerId()) return false;
        
        //Ensure two different pieces aren't moved on the same turn.
        if(moved.isPresent() && moved.get() != getPiece(x_from, y_from).get()) return false;
        
        //Move the piece, subject to game constraints.
        if(!getPiece(x_from, y_from).get().movePiece(x_to, y_to)) return false;
        
        //End turn if move was not a capture.
        if(Math.abs(x_to-x_from)==1 && Math.abs(y_to-y_from)==1) endTurn();
        
        //End turn if move was a capture but there are no more possible captures.
        else if(!((CheckersPiece)getPiece(x_to, y_to).get()).canCapture()) endTurn();
        
        return true;
    }
    
    /**
     * Returns the owner of the piece currently at the given position.<br>
     * <table border="1">
     * <tr><td>0</td><td>Empty tile.</td></tr>
     * <tr><td>1</td><td>Piece owned by player 1.</td></tr>
     * <tr><td>2</td><td>Piece owned by player 2.</td></tr>
     * </table>
     * @param x the x position at which to check for a piece.
     * @param y the y position at which to check for a piece.
     * @return the piece at (x, y) on the board.
     */
    public int getPieceOwner(int x, int y) {
        return getPiece(x, y).isPresent() ? getPiece(x, y).get().getOwnerId() : 0;
    }
    
    /**
     * Returns the type of piece currently at the given position.<br>
     * <table border="1">
     * <tr><td>0</td><td>Empty tile.</td></tr>
     * <tr><td>1</td><td>Regular piece.</td></tr>
     * <tr><td>2</td><td>King (crowned piece).</td></tr>
     * </table>
     * @param x the x position at which to check for a piece.
     * @param y the y position at which to check for a piece.
     * @return the piece at (x, y) on the board.
     */
    public int getPieceType(int x, int y) {
        //Return 0 for empty tiles.
        if(!getPiece(x, y).isPresent()) return 0;
        //Return 1 for regular pieces.
        else if(getPiece(x, y).get() instanceof Man) return 1;
        //Return 2 for crowned pieces.
        else return 2;
    }
    
    @Override
    protected void init() {
        
        //Set the board colours.
        getBoard().setBackground(Pattern.CHECKER, BOARD_COLOUR1, BOARD_COLOUR2);
        setHighlightColour(Colour.rgb(74, 105, 189));
        
        //Place the initial pieces.
        for(int x = 0; x < getWidth(); x++) {
            for(int y = 0; y < pieceRows; y++) {
                
                //Player one's pieces (red).
                if((x+getHeight()-y-1)%2==0) new Man(1, x, getHeight()-y-1);
                //Player two's pieces (white).
                if((x+y)%2==0) new Man(2, x, y);
            }
        }
    }
    
    @Override
    protected void checkWin() {
        
        //This player wins if the opponent has no remaining pieces.
        if(getPieces(getCurrentPlayerId()%2+1).size()==0)
            endGame(getCurrentPlayerId());
    }
    
    @Override
    protected String getPlayerName(int playerId) {
        return getPlayer(playerId).getName() + " ("+COLOUR_NAMES[playerId-1]+")";
    }
    
    /**
     * Implementation of Player<Checkers> for use in inserting a human-controlled player.<br>
     * Each CheckersController will make moves based on mouse input on the game display window.
     * @author Alec Dorrington
     */
    public static class CheckersController extends Controller<Checkers> {
        
        public CheckersController() {}
        
        public CheckersController(String name) { super(name); }

        @Override
        public void onTileClicked(Checkers game, int playerId, int x, int y) {
            
            //Select the piece if it belongs to this player.
            if(game.getPiece(x, y).isPresent() &&
                    game.getPiece(x, y).get().getOwnerId() == playerId) {
                
                selectPiece(game, playerId, x, y);
                
            //Otherwise, move the piece if there is one selected.
            } else if(getSelected().isPresent()) {
                
                movePiece(game, playerId, x, y);
            }
        }
        
        /**
         * Attempts to select a piece at the specified location.
         * @param game the game being played.
         * @param playerId the ID of the current player.
         * @param x the x position which was clicked.
         * @param y the y position which was clicked.
         */
        private void selectPiece(Checkers game, int playerId, int x, int y) {
            
            //Determine if there exists one of your pieces which can capture an enemy piece.
            boolean canCapture = false;
            for(Piece piece : game.getPieces(playerId)) {
                if(((CheckersPiece)piece).canCapture()) canCapture = true;
            }
            
            //Can't select a piece which can't capture if there exists a piece which can.
            if(!canCapture || ((CheckersPiece)game.getPiece(x, y).get()).canCapture()) {
                
                //Select the new piece if no piece has yet been moved.
                if(!game.moved.isPresent()) {
                    selectPiece(game, game.getPiece(x, y).get());
                }
            }
        }
        
        /**
         * Attempts to move the selected piece to the specified location.
         * @param game the game being played.
         * @param playerId the ID of the current player.
         * @param x the x position which was clicked.
         * @param y the y position which was clicked.
         */
        private void movePiece(Checkers game, int playerId, int x, int y) {
            
            //Determine move step size.
            int dx = Math.abs(x - getSelected().get().getCol());
            int dy = Math.abs(y - getSelected().get().getRow());
            
            //The piece being moved, before it is moved. Used to check for promotions.
            CheckersPiece before = (CheckersPiece)getSelected().get();
            
            //Move the selected piece to this location.
            if(game.movePiece(getSelected().get().getCol(),
                getSelected().get().getRow(), x, y)) {
                
                //Whether a piece was crowned on this turn.
                boolean promoted = (CheckersPiece)game.getPiece(x, y).get() != before;
                
                unselectPiece(game);
                
                //Reselect the piece if a capture was made and more captures are possible.
                if(((CheckersPiece)game.getPiece(x, y).get()).canCapture()
                        && (dx==2 && dy==2) && !promoted) {
                    selectPiece(game, game.getPiece(x, y).get());
                }
            }
        }
    }
    
    /**
     * Abstract supertype for checkers pieces (men and kings).<br>
     * Takes care of much of the move validation.
     * @author Alec Dorrington
     */
    private abstract class CheckersPiece extends Piece {
        
        CheckersPiece(int ownerId, int x, int y, String texture) {
            super(ownerId, x, y, texture);
        }
        
        @Override
        public boolean movePiece(int x_to, int y_to) {
            
            //The difference between the current position and the new position.
            int dx = x_to - getCol(), dy = y_to - getRow();
            
            //Move one space diagonally.
            if(Math.abs(dx)==1 && Math.abs(dy)==1) {
                
                //Can't perform a simple move if a jump is available.
                for(Piece piece : getPieces(getOwnerId())) {
                    if(((CheckersPiece)piece).canCapture()) return false;
                }
                
                //Can't perform a simple move onto another piece.
                if(getPiece(x_to, y_to).isPresent()) return false;
                
                //Move the piece.
                setBoardPos(x_to, y_to);
                
                //A simple diagonal move must be the only move.
                if(moved.isPresent()) return false;
                
            //Jump over another piece, capturing it.
            } else {
                
                //Ensure jump is a valid move.
                if(!validateCapture(x_to, y_to)) return false;
                
                //Delete the piece which was jumped over.
                getPiece(getCol() + dx/2, getRow() + dy/2).get().delete();
                
                //Move the piece.
                setBoardPos(x_to, y_to);
                
                //Store piece which was moved, used for chained captures.
                moved = Optional.ofNullable(canCapture() ? this : null);
            }
            return true;
        }
        
        /**
         * Ensures a move is a valid jump. <br>
         * Does not perform a check to ensure piece moves forward.<br>
         * Does not perform a check to ensure turn is active.<br>
         * Does not actually perform any moves.
         * @param x_to the x position to which the piece is to move.
         * @param y_to the y position to which the piece is to move.
         * @return whether the move is value.
         */
        boolean validateCapture(int x_to, int y_to) {
            
            //The difference between the current position and the new position.
            int dx = x_to - getCol(), dy = y_to - getRow();
            
            //Ensure positions are in bounds.
            if(!inBounds(x_to, y_to)) return false;
            
            //Ensure capturing piece moves 2 spaces diagonally.
            if(Math.abs(dx)!=2 || Math.abs(dy)!=2) return false;
            
            //Ensure capturing piece actually jumps over another piece.
            if(!getPiece(getCol() + dx/2, getRow() + dy/2).isPresent()) return false;
            
            //Ensure destination square is unoccupied.
            if(getPiece(x_to, y_to).isPresent()) return false;
            
            //Ensure captured piece belongs to the opponent.
            if(getPiece(getCol() + dx/2, getRow() + dy/2).get()
                    .getOwnerId() == getOwnerId()) return false;
            
            //Capture piece.
            return true;
        }
        
        /**
         * @return whether this piece is currently able to make a capture.
         */
        abstract boolean canCapture();
    }
    
    /**
     * Represents a regular checkers game piece (a 'man').
     * @author Alec Dorrington
     */
    private class Man extends CheckersPiece {
        
        Man(int ownerId, int x, int y) {
            super(ownerId, x, y, MAN_TEXTURES[ownerId-1]);
        }
        
        @Override
        public boolean movePiece(int x_to, int y_to) {
            
            //Ensure piece only moves forwards.
            if(Math.signum(y_to-getRow()) != (getOwnerId()==1?-1:1)) return false;
            
            if(!super.movePiece(x_to, y_to)) return false;
            
            //If the piece has reached the other end of the board, crown it.
            if(getOwnerId()==1 ? getRow()==0 : getRow()==Checkers.this.getHeight()-1) {
                
                new King(getOwnerId(), getCol(), getRow());
                //Crowning a piece immediately ends the turn.
                endTurn();
            }
            return true;
        }
        
        @Override
        boolean canCapture() {
            
            //Check if the forward diagonal jump in each diagonal is valid.
            for(int xx = -2; xx <= 2; xx += 4) {
                if(validateCapture(getCol()+xx, getRow()
                        + (getOwnerId()==1?-2:2))) return true;
            }
            return false;
        }
    }
    
    /**
     * Represents a crowned checkers game piece (a 'king').
     * @author Alec Dorrington
     */
    private class King extends CheckersPiece {
        
        King(int ownerId, int x, int y) {
            super(ownerId, x, y, KING_TEXTURES[ownerId-1]);
        }
        
        @Override
        boolean canCapture() {
            
            //Check if the diagonal jump in each diagonal is valid.
            for(int xx = -2; xx <= 2; xx += 4) {
                for(int yy = -2; yy <= 2; yy += 4) {
                    if(validateCapture(getCol()+xx, getRow()+yy)) return true;
                }
            }
            return false;
        }
    }
}