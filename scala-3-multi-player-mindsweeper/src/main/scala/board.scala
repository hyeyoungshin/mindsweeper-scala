// * Represents tile positions on the boards
//
// ** You give
// x : starting from top left corner (0, 0) grow to the right 
// y : starting from top left corner (0, 0) grow downards
// ** You get
// A tile position on the board
case class Coordinate (val x: Int, val y: Int)


case class Board[Tile] (val xsize: Int, val ysize:Int, val tile_map: Map[Coordinate, Tile]):
  def print_board: Unit = 
    val str_board = Array.fill(this.xsize)(Array.fill(this.ysize)(""))
    this.tile_map.map((pos, tile) => str_board(pos._1)(pos._2) = tile.toString())
    print[String](str_board)


type SolutionBoard = Board[SolutionTile]
type PlayerBoard = Board[PlayerTile]
type MineBoard = Board[Boolean]


trait Tile:
  def toString: String

// add purpose statement
enum SolutionTile extends Tile:
  case Empty
  case Mine
  case Hint (val hint: Int)

  override def toString() = this match {
    case Empty => "[E]"
    case Mine => "[x]"
    case Hint(n) => s"[$n]"
  }


enum PlayerTile extends Tile:
  case Hidden 
  case Revealed (val tile: SolutionTile)

  override def toString() = this match {
    case Hidden => "[ ]"
    case Revealed(t) => t.toString()
  }


def update_board(playerboard: PlayerBoard, pos: Coordinate, solutiontile: SolutionTile): PlayerBoard = 
  Board(
    xsize = playerboard.xsize,
    ysize = playerboard.ysize,
    tile_map = playerboard.tile_map + (pos -> PlayerTile.Revealed(solutiontile))
  )  


// * Reveals the corresponding SolutionTile at Coordinate on PlayerBoard 
//
// ** You give
// solutionboard : board with all tiles revealed (solution tiles)
// playerboard : player's version of board
// pos : tile position on playerboard to reveal (player click)
// * You get
// PlayerBoard with tile at pos revealed
def reveal(solutionboard: SolutionBoard, playerboard: PlayerBoard, pos: Coordinate): PlayerBoard = 
  // assuming playertile at pos is Hidden
  // assuming solution_tile can be Mine which we deal later
  val solution_tile = solutionboard.tile_map(pos)
  val updated_board = update_board(playerboard, pos, solution_tile)
  
  solution_tile match {
    case SolutionTile.Empty => reveal_neighbors(solutionboard, updated_board, pos)
    case SolutionTile.Mine => reveal_all_mines(solutionboard, updated_board)
    case _ => updated_board
  }

// * Reveals all Mines on PlayerBoard 
//
// ** You give
// solutionboard : board with all tiles revealed (solution tiles)
// playerboard : player's version of board
// pos : tile position on playerboard to reveal (player click)
// * You get
// PlayerBoard with all mines revealed
def reveal_all_mines(solutionboard: SolutionBoard, playerboard: PlayerBoard): PlayerBoard =
  val filtered_map = solutionboard.tile_map.filter((pos, tile) => tile == SolutionTile.Mine)
  
  filtered_map.keys.foldLeft(playerboard)((acc, pos) => update_board(acc, pos, SolutionTile.Mine))
  

// Get neighboring tiles of pos. Check what solutontiles corresponds to neighboring tiles
// If SolutionTile.Empty reveal_neighbors with updated playerboard at pos
def reveal_neighbors(solutionboard: SolutionBoard, playerboard: PlayerBoard, pos: Coordinate): PlayerBoard = 
  val neighbors = neighbors_inbounds(solutionboard.xsize, solutionboard.ysize, pos)

  neighbors.foldLeft(playerboard)(
    (acc, pos) =>  {
      if playerboard.tile_map(pos) != PlayerTile.Hidden then
        acc
      else 
        reveal(solutionboard, acc, pos)
    }
  )


def reveal_more(solutionboard: SolutionBoard, playerboard: PlayerBoard, loc: List[Coordinate]): PlayerBoard =
    loc.foldLeft(playerboard)((acc, pos) => reveal(solutionboard, acc, pos))


def neighbors_inbounds(xsize: Int, ysize: Int, pos: Coordinate): List[Coordinate] = 
  val all_neighbors = List((-1, -1), (-1, 0), (-1, 1), (0, -1), (0, 1), (1, -1), (1, 0), (1, 1)).map(
      (i, j) => Coordinate(pos.x + i, pos.y + j))
  
  all_neighbors.filter(pos => pos.x >= 0 && pos.x < xsize && pos.y >= 0 && pos.y < ysize)


def count_neighboring_mines(mineboard: MineBoard, pos: Coordinate): Int = 
  val neighbors = neighbors_inbounds(mineboard.xsize, mineboard.ysize, pos)

  neighbors.foldLeft(0)( (acc, pos) => if mineboard.tile_map(pos) then acc + 1 else acc )


// * Make SolutionTile at pos based on the number of neighboring 
//
// ** You give
// mineboard : mine locations 
// pos : Coordinate on Board
// * You get
// SolutionTile at Coordinate
def get_solutiontile_at(mineboard: MineBoard, pos: Coordinate): SolutionTile = 
  val num_mines = count_neighboring_mines(mineboard, pos)
  if mineboard.tile_map(pos) then
    SolutionTile.Mine

  else if num_mines == 0 then
    SolutionTile.Empty
  else 
    SolutionTile.Hint(num_mines)


// * Helper to generate Keys for populating Board.tile_map
// * For a board of size 3 * 3, it generates (0, 0), (0, 1), (0, 2), (1, 0), (1, 1), (1, 2), (2, 0), (2, 1), (2, 1)
//
// ** You give
// xlen : Board's horizontal size
// ylen : Board's vertical size
// * You get
// A sequence of Keys for Board.tile_map
def generate_coordinate_keys (xlen: Int, ylen: Int) : IndexedSeq[(Int, Int)] = 
  (0 until xlen).flatMap( x => (0 until ylen).map( y => (x, y) ) )


// * Creates SolutionBoard from MineBoard at the start of game
// * Derives Hint from the mine locations in MineBoard
//
// ** You give
// mineboard: mine locations
// ** You get
// SolutionBoard
def create_solutionboard(mineboard: MineBoard): SolutionBoard =
  val range = generate_coordinate_keys(mineboard.xsize, mineboard.ysize) 
    
  Board(
    xsize = mineboard.xsize,
    ysize = mineboard.ysize,
    tile_map = range.map( (x, y) => ( Coordinate(x, y), get_solutiontile_at(mineboard, Coordinate(x, y)) ) ).toMap
  )

// * Creates MineBoard which represents the mine locations from GameInput at the start of game
// * MineBoard is necessary for creating SolutionBoard 
//
// ** You give
// mine_locations : corresponds to GameInput.board from parse_game_input
// ** You get
// MineBoard where true means a mine; false no mine
def create_mineboard(mine_locations: Array[Array[Int]]): MineBoard = 
  val xlen = mine_locations.length
  val ylen = mine_locations.head.length

  val range = generate_coordinate_keys(xlen, ylen)
  
  Board(
    xsize = xlen,
    ysize = ylen,
    tile_map = range.map( (x, y) => ( Coordinate(x, y), mine_locations(x)(y) == 1 )).toMap
  )

// * Creates the initial PlayerBoard at the start of a game
//
// ** You give
// xlen : number of tiles horizontally
// ylen : number of tiles vertically
// ** You get
// PlayerBoard where all tiles are hidden
def create_playerboard(xsize: Int, ysize: Int): PlayerBoard = 
  val xlen = xsize
  val ylen = ysize

  val range = generate_coordinate_keys(xlen, ylen)
  
  Board(
    xsize = xlen,
    ysize = ylen,
    tile_map = range.map( (x, y) => (Coordinate(x, y), PlayerTile.Hidden )).toMap
  )


def simulate(filename: String): Unit = 
  val game_input = parse_game_input(filename)
  val mine_board = create_mineboard(game_input.board)  
  val solution_board = create_solutionboard(mine_board)
  println("solution_board:")
  solution_board.print_board

  val initial_board = create_playerboard(solution_board.xsize, solution_board.ysize)
  println("initial_board:")
  initial_board.print_board

  val loc = convert_input_coordinates(game_input.reveal)
  val current_board = reveal_more(solution_board, initial_board, loc) 
  println("current_board:")
  current_board.print_board
  






@main def hello(): Unit = 
  simulate("src/test/board_tests/4-in.json")
  
def msg = "I was compiled by Scala 3. :)"
