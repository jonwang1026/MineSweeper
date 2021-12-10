// Name:jwang414
// USC NetID:9305021141
// CS 455 PA3
// Fall 2021


/**
  VisibleField class
  This is the data that's being displayed at any one point in the game (i.e., visible field, because it's what the
  user can see about the minefield). Client can call getStatus(row, col) for any square.
  It actually has data about the whole current state of the game, including  
  the underlying mine field (getMineField()).  Other accessors related to game status: numMinesLeft(), isGameOver().
  It also has mutators related to actions the player could do (resetGameDisplay(), cycleGuess(), uncover()),
  and changes the game state accordingly.
  
  It, along with the MineField (accessible in mineField instance variable), forms
  the Model for the game application, whereas GameBoardPanel is the View and Controller, in the MVC design pattern.
  It contains the MineField that it's partially displaying.  That MineField can be accessed (or modified) from 
  outside this class via the getMineField accessor.  
 */
public class VisibleField {
   // ----------------------------------------------------------   
   // The following public constants (plus numbers mentioned in comments below) are the possible states of one
   // location (a "square") in the visible field (all are values that can be returned by public method 
   // getStatus(row, col)).
   
   // The following are the covered states (all negative values):
   public static final int COVERED = -1;   // initial value of all squares
   public static final int MINE_GUESS = -2;
   public static final int QUESTION = -3;
   
   private static final int ADJACENT_MINE = 0;//used for when determining the number of adjacent mines
   // The following are the uncovered states (all non-negative values):
   public static final int MINE = 9;      // this loc is a mine that hasn't been guessed already (end of losing game)
   public static final int INCORRECT_GUESS = 10;  // is displayed a specific way at the end of losing game
   public static final int EXPLODED_MINE = 11;   // the one you uncovered by mistake (that caused you to lose)
   // ----------------------------------------------------------   
  
   // <put instance variables here>

   private int numCols;	//number of columns in displayfield
   private int numRows;	//number of rows in displayfield
   private int [][] displayedField; // state of game seen by user
   private MineField currentMineField; // for underlying minefield used for displayField
   private int minesLeft;
   private int totalSpaces;
   /**
      Create a visible field that has the given underlying mineField.
      The initial state will have all the mines covered up, no mines guessed, and the game
      not over.
      @param mineField  the minefield to use for this VisibleField
    */
   public VisibleField(MineField mineField) {
      numCols = mineField.numCols();
      numRows = mineField.numRows();
      currentMineField = mineField;
      displayedField = new int [numCols][numRows];
      minesLeft = mineField.numMines();
      coverMineField();
   }
   
   /**
      Reset the object to its initial state (see constructor comments), using the same underlying
      MineField. 
   */     
   public void resetGameDisplay() {
      coverMineField();
      this.numRows = getMineField().numRows();
      this.numCols = getMineField().numCols();
      minesLeft = currentMineField.numMines();
   }
  
   
   /**
      Returns a reference to the mineField that this VisibleField "covers"
      @return the minefield
    */
   public MineField getMineField() {
      return currentMineField;
   }
   
   
   /**
      Returns the visible status of the square indicated.
      @param row  row of the square
      @param col  col of the square
      @return the status of the square at location (row, col).  See the public constants at the beginning of the class
      for the possible values that may be returned, and their meanings.
      PRE: getMineField().inRange(row, col)
    */
   public int getStatus(int row, int col) {
      return displayedField[row][col];
   }

   
   /**
      Returns the the number of mines left to guess.  This has nothing to do with whether the mines guessed are correct
      or not.  Just gives the user an indication of how many more mines the user might want to guess.  This value can
      be negative, if they have guessed more than the number of mines in the minefield.     
      @return the number of mines left to guess.
    */
   public int numMinesLeft() {
      int guessedMine = 0;
      for (int x=0; x< currentMineField.numCols(); x++){
         for (int y=0; y<currentMineField.numRows(); y++){
            if (getStatus(x,y) == MINE_GUESS){
               guessedMine++;
            }
         }
      }
      return minesLeft - guessedMine;
   }
 
   
   /**
      Cycles through covered states for a square, updating number of guesses as necessary.  Call on a COVERED square
      changes its status to MINE_GUESS; call on a MINE_GUESS square changes it to QUESTION;  call on a QUESTION square
      changes it to COVERED again; call on an uncovered square has no effect.  
      @param row  row of the square
      @param col  col of the square
      PRE: getMineField().inRange(row, col)
      public static final int COVERED = -1;   // initial value of all squares
      public static final int MINE_GUESS = -2;
      public static final int QUESTION = -3;
    */
   public void cycleGuess(int row, int col) {
      if (displayedField[row][col] < 0){
         displayedField[row][col]--;
      }
      if (displayedField[row][col] == -4){
         displayedField[row][col] = COVERED;
      }
   }

   /**
      Uncovers this square and returns false iff you uncover a mine here.
      If the square wasn't a mine or adjacent to a mine it also uncovers all the squares in 
      the neighboring area that are also not next to any mines, possibly uncovering a large region.
      Any mine-adjacent squares you reach will also be uncovered, and form 
      (possibly along with parts of the edge of the whole field) the boundary of this region.
      Does not uncover, or keep searching through, squares that have the status MINE_GUESS. 
      Note: this action may cause the game to end: either in a win (opened all the non-mine squares)
      or a loss (opened a mine).
      @param row  of the square
      @param col  of the square
      @return false   iff you uncover a mine at (row, col)
      PRE: getMineField().inRange(row, col)
    */
   public boolean uncover(int row, int col){
      if(!currentMineField.inRange(row, col)) {
         return false;
      }
      if(currentMineField.hasMine(row, col)){
         displayedField[row][col] = EXPLODED_MINE;
         return false;
      }
      else{
         revealNextSquare(row,col);
         return true;
      }
   }
   /**
      Returns whether the game is over.
      (Note: This is not a mutator.)
      @return whether game over
    */
   public boolean isGameOver() {
      if (checkLosing()){
         lostGameVisual(); //change the displayed space in a lost game
         return true;//return true because game is finished, and user wins
      }
      if (checkWin()){
         winGameVisual();
         return true;
      }
      return false;//game is not finished, returns false
   }

 
   
   /**
      Returns whether this square has been uncovered.  (i.e., is in any one of the uncovered states, 
      vs. any one of the covered states).
      @param row of the square
      @param col of the square
      @return whether the square is uncovered
      PRE: getMineField().inRange(row, col)
      public static final int MINE = 9;      // this loc is a mine that hasn't been guessed already (end of losing game)
      public static final int INCORRECT_GUESS = 10;  // is displayed a specific way at the end of losing game
      public static final int EXPLODED_MINE = 11;
    */
   public boolean isUncovered(int row, int col) {
      return getStatus(row, col) > COVERED;
   }
   
 
   // <put private methods here>
   //private method used to set all the minefield to COVERED
   private void coverMineField(){
      for (int x = 0; x<displayedField.length; x++) {
         for (int y = 0; y < displayedField.length; y++) {
            displayedField[x][y] = COVERED;
         }
      }
   }
   /*helper method to allow user to check if user has click an exploded mine area*/
   private boolean checkLosing(){
      for(int x = 0; x <numRows; x++){
         for(int y = 0; y < numCols; y++) {
            if(displayedField[x][y] == EXPLODED_MINE){
               return true;
            }
         }
      }
      return false;
   }
   /*helper method to allow user to check if user has won*/
   private boolean checkWin(){
      int totalSpace = numCols*numRows;
      for(int i = 0; i <numRows; i++){
         for(int j = 0; j < numCols; j++){
            
            if(isUncovered(i,j)){
               totalSpace--;
            }			  
         }
      }
      return totalSpace == currentMineField.numMines();
   }
   /*a recursive function that is used to reveal the next space while checking if game is over.
   checks adjacent 
   */
   private void revealNextSquare(int row, int col){
      int numOfAdjacentMines = currentMineField.numAdjacentMines(row, col);
      //end if game is over during the uncovering
      if(isGameOver()){
         return;
      }
      if(!currentMineField.inRange(row,col)|| isUncovered(row,col)|| getStatus(row,col) == QUESTION|| getStatus(row,col) == MINE_GUESS) {

         return;
      }
      /*start of the flood fill process if game is not over or uncovered/marked*/
      else if(numOfAdjacentMines == ADJACENT_MINE){
         displayedField[row][col] = ADJACENT_MINE; //mark the next square as no adjacent mine
         revealNextSquare(row+1,col+1);//shifts right up
         revealNextSquare(row-1,col+1); // shifts down right
         revealNextSquare(row-1,col-1); //shifts left down
         revealNextSquare(row+1,col-1);//shifts left up
         revealNextSquare(row+1,col);// shifts up
         revealNextSquare(row,col-1);//shifts left
         revealNextSquare(row,col+1);//shifts right
         revealNextSquare(row-1,col); //shifts down
         
         return;
      }
      //set location to number of adjacentmine 
      else
      {
         displayedField[row][col] = numOfAdjacentMines;
         return;
      }
   }
   /*used when the user wins and shows all MINE-GUESSES*/
   private void winGameVisual(){
      for(int x = 0; x < numRows; x++) {
         for(int y = 0; y < numCols; y++) {
            if(currentMineField.hasMine(x,y)) {
               displayedField[x][y] = MINE_GUESS;
            }
         }
      }
   }
   /* private method used when game loses. 
   The method also setgs the status of the location to MINE
   changes user's wrong guesses to INCORRECT GUESS*/
   private void lostGameVisual(){
      for (int x = 0; x<numRows; x++){
         for (int y = 0; y<numCols; y++){
             //allows game to set the location to a blacksquare/MINE if they missed this location when finishing the game
            if((displayedField[x][y] == COVERED || displayedField[x][y] == QUESTION) && currentMineField.hasMine(x,y)){
               displayedField[x][y] = MINE; 
            }
            //set as incorrect guess if user did not guess this was a mine previously
            else if((displayedField[x][y] == MINE_GUESS) && (!currentMineField.hasMine(x, y))){
               displayedField[x][y] = INCORRECT_GUESS;
            }
         }
      }
   }
}
