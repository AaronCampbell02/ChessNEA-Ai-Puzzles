package main;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.text.*;

import piece.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel implements Runnable{
	public static final int WIDTH = 1920;
	public static final int HEIGHT = 1080;
	public boolean active;
	public boolean closeGame = false;
	Color background = new Color(21, 37, 35);
	final int FPS = 60;
	Thread gameThread;
	
	ChessBoard board = new ChessBoard();
	Mouse mouse = new Mouse();
	
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	int currentColor = WHITE;
	int otherColor = BLACK;
	int moves = 0;
	int preMoves = 0;
	
	public static ArrayList<piece> pieces = new ArrayList<>();
	public static ArrayList<piece> simPieces = new ArrayList<>();
	ArrayList<piece> promoPieces = new ArrayList<>();
	
	piece activeP;
	public static piece castlingP;
	piece promotionP,checkingP;
	
	boolean canMove;
	boolean validSquare;
	boolean promotion;
	boolean gameOver = false;
	boolean stalemate = false;
	boolean draw = false;
	boolean offerDraw = false;
	boolean running = true;
	boolean delay = false;
	private TwoPlayerChessGame parentFrame;
	
	String url = "jdbc:mysql://localhost:3306/chessloginsystem";
	String uName = "root";
	String uPass = "pass";
	String nameElo = "";
	String AccountName = null;
	String AccountElo = null;
	
	BufferedImage WhiteLogo;
	BufferedImage wColourChange;
	BufferedImage bColourChange;
    private JTextPane logTextPane;
    private StyledDocument doc;
    String collumDraw;
    String rowDraw;
    String typeOfPiece;
    String colNum;
    String rowNum;
    String resultString;
	int holdRow = 0;
	int promoCol;
	int promoRow;
	boolean forfeit = false;
	boolean start = true;
	int randomNumber;
	int timer = 180;
	ArrayList<ArrayList<piece>> savedPieceArr = new ArrayList<>();
	ArrayList<ArrayList<Integer>> savedColArr = new ArrayList<>();
	ArrayList<ArrayList<Integer>> savedRowArr = new ArrayList<>();
	boolean stackActive = false;
	int stackPointer = -1;
	Shape backArrow, forwardArrow;
	piece savedP;
	Boolean newP = true;
	List<List<Integer>> drawPMoves = new ArrayList<>();
	ArrayList<Boolean> movedList = new ArrayList<>();
	ArrayList<Boolean> twoSteppedList = new ArrayList<>();
	String finalRank;
	Color whiteColour = null;
	Color blackColour = null;
    
	public GamePanel(TwoPlayerChessGame parentFrame) {
		this.parentFrame = parentFrame;
		setPreferredSize(new Dimension(WIDTH,HEIGHT));
		setBackground(background);
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
		
		setLayout(null);
		
		try {
			WhiteLogo = ImageIO.read(getClass().getResourceAsStream("/images/WhiteLogo.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			wColourChange = ImageIO.read(getClass().getResourceAsStream("/images/w-pawnChangeColour.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			bColourChange = ImageIO.read(getClass().getResourceAsStream("/images/b-pawnChangeColour.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
        Random random = new Random();
        randomNumber = random.nextInt(2) + 1;
		createScroll();
        
		setPieces();
		copyPiece(pieces,simPieces);
		nameElo = returnUser();
		finalRank = Integer.toString(returnRank());
		String[] parts = nameElo.split(",");
		AccountName = parts[0];
		AccountElo = parts[1];
		AccountName = checkLength(AccountName);
        
		addToStack();
	}
	
	public void setPieces() {
		//white team
		pieces.add(new pawn(WHITE,0,6,1));
		pieces.add(new pawn(WHITE,1,6,1));
		pieces.add(new pawn(WHITE,2,6,1));
		pieces.add(new pawn(WHITE,3,6,1));
		pieces.add(new pawn(WHITE,4,6,1));
		pieces.add(new pawn(WHITE,5,6,1));
		pieces.add(new pawn(WHITE,6,6,1));
		pieces.add(new pawn(WHITE,7,6,1));
		pieces.add(new rook(WHITE,0,7,1));
		pieces.add(new rook(WHITE,7,7,1));
		pieces.add(new knight(WHITE,1,7,1));
		pieces.add(new knight(WHITE,6,7,1));
		pieces.add(new bishop(WHITE,2,7,1));
		pieces.add(new bishop(WHITE,5,7,1));
		pieces.add(new queen(WHITE,3,7,1));
		pieces.add(new king(WHITE,4,7,1));
		//black team
		pieces.add(new pawn(BLACK,0,1,1));
		pieces.add(new pawn(BLACK,1,1,1));
		pieces.add(new pawn(BLACK,2,1,1));
		pieces.add(new pawn(BLACK,3,1,1));
		pieces.add(new pawn(BLACK,4,1,1));
		pieces.add(new pawn(BLACK,5,1,1));
		pieces.add(new pawn(BLACK,6,1,1));
		pieces.add(new pawn(BLACK,7,1,1));
		pieces.add(new rook(BLACK,0,0,1));
		pieces.add(new rook(BLACK,7,0,1));
		pieces.add(new knight(BLACK,1,0,1));
		pieces.add(new knight(BLACK,6,0,1));
		pieces.add(new bishop(BLACK,2,0,1));
		pieces.add(new bishop(BLACK,5,0,1));
		pieces.add(new queen(BLACK,3,0,1));
		pieces.add(new king(BLACK,4,0,1));
		
	}
	private void copyPiece(ArrayList<piece> source, ArrayList<piece> target) {
		
		target.clear();
		for(int i = 0; i < source.size(); i++) {
			target.add(source.get(i));
		}
	}
	
	private void update() {
		
		if(returnToMenu()) {
			active = false;
			optionsMenu OptionsMenu = new optionsMenu();
			running = false;
			parentFrame.dispose();
		}
		if(moves != preMoves && promotion == false) {
			preMoves += 1;
			addMove(resultString);			
		}
		if(stackActive) {
			if(stackMax()) {
//				System.out.println("stack is full");
				stackActive = false;
			}			
		}if(backArrowClicked()) {			
			if(stackEmpty() == false) {
				stackActive = true;
				stackPointer -= 1;
				changePieceState();
			}			
		}if(frontArrowClicked()){
			if(stackMax() == false) {
				stackActive = true;
				stackPointer += 1;
				changePieceState();
				if(stackMax()) {
					resetState();
				}
			}
		}
		if(circle1Clicked()){
			whiteColour = JColorChooser.showDialog(null, "Choose white piece colour", Color.WHITE);
		}
		if(circle2Clicked()) {
			blackColour = JColorChooser.showDialog(null, "Choose black piece colour", Color.BLACK);
		}
		if(promotion) {
			promoting();
		}else if(forfeitClicked()) {
			forfeit = true;
			gameOver = true;
		}else if(drawClicked()) {
			offerDraw = true;	
		}else if (drawGame()) {
			draw = true;
			gameOver = true;	            
		}else if(gameOver == false && stackActive == false) {
			if(mouse.pressed){
				if(activeP == null) {
					
					for(piece p: simPieces) {
						if(p.color == currentColor && p.col == (mouse.x-50)/ChessBoard.SQUARE_SIZE && p.row == (mouse.y - 50)/ChessBoard.SQUARE_SIZE) {
							activeP = p;
						}
					}
				}else {
					simulate();
				}
			}
			if(mouse.pressed == false) {
				if(activeP != null) {
					if (validSquare) {
						copyPiece(simPieces,pieces);
						activeP.updatePosition();
						moves += 1;
						playSound();
						typeOfPiece = activeP.type.toString();
						colNum = Integer.toString(activeP.col+1);
						colNum = returnCol(colNum);
						holdRow = activeP.row+1;
						holdRow = 9 - holdRow;
						rowNum = Integer.toString(holdRow);
						resultString = moves + "     " + typeOfPiece + " to " + colNum + rowNum;
						if(castlingP != null) {
							checkCastle();
							castlingP.updatePosition();
							String colourStr = "White";
							if(currentColor == BLACK) {
								colourStr = "Black";
							}
							resultString = moves + "     " + colourStr + " castled";
						}
						if(isKingInCheck(true) && isCheckmate()) {
							gameOver = true;
						}
						if(isStalemate()) {
							gameOver = true;
						}
						if(canPromote()) {
							promotion = true;
						}else {
							changeTurn();
						}
						activeP = null;
//						System.out.println("adding stack ");
						if(promotion == false) {
							addToStack();
						}
					}else {
						copyPiece(pieces,simPieces);
						activeP.resetPosition();
						activeP = null;
					}
				}
			}
		}

		
		
	}
	private void simulate() {
		
		canMove = false;
		validSquare = false;
		
		copyPiece(pieces,simPieces);
		
		if(castlingP != null) {
			castlingP.col = castlingP.preCol;
			castlingP.x = castlingP.getX(castlingP.col);
			castlingP = null;
		}
		
		promoCol = activeP.preCol;
		promoRow = activeP.preRow;		
		
		activeP.x = mouse.x - ChessBoard.HALF_SQUARE_SIZE;
		activeP.y = mouse.y - ChessBoard.HALF_SQUARE_SIZE;
		activeP.col = activeP.getCol(activeP.x);
		activeP.row = activeP.getRow(activeP.y);
		
		if(activeP.canMove(activeP.col, activeP.row)) {
			canMove = true;
			
			if(activeP.hittingP != null) {
				savedP = activeP.hittingP;
				simPieces.remove(activeP.hittingP);
			}else {
				savedP = null;
			}
			if(isIllegal(activeP) == false && opponentCaptureKing() == false) {
				validSquare = true;
			}
		}
	}
	private boolean frontArrowClicked() {

        forwardArrow = forwardArrowPath();

        if(forwardArrow.contains(mouse.x, mouse.y) && mouse.pressed) {
			try {
				gameThread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	return true;
        }
        return false;
	}
	private boolean backArrowClicked() {
        backArrow = backArrowPath();
        
        if(backArrow.contains(mouse.x, mouse.y) && mouse.pressed) {
			try {
				gameThread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	return true;
        }
        return false;
	}
	private boolean circle2Clicked() {
		Ellipse2D.Double circle = circlePath(1170, 30);
        if(circle.contains(mouse.x,mouse.y) && mouse.pressed) {
			try {
				gameThread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	return true;
        }
        return false;
        
	}
	private boolean circle1Clicked() {
        Ellipse2D.Double circle = circlePath(1070, 30);
        if(circle.contains(mouse.x,mouse.y) && mouse.pressed) {
			try {
				gameThread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        	return true;
        }
        return false;
        
	}
	private boolean stackMax() {
		if(stackPointer == moves) {
			return true;
		}
		return false;
	}
	private boolean stackEmpty() {
		if(stackPointer == 0) {
			return true;
		}
		return false;
	}
	private void changePieceState() {
		pieces.clear();
		simPieces.clear();
		ArrayList<piece> pieceList = savedPieceArr.get(stackPointer);
		int counter = 0;
		
		for (piece p : pieceList) {
			p.col = savedColArr.get(stackPointer).get(counter);
			p.row = savedRowArr.get(stackPointer).get(counter);
			switch(p.type) {
			case PAWN: simPieces.add(new pawn (p.color,p.col, p.row,1)); break;
			case KNIGHT: simPieces.add(new knight (p.color,p.col, p.row,1)); break;
			case BISHOP: simPieces.add(new bishop (p.color,p.col, p.row,1)); break;
			case ROOK: simPieces.add(new rook (p.color,p.col, p.row,1)); break;
			case QUEEN: simPieces.add(new queen (p.color,p.col, p.row,1)); break;
			case KING: simPieces.add(new king (p.color,p.col, p.row,1)); break;
			default:break;			
			}
			counter += 1;
		}
		copyPiece(simPieces,pieces);
	}
	private void addToStack() {
		ArrayList<piece> savedPiece = new ArrayList<>();
		ArrayList<Integer> savedCol = new ArrayList<>();
		ArrayList<Integer> savedRow = new ArrayList<>();
		updateState();
		for(piece p: simPieces) {
			savedPiece.add(p);
			savedCol.add(p.col);
			savedRow.add(p.row);
		}
		savedPieceArr.add(savedPiece);
		savedColArr.add(savedCol);
		savedRowArr.add(savedRow);
		stackPointer += 1;
	}
	private void resetState() {
		int i = 0;
		for(piece p: simPieces) {
			if(twoSteppedList.get(i) == true) {
				p.twoStepped = true;
			}else {
				p.twoStepped = false;
			}
			if(movedList.get(i) == true) {
				p.moved = true;
			}else {
				p.moved = false;
			}
			i += 1;
		}
	}
	private void updateState() {
		movedList.clear();
		twoSteppedList.clear();
		
		for(piece p: simPieces) {
			if(p.moved) {
				movedList.add(true);
			}else {
				movedList.add(false);
			}
			if(p.twoStepped) {
				twoSteppedList.add(true);
			}else {
				twoSteppedList.add(false);
			}
		}
	}
	private void showStack() {
		System.out.println(savedPieceArr);
		System.out.println(savedColArr);
		System.out.println(savedRowArr);
		
	}
    private void resetGameState() {
    	timer = 180;
        gameOver = false;
        draw = false;
        stalemate = false;
        offerDraw = false;
        promotion = false;
        moves = 0;
        currentColor = WHITE;
        otherColor = BLACK;
        pieces.clear();
        setPieces();
        copyPiece(pieces, simPieces);
    }
	private void changeTurn() {
		if (currentColor == WHITE) {
			currentColor = BLACK;
			for(piece p : GamePanel.simPieces) {
				if(p.color == BLACK) {
					p.twoStepped = false;
				}
			}
		}else {
			currentColor = WHITE;
			for(piece p : GamePanel.simPieces) {
				if(p.color == WHITE) {
					p.twoStepped = false;
				}
			}
		}

	}
	private void checkCastle() {
		if(castlingP != null) {
			if(castlingP.col == 0) {
				castlingP.col += 3;
			}else if(castlingP.col == 7) {
				castlingP.col -= 2;
			}
			castlingP.x = castlingP.getX(castlingP.col);
		}
	}
	private boolean canPromote() {
		
		if(activeP.type == Type.PAWN) {
			if(currentColor == WHITE && activeP.row == 0) {
				promotionP = activeP;
				promoPieces.clear();
				promoPieces.add(new queen(currentColor,activeP.col,activeP.row,1));
				promoPieces.add(new knight(currentColor,activeP.col,activeP.row+1,1));
				promoPieces.add(new rook(currentColor,activeP.col,activeP.row+2,1));
				promoPieces.add(new bishop(currentColor,activeP.col,activeP.row+3,1));
				promoPieces.add(new x(currentColor,activeP.col,activeP.row+4,1));
				return true;
			}else if(currentColor == BLACK && activeP.row == 7) {
				promotionP = activeP;
				promoPieces.clear();
				promoPieces.add(new queen(currentColor,activeP.col,activeP.row,1));
				promoPieces.add(new knight(currentColor,activeP.col,activeP.row-1,1));
				promoPieces.add(new rook(currentColor,activeP.col,activeP.row-2,1));
				promoPieces.add(new bishop(currentColor,activeP.col,activeP.row-3,1));
				promoPieces.add(new x(currentColor,activeP.col,activeP.row-4,1));
				return true;
			}
		}
		
		return false;
	}
	private boolean drawClicked() {
		if(mouse.pressed && offerDraw == false && forfeit == false && gameOver == false) {
			if(mouse.x >= 1100 && mouse.x <= 1450 && mouse.y >= 850 && mouse.y <= 950) {
				return true;
			}
		}
		return false;
	}
	private boolean forfeitClicked() {
		if(mouse.pressed && offerDraw == false && forfeit == false && gameOver == false) {
			if(mouse.x >= 1500 && mouse.x <= 1850 && mouse.y >= 850 && mouse.y <= 950) {
				return true;
			}
		}
		
		return false;
		
	}
	private boolean returnToMenu() {
		if(delay) {
			delay = false;
			try {
				gameThread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}else if(mouse.pressed && gameOver) {
			if(mouse.x >= 290 && mouse.x <= 770 && mouse.y >= 570 && mouse.y <= 650) {
				return true;
			}
		}

		return false;
	}
	private boolean drawGame() {
		if(mouse.pressed && offerDraw) {
			if(mouse.x >= 290 && mouse.x <= 530 && mouse.y >= 570 && mouse.y <= 650) {
				offerDraw = false;
				delay = true;
				return true;
			}else if(mouse.x >= 530 && mouse.x <= 770 && mouse.y >= 570 && mouse.y <= 650) {
				offerDraw = false;
				return false;
			}
		}
		return false;
	}
	private void promoting() {
		
		if(mouse.pressed) {
			for(piece p: promoPieces) {
				if(p.col == (mouse.x-50)/ChessBoard.SQUARE_SIZE && p.row == (mouse.y - 50)/ChessBoard.SQUARE_SIZE) {
					switch(p.type) {
					case QUEEN: simPieces.add(new queen(currentColor,promotionP.col,promotionP.row,1)); break;
					case KNIGHT: simPieces.add(new knight(currentColor,promotionP.col,promotionP.row,1)); break;
					case ROOK: simPieces.add(new rook(currentColor,promotionP.col,promotionP.row,1)); break;
					case BISHOP: simPieces.add(new bishop(currentColor,promotionP.col,promotionP.row,1)); break;
					case X:promotion = false;break;
					default:break;
					}
					if(promotion != false) {
						simPieces.remove(promotionP);
						copyPiece(simPieces, pieces);
						promotion = false;
						if(isKingInCheck(true) && isCheckmate()) {
							gameOver = true;
						}
						
						addToStack();
						changeTurn();
					}
					else {
						simPieces.add(new pawn(currentColor,promoCol,promoRow,1));
						if(currentColor == WHITE) {
							otherColor = BLACK;
						}else {
							otherColor = WHITE;
						}
						
						if(savedP != null) {
							switch(savedP.type) {
							case QUEEN: simPieces.add(new queen(otherColor,promotionP.col,promotionP.row,1));break;
							case ROOK: simPieces.add(new rook(otherColor,promotionP.col,promotionP.row,1));break;
							case KNIGHT: simPieces.add(new knight(otherColor,promotionP.col,promotionP.row,1));break;
							case BISHOP: simPieces.add(new bishop(otherColor,promotionP.col,promotionP.row,1));break;
							case KING: simPieces.add(new king(otherColor,promotionP.col,promotionP.row,1));break;
							default:break;
							}
							savedP = null;
						}
						moves -= 1;
						simPieces.remove(promotionP);
						copyPiece(simPieces,pieces);
						promotionP = null;
						activeP = null;
					}
					
				}
			}
		}
		
	}
	private boolean isIllegal(piece king) {
		if(king.type == Type.KING) {
			for(piece p: simPieces) {
				if(p != king && p.color != king.color && p.canMove(king.col, king.row)) {
					return true;
				}
			}
		}
		return false;
	}
	private boolean isKingInCheck(boolean team) {
		
		piece king = getKing(team); 
		for(piece p: simPieces) {
			if(p.canMove(king.col, king.row)) {
				checkingP = p;
				return true;
			}
		}
		checkingP = null;
		return false;
	}
	private piece getKing(boolean opponent){
		piece king = null;
		for(piece p: simPieces) {
			if(opponent) {
				if(p.type == Type.KING && p.color != currentColor) {
					king = p;
				}
			}
			else {
				if(p.type == Type.KING && p.color == currentColor) {
					king = p;
				}
			}
		}
		
		
		return king;
	}
	private boolean opponentCaptureKing() {
		
		piece King = getKing(false);
		
		for(piece p: simPieces) {
//			System.out.println(p + " " + p.col + " " + p.row);
			if(p.color != King.color && p.canMove(King.col, King.row)) {
				return true;
			}
		}
		
		return false;
	}
	private boolean isCheckmate() {
		
		piece king = getKing(true);
		
		if (kingCanMove(king)) {
			return false;
		}else {
			int colDiff = Math.abs(checkingP.col - king.col);
			int rowDiff = Math.abs(checkingP.row - king.row);
			System.out.println(colDiff);
			System.out.println(rowDiff);
			if(colDiff == 0) {
				if(checkingP.row < king.row) {
					for(int row = checkingP.row; row <= king.row; row ++) {
						for(piece p: simPieces) {
							if(p != king && p.color != currentColor && p.canMove(checkingP.col, row)) {
								return false;
							}
						}
					}
				}
				if(checkingP.row > king.row) {
					for(int row = checkingP.row; row >= king.row; row --) {
						for(piece p: simPieces) {
							if(p != king && p.color != currentColor && p.canMove(checkingP.col, row)) {
								return false;
							}
						}
					}
				}
			}else if(rowDiff == 0) {
				if(checkingP.col < king.col) {
					for(int col = checkingP.col; col <= king.col; col ++) {
						for(piece p: simPieces) {
							if(p != king && p.color != currentColor && p.canMove(col, checkingP.row)) {
								return false;
							}
						}
					}
				}
				if(checkingP.col > king.col) {
					for(int col = checkingP.col; col >= king.col; col --) {
						for(piece p: simPieces) {
							if(p != king && p.color != currentColor && p.canMove(col, checkingP.row)) {
								return false;
							}
						}
					}
				}
			}else if(colDiff == rowDiff) {
				if(checkingP.row < king.row) {
					if(checkingP.col < king.col) {
						for(int col = checkingP.col, row = checkingP.row ; col <= king.col;col++, row++) {
							for(piece p: simPieces) {
								if(p != king && p.color != currentColor && p.canMove(col, row)) {
									return false;
								}
							}
						}
					}
					if(checkingP.col > king.col) {
						for(int col = checkingP.col, row = checkingP.row ; col >= king.col;col--, row++) {
							for(piece p: simPieces) {
								if(p != king && p.color != currentColor && p.canMove(col, row)) {
									return false;
								}
							}
						}
					}
				}
				if(checkingP.row > king.row) {
					if(checkingP.col < king.col) {
						for(int col = checkingP.col, row = checkingP.row ; col <= king.col;col++, row--) {
							for(piece p: simPieces) {
								if(p != king && p.color != currentColor && p.canMove(col, row)) {
									return false;
								}
							}
						}
					}
					if(checkingP.col > king.col) {
						for(int col = checkingP.col, row = checkingP.row ; col >= king.col;col--, row--) {
							for(piece p: simPieces) {
								if(p != king && p.color != currentColor && p.canMove(col, row)) {
									return false;
								}
							}
						}
					}
				}
			}else {
				for(piece p: simPieces) {
					if(p != king && p.color != currentColor && p.canMove(checkingP.col, checkingP.row)) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	private boolean isStalemate() {
		
		piece king = getKing(true);
		if(kingCanMove(king) == false && isKingInCheck(true) == false) {
			for(piece p: simPieces) {
				if(p.color != currentColor && p != king) {
					for(int col = 0; col < 9; col++) {
						for(int row = 0; row < 9; row++) {
							if(p.canMove(col, row)) {
								return false;
							}
						}
					} 
				}
			}
		}else {
			return false;
		}

		
		stalemate = true;
		return true;
	}
	private boolean kingCanMove(piece king) {
		
		if(isValidMove(king,-1,-1)) {return true;}
		if(isValidMove(king,0,-1)) {return true;}
		if(isValidMove(king,1,-1)) {return true;}
		if(isValidMove(king,-1,1)) {return true;}
		if(isValidMove(king,-1,0)) {return true;}
		if(isValidMove(king,1,0)) {return true;}
		if(isValidMove(king,1,1)) {return true;}
		if(isValidMove(king,0,1)) {return true;}
		
		return false;
	}
	private boolean isValidMove(piece king, int colPlus, int rowPlus) {
		
		boolean isValidMove = false;
		king.col += colPlus;
		king.row += rowPlus;
		if(king.canMove(king.col, king.row)) {
			if(king.hittingP != null) {
				simPieces.remove(king.hittingP);
			}
			if(isIllegal(king) == false) {
			isValidMove = true;
			}
		}
		king.resetPosition();
		copyPiece(pieces, simPieces);
		return isValidMove;
	}
	private String returnUser() {
		String userName = "";
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	    try {
	        String query = "SELECT UserName, puzzleattempts.userElo FROM puzzleattempts, userdetails WHERE puzzleattempts.userID = userdetails.userID AND puzzleattempts.userID = ?";	        
	        Connection connect = DriverManager.getConnection(url, uName, uPass);
	        PreparedStatement preparedStatement = connect.prepareStatement(query);
	        preparedStatement.setInt(1, LoginSystem.userID);
	        ResultSet results = preparedStatement.executeQuery();
	        
	        int total = 0;
	        int counter = 0;
	        String userElo = "";	        
	        while (results.next()) {
	            if (counter == 0) {
	                userName = results.getString("UserName");
	            }
	            total += results.getInt("userElo");
	            counter++;
	        }
	        if (counter != 0) {
	            userElo = Integer.toString(total/counter);
	        } else {
	            userElo = "0";
	        }
	        if(userName == "") {
	        		userName = LoginSystem.name;
	        }
	        String value = userName + "," + userElo;
			return value;
        }
		
		catch(SQLException e) {
			e.printStackTrace();
		}
		return "";
		
		
	}
	private int getID() {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	    try {
	        String query = "SELECT userID FROM userdetails";	        
	        Connection connect = DriverManager.getConnection(url, uName, uPass);
	        PreparedStatement preparedStatement = connect.prepareStatement(query);
	        ResultSet results = preparedStatement.executeQuery();

	        int counter = 0;      
	        while (results.next()) {
	            counter++;
	        }
			return counter;
        }
		
		catch(SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	private int returnRank() {
		int num = getID();
		int rank = num;
		int[] totalValues = new int[num];
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	    try {
	    	for(int i = 0; i < num;i++) {
    	        String query = "SELECT puzzleattempts.userElo FROM puzzleattempts WHERE puzzleattempts.userID = ?";	        
    	        Connection connect = DriverManager.getConnection(url, uName, uPass);
    	        PreparedStatement preparedStatement = connect.prepareStatement(query);
    	        preparedStatement.setInt(1, i+1);
    	        ResultSet results = preparedStatement.executeQuery();
    	        
    	        int total = 0;
    	        int counter = 0;
    	        while (results.next()) {
    	            total += results.getInt("userElo");
    	            counter ++;
    	        }
    	        if(counter == 0) {
    	        	counter ++;
    	        }
    	        totalValues[i] = total/counter;    		
	    	}
	    	if(totalValues[LoginSystem.userID-1] == 0) {
	    		return rank;
	    	}else {
	    		rank = 0;
	    		for(int i = 0; i < num;i++) {
	    			if(totalValues[i] >= totalValues[LoginSystem.userID-1]) {
	    				rank += 1;
	    			}
	    		}
	    		return rank;
	    	}
        }		
		catch(SQLException e) {
			e.printStackTrace();
		}
		return rank;
	}
	private String checkLength(String name) {
		
		int length = name.length();
		if(length > 8) {
			name = name.substring(0, 8) + "...";
		}
		return name;
	}
	private void createScroll() {
		
		logTextPane = new JTextPane();
		logTextPane.setBackground(Color.BLACK);
		logTextPane.setForeground(Color.WHITE);
		logTextPane.setEditable(false);
		logTextPane.setBorder(BorderFactory.createEmptyBorder());
		
        
		doc = logTextPane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(logTextPane);
        scrollPane.setBounds(1150, 270, 650, 400);
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); 
        add(scrollPane);
		
        appendColoredText("\n", Color.WHITE,30);
        appendColoredText("     Game Started\n", Color.RED,30);
        if(randomNumber == 1) {
	        appendColoredText("     Player 1: White\n", Color.WHITE,30);
	        appendColoredText("     Player 2: Black\n", Color.GRAY,30);
        }else {
	        appendColoredText("     Player 1: Black\n", Color.GRAY,30);
	        appendColoredText("     Player 2: White\n", Color.WHITE,30);
        }
	}
	private String returnCol(String colNum) {
        return ColMap.getColLet(colNum);    
	}
	private void addMove(String result) {
		if(currentColor != WHITE) {
			appendColoredText(result +"\n", Color.WHITE,25);
		}else {
			appendColoredText(result +"\n", Color.GRAY,25);
		}		
	}

    private void appendColoredText(String text, Color color,int fontSize) {
        Style style = logTextPane.addStyle("Style", null);
        StyleConstants.setForeground(style, color);
        StyleConstants.setFontFamily(style, "Palatino Linotype");
        StyleConstants.setBold(style, true);
        StyleConstants.setFontSize(style, fontSize);
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> {
            logTextPane.setCaretPosition(doc.getLength());
        });
    }
    public boolean isActive() {
    	if(active) {
    		return true;
    	}
    	return false;
    }
    private Shape backArrowPath() {
        GeneralPath path = new GeneralPath();

        path.moveTo(1440, 980);
        path.quadTo(1404.5, 974.5, 1379.5, 981.5);
        path.lineTo(1379.5, 969);
        path.lineTo(1363, 994);
        path.lineTo(1392, 1001);
        path.lineTo(1384.5, 992);
        path.quadTo(1411.5, 982.5, 1436.5, 987.5);
        path.quadTo(1442.5, 985.5, 1440, 980);

        path.closePath();
        return path;
    }

    private Shape forwardArrowPath() {
        GeneralPath path = new GeneralPath();

        path.moveTo(1510, 980);
        path.quadTo(1545.5, 974.5, 1570.5, 981.5);
        path.lineTo(1570.5, 969);
        path.lineTo(1586, 994);
        path.lineTo(1557, 1001);
        path.lineTo(1564.5, 992);
        path.quadTo(1541.5, 982.5, 1516.5, 987.5);
        path.quadTo(1510.5, 985.5, 1510, 980);

        path.closePath();
        return path;
    }
    private Ellipse2D.Double circlePath(int x, int y) {    	
    	return new Ellipse2D.Double(x, y, 70, 70);
    }
	public void playSound() {
        String filePath = "src//images//ChessMoveSound.wav"; 

        try {
            File soundFile = new File(filePath);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            clip.start();
            
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.stop();
                    clip.close();
                }
            });
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
	}
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		active = true;
		Graphics2D g2 = (Graphics2D)g;
		
		board.draw(g2);

		for (piece p: simPieces) {
			if(p.color != currentColor) {
				if(p.color == WHITE) {
					if(whiteColour != null) {
						p.drawCpiece(g2, whiteColour);
					}else {
						p.draw(g2);
					}
				}else {
					if(blackColour != null) {
						p.drawCpiece(g2, blackColour);
					}else {
						p.draw(g2);
					}
				} 				
			}
		}
		for (piece p1: simPieces) {
			if(p1.color == currentColor && p1 != activeP) {
				if(p1.color == WHITE) {
					if(whiteColour != null) {
						p1.drawCpiece(g2, whiteColour);
					}else {
						p1.draw(g2);
					}						
				}else {
					if(blackColour != null) {
						p1.drawCpiece(g2, blackColour);
					}else {
						p1.draw(g2);
					}
				} 				
				
			}
		}
		if(activeP != null) {
			if(activeP.color == WHITE) {
				if(whiteColour != null) {
					activeP.drawCpiece(g2, whiteColour);
				}else {
					activeP.draw(g2);
				}						
			}else {
				if(blackColour != null) {
					activeP.drawCpiece(g2, blackColour);
				}else {
					activeP.draw(g2);
				}
			}
		}

		
		
		
		if(activeP != null) {
			if(newP) {
				Boolean storeMoved = false;
				Boolean storeStepped = false;
				for (int col = 0; col < 9; col++) {
	                for (int row = 0; row < 9; row++) {
	                	piece tempP = null;
	                	Boolean tempValid = false;
	            		activeP.col = col;
	            		activeP.row = row;
	            		
	            		if(activeP.canMove(activeP.col, activeP.row)) {
	            			canMove = true;
	            			if(activeP.hittingP != null) {
	            				tempP = activeP.hittingP;
	            				storeMoved = tempP.moved;
	            				storeStepped = tempP.twoStepped;
	            				simPieces.remove(activeP.hittingP);
	            			}
	            			if(isIllegal(activeP) == false && opponentCaptureKing() == false) {
//	            				System.out.println(simPieces);
	            				tempValid = true;
	            			}
	            			if(tempP != null) {
	            				int val = 1;
	            				if(activeP.color == 1) {
	            					val = 0;
	            				}
            					switch(tempP.type) {
            					case PAWN: simPieces.add(new pawn (val,activeP.col, activeP.row,1));System.out.println("returning piece"); break;
            					case KNIGHT: simPieces.add(new knight (val ,activeP.col, activeP.row,1));System.out.println("returning piece");  break;
            					case BISHOP: simPieces.add(new bishop (val,activeP.col, activeP.row,1));System.out.println("returning piece");  break;
            					case ROOK: simPieces.add(new rook (val,activeP.col, activeP.row,1));System.out.println("returning piece");  break;
            					case QUEEN: simPieces.add(new queen (val,activeP.col,activeP.row,1));System.out.println("returning queen");  break;
            					case KING: simPieces.add(new king (val,activeP.col, activeP.row,1));System.out.println("returning piece");  break;
            					default:break;			
            					}
            					for(piece p:simPieces) {
            						if(p != activeP) {
            							if(p.col == activeP.col && p.row == activeP.row) {
            								p.moved = storeMoved;
            								p.twoStepped = storeStepped;
            							}
            						}
            					}
	            			}
	            		}
	            		activeP.resetPosition();
	                	if(tempValid) {
	                        List<Integer> row1 = new ArrayList<>();
	                        row1.add(col);
	                        row1.add(row);
	                        drawPMoves.add(row1);
	                		g2.setColor(Color.white);
	                		int x = (50 + (col * ChessBoard.SQUARE_SIZE));
	                		int y = (50 + (row * ChessBoard.SQUARE_SIZE));
							g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));
							g2.fill(new Ellipse2D.Double(x +ChessBoard.SQUARE_SIZE/4 , y + ChessBoard.SQUARE_SIZE/4, ChessBoard.SQUARE_SIZE/2, ChessBoard.SQUARE_SIZE/2));
							g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));							
	                	}
	                }
				}
				newP = false;
			}else {
            	int lengthOfArr = drawPMoves.size();
            	int j = 0;
            	while(j < lengthOfArr) {
            		int col = drawPMoves.get(j).get(0);
            		int row = drawPMoves.get(j).get(1);
            		g2.setColor(Color.white);
            		int x = (50 + (col * ChessBoard.SQUARE_SIZE));
            		int y = (50 + (row * ChessBoard.SQUARE_SIZE));
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.5f));
					g2.fill(new Ellipse2D.Double(x +ChessBoard.SQUARE_SIZE/4 , y + ChessBoard.SQUARE_SIZE/4, ChessBoard.SQUARE_SIZE/2, ChessBoard.SQUARE_SIZE/2));
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));	
					j += 1;
            	}
			}
			if(canMove) {
				if(isIllegal(activeP) || opponentCaptureKing()) {
					g2.setColor(Color.red);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
					g2.fillRect(50+(activeP.col*ChessBoard.SQUARE_SIZE), 50 +(activeP.row*ChessBoard.SQUARE_SIZE),ChessBoard.SQUARE_SIZE,ChessBoard.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
					if(activeP != null) {
						if(activeP.color == WHITE) {
							if(whiteColour != null) {
								activeP.drawCpiece(g2, whiteColour);
							}else {
								activeP.draw(g2);
							}						
						}else {
							if(blackColour != null) {
								activeP.drawCpiece(g2, blackColour);
							}else {
								activeP.draw(g2);
							}
						}
					}					
				}else {
					g2.setColor(Color.white);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
					g2.fillRect(50+(activeP.col*ChessBoard.SQUARE_SIZE), 50 +(activeP.row*ChessBoard.SQUARE_SIZE),ChessBoard.SQUARE_SIZE,ChessBoard.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
					if(activeP != null) {
						if(activeP.color == WHITE) {
							if(whiteColour != null) {
								activeP.drawCpiece(g2, whiteColour);
							}else {
								activeP.draw(g2);
							}						
						}else {
							if(blackColour != null) {
								activeP.drawCpiece(g2, blackColour);
							}else {
								activeP.draw(g2);
							}
						}
					}					
				}

			}
		}else {
			newP = true;
			drawPMoves.clear();
		}
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(new Font("Georgia", Font.BOLD, 60));
		g2.setColor(Color.white);
		if(currentColor == WHITE && promotion == true) {
			g2.drawString("PROMOTION", 1250, 250);
		}else if(currentColor == WHITE) {
			g2.drawString("WHITE'S TURN", 1250, 250);
		}else if(promotion){
			g2.setColor(Color.gray);
			g2.drawString("PROMOTION", 1300, 250);
		}else {
			g2.setColor(Color.gray);
			g2.drawString("BLACK'S TURN", 1250, 250);
		}
		if(checkingP != null) {
			g2.setColor(Color.red);
			g2.drawString("THE KING IS IN CHECK", 1100, 750);
		}
	
		if(promotion) {
			g2.setColor(Color.white);
			if(currentColor == WHITE) {
				g2.fillRect(promotionP.getX(promotionP.col), promotionP.getY(promotionP.row), ChessBoard.SQUARE_SIZE, 5*ChessBoard.SQUARE_SIZE);
			}else {
				g2.fillRect(promotionP.getX(promotionP.col), promotionP.getY(promotionP.row+1), ChessBoard.SQUARE_SIZE, -5*ChessBoard.SQUARE_SIZE);
			}
		
			for(piece p: promoPieces) {
				if(p.type == Type.X) {
					p.draw(g2);
				}else {
					if(p.color == WHITE) {
						if(whiteColour != null) {
							p.drawCpiece(g2, whiteColour);
						}else {
							p.draw(g2);
						}						
					}else {
						if(blackColour != null) {
							p.drawCpiece(g2, blackColour);
						}else {
							p.draw(g2);
						}
					}
				}													

			}

			for(piece p1:promoPieces) {
				if(p1.col == (mouse.x-50)/ChessBoard.SQUARE_SIZE && p1.row == (mouse.y - 50)/ChessBoard.SQUARE_SIZE) {
					g2.setColor(Color.gray);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
					g2.fillRect(50+(p1.col*ChessBoard.SQUARE_SIZE), 50 +(p1.row*ChessBoard.SQUARE_SIZE),ChessBoard.SQUARE_SIZE,ChessBoard.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
					if(p1.type == Type.X) {
						p1.draw(g2);
					}else {
						if(p1.color == WHITE) {
							if(whiteColour != null) {
								p1.drawCpiece(g2, whiteColour);
							}else {
								p1.draw(g2);
							}						
						}else {
							if(blackColour != null) {
								p1.drawCpiece(g2, blackColour);
							}else {
								p1.draw(g2);
							}
						}
					}
				}
			}
		
		}
		if(gameOver) {
			g2.setColor(Color.WHITE);
			g2.fillRect(290, 410, 480, 240);
			g2.setColor(Color.BLACK);
			g2.setFont(new Font("Palatino Linotype", Font.BOLD, 60));
			g2.drawString("GAME OVER", 340, 500);
			g2.setColor(Color.GRAY);
			g2.fillRect(290, 570, 480, 80);
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(3));
			g2.drawRect(290, 410, 480, 240);
			g2.drawRect(290, 570, 480, 80);
			g2.setStroke(new BasicStroke(1));
			g2.setFont(new Font("Palatino Linotype", Font.BOLD, 40));
			g2.drawString("Return to Menu", 380, 620);
			if(mouse.x >= 290 && mouse.x <= 770 && mouse.y >= 570 && mouse.y <= 650) {
				g2.setColor(Color.white);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
				g2.fillRect(290, 570, 480, 80);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
			}
		}
		if(gameOver == true && stalemate == false) {
			if(draw) {
				g2.setFont(new Font("Georgia", Font.BOLD, 60));
				g2.setColor(Color.GRAY);
				g2.drawString("DRAW", 1360, 800);
			}else {
				checkingP = null;
				String a = "";
				if(forfeit) {
					if(currentColor == WHITE) {
						g2.setColor(Color.BLACK);
						a = "BLACK WON";
					}else {
						g2.setColor(Color.white);
						a = "WHITE WON";
					}
				}else {
					if(currentColor == WHITE) {
						g2.setColor(Color.BLACK);
						a = "BLACK WON";
					}else {
						g2.setColor(Color.white);
						a = "WHITE WON";
					}
				}
				g2.setFont(new Font("Georgia", Font.BOLD, 40));
				g2.drawString(a, 1300, 800);
			}
		}else if(stalemate == true) {
			g2.setFont(new Font("Georgia", Font.BOLD, 40));
			g2.setColor(Color.GRAY);
			g2.drawString("STALEMATE", 1300, 800);
		}

		g2.setColor(Color.WHITE);
		g2.fillRect(1100, 850, 350, 100);
		g2.fillRect(1500, 850, 350, 100);
		g2.setFont(new Font("Georgia", Font.BOLD, 60));
		g2.setColor(Color.BLACK);
		g2.drawString("DRAW", 1165, 920);
		g2.drawString("FORFEIT", 1530, 920);
		
		if(offerDraw) {
			g2.setColor(Color.WHITE);
			g2.fillRect(290, 410, 480, 240);
			g2.setColor(Color.BLACK);
			g2.setFont(new Font("Palatino Linotype", Font.BOLD, 40));
			g2.drawString("Your Opponent Has", 340, 460);
			g2.drawString("Offered a Draw", 380, 500);
			g2.setColor(Color.GRAY);
			g2.fillRect(290, 570, 240, 80);
			g2.fillRect(530, 570, 240, 80);
			g2.setColor(Color.BLACK);
			//Drawing outline for rectangles
			g2.setStroke(new BasicStroke(3));
			g2.drawRect(290, 410, 480, 240);
			g2.drawRect(290, 570, 240, 80);
			g2.drawRect(530, 570, 240, 80);
			g2.setStroke(new BasicStroke(1));
			g2.drawString("Accept", 350, 620);
			g2.drawString("Decline", 580, 620);
			if(mouse.x >= 290 && mouse.x <= 530 && mouse.y >= 570 && mouse.y <= 650) {
				g2.setColor(Color.white);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
				g2.fillRect(290, 570, 240, 80);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
			}
			if(mouse.x >= 530 && mouse.x <= 770 && mouse.y >= 570 && mouse.y <= 650) {
				g2.setColor(Color.white);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
				g2.fillRect(530, 570, 240, 80);
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
			}
		}
		if(mouse.x >= 1100 && mouse.x <= 1450 && mouse.y >= 850 && mouse.y <= 950) {
			g2.setColor(Color.gray);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
			g2.fillRect(1100, 850, 350, 100);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
		}
		if(mouse.x >= 1500 && mouse.x <= 1850 && mouse.y >= 850 && mouse.y <= 950) {
			g2.setColor(Color.gray);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
			g2.fillRect(1500, 850, 350, 100);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
		}
		//draw account details and name in corner
		
		g2.setColor(Color.BLACK);
		g2.fillRect(1300, 20, 600, 150);
		g2.setColor(Color.WHITE);
		g2.drawString(AccountName, 1400,100);
		g2.setFont(new Font("Georgia", Font.BOLD, 20));
		g2.drawString("elo:  " + AccountElo, 1400,130);
		g2.drawImage(WhiteLogo,1770,40,100,100, null);
		
		//draw on user rank
		g2.setColor(Color.WHITE);
		g2.drawString("rank: #" + finalRank, 1400,150);
		//Draw row and Col nums
		
		for(int i = 1; i < 9; i++) {
			collumDraw = Integer.toString(i);
			collumDraw = returnCol(collumDraw);
			g2.drawString( collumDraw,(i*ChessBoard.SQUARE_SIZE)-20,1040);
		}
		for(int i = 1; i < 9; i++) {
			rowDraw = Integer.toString(i);
			g2.drawString( rowDraw,20,1070 - (i*ChessBoard.SQUARE_SIZE));
		}
		if(start) {
			g2.setColor(Color.WHITE);
			g2.fillRect(290, 410, 480, 240);
			g2.setColor(Color.BLACK);
			g2.setFont(new Font("Palatino Linotype", Font.BOLD, 40));
			if(randomNumber == 1) {
				g2.drawString("Player 1 is White", 380, 520);
				g2.drawString("Player 2 is Black", 380, 570);
			}else {
				g2.drawString("Player 1 is Black", 380, 520);
				g2.drawString("Player 2 is White", 380, 570);
			}
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(3));
			g2.drawRect(290, 410, 480, 240);
			g2.setStroke(new BasicStroke(1));
			timer -= 2;
			if (timer == 0) {
				start = false;
			}
		}
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        
        
        backArrow = backArrowPath();
        g2.fill(backArrow);
        g2.draw(backArrow);

        forwardArrow = forwardArrowPath();
        g2.fill(forwardArrow);
        g2.draw(forwardArrow);
        
        if(backArrow.contains(mouse.x, mouse.y)) {
            g2.setColor(Color.GRAY);
            g2.fill(backArrow);
            g2.draw(backArrow);
        }
        if(forwardArrow.contains(mouse.x, mouse.y)) {
            g2.setColor(Color.GRAY);
            g2.fill(forwardArrow);
            g2.draw(forwardArrow);
        }

        
        Ellipse2D.Double circle = circlePath(1070, 30);
        Ellipse2D.Double circle2 = circlePath(1170, 30);

        g2.setColor(Color.WHITE);
        g2.fill(circle);        
        g2.fill(circle2);
		g2.setStroke(new BasicStroke(3));
        g2.setColor(Color.BLACK);
        g2.draw(circle2);
        g2.draw(circle);
        g2.setStroke(new BasicStroke(1));
        
        g2.drawImage(bColourChange,1170,30,70,70, null);
        g2.drawImage(wColourChange,1070,30,70,70, null);
        if(circle.contains(mouse.x,mouse.y)) {
    		g2.setColor(Color.gray);
    		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
    		g2.fill(circle);
    		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
        }
        if(circle2.contains(mouse.x,mouse.y)) {
    		g2.setColor(Color.gray);
    		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,0.7f));
    		g2.fill(circle2);
    		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1.0f));
        }

        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
	}
	public void launchGame() {
		resetGameState();
		gameThread = new Thread(this);
		gameThread.start();
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		double drawInterval = 1000000000/FPS;
		double delta = 0;
		long lastTime = System.nanoTime();
		long currentTime;
		
		while(running) {
			currentTime = System.nanoTime();
			delta += (currentTime - lastTime)/drawInterval;
			lastTime = currentTime;
			if(delta >= 1) {
				update();
				repaint();
				delta--;
			}
		}
	}

}